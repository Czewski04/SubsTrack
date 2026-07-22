package org.wilczewski.substrack.subscription.api.dto.response;

import org.wilczewski.substrack.subscription.internal.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        String name,
        UUID emailId,
        BigDecimal price,
        Currency currency,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int period,
        PeriodType periodType,
        boolean isActive,
        boolean includeTrail,
        int trailLength
) {
}
