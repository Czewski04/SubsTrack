package org.wilczewski.substrack.subscription.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
class SubscriptionController {
    private final SubscriptionService subscriptionService;
    private final SubscriptionMapper subscriptionMapper;

    @PostMapping
    public ResponseEntity<Void> createSubscription(@AuthenticationPrincipal UUID userId, @RequestBody @Valid CreateSubscriptionRequest request) {
        CreateSubscriptionCommand command = subscriptionMapper.toCreateSubscriptionCommand(request, userId);
        UUID subscriptionId = subscriptionService.createSubscription(command);
        return ResponseEntity.created(URI.create("/api/v1/subscriptions/" + subscriptionId)).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSubscription(@AuthenticationPrincipal UUID userId, @RequestBody @Valid UpdateSubscriptionRequest request, @PathVariable UUID id) {
        UpdateSubscriptionCommand command = subscriptionMapper.toUpdateSubscriptionCommand(request, userId, id);
        subscriptionService.updateSubscription(command);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        DeleteSubscriptionCommand command = new DeleteSubscriptionCommand(userId, id);
        subscriptionService.deleteSubscription(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getSubscription(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        GetUserSubscriptionQuery query = new GetUserSubscriptionQuery(userId, id);
        SubscriptionResponse response = subscriptionService.getSubscriptionById(query);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptions(@AuthenticationPrincipal UUID userId) {
        List<SubscriptionResponse> responses = subscriptionService.getSubscriptionsByUserId(userId);
        return ResponseEntity.ok(responses);
    }
}
