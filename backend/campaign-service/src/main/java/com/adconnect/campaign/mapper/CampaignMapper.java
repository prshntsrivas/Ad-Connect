package com.adconnect.campaign.mapper;

import com.adconnect.campaign.dto.CampaignDtos.CampaignDto;
import com.adconnect.campaign.dto.CampaignDtos.CampaignRequest;
import com.adconnect.campaign.dto.DealDtos.HistoryDto;
import com.adconnect.campaign.dto.DealDtos.ReviewDto;
import com.adconnect.campaign.entity.Campaign;
import com.adconnect.campaign.entity.DealStatusHistory;
import com.adconnect.campaign.entity.Review;
import com.adconnect.common.events.CampaignEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.Instant;
import java.util.UUID;

@Mapper
public interface CampaignMapper {

    @Mapping(target = "acceptingApplications", expression = "java(campaign.acceptsApplications())")
    CampaignDto toDto(Campaign campaign);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "brandUserId", ignore = true)
    @Mapping(target = "brandName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget Campaign campaign, CampaignRequest request);

    HistoryDto toDto(DealStatusHistory history);

    @Mapping(target = "dealId", source = "deal.id")
    ReviewDto toDto(Review review);

    default CampaignEvent toEvent(Campaign c) {
        return new CampaignEvent(UUID.randomUUID().toString(), c.getId(), c.getBrandUserId(), c.getBrandName(),
                c.getTitle(), c.getDescription(), c.getCity(), c.getNiche(), c.getPlatform().name(), c.getBudget(),
                c.getStatus().name(), c.getApplicationDeadline(), Instant.now());
    }
}
