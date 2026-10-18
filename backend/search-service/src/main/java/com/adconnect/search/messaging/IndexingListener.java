package com.adconnect.search.messaging;

import com.adconnect.common.events.CampaignEvent;
import com.adconnect.common.events.InfluencerProfileEvent;
import com.adconnect.common.events.Topics;
import com.adconnect.search.document.CampaignDocument;
import com.adconnect.search.document.InfluencerDocument;
import com.adconnect.search.repository.CampaignSearchRepository;
import com.adconnect.search.repository.InfluencerSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Builds the read-optimised search index from domain events (CQRS read side). Events are keyed by
 * entity id, so per-entity ordering is preserved within a partition and upserts are idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingListener {

    private final InfluencerSearchRepository influencers;
    private final CampaignSearchRepository campaigns;

    @KafkaListener(topics = Topics.INFLUENCER_PROFILE)
    public void onInfluencer(InfluencerProfileEvent e) {
        InfluencerDocument doc = new InfluencerDocument();
        doc.setId(e.userId().toString());
        doc.setUserId(e.userId());
        doc.setDisplayName(e.displayName());
        doc.setNameSuggest(e.displayName());
        doc.setBio(e.bio());
        doc.setCity(e.city());
        doc.setState(e.state());
        doc.setCountry(e.country());
        doc.setNiches(Optional.ofNullable(e.niches()).orElse(List.of()).stream().map(String::toLowerCase).toList());
        doc.setPlatforms(e.platforms());
        doc.setInstagramFollowers(e.instagramFollowers());
        doc.setYoutubeSubscribers(e.youtubeSubscribers());
        doc.setTwitterFollowers(e.twitterFollowers());
        doc.setTotalFollowers(e.totalFollowers());
        doc.setEngagementRate(e.engagementRate());
        doc.setPricePerPost(e.pricePerPost() == null ? 0 : e.pricePerPost().doubleValue());
        doc.setRating(e.rating());
        doc.setCompletedDeals(e.completedDeals());
        doc.setVerified(e.verified());
        doc.setUpdatedAt(e.occurredAt());
        influencers.save(doc);
        log.info("Indexed influencer {}", e.userId());
    }

    @KafkaListener(topics = Topics.CAMPAIGN)
    public void onCampaign(CampaignEvent e) {
        CampaignDocument doc = new CampaignDocument();
        doc.setId(e.campaignId().toString());
        doc.setCampaignId(e.campaignId());
        doc.setBrandUserId(e.brandUserId());
        doc.setBrandName(e.brandName());
        doc.setTitle(e.title());
        doc.setDescription(e.description());
        doc.setNiche(e.niche() == null ? null : e.niche().toLowerCase());
        doc.setPlatform(e.platform());
        doc.setCity(e.city());
        doc.setBudget(e.budget() == null ? 0 : e.budget().doubleValue());
        doc.setStatus(e.status());
        doc.setDeadline(e.deadline());
        doc.setUpdatedAt(e.occurredAt());
        campaigns.save(doc);
        log.info("Indexed campaign {} ({})", e.campaignId(), e.status());
    }
}
