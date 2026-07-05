package org.wilczewski.substrack.notification.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "is_active")
    private boolean isActive;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "notification_durations",
            joinColumns = @JoinColumn(name = "notification_id")
    )
    @Column(name = "duration_before")
    List<Duration> durations;
}
