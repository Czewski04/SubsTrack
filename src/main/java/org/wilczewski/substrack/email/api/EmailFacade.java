package org.wilczewski.substrack.email.api;

import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;

import java.time.LocalDate;

public interface EmailFacade {
    void sendPaymentReminder(String to, SubscriptionResponse subscription, LocalDate billingDate, long daysBefore);
}
