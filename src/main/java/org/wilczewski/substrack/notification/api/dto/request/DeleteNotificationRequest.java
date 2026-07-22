package org.wilczewski.substrack.notification.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeleteNotificationRequest(
        @NotNull
        UUID id
) {}
