package org.wilczewski.substrack.notification.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationCronJob {
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    public void processNotification() {
        notificationService.sendNotifications();
    }
}
