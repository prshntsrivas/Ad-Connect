package com.adconnect.campaign.repository;

import com.adconnect.campaign.entity.Deal;
import com.adconnect.campaign.entity.DealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DealRepository extends JpaRepository<Deal, Long> {

    @EntityGraph(attributePaths = "campaign")
    @Query("""
            select d from Deal d
            where (d.brandUserId = :userId or d.influencerUserId = :userId)
              and (:status is null or d.status = :status)
            """)
    Page<Deal> findForUser(@Param("userId") Long userId, @Param("status") DealStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "campaign")
    List<Deal> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);

    @EntityGraph(attributePaths = {"campaign", "history"})
    @Query("select d from Deal d where d.id = :id")
    Optional<Deal> findDetailedById(@Param("id") Long id);

    boolean existsByCampaignIdAndInfluencerUserId(Long campaignId, Long influencerUserId);

    long countByCampaignIdAndStatusIn(Long campaignId, Collection<DealStatus> statuses);

    @Query("select d.status as status, count(d) as total from Deal d "
            + "where d.brandUserId = :userId or d.influencerUserId = :userId group by d.status")
    List<StatusCount> countByStatusForUser(@Param("userId") Long userId);

    @Query("select coalesce(sum(d.amount), 0) from Deal d where d.brandUserId = :userId and d.status in :statuses")
    BigDecimal sumForBrand(@Param("userId") Long userId, @Param("statuses") Collection<DealStatus> statuses);

    @Query("select coalesce(sum(d.amount), 0) from Deal d where d.influencerUserId = :userId and d.status in :statuses")
    BigDecimal sumForInfluencer(@Param("userId") Long userId, @Param("statuses") Collection<DealStatus> statuses);

    @Query("select d.status as status, count(d) as total from Deal d group by d.status")
    List<StatusCount> countByStatus();

    @Query("select coalesce(sum(d.amount), 0) from Deal d where d.status in :statuses")
    BigDecimal sumAll(@Param("statuses") Collection<DealStatus> statuses);

    interface StatusCount {
        DealStatus getStatus();

        long getTotal();
    }
}
