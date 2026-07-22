package org.wilczewski.substrack.subscription.api.dto.command;

import java.util.UUID;

public record DeleteSubscriptionCommand(
        UUID userId,
        UUID subscriptionId
) {}
