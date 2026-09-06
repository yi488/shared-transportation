package com.example.shared_transportation.repository;

import com.example.shared_transportation.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserId(Long userId);

    @Query("select s from Subscription s where s.userId = :userId and s.quotaUsed < s.quotaTotal order by s.id asc")
    List<Subscription> findActiveByUserId(@Param("userId") Long userId);
}
