package com.adconnect.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.json.JsonData;
import com.adconnect.search.document.CampaignDocument;
import com.adconnect.search.document.InfluencerDocument;
import com.adconnect.search.dto.SearchDtos.CampaignQuery;
import com.adconnect.search.dto.SearchDtos.FacetBucket;
import com.adconnect.search.dto.SearchDtos.InfluencerQuery;
import com.adconnect.search.dto.SearchDtos.SearchResult;
import com.adconnect.search.dto.SearchDtos.Suggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final Map<String, String> INFLUENCER_FACETS = Map.of(
            "city", "city.keyword", "niche", "niches", "platform", "platforms");

    private final ElasticsearchOperations operations;

    /**
     * Free text runs as a fuzzy multi_match (scored); structured criteria run as non-scoring filters so
     * they are cacheable. Facet counts come from terms aggregations over the same filtered result set.
     */
    public SearchResult<InfluencerDocument> searchInfluencers(InfluencerQuery query) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        if (hasText(query.q())) {
            bool.must(m -> m.multiMatch(mm -> mm.query(query.q())
                    .fields("displayName^3", "nameSuggest^2", "niches^2", "bio", "city")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")));
        }
        if (hasText(query.city())) {
            bool.filter(f -> f.term(t -> t.field("city.keyword").value(query.city())));
        }
        if (hasText(query.niche())) {
            bool.filter(f -> f.term(t -> t.field("niches").value(query.niche().toLowerCase())));
        }
        if (hasText(query.platform())) {
            bool.filter(f -> f.term(t -> t.field("platforms").value(query.platform().toUpperCase())));
        }
        if (query.minFollowers() != null) {
            bool.filter(f -> f.range(r -> r.field("totalFollowers").gte(JsonData.of(query.minFollowers()))));
        }
        if (query.maxPrice() != null) {
            bool.filter(f -> f.range(r -> r.field("pricePerPost").lte(JsonData.of(query.maxPrice()))));
        }
        if (query.minRating() != null) {
            bool.filter(f -> f.range(r -> r.field("rating").gte(JsonData.of(query.minRating()))));
        }
        if (query.verified() != null) {
            bool.filter(f -> f.term(t -> t.field("verified").value(query.verified())));
        }

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(bool.build())))
                .withPageable(PageRequest.of(query.page(), clamp(query.size())))
                .withTrackTotalHits(true);
        INFLUENCER_FACETS.forEach((name, field) ->
                builder.withAggregation(name, Aggregation.of(a -> a.terms(t -> t.field(field).size(15)))));
        switch (query.sort() == null ? "relevance" : query.sort()) {
            case "followers" -> builder.withSort(s -> s.field(f -> f.field("totalFollowers").order(SortOrder.Desc)));
            case "rating" -> builder.withSort(s -> s.field(f -> f.field("rating").order(SortOrder.Desc)));
            case "price" -> builder.withSort(s -> s.field(f -> f.field("pricePerPost").order(SortOrder.Asc)));
            case "engagement" -> builder.withSort(s -> s.field(f -> f.field("engagementRate").order(SortOrder.Desc)));
            default -> {
                builder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
                builder.withSort(s -> s.field(f -> f.field("totalFollowers").order(SortOrder.Desc)));
            }
        }
        long start = System.currentTimeMillis();
        SearchHits<InfluencerDocument> hits = operations.search(builder.build(), InfluencerDocument.class);
        return new SearchResult<>(hits.getSearchHits().stream().map(SearchHit::getContent).toList(),
                hits.getTotalHits(), query.page(), clamp(query.size()), facets(hits),
                System.currentTimeMillis() - start);
    }

    public SearchResult<CampaignDocument> searchCampaigns(CampaignQuery query) {
        BoolQuery.Builder bool = new BoolQuery.Builder()
                .filter(f -> f.term(t -> t.field("status").value("OPEN")));
        if (hasText(query.q())) {
            bool.must(m -> m.multiMatch(mm -> mm.query(query.q())
                    .fields("title^3", "brandName^2", "description", "niche", "city")
                    .fuzziness("AUTO")));
        }
        if (hasText(query.niche())) {
            bool.filter(f -> f.term(t -> t.field("niche").value(query.niche().toLowerCase())));
        }
        if (hasText(query.platform())) {
            bool.filter(f -> f.term(t -> t.field("platform").value(query.platform().toUpperCase())));
        }
        if (hasText(query.city())) {
            bool.filter(f -> f.term(t -> t.field("city.keyword").value(query.city())));
        }
        if (query.minBudget() != null) {
            bool.filter(f -> f.range(r -> r.field("budget").gte(JsonData.of(query.minBudget()))));
        }
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(bool.build())))
                .withAggregation("niche", Aggregation.of(a -> a.terms(t -> t.field("niche").size(15))))
                .withAggregation("platform", Aggregation.of(a -> a.terms(t -> t.field("platform").size(10))))
                .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("updatedAt").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(query.page(), clamp(query.size())))
                .withTrackTotalHits(true)
                .build();
        long start = System.currentTimeMillis();
        SearchHits<CampaignDocument> hits = operations.search(nativeQuery, CampaignDocument.class);
        return new SearchResult<>(hits.getSearchHits().stream().map(SearchHit::getContent).toList(),
                hits.getTotalHits(), query.page(), clamp(query.size()), facets(hits),
                System.currentTimeMillis() - start);
    }

    /** Prefix autocomplete over influencer names using the search_as_you_type sub-fields. */
    public List<Suggestion> suggest(String prefix) {
        if (!hasText(prefix)) {
            return List.of();
        }
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(mm -> mm.query(prefix)
                        .type(TextQueryType.BoolPrefix)
                        .fields("nameSuggest", "nameSuggest._2gram", "nameSuggest._3gram")))
                .withPageable(PageRequest.of(0, 8))
                .build();
        return operations.search(nativeQuery, InfluencerDocument.class).getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(d -> new Suggestion(d.getUserId(), d.getDisplayName(), d.getCity()))
                .toList();
    }

    private static Map<String, List<FacetBucket>> facets(SearchHits<?> hits) {
        Map<String, List<FacetBucket>> facets = new LinkedHashMap<>();
        if (hits.getAggregations() instanceof ElasticsearchAggregations aggregations) {
            aggregations.aggregationsAsMap().forEach((name, agg) -> {
                var aggregate = agg.aggregation().getAggregate();
                if (aggregate.isSterms()) {
                    List<StringTermsBucket> buckets = aggregate.sterms().buckets().array();
                    facets.put(name, buckets.stream()
                            .map(b -> new FacetBucket(b.key().stringValue(), b.docCount()))
                            .toList());
                }
            });
        }
        return facets;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, 50));
    }
}
