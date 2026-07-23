package org.wilczewski.substrack.notification.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.wilczewski.substrack.common.exception.ErrorResponse;
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
@Tag(name = "Notifications", description = "Manage subscription payment reminders")
class NotificationController {
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @PostMapping
    @Operation(summary = "Create a notification for a subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subscription not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> createNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @PathVariable UUID subscriptionId,
            @RequestBody @Valid CreateNotificationRequest request) {
        CreateNotificationCommand command = notificationMapper.toCreateNotificationCommand(request, userId, subscriptionId);
        UUID notificationId = notificationService.createNotification(command);
        return ResponseEntity.created(URI.create("/api/v1/subscriptions/" + subscriptionId + "/notifications/" + notificationId)).build();
    }

    @PutMapping
    @Operation(summary = "Update a notification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updateNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @PathVariable UUID subscriptionId,
            @RequestBody @Valid UpdateNotificationRequest request) {
        UpdateNotificationCommand command = notificationMapper.toUpdateNotificationCommand(request, userId, subscriptionId);
        notificationService.updateNotification(command);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(summary = "Delete a notification")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notification deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @PathVariable UUID subscriptionId,
            @RequestBody @Valid DeleteNotificationRequest request) {
        DeleteNotificationCommand command = new DeleteNotificationCommand(request.id(), userId, subscriptionId);
        notificationService.deleteNotification(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get the notification for a subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<NotificationResponse> getNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @PathVariable UUID subscriptionId) {
        NotificationResponse response = notificationService.getNotificationBySubscriptionId(userId, subscriptionId);
        return ResponseEntity.ok(response);
    }
}
