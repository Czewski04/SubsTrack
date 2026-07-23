package org.wilczewski.substrack.notification.api.dto.request;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public record UpdateNotificationRequest(
        @NotNull
        UUID id,
        @NotNull
        boolean isActive,
        @NotNull
        List<@NotNull @DurationMin Duration> durations
) {}
