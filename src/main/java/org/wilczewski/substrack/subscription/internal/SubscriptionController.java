package org.wilczewski.substrack.subscription.internal;

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
import org.wilczewski.substrack.subscription.api.dto.command.CreateSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.command.DeleteSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.command.UpdateSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.query.GetUserSubscriptionQuery;
import org.wilczewski.substrack.subscription.api.dto.request.CreateSubscriptionRequest;
import org.wilczewski.substrack.subscription.api.dto.request.UpdateSubscriptionRequest;
import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Manage user subscriptions")
class SubscriptionController {
    private final SubscriptionService subscriptionService;
    private final SubscriptionMapper subscriptionMapper;

    @PostMapping
    @Operation(summary = "Create a subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subscription created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> createSubscription(@Parameter(hidden = true) @AuthenticationPrincipal UUID userId, @RequestBody @Valid CreateSubscriptionRequest request) {
        CreateSubscriptionCommand command = subscriptionMapper.toCreateSubscriptionCommand(request, userId);
        UUID subscriptionId = subscriptionService.createSubscription(command);
        return ResponseEntity.created(URI.create("/api/v1/subscriptions/" + subscriptionId)).build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subscription not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updateSubscription(@Parameter(hidden = true) @AuthenticationPrincipal UUID userId, @RequestBody @Valid UpdateSubscriptionRequest request, @PathVariable UUID id) {
        UpdateSubscriptionCommand command = subscriptionMapper.toUpdateSubscriptionCommand(request, userId, id);
        subscriptionService.updateSubscription(command);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Subscription deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subscription not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteSubscription(@Parameter(hidden = true) @AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        DeleteSubscriptionCommand command = new DeleteSubscriptionCommand(userId, id);
        subscriptionService.deleteSubscription(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a subscription by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subscription not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubscriptionResponse> getSubscription(@Parameter(hidden = true) @AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        GetUserSubscriptionQuery query = new GetUserSubscriptionQuery(userId, id);
        SubscriptionResponse response = subscriptionService.getSubscriptionById(query);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all subscriptions for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscriptions listed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptions(@Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        List<SubscriptionResponse> responses = subscriptionService.getSubscriptionsByUserId(userId);
        return ResponseEntity.ok(responses);
    }
}
