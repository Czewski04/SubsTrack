package org.wilczewski.substrack.subscription.internal;

import org.mapstruct.*;
import org.wilczewski.substrack.subscription.api.dto.command.CreateSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.command.UpdateSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.request.CreateSubscriptionRequest;
import org.wilczewski.substrack.subscription.api.dto.request.UpdateSubscriptionRequest;
import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface SubscriptionMapper {
    SubscriptionResponse toSubscriptionResponse(Subscription subscription);
    Subscription toSubscription(CreateSubscriptionCommand createSubscriptionCommand);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    void updateSubscription(@MappingTarget Subscription subscription, UpdateSubscriptionCommand updateSubscriptionCommand);
    @Mapping(target = "userId", source = "userId")
    CreateSubscriptionCommand toCreateSubscriptionCommand(CreateSubscriptionRequest createSubscriptionRequest, UUID userId);
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "id", source = "id")
    UpdateSubscriptionCommand toUpdateSubscriptionCommand(UpdateSubscriptionRequest updateSubscriptionRequest, UUID userId, UUID id);
}
