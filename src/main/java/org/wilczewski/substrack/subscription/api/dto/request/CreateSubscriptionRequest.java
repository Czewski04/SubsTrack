package org.wilczewski.substrack.subscription.api.dto.request;

import jakarta.validation.constraints.*;
import org.wilczewski.substrack.subscription.internal.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotBlank
        @Size(min = 1, max = 100)
        String name,
        @NotNull
        UUID emailId,
        @NotNull
        @Positive
        BigDecimal price,
        @NotNull
        Currency currency,
        @NotNull
        @FutureOrPresent
        LocalDateTime startDate,
        LocalDateTime endDate,
        @Positive
        int period,
        @NotNull
        PeriodType periodType,
        @NotNull
        boolean isActive,
        @NotNull
        boolean includeTrail,
        @PositiveOrZero
        int trailLength
) {}
