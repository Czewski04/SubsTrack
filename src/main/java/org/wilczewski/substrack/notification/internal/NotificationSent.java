package org.wilczewski.substrack.notification.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "notification_sent",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"notification_id", "billing_date", "days_before"}
        )
)
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
class NotificationSent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    @Column(name = "days_before", nullable = false)
    private long daysBefore;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public NotificationSent(UUID notificationId, LocalDate nextBilling, long daysBefore) {
        this.notificationId = notificationId;
        this.billingDate = nextBilling;
        this.daysBefore = daysBefore;
        this.sentAt = LocalDateTime.now();
    }
}
