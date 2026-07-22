package org.wilczewski.substrack.subscription.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Boolean existsByName(String name);
    Subscription findByName(String name);
    List<Subscription> findAllByUserId(UUID userId);
    List<Subscription> findAllByEmailId(UUID emailId);
}
