package org.wilczewski.substrack.subscription.api.dto.command;

import org.wilczewski.substrack.subscription.internal.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

public record UpdateSubscriptionCommand(
        UUID id,
        UUID userId,
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
) {}
