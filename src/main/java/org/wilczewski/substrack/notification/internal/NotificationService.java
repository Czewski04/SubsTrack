package org.wilczewski.substrack.notification.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wilczewski.substrack.notification.api.dto.command.CreateNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.command.DeleteNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.command.UpdateNotificationCommand;
import org.wilczewski.substrack.notification.api.dto.response.NotificationResponse;
import org.wilczewski.substrack.subscription.api.dto.SubscriptionFacade;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class NotificationService {
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;
    private final SubscriptionFacade subscriptionFacade;

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
}
