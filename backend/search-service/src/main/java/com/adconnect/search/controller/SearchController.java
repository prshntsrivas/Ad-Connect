package com.adconnect.search.controller;

import com.adconnect.search.document.CampaignDocument;
import com.adconnect.search.document.InfluencerDocument;
import com.adconnect.search.dto.SearchDtos.CampaignQuery;
import com.adconnect.search.dto.SearchDtos.InfluencerQuery;
import com.adconnect.search.dto.SearchDtos.SearchResult;
import com.adconnect.search.dto.SearchDtos.Suggestion;
import com.adconnect.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Search")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService search;

    @Operation(summary = "Discover influencers with full-text, filters and facet counts")
    @GetMapping("/influencers")
    public SearchResult<InfluencerDocument> influencers(@RequestParam(required = false) String q,
                                                        @RequestParam(required = false) String city,
                                                        @RequestParam(required = false) String niche,
                                                        @RequestParam(required = false) String platform,
                                                        @RequestParam(required = false) Long minFollowers,
                                                        @RequestParam(required = false) Double maxPrice,
                                                        @RequestParam(required = false) Double minRating,
                                                        @RequestParam(required = false) Boolean verified,
                                                        @RequestParam(defaultValue = "relevance") String sort,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "12") int size) {
        return search.searchInfluencers(new InfluencerQuery(q, city, niche, platform, minFollowers, maxPrice,
                minRating, verified, sort, page, size));
    }

    @GetMapping("/campaigns")
    public SearchResult<CampaignDocument> campaigns(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) String niche,
                                                    @RequestParam(required = false) String platform,
                                                    @RequestParam(required = false) String city,
                                                    @RequestParam(required = false) Double minBudget,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "12") int size) {
        return search.searchCampaigns(new CampaignQuery(q, niche, platform, city, minBudget, page, size));
    }

    @GetMapping("/suggest")
    public List<Suggestion> suggest(@RequestParam String q) {
        return search.suggest(q);
    }
}
