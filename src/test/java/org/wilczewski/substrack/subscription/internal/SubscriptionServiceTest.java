package org.wilczewski.substrack.subscription.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wilczewski.substrack.common.exception.BadRequestException;
import org.wilczewski.substrack.common.exception.ForbiddenException;
import org.wilczewski.substrack.common.exception.ResourceNotFoundException;
import org.wilczewski.substrack.subscription.api.dto.command.CreateSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.command.DeleteSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.command.UpdateSubscriptionCommand;
import org.wilczewski.substrack.subscription.api.dto.query.GetUserSubscriptionQuery;
import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;
import org.wilczewski.substrack.user.api.UserFacade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final LocalDateTime START_DATE = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final LocalDateTime END_DATE = START_DATE.plusMonths(1);

    @Mock
    private UserFacade userFacade;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionMapper subscriptionMapper;
    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void createSubscriptionValidatesMapsSavesAndReturnsId() {
        UUID userId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        CreateSubscriptionCommand command = createCommand(userId, emailId, START_DATE, END_DATE);
        Subscription mapped = subscription(UUID.randomUUID(), userId, emailId);
        Subscription saved = subscription(UUID.randomUUID(), userId, emailId);
        when(userFacade.userByIdExists(userId)).thenReturn(true);
        when(userFacade.userContainsEmail(userId, emailId)).thenReturn(true);
        when(subscriptionMapper.toSubscription(command)).thenReturn(mapped);
        when(subscriptionRepository.save(mapped)).thenReturn(saved);

        UUID result = subscriptionService.createSubscription(command);

        assertEquals(saved.getId(), result);
        verify(subscriptionMapper).toSubscription(command);
        verify(subscriptionRepository).save(mapped);
    }

    @Test
    void createSubscriptionAllowsNullEndDate() {
        UUID userId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        CreateSubscriptionCommand command = createCommand(userId, emailId, START_DATE, null);
        Subscription subscription = subscription(UUID.randomUUID(), userId, emailId);
        when(userFacade.userByIdExists(userId)).thenReturn(true);
        when(userFacade.userContainsEmail(userId, emailId)).thenReturn(true);
        when(subscriptionMapper.toSubscription(command)).thenReturn(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);

        assertEquals(subscription.getId(), subscriptionService.createSubscription(command));
    }

    @Test
    void createSubscriptionRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        CreateSubscriptionCommand command =
                createCommand(userId, UUID.randomUUID(), START_DATE, END_DATE);
        when(userFacade.userByIdExists(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.createSubscription(command));
        verify(userFacade, never()).userContainsEmail(any(), any());
        verifyNoInteractions(subscriptionMapper, subscriptionRepository);
    }

    @Test
    void createSubscriptionRejectsStartDateAfterEndDate() {
        UUID userId = UUID.randomUUID();
        CreateSubscriptionCommand command =
                createCommand(userId, UUID.randomUUID(), END_DATE, START_DATE);
        when(userFacade.userByIdExists(userId)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> subscriptionService.createSubscription(command));
        verify(userFacade, never()).userContainsEmail(any(), any());
        verifyNoInteractions(subscriptionMapper, subscriptionRepository);
    }

    @Test
    void createSubscriptionRejectsEmailNotOwnedByUser() {
        UUID userId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        CreateSubscriptionCommand command = createCommand(userId, emailId, START_DATE, END_DATE);
        when(userFacade.userByIdExists(userId)).thenReturn(true);
        when(userFacade.userContainsEmail(userId, emailId)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> subscriptionService.createSubscription(command));
        verifyNoInteractions(subscriptionMapper, subscriptionRepository);
    }

    @Test
    void updateSubscriptionValidatesAndDelegatesToMapper() {
        UUID userId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UpdateSubscriptionCommand command =
                updateCommand(subscriptionId, userId, emailId, START_DATE, END_DATE);
        Subscription subscription = subscription(subscriptionId, userId, emailId);
        when(userFacade.userByIdExists(userId)).thenReturn(true);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(userFacade.userContainsEmail(userId, emailId)).thenReturn(true);

        subscriptionService.updateSubscription(command);

        verify(subscriptionMapper).updateSubscription(subscription, command);
    }

    @Test
    void updateSubscriptionRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        UpdateSubscriptionCommand command =
                updateCommand(UUID.randomUUID(), userId, UUID.randomUUID(), START_DATE, END_DATE);
        when(userFacade.userByIdExists(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.updateSubscription(command));
        verifyNoInteractions(subscriptionMapper);
    }

    @Test
    void updateSubscriptionRejectsStartDateAfterEndDate() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UpdateSubscriptionCommand command =
                updateCommand(subscriptionId, userId, UUID.randomUUID(), END_DATE, START_DATE);
        Subscription subscription = subscription(subscriptionId, userId, command.emailId());
        stubOwnedSubscription(userId, subscription);

        assertThrows(BadRequestException.class,
                () -> subscriptionService.updateSubscription(command));
        verify(userFacade, never()).userContainsEmail(any(), any());
        verifyNoInteractions(subscriptionMapper);
    }

    @Test
    void updateSubscriptionRejectsEmailNotOwnedByUser() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        UpdateSubscriptionCommand command =
                updateCommand(subscriptionId, userId, emailId, START_DATE, END_DATE);
        Subscription subscription = subscription(subscriptionId, userId, emailId);
        stubOwnedSubscription(userId, subscription);
        when(userFacade.userContainsEmail(userId, emailId)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> subscriptionService.updateSubscription(command));
        verifyNoInteractions(subscriptionMapper);
    }

    @Test
    void deleteSubscriptionValidatesOwnershipAndDeletesById() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = subscription(subscriptionId, userId, UUID.randomUUID());
        stubOwnedSubscription(userId, subscription);

        subscriptionService.deleteSubscription(new DeleteSubscriptionCommand(userId, subscriptionId));

        verify(subscriptionRepository).deleteById(subscriptionId);
    }

    @Test
    void deleteSubscriptionRejectsSubscriptionOwnedByAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription =
                subscription(subscriptionId, UUID.randomUUID(), UUID.randomUUID());
        when(userFacade.userByIdExists(userId)).thenReturn(true);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        assertThrows(ForbiddenException.class, () -> subscriptionService.deleteSubscription(
                new DeleteSubscriptionCommand(userId, subscriptionId)));
        verify(subscriptionRepository, never()).deleteById(any());
    }

    @Test
    void getSubscriptionByIdValidatesOwnershipAndMapsResult() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Subscription subscription = subscription(subscriptionId, userId, UUID.randomUUID());
        SubscriptionResponse response = response(subscription);
        stubOwnedSubscription(userId, subscription);
        when(subscriptionMapper.toSubscriptionResponse(subscription)).thenReturn(response);

        SubscriptionResponse result = subscriptionService.getSubscriptionById(
                new GetUserSubscriptionQuery(userId, subscriptionId));

        assertSame(response, result);
    }

    @Test
    void getSubscriptionByIdRejectsMissingSubscription() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        when(userFacade.userByIdExists(userId)).thenReturn(true);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subscriptionService.getSubscriptionById(
                new GetUserSubscriptionQuery(userId, subscriptionId)));
        verifyNoInteractions(subscriptionMapper);
    }

    @Test
    void validateSubscriptionAcceptsOwnedSubscription() {
        UUID userId = UUID.randomUUID();
        Subscription subscription = subscription(UUID.randomUUID(), userId, UUID.randomUUID());
        stubOwnedSubscription(userId, subscription);

        subscriptionService.validateSubscription(userId, subscription.getId());

        verify(subscriptionRepository).findById(subscription.getId());
    }

    @Test
    void validateSubscriptionRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userFacade.userByIdExists(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.validateSubscription(userId, UUID.randomUUID()));
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void validateSubscriptionRejectsMissingSubscription() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        when(userFacade.userByIdExists(userId)).thenReturn(true);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.validateSubscription(userId, subscriptionId));
    }

    @Test
    void validateSubscriptionRejectsSubscriptionOwnedByAnotherUser() {
        UUID userId = UUID.randomUUID();
        Subscription subscription =
                subscription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(userFacade.userByIdExists(userId)).thenReturn(true);
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        assertThrows(ForbiddenException.class,
                () -> subscriptionService.validateSubscription(userId, subscription.getId()));
    }

    @Test
    void getSubscriptionsByUserIdMapsAllSubscriptions() {
        UUID userId = UUID.randomUUID();
        Subscription first = subscription(UUID.randomUUID(), userId, UUID.randomUUID());
        Subscription second = subscription(UUID.randomUUID(), userId, UUID.randomUUID());
        SubscriptionResponse firstResponse = response(first);
        SubscriptionResponse secondResponse = response(second);
        when(userFacade.userByIdExists(userId)).thenReturn(true);
        when(subscriptionRepository.findAllByUserId(userId)).thenReturn(List.of(first, second));
        when(subscriptionMapper.toSubscriptionResponse(first)).thenReturn(firstResponse);
        when(subscriptionMapper.toSubscriptionResponse(second)).thenReturn(secondResponse);

        assertEquals(List.of(firstResponse, secondResponse),
                subscriptionService.getSubscriptionsByUserId(userId));
    }

    @Test
    void getSubscriptionsByUserIdRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userFacade.userByIdExists(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.getSubscriptionsByUserId(userId));
        verify(subscriptionRepository, never()).findAllByUserId(any());
        verifyNoInteractions(subscriptionMapper);
    }

    @Test
    void subscriptionExistDelegatesToRepository() {
        UUID existingId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        when(subscriptionRepository.existsById(existingId)).thenReturn(true);
        when(subscriptionRepository.existsById(missingId)).thenReturn(false);

        assertTrue(subscriptionService.subscriptionExist(existingId));
        assertFalse(subscriptionService.subscriptionExist(missingId));
    }

    @Test
    void getAllActiveSubscriptionsMapsActiveRepositoryResults() {
        Subscription first =
                subscription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        Subscription second =
                subscription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        SubscriptionResponse firstResponse = response(first);
        SubscriptionResponse secondResponse = response(second);
        when(subscriptionRepository.findAllByIsActiveTrue()).thenReturn(List.of(first, second));
        when(subscriptionMapper.toSubscriptionResponse(first)).thenReturn(firstResponse);
        when(subscriptionMapper.toSubscriptionResponse(second)).thenReturn(secondResponse);

        assertEquals(List.of(firstResponse, secondResponse),
                subscriptionService.getAllActiveSubscriptions());
    }

    private void stubOwnedSubscription(UUID userId, Subscription subscription) {
        when(userFacade.userByIdExists(userId)).thenReturn(true);
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
    }

    private static CreateSubscriptionCommand createCommand(
            UUID userId, UUID emailId, LocalDateTime startDate, LocalDateTime endDate) {
        return new CreateSubscriptionCommand(
                userId,
                "Streaming",
                emailId,
                new BigDecimal("19.99"),
                Currency.getInstance("USD"),
                startDate,
                endDate,
                1,
                PeriodType.MONTH,
                true,
                false,
                0);
    }

    private static UpdateSubscriptionCommand updateCommand(
            UUID id, UUID userId, UUID emailId, LocalDateTime startDate, LocalDateTime endDate) {
        return new UpdateSubscriptionCommand(
                id,
                userId,
                "Updated streaming",
                emailId,
                new BigDecimal("24.99"),
                Currency.getInstance("USD"),
                startDate,
                endDate,
                1,
                PeriodType.MONTH,
                true,
                false,
                0);
    }

    private static Subscription subscription(UUID id, UUID userId, UUID emailId) {
        Subscription subscription = Subscription.builder()
                .name("Streaming")
                .userId(userId)
                .emailId(emailId)
                .price(new BigDecimal("19.99"))
                .currency(Currency.getInstance("USD"))
                .startDate(START_DATE)
                .endDate(END_DATE)
                .period(1)
                .periodType(PeriodType.MONTH)
                .isActive(true)
                .includeTrail(false)
                .trailLength(0)
                .build();
        subscription.setId(id);
        return subscription;
    }

    private static SubscriptionResponse response(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getName(),
                subscription.getEmailId(),
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getPeriod(),
                subscription.getPeriodType(),
                subscription.isActive(),
                subscription.isIncludeTrail(),
                subscription.getTrailLength());
    }
}
