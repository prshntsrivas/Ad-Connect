package com.adconnect.search.repository;

import com.adconnect.search.document.InfluencerDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface InfluencerSearchRepository extends ElasticsearchRepository<InfluencerDocument, String> {
}
