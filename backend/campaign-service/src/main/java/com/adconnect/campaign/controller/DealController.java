package com.adconnect.campaign.controller;

import com.adconnect.campaign.dto.DashboardDto;
import com.adconnect.campaign.dto.DealDtos.DealDto;
import com.adconnect.campaign.dto.DealDtos.DeliverRequest;
import com.adconnect.campaign.dto.DealDtos.NoteRequest;
import com.adconnect.campaign.dto.DealDtos.OfferRequest;
import com.adconnect.campaign.dto.DealDtos.ReviewDto;
import com.adconnect.campaign.dto.DealDtos.ReviewRequest;
import com.adconnect.campaign.entity.DealStatus;
import com.adconnect.campaign.service.DashboardService;
import com.adconnect.campaign.service.DealService;
import com.adconnect.common.security.AuthUser;
import com.adconnect.common.security.CurrentUser;
import com.adconnect.common.web.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Deals, reviews and dashboard")
@RestController
@RequiredArgsConstructor
public class DealController {

    private final DealService deals;
    private final DashboardService dashboard;

    @PostMapping("/api/deals/offer")
    @ResponseStatus(HttpStatus.CREATED)
    public DealDto offer(@AuthUser CurrentUser user, @Valid @RequestBody OfferRequest request) {
        return deals.offer(user, request);
    }

    @GetMapping("/api/deals/mine")
    public PageResponse<DealDto> mine(@AuthUser CurrentUser user,
                                      @RequestParam(required = false) DealStatus status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return deals.mine(user, status, page, size);
    }

    @GetMapping("/api/deals/{id}")
    public DealDto get(@AuthUser CurrentUser user, @PathVariable Long id) {
        return deals.get(user, id);
    }

    @PostMapping("/api/deals/{id}/accept")
    public DealDto accept(@AuthUser CurrentUser user, @PathVariable Long id) {
        return deals.accept(user, id);
    }

    @PostMapping("/api/deals/{id}/reject")
    public DealDto reject(@AuthUser CurrentUser user, @PathVariable Long id,
                          @Valid @RequestBody(required = false) NoteRequest request) {
        return deals.reject(user, id, request == null ? null : request.note());
    }

    @PostMapping("/api/deals/{id}/cancel")
    public DealDto cancel(@AuthUser CurrentUser user, @PathVariable Long id,
                          @Valid @RequestBody(required = false) NoteRequest request) {
        return deals.cancel(user, id, request == null ? null : request.note());
    }

    @PostMapping("/api/deals/{id}/deliver")
    public DealDto deliver(@AuthUser CurrentUser user, @PathVariable Long id,
                           @Valid @RequestBody DeliverRequest request) {
        return deals.deliver(user, id, request);
    }

    @PostMapping("/api/deals/{id}/approve")
    public DealDto approve(@AuthUser CurrentUser user, @PathVariable Long id) {
        return deals.approve(user, id);
    }

    @PostMapping("/api/deals/{id}/request-revision")
    public DealDto requestRevision(@AuthUser CurrentUser user, @PathVariable Long id,
                                   @Valid @RequestBody NoteRequest request) {
        return deals.requestRevision(user, id, request.note());
    }

    @PostMapping("/api/deals/{id}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto review(@AuthUser CurrentUser user, @PathVariable Long id,
                            @Valid @RequestBody ReviewRequest request) {
        return deals.review(user, id, request);
    }

    @GetMapping("/api/reviews/influencer/{userId}")
    public PageResponse<ReviewDto> reviews(@PathVariable Long userId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return deals.reviewsFor(userId, page, size);
    }

    @GetMapping("/api/dashboard")
    public DashboardDto dashboard(@AuthUser CurrentUser user) {
        return dashboard.forUser(user);
    }
}
