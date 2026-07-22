package org.wilczewski.substrack.notification.api.dto.response;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID subscriptionId,
        boolean isActive,
        List<Duration> durations
) {}
