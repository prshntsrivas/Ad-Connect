package com.adconnect.search.repository;

import com.adconnect.search.document.CampaignDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CampaignSearchRepository extends ElasticsearchRepository<CampaignDocument, String> {
}
