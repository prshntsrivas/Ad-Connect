package com.adconnect.campaign.service;

import com.adconnect.campaign.client.UserDirectoryClient;
import com.adconnect.campaign.dto.CampaignDtos.CampaignDto;
import com.adconnect.campaign.dto.CampaignDtos.CampaignRequest;
import com.adconnect.campaign.entity.BrandProfile;
import com.adconnect.campaign.entity.Campaign;
import com.adconnect.campaign.entity.CampaignStatus;
import com.adconnect.campaign.entity.DealStatus;
import com.adconnect.campaign.entity.Platform;
import com.adconnect.campaign.mapper.CampaignMapper;
import com.adconnect.campaign.repository.BrandProfileRepository;
import com.adconnect.campaign.repository.CampaignRepository;
import com.adconnect.campaign.repository.CampaignSpecifications;
import com.adconnect.campaign.repository.DealRepository;
import com.adconnect.common.events.EventPublisher;
import com.adconnect.common.events.Topics;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.security.Role;
import com.adconnect.common.web.ApiException;
import com.adconnect.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaigns;
    private final BrandProfileRepository brands;
    private final DealRepository deals;
    private final CampaignMapper mapper;
    private final EventPublisher events;
    private final UserDirectoryClient userDirectory;

    @Transactional
    public CampaignDto create(CurrentUser user, CampaignRequest request) {
        user.require(Role.BRAND);
        userDirectory.requireVerified(user.id());
        BrandProfile brand = brands.findById(user.id())
                .orElseThrow(() -> ApiException.badRequest("Complete your brand profile before creating campaigns"));
        Campaign campaign = new Campaign();
        mapper.update(campaign, request);
        campaign.setBrandUserId(user.id());
        campaign.setBrandName(brand.getCompanyName());
        campaign.setStatus(CampaignStatus.OPEN);
        Campaign saved = campaigns.save(campaign);
        events.publish(Topics.CAMPAIGN, saved.getId(), mapper.toEvent(saved));
        return mapper.toDto(saved);
    }

    @Transactional
    @CachePut(cacheNames = "campaigns", key = "#id")
    public CampaignDto update(CurrentUser user, Long id, CampaignRequest request) {
        Campaign campaign = owned(user, id);
        if (campaign.getStatus() != CampaignStatus.OPEN) {
            throw ApiException.conflict("Only open campaigns can be edited");
        }
        mapper.update(campaign, request);
        events.publish(Topics.CAMPAIGN, campaign.getId(), mapper.toEvent(campaign));
        return mapper.toDto(campaign);
    }

    @Transactional
    @CachePut(cacheNames = "campaigns", key = "#id")
    public CampaignDto changeStatus(CurrentUser user, Long id, CampaignStatus status) {
        Campaign campaign = owned(user, id);
        CampaignStatus current = campaign.getStatus();
        boolean allowed = switch (status) {
            case OPEN -> current == CampaignStatus.CLOSED;
            case CLOSED -> current == CampaignStatus.OPEN;
            case CANCELLED -> current == CampaignStatus.OPEN || current == CampaignStatus.CLOSED;
            case COMPLETED -> current != CampaignStatus.CANCELLED;
        };
        if (!allowed) {
            throw ApiException.conflict("Cannot move campaign from " + current + " to " + status);
        }
        if (status == CampaignStatus.CANCELLED && deals.countByCampaignIdAndStatusIn(id,
                EnumSet.of(DealStatus.FUNDED, DealStatus.DELIVERED)) > 0) {
            throw ApiException.conflict("Campaign has funded deals in progress and cannot be cancelled");
        }
        campaign.setStatus(status);
        events.publish(Topics.CAMPAIGN, campaign.getId(), mapper.toEvent(campaign));
        return mapper.toDto(campaign);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "campaigns", key = "#id")
    public CampaignDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<CampaignDto> browse(CampaignStatus status, String niche, Platform platform, String city,
                                            BigDecimal minBudget, String q, int page, int size) {
        Specification<Campaign> spec = Specification.where(CampaignSpecifications.hasStatus(status))
                .and(CampaignSpecifications.hasNiche(niche))
                .and(CampaignSpecifications.onPlatform(platform))
                .and(CampaignSpecifications.inCity(city))
                .and(CampaignSpecifications.minBudget(minBudget))
                .and(CampaignSpecifications.textContains(q));
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(campaigns.findAll(spec, pageable), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public PageResponse<CampaignDto> mine(CurrentUser user, int page, int size) {
        user.require(Role.BRAND);
        return PageResponse.of(campaigns.findByBrandUserId(user.id(),
                PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"))), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public int reindexAll() {
        var all = campaigns.findAll();
        all.forEach(c -> events.publish(Topics.CAMPAIGN, c.getId(), mapper.toEvent(c)));
        return all.size();
    }

    Campaign find(Long id) {
        return campaigns.findById(id).orElseThrow(() -> ApiException.notFound("Campaign not found"));
    }

    Campaign owned(CurrentUser user, Long id) {
        Campaign campaign = find(id);
        if (!campaign.getBrandUserId().equals(user.id())) {
            throw ApiException.forbidden("You do not own this campaign");
        }
        return campaign;
    }
}
