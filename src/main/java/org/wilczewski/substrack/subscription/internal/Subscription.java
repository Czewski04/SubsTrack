package org.wilczewski.substrack.subscription.internal;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "email_id", nullable = false)
    private UUID emailId;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "period")
    private int period;

    @Column(name = "period_type")
    private PeriodType periodType;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "include_trail", nullable = false)
    private boolean includeTrail;

    @Column(name = "trail_length")
    private int trailLength;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Subscription(String name, UUID userId, UUID emailId, BigDecimal price, Currency currency, LocalDateTime startDate, LocalDateTime endDate, int period, PeriodType periodType, boolean isActive, boolean includeTrail, int trailLength, UUID notificationId) {
        this.name = name;
        this.userId = userId;
        this.emailId = emailId;
        this.price = price;
        this.currency = currency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.period = period;
        this.periodType = periodType;
        this.isActive = isActive;
        this.includeTrail = includeTrail;
        this.trailLength = trailLength;
    }
}
