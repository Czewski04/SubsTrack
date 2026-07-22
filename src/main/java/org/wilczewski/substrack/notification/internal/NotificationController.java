package org.wilczewski.substrack.notification.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.wilczewski.substrack.notification.api.dto.command.CreateNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.command.DeleteNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.command.UpdateNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.request.CreateNotificationRequest;
import org.wilczewski.substrack.notification.api.dto.request.DeleteNotificationRequest;
import org.wilczewski.substrack.notification.api.dto.request.UpdateNotificationRequest;
import org.wilczewski.substrack.notification.api.dto.response.NotificationResponse;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions/{subscriptionId}/notifications")
@RequiredArgsConstructor
class NotificationController {
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @PostMapping
    public ResponseEntity<Void> createNotification(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID subscriptionId,
            @RequestBody @Valid CreateNotificationRequest request) {
        CreateNotificationCommand command = notificationMapper.toCreateNotificationCommand(request, userId, subscriptionId);
        UUID notificationId = notificationService.createNotification(command);
        return ResponseEntity.created(URI.create("/api/v1/subscriptions/" + subscriptionId + "/notifications/" + notificationId)).build();
    }

    @PutMapping
    public ResponseEntity<Void> updateNotification(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID subscriptionId,
            @RequestBody @Valid UpdateNotificationRequest request) {
        UpdateNotificationCommand command = notificationMapper.toUpdateNotificationCommand(request, userId, subscriptionId);
        notificationService.updateNotification(command);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID subscriptionId,
            @RequestBody @Valid DeleteNotificationRequest request) {
        DeleteNotificationCommand command = new DeleteNotificationCommand(request.id(), userId, subscriptionId);
        notificationService.deleteNotification(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotificationResponse> getNotification(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID subscriptionId) {
        NotificationResponse response = notificationService.getNotificationBySubscriptionId(userId, subscriptionId);
        return ResponseEntity.ok(response);
    }
}
