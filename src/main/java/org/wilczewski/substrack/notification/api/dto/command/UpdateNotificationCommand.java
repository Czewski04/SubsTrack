package org.wilczewski.substrack.notification.api.dto.command;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public record UpdateNotificationCommand(
        UUID id,
        UUID userId,
        UUID subscriptionId,
        boolean isActive,
        List<Duration> durations
) {
}
