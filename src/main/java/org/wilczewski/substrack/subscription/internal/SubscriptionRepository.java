package org.wilczewski.substrack.subscription.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findAllByUserId(UUID userId);
    @Query("select s from Subscription s where s.isActive = true")
    List<Subscription> findAllByIsActiveTrue();
}
