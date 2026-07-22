package org.wilczewski.substrack.notification.internal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.wilczewski.substrack.notification.api.dto.command.CreateNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.command.UpdateNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.request.CreateNotificationRequest;
import org.wilczewski.substrack.notification.api.dto.request.UpdateNotificationRequest;
import org.wilczewski.substrack.notification.api.dto.response.NotificationResponse;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface NotificationMapper {
    NotificationResponse toNotificationResponse(Notification notification);
    Notification toNotification(CreateNotificationCommand createNotificationCommand);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscriptionId", ignore = true)
    void updateNotification(@MappingTarget Notification notification, UpdateNotificationCommand updateNotificationCommand);
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "subscriptionId", source = "subscriptionId")
    CreateNotificationCommand toCreateNotificationCommand(CreateNotificationRequest createNotificationRequest, UUID userId, UUID subscriptionId);
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "subscriptionId", source = "subscriptionId")
    UpdateNotificationCommand toUpdateNotificationCommand(UpdateNotificationRequest updateNotificationRequest, UUID userId, UUID subscriptionId);
}
