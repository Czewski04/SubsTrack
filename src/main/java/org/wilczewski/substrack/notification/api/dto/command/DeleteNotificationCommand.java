package org.wilczewski.substrack.notification.api.dto.command;

import java.util.UUID;

public record DeleteNotificationCommand(
        UUID notificationId,
        UUID userId,
        UUID subscriptionId
) {
}
