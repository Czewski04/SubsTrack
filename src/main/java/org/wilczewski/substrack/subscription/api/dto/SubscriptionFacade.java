package org.wilczewski.substrack.subscription.api.dto;

import org.wilczewski.substrack.subscription.api.dto.query.GetUserSubscriptionQuery;
import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;

import java.util.UUID;

public interface SubscriptionFacade {
    boolean subscriptionExist(UUID subscriptionId);
    SubscriptionResponse getSubscriptionById(GetUserSubscriptionQuery query);
    void validateSubscription(UUID userId, UUID subscriptionId);
}
