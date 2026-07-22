package org.wilczewski.substrack.notification.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Duration;
import java.util.List;

public record CreateNotificationRequest(
        @NotNull
        boolean isActive,
        @NotNull
        List<@PositiveOrZero Duration> durations
) {}
