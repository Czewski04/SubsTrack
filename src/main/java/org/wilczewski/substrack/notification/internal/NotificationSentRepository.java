package org.wilczewski.substrack.notification.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
interface NotificationSentRepository extends JpaRepository<NotificationSent, UUID> {

    void deleteByNotificationId(UUID notificationId);

    void deleteByNotificationIdAndBillingDateAndDaysBefore(
            UUID notificationId,
            LocalDate billingDate,
            long daysBefore
    );
}
