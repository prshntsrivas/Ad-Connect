package com.adconnect.campaign.mapper;

import com.adconnect.campaign.dto.ProfileDtos.BrandProfileDto;
import com.adconnect.campaign.dto.ProfileDtos.BrandProfileRequest;
import com.adconnect.campaign.dto.ProfileDtos.InfluencerProfileDto;
import com.adconnect.campaign.dto.ProfileDtos.InfluencerProfileRequest;
import com.adconnect.campaign.entity.BrandProfile;
import com.adconnect.campaign.entity.InfluencerProfile;
import com.adconnect.common.events.InfluencerProfileEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Mapper
public interface ProfileMapper {

    BrandProfileDto toDto(BrandProfile profile);

    InfluencerProfileDto toDto(InfluencerProfile profile);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(@MappingTarget BrandProfile profile, BrandProfileRequest request);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "ratingCount", ignore = true)
    @Mapping(target = "completedDeals", ignore = true)
    @Mapping(target = "verified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "country", defaultValue = "India")
    void update(@MappingTarget InfluencerProfile profile, InfluencerProfileRequest request);

    default InfluencerProfileEvent toEvent(InfluencerProfile p) {
        return new InfluencerProfileEvent(UUID.randomUUID().toString(), p.getUserId(), p.getDisplayName(), p.getCity(),
                p.getState(), p.getCountry(), new ArrayList<>(p.getNiches()), p.getPlatforms(),
                p.getInstagramFollowers(), p.getYoutubeSubscribers(), p.getTwitterFollowers(), p.getTotalFollowers(),
                p.getEngagementRate(), p.getPricePerPost(), p.getBio(), p.getRating(), p.getCompletedDeals(),
                p.isVerified(), Instant.now());
    }
}
