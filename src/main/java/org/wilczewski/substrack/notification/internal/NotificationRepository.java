package org.wilczewski.substrack.notification.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Notification findBySubscriptionId(UUID subscriptionId);
    Boolean existsBySubscriptionId(UUID subscriptionId);
}
