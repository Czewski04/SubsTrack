package org.wilczewski.substrack.subscription.api.dto.query;

import java.util.UUID;

public record GetUserSubscriptionQuery(
        UUID userId,
        UUID subscriptionId
) {}
