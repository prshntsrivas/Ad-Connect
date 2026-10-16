package com.adconnect.campaign.controller;

import com.adconnect.campaign.dto.ProfileDtos.BrandProfileDto;
import com.adconnect.campaign.dto.ProfileDtos.BrandProfileRequest;
import com.adconnect.campaign.dto.ProfileDtos.InfluencerProfileDto;
import com.adconnect.campaign.dto.ProfileDtos.InfluencerProfileRequest;
import com.adconnect.campaign.dto.ProfileDtos.MyProfileResponse;
import com.adconnect.campaign.service.CampaignService;
import com.adconnect.campaign.service.ProfileService;
import com.adconnect.common.security.AuthUser;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.security.Role;
import com.adconnect.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Profiles")
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profiles;
    private final CampaignService campaigns;

    @GetMapping("/me")
    public MyProfileResponse me(@AuthUser CurrentUser user) {
        return profiles.myProfile(user);
    }

    @PutMapping("/brand")
    public BrandProfileDto upsertBrand(@AuthUser CurrentUser user, @Valid @RequestBody BrandProfileRequest request) {
        return profiles.upsertBrand(user, request);
    }

    @PutMapping("/influencer")
    public InfluencerProfileDto upsertInfluencer(@AuthUser CurrentUser user,
                                                 @Valid @RequestBody InfluencerProfileRequest request) {
        return profiles.upsertInfluencer(user, request);
    }

    @GetMapping("/influencers/{userId}")
    public InfluencerProfileDto influencer(@PathVariable Long userId) {
        return profiles.getInfluencer(userId);
    }

    @GetMapping("/brands/{userId}")
    public BrandProfileDto brand(@PathVariable Long userId) {
        return profiles.getBrand(userId);
    }

    @GetMapping("/influencers")
    public PageResponse<InfluencerProfileDto> influencers(@AuthUser CurrentUser user,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return profiles.listInfluencers(page, size);
    }

    @Operation(summary = "Re-publish all profiles and campaigns to Kafka (rebuilds the search index)")
    @PostMapping("/reindex")
    public Map<String, Integer> reindex(@AuthUser CurrentUser user) {
        user.require(Role.MANAGER, Role.ADMIN);
        return Map.of("influencers", profiles.reindexAll(), "campaigns", campaigns.reindexAll());
    }
}
