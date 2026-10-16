package com.adconnect.campaign.service;

import com.adconnect.campaign.client.UserDirectoryClient;
import com.adconnect.campaign.client.UserInfo;
import com.adconnect.campaign.dto.DealDtos.ApplyRequest;
import com.adconnect.campaign.dto.DealDtos.DealDto;
import com.adconnect.campaign.dto.DealDtos.DealPaymentView;
import com.adconnect.campaign.dto.DealDtos.DeliverRequest;
import com.adconnect.campaign.dto.DealDtos.OfferRequest;
import com.adconnect.campaign.dto.DealDtos.ReviewDto;
import com.adconnect.campaign.dto.DealDtos.ReviewRequest;
import com.adconnect.campaign.entity.Campaign;
import com.adconnect.campaign.entity.CampaignStatus;
import com.adconnect.campaign.entity.Deal;
import com.adconnect.campaign.entity.DealInitiator;
import com.adconnect.campaign.entity.DealStatus;
import com.adconnect.campaign.entity.InfluencerProfile;
import com.adconnect.campaign.entity.Review;
import com.adconnect.campaign.mapper.CampaignMapper;
import com.adconnect.campaign.mapper.ProfileMapper;
import com.adconnect.campaign.repository.DealRepository;
import com.adconnect.campaign.repository.InfluencerProfileRepository;
import com.adconnect.campaign.repository.ReviewRepository;
import com.adconnect.common.events.DealEvent;
import com.adconnect.common.events.EventPublisher;
import com.adconnect.common.events.Topics;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.security.Role;
import com.adconnect.common.web.ApiException;
import com.adconnect.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealService {

    private final DealRepository deals;
    private final ReviewRepository reviews;
    private final InfluencerProfileRepository influencers;
    private final CampaignService campaignService;
    private final UserDirectoryClient userDirectory;
    private final EventPublisher events;
    private final CampaignMapper campaignMapper;
    private final ProfileMapper profileMapper;
    private final CacheManager cacheManager;

    @Transactional
    public DealDto apply(CurrentUser user, Long campaignId, ApplyRequest request) {
        user.require(Role.INFLUENCER);
        userDirectory.requireVerified(user.id());
        InfluencerProfile profile = influencers.findById(user.id())
                .orElseThrow(() -> ApiException.badRequest("Complete your influencer profile before applying"));
        Campaign campaign = campaignService.find(campaignId);
        if (!campaign.acceptsApplications()) {
            throw ApiException.conflict("This campaign is not accepting applications");
        }
        if (profile.getTotalFollowers() < campaign.getMinFollowers()) {
            throw ApiException.badRequest("This campaign requires at least " + campaign.getMinFollowers() + " followers");
        }
        if (deals.existsByCampaignIdAndInfluencerUserId(campaignId, user.id())) {
            throw ApiException.conflict("You have already applied to this campaign");
        }
        Deal deal = newDeal(campaign, user.id(), DealInitiator.INFLUENCER, request.message(), request.amount(),
                DealStatus.APPLIED);
        return created(deal, user.id());
    }

    @Transactional
    public DealDto offer(CurrentUser user, OfferRequest request) {
        user.require(Role.BRAND);
        userDirectory.requireVerified(user.id());
        Campaign campaign = campaignService.owned(user, request.campaignId());
        if (campaign.getStatus() != CampaignStatus.OPEN) {
            throw ApiException.conflict("Offers can only be made on open campaigns");
        }
        if (!influencers.existsById(request.influencerUserId())) {
            throw ApiException.notFound("Influencer profile not found");
        }
        if (deals.existsByCampaignIdAndInfluencerUserId(campaign.getId(), request.influencerUserId())) {
            throw ApiException.conflict("This influencer already has a deal on this campaign");
        }
        Deal deal = newDeal(campaign, request.influencerUserId(), DealInitiator.BRAND, request.message(),
                request.amount(), DealStatus.OFFERED);
        return created(deal, user.id());
    }

    @Transactional
    public DealDto accept(CurrentUser user, Long id) {
        Deal deal = participantDeal(user, id);
        requireCounterparty(user, deal);
        transition(deal, DealStatus.ACCEPTED, user.id(), null);
        return toDto(deal);
    }

    @Transactional
    public DealDto reject(CurrentUser user, Long id, String note) {
        Deal deal = participantDeal(user, id);
        requireCounterparty(user, deal);
        transition(deal, DealStatus.REJECTED, user.id(), note);
        return toDto(deal);
    }

    @Transactional
    public DealDto cancel(CurrentUser user, Long id, String note) {
        Deal deal = participantDeal(user, id);
        boolean pending = deal.getStatus() == DealStatus.APPLIED || deal.getStatus() == DealStatus.OFFERED;
        if (pending && !isInitiator(user, deal)) {
            throw ApiException.forbidden("Only the party who started this deal can withdraw it; reject it instead");
        }
        transition(deal, DealStatus.CANCELLED, user.id(), note);
        return toDto(deal);
    }

    @Transactional
    public DealDto deliver(CurrentUser user, Long id, DeliverRequest request) {
        Deal deal = participantDeal(user, id);
        if (!deal.getInfluencerUserId().equals(user.id())) {
            throw ApiException.forbidden("Only the influencer can submit deliverables");
        }
        deal.setDeliverableUrl(request.deliverableUrl());
        deal.setDeliveryNote(request.note());
        transition(deal, DealStatus.DELIVERED, user.id(), request.note());
        return toDto(deal);
    }

    @Transactional
    public DealDto approve(CurrentUser user, Long id) {
        Deal deal = participantDeal(user, id);
        requireBrand(user, deal);
        transition(deal, DealStatus.COMPLETED, user.id(), "Deliverable approved");
        influencers.findById(deal.getInfluencerUserId()).ifPresent(p -> {
            p.setCompletedDeals(p.getCompletedDeals() + 1);
            publishProfile(p);
        });
        return toDto(deal);
    }

    @Transactional
    public DealDto requestRevision(CurrentUser user, Long id, String note) {
        Deal deal = participantDeal(user, id);
        requireBrand(user, deal);
        if (note == null || note.isBlank()) {
            throw ApiException.badRequest("Describe the revision needed");
        }
        transition(deal, DealStatus.FUNDED, user.id(), "Revision requested: " + note);
        return toDto(deal);
    }

    @Transactional
    public ReviewDto review(CurrentUser user, Long id, ReviewRequest request) {
        Deal deal = participantDeal(user, id);
        if (deal.getStatus() != DealStatus.COMPLETED) {
            throw ApiException.conflict("Reviews are allowed once the deal is completed");
        }
        if (reviews.existsByDealIdAndReviewerUserId(id, user.id())) {
            throw ApiException.conflict("You have already reviewed this deal");
        }
        Long reviewee = deal.getBrandUserId().equals(user.id()) ? deal.getInfluencerUserId() : deal.getBrandUserId();
        Review review = new Review();
        review.setDeal(deal);
        review.setReviewerUserId(user.id());
        review.setRevieweeUserId(reviewee);
        review.setReviewerName(user.is(Role.BRAND) ? deal.getCampaign().getBrandName() : user.name());
        review.setRating(request.rating());
        review.setComment(request.comment());
        reviews.save(review);
        if (reviewee.equals(deal.getInfluencerUserId())) {
            influencers.findById(reviewee).ifPresent(p -> {
                p.addRating(request.rating());
                publishProfile(p);
            });
        }
        return campaignMapper.toDto(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> reviewsFor(Long userId, int page, int size) {
        return PageResponse.of(reviews.findByRevieweeUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(page, Math.min(size, 50))), campaignMapper::toDto);
    }

    @Transactional(readOnly = true)
    public DealDto get(CurrentUser user, Long id) {
        Deal deal = deals.findDetailedById(id).orElseThrow(() -> ApiException.notFound("Deal not found"));
        if (!deal.involves(user.id()) && !user.isStaff()) {
            throw ApiException.forbidden("Not a participant in this deal");
        }
        return toDto(deal);
    }

    @Transactional(readOnly = true)
    public PageResponse<DealDto> mine(CurrentUser user, DealStatus status, int page, int size) {
        var result = deals.findForUser(user.id(), status,
                PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "updatedAt")));
        Map<Long, String> names = influencerNames(result.getContent());
        return PageResponse.of(result, d -> toSummary(d, names));
    }

    @Transactional(readOnly = true)
    public List<DealDto> forCampaign(CurrentUser user, Long campaignId) {
        campaignService.owned(user, campaignId);
        List<Deal> list = deals.findByCampaignIdOrderByCreatedAtDesc(campaignId);
        Map<Long, String> names = influencerNames(list);
        return list.stream().map(d -> toSummary(d, names)).toList();
    }

    @Transactional(readOnly = true)
    public DealPaymentView paymentView(Long id) {
        Deal deal = deals.findDetailedById(id).orElseThrow(() -> ApiException.notFound("Deal not found"));
        return new DealPaymentView(deal.getId(), deal.getCampaign().getId(), deal.getCampaign().getTitle(),
                deal.getBrandUserId(), deal.getInfluencerUserId(), deal.getAmount(), deal.getStatus(),
                deal.getPaymentId());
    }

    /** Called by payment-service once escrow funds are captured. Safe to call more than once. */
    @Transactional
    public DealPaymentView markFunded(Long id, Long paymentId) {
        Deal deal = deals.findDetailedById(id).orElseThrow(() -> ApiException.notFound("Deal not found"));
        if (deal.getStatus() == DealStatus.FUNDED && Objects.equals(deal.getPaymentId(), paymentId)) {
            return paymentView(id);
        }
        deal.setPaymentId(paymentId);
        transition(deal, DealStatus.FUNDED, null, "Payment #" + paymentId + " captured");
        return new DealPaymentView(deal.getId(), deal.getCampaign().getId(), deal.getCampaign().getTitle(),
                deal.getBrandUserId(), deal.getInfluencerUserId(), deal.getAmount(), deal.getStatus(),
                deal.getPaymentId());
    }

    private Deal newDeal(Campaign campaign, Long influencerId, DealInitiator initiator, String message,
                         java.math.BigDecimal amount, DealStatus status) {
        Deal deal = new Deal();
        deal.setCampaign(campaign);
        deal.setBrandUserId(campaign.getBrandUserId());
        deal.setInfluencerUserId(influencerId);
        deal.setInitiatedBy(initiator);
        deal.setMessage(message);
        deal.setAmount(amount);
        deal.setStatus(status);
        return deal;
    }

    private DealDto created(Deal deal, Long actorId) {
        deal.recordTransition(null, deal.getStatus(), actorId, deal.getMessage());
        deals.save(deal);
        publishDeal(deal, null);
        return toDto(deal);
    }

    private void transition(Deal deal, DealStatus target, Long actorId, String note) {
        DealStatus from = deal.getStatus();
        if (!from.canMoveTo(target)) {
            throw ApiException.conflict("Deal cannot move from " + from + " to " + target);
        }
        deal.setStatus(target);
        deal.recordTransition(from, target, actorId, note);
        publishDeal(deal, from);
        log.info("Deal {} {} -> {} by {}", deal.getId(), from, target, actorId);
    }

    private void publishDeal(Deal deal, DealStatus previous) {
        Map<Long, UserInfo> people = userDirectory.getUsers(Set.of(deal.getBrandUserId(), deal.getInfluencerUserId()));
        UserInfo brand = people.get(deal.getBrandUserId());
        UserInfo influencer = people.get(deal.getInfluencerUserId());
        events.publish(Topics.DEAL_STATUS, deal.getId(), new DealEvent(UUID.randomUUID().toString(), deal.getId(),
                deal.getCampaign().getId(), deal.getCampaign().getTitle(), deal.getBrandUserId(),
                brand == null ? null : brand.email(), deal.getCampaign().getBrandName(),
                deal.getInfluencerUserId(), influencer == null ? null : influencer.email(),
                influencer == null ? null : influencer.fullName(), deal.getAmount(),
                previous == null ? null : previous.name(), deal.getStatus().name(), Instant.now()));
    }

    private void publishProfile(InfluencerProfile profile) {
        events.publish(Topics.INFLUENCER_PROFILE, profile.getUserId(), profileMapper.toEvent(profile));
        Optional.ofNullable(cacheManager.getCache("influencers")).ifPresent(c -> c.evict(profile.getUserId()));
    }

    private Deal participantDeal(CurrentUser user, Long id) {
        Deal deal = deals.findDetailedById(id).orElseThrow(() -> ApiException.notFound("Deal not found"));
        if (!deal.involves(user.id())) {
            throw ApiException.forbidden("Not a participant in this deal");
        }
        return deal;
    }

    private static boolean isInitiator(CurrentUser user, Deal deal) {
        return deal.getInitiatedBy() == DealInitiator.BRAND
                ? deal.getBrandUserId().equals(user.id())
                : deal.getInfluencerUserId().equals(user.id());
    }

    private static void requireCounterparty(CurrentUser user, Deal deal) {
        if (isInitiator(user, deal)) {
            throw ApiException.forbidden("Waiting for the other party to respond");
        }
    }

    private static void requireBrand(CurrentUser user, Deal deal) {
        if (!deal.getBrandUserId().equals(user.id())) {
            throw ApiException.forbidden("Only the brand can do this");
        }
    }

    private Map<Long, String> influencerNames(List<Deal> list) {
        Set<Long> ids = list.stream().map(Deal::getInfluencerUserId).collect(Collectors.toCollection(HashSet::new));
        return influencers.findAllById(ids).stream()
                .collect(Collectors.toMap(InfluencerProfile::getUserId, InfluencerProfile::getDisplayName));
    }

    private DealDto toSummary(Deal d, Map<Long, String> names) {
        return new DealDto(d.getId(), d.getCampaign().getId(), d.getCampaign().getTitle(),
                d.getCampaign().getBrandName(), d.getBrandUserId(), d.getInfluencerUserId(),
                names.get(d.getInfluencerUserId()), d.getInitiatedBy(), d.getMessage(), d.getAmount(), d.getStatus(),
                d.getPaymentId(), d.getDeliverableUrl(), d.getDeliveryNote(), List.of(), List.of(), d.getCreatedAt(),
                d.getUpdatedAt());
    }

    private DealDto toDto(Deal d) {
        String name = influencers.findById(d.getInfluencerUserId()).map(InfluencerProfile::getDisplayName).orElse(null);
        List<ReviewDto> dealReviews = d.getId() == null ? List.of()
                : reviews.findByDealId(d.getId()).stream().map(campaignMapper::toDto).toList();
        return new DealDto(d.getId(), d.getCampaign().getId(), d.getCampaign().getTitle(),
                d.getCampaign().getBrandName(), d.getBrandUserId(), d.getInfluencerUserId(), name,
                d.getInitiatedBy(), d.getMessage(), d.getAmount(), d.getStatus(), d.getPaymentId(),
                d.getDeliverableUrl(), d.getDeliveryNote(),
                d.getHistory().stream().map(campaignMapper::toDto).toList(), dealReviews,
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
