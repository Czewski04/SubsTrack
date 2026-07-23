package org.wilczewski.substrack.notification.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.wilczewski.substrack.common.exception.ConflictException;
import org.wilczewski.substrack.common.exception.ForbiddenException;
import org.wilczewski.substrack.common.exception.ResourceNotFoundException;
import org.wilczewski.substrack.email.api.EmailFacade;
import org.wilczewski.substrack.notification.api.dto.command.CreateNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.command.DeleteNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.command.UpdateNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.response.NotificationResponse;
import org.wilczewski.substrack.subscription.api.dto.SubscriptionFacade;
import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;
import org.wilczewski.substrack.subscription.internal.PeriodType;
import org.wilczewski.substrack.user.api.UserFacade;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SubscriptionFacade subscriptionFacade;
    @Mock
    private EmailFacade emailFacade;
    @Mock
    private NotificationSentRepository notificationSentRepository;
    @Mock
    private UserFacade userFacade;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(
                notificationMapper,
                notificationRepository,
                subscriptionFacade,
                emailFacade,
                notificationSentRepository,
                userFacade
        );
    }

    @Test
    void createsNotificationAfterValidatingSubscription() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        CreateNotificationCommand command =
                new CreateNotificationCommand(userId, subscriptionId, true, List.of(Duration.ofDays(2)));
        Notification notification = notification(notificationId, subscriptionId, Duration.ofDays(2));

        when(notificationRepository.existsBySubscriptionId(subscriptionId)).thenReturn(false);
        when(notificationMapper.toNotification(command)).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);

        assertEquals(notificationId, service.createNotification(command));

        verify(subscriptionFacade).validateSubscription(userId, subscriptionId);
        verify(notificationRepository).save(notification);
    }

    @Test
    void rejectsDuplicateNotification() {
        UUID subscriptionId = UUID.randomUUID();
        CreateNotificationCommand command =
                new CreateNotificationCommand(UUID.randomUUID(), subscriptionId, true, List.of());
        when(notificationRepository.existsBySubscriptionId(subscriptionId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createNotification(command));

        verifyNoInteractions(subscriptionFacade, notificationMapper);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void updatesOwnedNotification() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UpdateNotificationCommand command =
                new UpdateNotificationCommand(notificationId, userId, subscriptionId, false, List.of());
        Notification notification = notification(notificationId, subscriptionId, Duration.ofDays(1));
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        service.updateNotification(command);

        verify(subscriptionFacade).validateSubscription(userId, subscriptionId);
        verify(notificationMapper).updateNotification(notification, command);
    }

    @Test
    void updateFailsWhenNotificationDoesNotExist() {
        UUID notificationId = UUID.randomUUID();
        UpdateNotificationCommand command = new UpdateNotificationCommand(
                notificationId, UUID.randomUUID(), UUID.randomUUID(), true, List.of());
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateNotification(command));

        verify(notificationMapper, never()).updateNotification(any(), any());
    }

    @Test
    void updateRejectsNotificationOwnedByAnotherSubscription() {
        UUID notificationId = UUID.randomUUID();
        UUID requestedSubscriptionId = UUID.randomUUID();
        UpdateNotificationCommand command = new UpdateNotificationCommand(
                notificationId, UUID.randomUUID(), requestedSubscriptionId, true, List.of());
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(notification(notificationId, UUID.randomUUID(), Duration.ZERO)));

        assertThrows(ForbiddenException.class, () -> service.updateNotification(command));

        verify(notificationMapper, never()).updateNotification(any(), any());
    }

    @Test
    void deletesOwnedNotificationAndItsSentClaims() {
        UUID notificationId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        DeleteNotificationCommand command =
                new DeleteNotificationCommand(notificationId, UUID.randomUUID(), subscriptionId);
        Notification notification = notification(notificationId, subscriptionId, Duration.ZERO);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        service.deleteNotification(command);

        verify(notificationSentRepository).deleteByNotificationId(notificationId);
        verify(notificationRepository).delete(notification);
    }

    @Test
    void returnsNotificationForValidatedSubscription() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Notification notification = notification(UUID.randomUUID(), subscriptionId, Duration.ZERO);
        NotificationResponse response =
                new NotificationResponse(notification.getId(), subscriptionId, true, List.of(Duration.ZERO));
        when(notificationRepository.findBySubscriptionId(subscriptionId)).thenReturn(notification);
        when(notificationMapper.toNotificationResponse(notification)).thenReturn(response);

        assertSame(response, service.getNotificationBySubscriptionId(userId, subscriptionId));

        verify(subscriptionFacade).validateSubscription(userId, subscriptionId);
    }

    @Test
    void getFailsWhenNotificationDoesNotExist() {
        UUID subscriptionId = UUID.randomUUID();
        when(notificationRepository.findBySubscriptionId(subscriptionId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class,
                () -> service.getNotificationBySubscriptionId(UUID.randomUUID(), subscriptionId));
    }

    @Test
    void sendsReminderWhenNotificationDateMatchesToday() {
        LocalDate today = LocalDate.now();
        Notification notification = notification(UUID.randomUUID(), UUID.randomUUID(), Duration.ofDays(3));
        SubscriptionResponse subscription =
                subscription(notification.getSubscriptionId(), today.plusDays(3), null, 1, PeriodType.MONTH, true, false, 0);
        stubSendCandidates(notification, subscription);
        when(userFacade.getUserEmailByEmailId(subscription.emailId())).thenReturn("user@example.com");

        service.sendNotifications();

        ArgumentCaptor<NotificationSent> claim = ArgumentCaptor.forClass(NotificationSent.class);
        verify(notificationSentRepository).saveAndFlush(claim.capture());
        assertEquals(notification.getId(), claim.getValue().getNotificationId());
        assertEquals(today.plusDays(3), claim.getValue().getBillingDate());
        assertEquals(3, claim.getValue().getDaysBefore());
        verify(emailFacade).sendPaymentReminder("user@example.com", subscription, today.plusDays(3), 3);
    }

    @Test
    void skipsMissingAndInactiveSubscriptions() {
        Notification missing = notification(UUID.randomUUID(), UUID.randomUUID(), Duration.ZERO);
        Notification inactive = notification(UUID.randomUUID(), UUID.randomUUID(), Duration.ZERO);
        SubscriptionResponse inactiveSubscription =
                subscription(inactive.getSubscriptionId(), LocalDate.now(), null, 1, PeriodType.DAY, false, false, 0);
        when(subscriptionFacade.getAllActiveSubscriptions()).thenReturn(List.of(inactiveSubscription));
        when(notificationRepository.findAllByIsActiveTrue()).thenReturn(List.of(missing, inactive));

        service.sendNotifications();

        verifyNoInteractions(notificationSentRepository, emailFacade, userFacade);
    }

    @Test
    void skipsReminderWhenNextBillingIsAfterEndDate() {
        LocalDate today = LocalDate.now();
        Notification notification = notification(UUID.randomUUID(), UUID.randomUUID(), Duration.ofDays(2));
        SubscriptionResponse subscription =
                subscription(notification.getSubscriptionId(), today.plusDays(2), today.plusDays(1),
                        1, PeriodType.MONTH, true, false, 0);
        stubSendCandidates(notification, subscription);

        service.sendNotifications();

        verifyNoInteractions(notificationSentRepository, emailFacade, userFacade);
    }

    @Test
    void skipsReminderWhenNotifyDateDoesNotMatchToday() {
        LocalDate today = LocalDate.now();
        Notification notification = notification(UUID.randomUUID(), UUID.randomUUID(), Duration.ofDays(1));
        SubscriptionResponse subscription =
                subscription(notification.getSubscriptionId(), today.plusDays(3), null,
                        1, PeriodType.MONTH, true, false, 0);
        stubSendCandidates(notification, subscription);

        service.sendNotifications();

        verifyNoInteractions(notificationSentRepository, emailFacade, userFacade);
    }

    @Test
    void treatsDuplicateClaimAsIdempotentAndDoesNotSendEmail() {
        LocalDate today = LocalDate.now();
        Notification notification = notification(UUID.randomUUID(), UUID.randomUUID(), Duration.ZERO);
        SubscriptionResponse subscription =
                subscription(notification.getSubscriptionId(), today, null, 1, PeriodType.DAY, true, false, 0);
        stubSendCandidates(notification, subscription);
        when(notificationSentRepository.saveAndFlush(any(NotificationSent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        service.sendNotifications();

        verifyNoInteractions(emailFacade, userFacade);
        verify(notificationSentRepository, never())
                .deleteByNotificationIdAndBillingDateAndDaysBefore(any(), any(), any(Long.class));
    }

    @Test
    void deletesClaimWhenSendingEmailFails() {
        LocalDate today = LocalDate.now();
        Notification notification = notification(UUID.randomUUID(), UUID.randomUUID(), Duration.ZERO);
        SubscriptionResponse subscription =
                subscription(notification.getSubscriptionId(), today, null, 1, PeriodType.DAY, true, false, 0);
        stubSendCandidates(notification, subscription);
        when(userFacade.getUserEmailByEmailId(subscription.emailId())).thenReturn("user@example.com");
        doThrow(new RuntimeException("mail unavailable")).when(emailFacade)
                .sendPaymentReminder("user@example.com", subscription, today, 0);

        service.sendNotifications();

        verify(notificationSentRepository)
                .deleteByNotificationIdAndBillingDateAndDaysBefore(notification.getId(), today, 0);
    }

    @Test
    void supportsEveryBillingPeriodAndTrialAnchorThroughSendNotifications() {
        LocalDate today = LocalDate.now();
        List<SubscriptionResponse> subscriptions = Stream.of(
                subscription(UUID.randomUUID(), today.minusDays(4), null, 2, PeriodType.DAY, true, false, 0),
                subscription(UUID.randomUUID(), today.minusWeeks(2), null, 1, PeriodType.WEEK, true, false, 0),
                subscription(UUID.randomUUID(), today.minusMonths(2), null, 1, PeriodType.MONTH, true, false, 0),
                subscription(UUID.randomUUID(), today.minusYears(2), null, 1, PeriodType.YEAR, true, false, 0),
                subscription(UUID.randomUUID(), today.minusDays(3), null, 1, PeriodType.MONTH, true, true, 3)
        ).toList();
        List<Notification> notifications = subscriptions.stream()
                .map(subscription -> notification(UUID.randomUUID(), subscription.id(), Duration.ZERO))
                .toList();
        when(subscriptionFacade.getAllActiveSubscriptions()).thenReturn(subscriptions);
        when(notificationRepository.findAllByIsActiveTrue()).thenReturn(notifications);
        subscriptions.forEach(subscription ->
                when(userFacade.getUserEmailByEmailId(subscription.emailId())).thenReturn("user@example.com"));

        service.sendNotifications();

        subscriptions.forEach(subscription ->
                verify(emailFacade).sendPaymentReminder("user@example.com", subscription, today, 0));
    }

    private void stubSendCandidates(Notification notification, SubscriptionResponse subscription) {
        when(subscriptionFacade.getAllActiveSubscriptions()).thenReturn(List.of(subscription));
        when(notificationRepository.findAllByIsActiveTrue()).thenReturn(List.of(notification));
    }

    private Notification notification(UUID id, UUID subscriptionId, Duration... durations) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setSubscriptionId(subscriptionId);
        notification.setActive(true);
        notification.setDurations(List.of(durations));
        return notification;
    }

    private SubscriptionResponse subscription(
            UUID id,
            LocalDate startDate,
            LocalDate endDate,
            int period,
            PeriodType periodType,
            boolean active,
            boolean includeTrial,
            int trialLength
    ) {
        return new SubscriptionResponse(
                id,
                "Streaming",
                UUID.randomUUID(),
                BigDecimal.TEN,
                Currency.getInstance("USD"),
                startDate.atStartOfDay(),
                endDate == null ? null : endDate.atStartOfDay(),
                period,
                periodType,
                active,
                includeTrial,
                trialLength
        );
    }
}
