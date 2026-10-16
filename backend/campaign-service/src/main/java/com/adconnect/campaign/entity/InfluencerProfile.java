package com.adconnect.campaign.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "influencer_profiles")
@Getter
@Setter
@NoArgsConstructor
public class InfluencerProfile {

    @Id
    private Long userId;

    @Column(nullable = false)
    private String displayName;

    private String bio;

    @Column(nullable = false)
    private String city;

    private String state;

    @Column(nullable = false)
    private String country = "India";

    private String photoUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "influencer_niches", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "niche")
    private Set<String> niches = new LinkedHashSet<>();

    private String instagramHandle;
    private long instagramFollowers;
    private String youtubeChannel;
    private long youtubeSubscribers;
    private String twitterHandle;
    private long twitterFollowers;
    private double engagementRate;

    @Column(nullable = false)
    private BigDecimal pricePerPost;

    private double rating;
    private int ratingCount;
    private int completedDeals;
    private boolean verified;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public long getTotalFollowers() {
        return instagramFollowers + youtubeSubscribers + twitterFollowers;
    }

    public List<String> getPlatforms() {
        List<String> platforms = new ArrayList<>();
        if (instagramFollowers > 0) {
            platforms.add("INSTAGRAM");
        }
        if (youtubeSubscribers > 0) {
            platforms.add("YOUTUBE");
        }
        if (twitterFollowers > 0) {
            platforms.add("TWITTER");
        }
        return platforms;
    }

    public void addRating(int stars) {
        rating = ((rating * ratingCount) + stars) / (ratingCount + 1);
        ratingCount++;
    }
}
