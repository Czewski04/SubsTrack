package org.wilczewski.substrack.notification.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wilczewski.substrack.email.api.EmailFacade;
import org.wilczewski.substrack.notification.api.dto.command.CreateNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.command.DeleteNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.command.UpdateNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.response.NotificationResponse;
import org.wilczewski.substrack.subscription.api.dto.SubscriptionFacade;
import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;
import org.wilczewski.substrack.user.api.UserFacade;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class NotificationService {
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;
    private final SubscriptionFacade subscriptionFacade;
    private final EmailFacade emailFacade;
    private final NotificationSentRepository notificationSentRepository;
    private final UserFacade userFacade;

    @Transactional
    public UUID createNotification(CreateNotificationCommand command) {
        if (notificationRepository.existsBySubscriptionId(command.subscriptionId())) {
            throw new IllegalArgumentException("Notification for this subscription already exists");
        }
        subscriptionFacade.validateSubscription(command.userId(), command.subscriptionId());
        Notification notification = notificationMapper.toNotification(command);
        notification = notificationRepository.save(notification);
        return notification.getId();
    }

    @Transactional
    public void updateNotification(UpdateNotificationCommand command) {
        Notification notification = validateNotificationOwnership(
                command.userId(), command.subscriptionId(), command.id());
        notificationMapper.updateNotification(notification, command);
    }

    @Transactional
    public void deleteNotification(DeleteNotificationCommand command) {
        Notification notification = validateNotificationOwnership(
                command.userId(), command.subscriptionId(), command.notificationId());
        notificationSentRepository.deleteByNotificationId(notification.getId());
        notificationRepository.delete(notification);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationBySubscriptionId(UUID userId, UUID subscriptionId) {
        subscriptionFacade.validateSubscription(userId, subscriptionId);
        Notification notification = notificationRepository.findBySubscriptionId(subscriptionId);
        if (notification == null) {
            throw new IllegalArgumentException("Notification not found");
        }
        return notificationMapper.toNotificationResponse(notification);
    }

    private Notification validateNotificationOwnership(UUID userId, UUID subscriptionId, UUID notificationId) {
        subscriptionFacade.validateSubscription(userId, subscriptionId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getSubscriptionId().equals(subscriptionId)) {
            throw new IllegalArgumentException("Notification does not belong to subscription");
        }
        return notification;
    }

    public void sendNotifications() {
        LocalDate today = LocalDate.now();

        Map<UUID, SubscriptionResponse> subscriptionsById =
                subscriptionFacade.getAllActiveSubscriptions().stream()
                        .collect(Collectors.toMap(SubscriptionResponse::id, Function.identity()));

        for (Notification notification : notificationRepository.findAllByIsActiveTrue()) {
            SubscriptionResponse subscription = subscriptionsById.get(notification.getSubscriptionId());
            if (subscription == null || !subscription.isActive()) {
                continue;
            }

            LocalDate nextBilling = calculateNextBillingDate(subscription, today);
            if (subscription.endDate() != null
                    && nextBilling.isAfter(subscription.endDate().toLocalDate())) {
                continue;
            }

            for (Duration duration : notification.getDurations()) {
                long daysBefore = duration.toDays();
                LocalDate notifyDate = nextBilling.minusDays(daysBefore);
                if (notifyDate.equals(today)) {
                    try {
                        notificationSentRepository.saveAndFlush(
                                new NotificationSent(notification.getId(), nextBilling, daysBefore));
                    } catch (DataIntegrityViolationException e) {
                        log.debug("Reminder already claimed: notificationId={}, billingDate={}, daysBefore={}",
                                notification.getId(), nextBilling, daysBefore);
                        continue;
                    }

                    try {
                        String userEmail = userFacade.getUserEmailByEmailId(subscription.emailId());
                        emailFacade.sendPaymentReminder(userEmail, subscription, nextBilling, daysBefore);
                    } catch (Exception e) {
                        notificationSentRepository.deleteByNotificationIdAndBillingDateAndDaysBefore(
                                notification.getId(), nextBilling, daysBefore);
                        log.error("Failed to send reminder: notificationId={}, subscriptionId={}, daysBefore={}",
                                notification.getId(), subscription.id(), daysBefore, e);
                    }
                }
            }
        }
    }

    private LocalDate calculateNextBillingDate(SubscriptionResponse subscription, LocalDate today) {
        LocalDate anchor = subscription.startDate().toLocalDate();
        if (subscription.includeTrail()) {
            anchor = anchor.plusDays(subscription.trailLength());
        }

        LocalDate next = anchor;
        while (next.isBefore(today)) {
            next = switch (subscription.periodType()) {
                case DAY   -> next.plusDays(subscription.period());
                case WEEK  -> next.plusWeeks(subscription.period());
                case MONTH -> next.plusMonths(subscription.period());
                case YEAR  -> next.plusYears(subscription.period());
            };
        }
        return next;
    }
}
