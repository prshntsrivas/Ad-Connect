package com.adconnect.campaign.controller;

import com.adconnect.campaign.dto.CampaignDtos.CampaignDto;
import com.adconnect.campaign.dto.CampaignDtos.CampaignRequest;
import com.adconnect.campaign.dto.CampaignDtos.CampaignStatusRequest;
import com.adconnect.campaign.dto.DealDtos.ApplyRequest;
import com.adconnect.campaign.dto.DealDtos.DealDto;
import com.adconnect.campaign.entity.CampaignStatus;
import com.adconnect.campaign.entity.Platform;
import com.adconnect.campaign.service.CampaignService;
import com.adconnect.campaign.service.DealService;
import com.adconnect.common.security.AuthUser;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.web.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Campaigns")
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaigns;
    private final DealService deals;

    @GetMapping
    public PageResponse<CampaignDto> browse(@RequestParam(defaultValue = "OPEN") CampaignStatus status,
                                            @RequestParam(required = false) String niche,
                                            @RequestParam(required = false) Platform platform,
                                            @RequestParam(required = false) String city,
                                            @RequestParam(required = false) BigDecimal minBudget,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "12") int size) {
        return campaigns.browse(status, niche, platform, city, minBudget, q, page, size);
    }

    @GetMapping("/mine")
    public PageResponse<CampaignDto> mine(@AuthUser CurrentUser user,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return campaigns.mine(user, page, size);
    }

    @GetMapping("/{id}")
    public CampaignDto get(@PathVariable Long id) {
        return campaigns.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignDto create(@AuthUser CurrentUser user, @Valid @RequestBody CampaignRequest request) {
        return campaigns.create(user, request);
    }

    @PutMapping("/{id}")
    public CampaignDto update(@AuthUser CurrentUser user, @PathVariable Long id,
                              @Valid @RequestBody CampaignRequest request) {
        return campaigns.update(user, id, request);
    }

    @PatchMapping("/{id}/status")
    public CampaignDto changeStatus(@AuthUser CurrentUser user, @PathVariable Long id,
                                    @Valid @RequestBody CampaignStatusRequest request) {
        return campaigns.changeStatus(user, id, request.status());
    }

    @PostMapping("/{id}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public DealDto apply(@AuthUser CurrentUser user, @PathVariable Long id, @Valid @RequestBody ApplyRequest request) {
        return deals.apply(user, id, request);
    }

    @GetMapping("/{id}/deals")
    public List<DealDto> deals(@AuthUser CurrentUser user, @PathVariable Long id) {
        return deals.forCampaign(user, id);
    }
}
