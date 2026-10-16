package com.adconnect.campaign.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "brand_profiles")
@Getter
@Setter
@NoArgsConstructor
public class BrandProfile {

    /** Same id as the account in auth-service. */
    @Id
    private Long userId;

    @Column(nullable = false)
    private String companyName;

    private String industry;
    private String website;
    private String city;
    private String state;
    private String description;
    private String logoUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
