package org.wilczewski.substrack.notification.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Repository
interface NotificationSentRepository extends JpaRepository<NotificationSent, UUID> {

    void deleteByNotificationId(UUID notificationId);

    @Modifying
    @Transactional
    @Query("""
            delete from NotificationSent sent
            where sent.notificationId = :notificationId
              and sent.billingDate = :billingDate
              and sent.daysBefore = :daysBefore
            """)
    void deleteByNotificationIdAndBillingDateAndDaysBefore(
            @Param("notificationId") UUID notificationId,
            @Param("billingDate") LocalDate billingDate,
            @Param("daysBefore") long daysBefore
    );
}
