package com.adconnect.campaign.repository;

import com.adconnect.campaign.entity.Campaign;
import com.adconnect.campaign.entity.CampaignStatus;
import com.adconnect.campaign.entity.Platform;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class CampaignSpecifications {

    private CampaignSpecifications() {
    }

    public static Specification<Campaign> hasStatus(CampaignStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Campaign> hasNiche(String niche) {
        return (root, query, cb) -> niche == null || niche.isBlank() ? null
                : cb.equal(cb.lower(root.get("niche")), niche.toLowerCase());
    }

    public static Specification<Campaign> onPlatform(Platform platform) {
        return (root, query, cb) -> platform == null ? null : cb.equal(root.get("platform"), platform);
    }

    public static Specification<Campaign> inCity(String city) {
        return (root, query, cb) -> city == null || city.isBlank() ? null
                : cb.equal(cb.lower(root.get("city")), city.toLowerCase());
    }

    public static Specification<Campaign> minBudget(BigDecimal min) {
        return (root, query, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("budget"), min);
    }

    public static Specification<Campaign> textContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return null;
            }
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(root.get("brandName")), like));
        };
    }
}
