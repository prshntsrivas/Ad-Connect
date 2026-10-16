package com.adconnect.campaign.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deals")
@Getter
@Setter
@NoArgsConstructor
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(nullable = false)
    private Long brandUserId;

    @Column(nullable = false)
    private Long influencerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DealInitiator initiatedBy;

    private String message;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DealStatus status;

    private Long paymentId;
    private String deliverableUrl;
    private String deliveryNote;

    @OneToMany(mappedBy = "deal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<DealStatusHistory> history = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public boolean involves(Long userId) {
        return brandUserId.equals(userId) || influencerUserId.equals(userId);
    }

    public void recordTransition(DealStatus from, DealStatus to, Long actorId, String note) {
        DealStatusHistory entry = new DealStatusHistory();
        entry.setDeal(this);
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setChangedBy(actorId);
        entry.setNote(note);
        entry.setCreatedAt(Instant.now());
        history.add(entry);
    }
}
