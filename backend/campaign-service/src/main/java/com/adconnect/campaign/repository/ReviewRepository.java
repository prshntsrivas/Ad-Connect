package com.adconnect.campaign.repository;

import com.adconnect.campaign.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRevieweeUserIdOrderByCreatedAtDesc(Long revieweeUserId, Pageable pageable);

    boolean existsByDealIdAndReviewerUserId(Long dealId, Long reviewerUserId);

    List<Review> findByDealId(Long dealId);
}
