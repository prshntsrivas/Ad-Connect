package com.adconnect.campaign.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long brandUserId;

    @Column(nullable = false)
    private String brandName;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    private String deliverables;

    @Column(nullable = false)
    private String niche;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    private String city;

    @Column(nullable = false)
    private BigDecimal budget;

    private long minFollowers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.OPEN;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate applicationDeadline;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public boolean acceptsApplications() {
        return status == CampaignStatus.OPEN
                && (applicationDeadline == null || !applicationDeadline.isBefore(LocalDate.now()));
    }
}
