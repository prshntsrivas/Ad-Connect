package com.adconnect.campaign.repository;

import com.adconnect.campaign.entity.Campaign;
import com.adconnect.campaign.entity.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long>, JpaSpecificationExecutor<Campaign> {

    Page<Campaign> findByBrandUserId(Long brandUserId, Pageable pageable);

    long countByStatus(CampaignStatus status);

    @Query("select c.status as status, count(c) as total from Campaign c where c.brandUserId = :brandId group by c.status")
    List<StatusCount> countByStatusForBrand(@Param("brandId") Long brandId);

    interface StatusCount {
        CampaignStatus getStatus();

        long getTotal();
    }
}
