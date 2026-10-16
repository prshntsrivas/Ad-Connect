package com.adconnect.campaign.service;

import com.adconnect.campaign.dto.ProfileDtos.BrandProfileDto;
import com.adconnect.campaign.dto.ProfileDtos.BrandProfileRequest;
import com.adconnect.campaign.dto.ProfileDtos.InfluencerProfileDto;
import com.adconnect.campaign.dto.ProfileDtos.InfluencerProfileRequest;
import com.adconnect.campaign.dto.ProfileDtos.MyProfileResponse;
import com.adconnect.campaign.client.UserDirectoryClient;
import com.adconnect.campaign.entity.BrandProfile;
import com.adconnect.campaign.entity.InfluencerProfile;
import com.adconnect.campaign.mapper.ProfileMapper;
import com.adconnect.campaign.repository.BrandProfileRepository;
import com.adconnect.campaign.repository.InfluencerProfileRepository;
import com.adconnect.common.events.EventPublisher;
import com.adconnect.common.events.Topics;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.security.Role;
import com.adconnect.common.web.ApiException;
import com.adconnect.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final BrandProfileRepository brands;
    private final InfluencerProfileRepository influencers;
    private final ProfileMapper mapper;
    private final EventPublisher events;
    private final UserDirectoryClient userDirectory;

    @Transactional(readOnly = true)
    public MyProfileResponse myProfile(CurrentUser user) {
        return switch (user.role()) {
            case BRAND -> new MyProfileResponse(user.role().name(),
                    brands.findById(user.id()).map(mapper::toDto).orElse(null), null);
            case INFLUENCER -> new MyProfileResponse(user.role().name(), null,
                    influencers.findById(user.id()).map(mapper::toDto).orElse(null));
            default -> new MyProfileResponse(user.role().name(), null, null);
        };
    }

    @Transactional
    @CachePut(cacheNames = "brands", key = "#user.id()")
    public BrandProfileDto upsertBrand(CurrentUser user, BrandProfileRequest request) {
        user.require(Role.BRAND);
        BrandProfile profile = brands.findById(user.id()).orElseGet(() -> {
            BrandProfile created = new BrandProfile();
            created.setUserId(user.id());
            return created;
        });
        mapper.update(profile, request);
        return mapper.toDto(brands.save(profile));
    }

    @Transactional
    @CachePut(cacheNames = "influencers", key = "#user.id()")
    public InfluencerProfileDto upsertInfluencer(CurrentUser user, InfluencerProfileRequest request) {
        user.require(Role.INFLUENCER);
        InfluencerProfile profile = influencers.findById(user.id()).orElseGet(() -> {
            InfluencerProfile created = new InfluencerProfile();
            created.setUserId(user.id());
            created.setVerified(userDirectory.getUser(user.id()).isVerified());
            return created;
        });
        mapper.update(profile, request);
        InfluencerProfile saved = influencers.save(profile);
        events.publish(Topics.INFLUENCER_PROFILE, saved.getUserId(), mapper.toEvent(saved));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "influencers", key = "#userId")
    public InfluencerProfileDto getInfluencer(Long userId) {
        return influencers.findById(userId).map(mapper::toDto)
                .orElseThrow(() -> ApiException.notFound("Influencer profile not found"));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "brands", key = "#userId")
    public BrandProfileDto getBrand(Long userId) {
        return brands.findById(userId).map(mapper::toDto)
                .orElseThrow(() -> ApiException.notFound("Brand profile not found"));
    }

    @Transactional(readOnly = true)
    public PageResponse<InfluencerProfileDto> listInfluencers(int page, int size) {
        return PageResponse.of(influencers.findAll(PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "rating"))), mapper::toDto);
    }

    /** Keeps the verified badge in sync with the account status owned by auth-service. */
    @Transactional
    @CacheEvict(cacheNames = "influencers", key = "#userId")
    public void syncVerification(Long userId, boolean verified) {
        influencers.findById(userId).ifPresent(profile -> {
            if (profile.isVerified() != verified) {
                profile.setVerified(verified);
                events.publish(Topics.INFLUENCER_PROFILE, userId, mapper.toEvent(profile));
            }
        });
    }

    /** Re-publishes every influencer profile, e.g. to rebuild the search index. */
    @Transactional(readOnly = true)
    public int reindexAll() {
        var all = influencers.findAll();
        all.forEach(p -> events.publish(Topics.INFLUENCER_PROFILE, p.getUserId(), mapper.toEvent(p)));
        return all.size();
    }
}
