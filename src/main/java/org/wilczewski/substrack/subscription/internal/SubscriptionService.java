package org.wilczewski.substrack.subscription.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wilczewski.substrack.subscription.api.dto.SubscriptionFacade;
import org.wilczewski.substrack.subscription.api.dto.command.CreateSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.command.DeleteSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.command.UpdateSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.query.GetUserSubscriptionQuery;
import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;
import org.wilczewski.substrack.user.api.UserFacade;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class SubscriptionService implements SubscriptionFacade {
    private final UserFacade userFacade;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Transactional
    public UUID createSubscription(CreateSubscriptionCommand createSubscriptionCommand){
        if(!userFacade.userByIdExists(createSubscriptionCommand.userId())){
            throw new IllegalArgumentException("User not found");
        }
        if(createSubscriptionCommand.endDate() != null && createSubscriptionCommand.startDate().isAfter(createSubscriptionCommand.endDate())){
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        if(!userFacade.userContainsEmail(createSubscriptionCommand.userId(), createSubscriptionCommand.emailId())){
            throw new IllegalArgumentException("User does not contain email");
        }
        Subscription subscription = subscriptionMapper.toSubscription(createSubscriptionCommand);
        subscription = subscriptionRepository.save(subscription);
        return subscription.getId();
    }

    @Transactional
    public void updateSubscription(UpdateSubscriptionCommand updateSubscriptionCommand){
        Subscription subscription = validateSubscriptionOwnership(updateSubscriptionCommand.userId(), updateSubscriptionCommand.id());
        if(updateSubscriptionCommand.endDate() != null && updateSubscriptionCommand.startDate().isAfter(updateSubscriptionCommand.endDate())){
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        if(!userFacade.userContainsEmail(updateSubscriptionCommand.userId(), updateSubscriptionCommand.emailId())){
            throw new IllegalArgumentException("User does not contain email");
        }
        subscriptionMapper.updateSubscription(subscription, updateSubscriptionCommand);
    }

    @Transactional
    public void deleteSubscription(DeleteSubscriptionCommand deleteSubscriptionCommand){
        Subscription subscription = validateSubscriptionOwnership(deleteSubscriptionCommand.userId(), deleteSubscriptionCommand.subscriptionId());
        subscriptionRepository.deleteById(deleteSubscriptionCommand.subscriptionId());
    }

    @Transactional(readOnly = true)
    @Override
    public SubscriptionResponse getSubscriptionById(GetUserSubscriptionQuery getUserSubscriptionQuery){
        Subscription subscription = validateSubscriptionOwnership(getUserSubscriptionQuery.userId(), getUserSubscriptionQuery.subscriptionId());
        return subscriptionMapper.toSubscriptionResponse(subscription);
    }

    private Subscription validateSubscriptionOwnership(UUID userId, UUID subscriptionId) {
        if(!userFacade.userByIdExists(userId)){
            throw new IllegalArgumentException("User not found");
        }
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Subscription does not belong to user");
        }
        return subscription;
    }

    @Override
    public void validateSubscription(UUID userId, UUID subscriptionId) {
        if(!userFacade.userByIdExists(userId)){
            throw new IllegalArgumentException("User not found");
        }
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Subscription does not belong to user");
        }
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionsByUserId(UUID userId){
        if(!userFacade.userByIdExists(userId)){
            throw new IllegalArgumentException("User not found");
        }
        List<Subscription> subscriptions = subscriptionRepository.findAllByUserId(userId);
        return subscriptions.stream()
                .map(subscriptionMapper::toSubscriptionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean subscriptionExist(UUID subscriptionId) {
        return subscriptionRepository.existsById(subscriptionId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SubscriptionResponse> getAllActiveSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAllByIsActiveTrue();
        return subscriptions.stream()
                .map(subscriptionMapper::toSubscriptionResponse)
                .toList();
    }
}
