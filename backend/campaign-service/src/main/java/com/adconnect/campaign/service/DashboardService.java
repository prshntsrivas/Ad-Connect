package com.adconnect.campaign.service;

import com.adconnect.campaign.dto.DashboardDto;
import com.adconnect.campaign.entity.CampaignStatus;
import com.adconnect.campaign.entity.DealStatus;
import com.adconnect.campaign.repository.BrandProfileRepository;
import com.adconnect.campaign.repository.CampaignRepository;
import com.adconnect.campaign.repository.DealRepository;
import com.adconnect.campaign.repository.InfluencerProfileRepository;
import com.adconnect.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final EnumSet<DealStatus> COMMITTED = EnumSet.of(DealStatus.FUNDED, DealStatus.DELIVERED,
            DealStatus.COMPLETED);
    private static final EnumSet<DealStatus> IN_ESCROW = EnumSet.of(DealStatus.FUNDED, DealStatus.DELIVERED);

    private final CampaignRepository campaigns;
    private final DealRepository deals;
    private final BrandProfileRepository brands;
    private final InfluencerProfileRepository influencers;

    @Transactional(readOnly = true)
    public DashboardDto forUser(CurrentUser user) {
        return switch (user.role()) {
            case BRAND -> {
                Map<String, Long> byCampaign = new LinkedHashMap<>();
                campaigns.countByStatusForBrand(user.id()).forEach(r -> byCampaign.put(r.getStatus().name(), r.getTotal()));
                yield new DashboardDto("BRAND", byCampaign, dealCounts(user.id()),
                        deals.sumForBrand(user.id(), COMMITTED), deals.sumForBrand(user.id(), IN_ESCROW),
                        null, null, brands.existsById(user.id()));
            }
            case INFLUENCER -> {
                var profile = influencers.findById(user.id());
                yield new DashboardDto("INFLUENCER", Map.of(), dealCounts(user.id()),
                        deals.sumForInfluencer(user.id(), EnumSet.of(DealStatus.COMPLETED)),
                        deals.sumForInfluencer(user.id(), IN_ESCROW),
                        profile.map(p -> p.getRating()).orElse(null),
                        profile.map(p -> p.getCompletedDeals()).orElse(null), profile.isPresent());
            }
            case MANAGER, ADMIN -> {
                Map<String, Long> byCampaign = new LinkedHashMap<>();
                for (CampaignStatus s : CampaignStatus.values()) {
                    byCampaign.put(s.name(), campaigns.countByStatus(s));
                }
                Map<String, Long> byDeal = new LinkedHashMap<>();
                deals.countByStatus().forEach(r -> byDeal.put(r.getStatus().name(), r.getTotal()));
                yield new DashboardDto(user.role().name(), byCampaign, byDeal, deals.sumAll(COMMITTED),
                        deals.sumAll(IN_ESCROW), null, null, true);
            }
        };
    }

    private Map<String, Long> dealCounts(Long userId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        deals.countByStatusForUser(userId).forEach(r -> counts.put(r.getStatus().name(), r.getTotal()));
        return counts;
    }
}
