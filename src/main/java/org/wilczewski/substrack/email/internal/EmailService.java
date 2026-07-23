package org.wilczewski.substrack.email.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.wilczewski.substrack.common.exception.InternalServerException;
import org.wilczewski.substrack.email.api.EmailFacade;
import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
class EmailService implements EmailFacade {
    private final JavaMailSender mailSender;

    @Override
    public void sendPaymentReminder(String to, SubscriptionResponse subscription, LocalDate billingDate, long daysBefore) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@substrack.com");
            message.setTo(to);
            message.setSubject(subscription.name() + " - Payment reminder");
            String text = "Hello!\nWe are reminding you that your subscription "
                    + subscription.name()
                    + " will be billed in "
                    + daysBefore
                    + " days on "
                    + billingDate
                    + ".\nBest regards,\nSubstrack Team";
            message.setText(text);
            mailSender.send(message);
        } catch (MailException e) {
            throw new InternalServerException("Failed to send payment reminder to " + to, e);
        }
    }
}
