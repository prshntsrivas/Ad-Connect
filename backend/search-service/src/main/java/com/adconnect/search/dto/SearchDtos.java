package com.adconnect.search.dto;

import java.util.List;
import java.util.Map;

public final class SearchDtos {

    private SearchDtos() {
    }

    public record InfluencerQuery(String q, String city, String niche, String platform, Long minFollowers,
                                  Double maxPrice, Double minRating, Boolean verified, String sort,
                                  int page, int size) {
    }

    public record CampaignQuery(String q, String niche, String platform, String city, Double minBudget,
                                int page, int size) {
    }

    public record FacetBucket(String value, long count) {
    }

    public record SearchResult<T>(List<T> content, long totalElements, int page, int size,
                                  Map<String, List<FacetBucket>> facets, long tookMs) {
    }

    public record Suggestion(Long userId, String displayName, String city) {
    }
}
