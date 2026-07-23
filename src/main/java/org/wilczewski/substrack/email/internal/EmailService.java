package org.wilczewski.substrack.email.internal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.wilczewski.substrack.common.exception.InternalServerException;
import org.wilczewski.substrack.email.api.EmailFacade;
import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;

import java.time.LocalDate;

@Service
class EmailService implements EmailFacade {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine emailTemplateEngine;
    private final String from;

    EmailService(
            JavaMailSender mailSender,
            @Qualifier("emailTemplateEngine") SpringTemplateEngine emailTemplateEngine,
            @Value("${app.mail.from}") String from
    ) {
        this.mailSender = mailSender;
        this.emailTemplateEngine = emailTemplateEngine;
        this.from = from;
    }

    @Override
    public void sendPaymentReminder(String to, SubscriptionResponse subscription, LocalDate billingDate, long daysBefore) {
        try {
            Context context = new Context();
            context.setVariable("subscriptionName", subscription.name());
            context.setVariable("daysBefore", daysBefore);
            context.setVariable("billingDate", billingDate);

            String text = emailTemplateEngine.process("payment-reminder", context);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subscription.name() + " - Payment reminder");
            message.setText(text);
            mailSender.send(message);
        } catch (MailException e) {
            throw new InternalServerException("Failed to send payment reminder to " + to, e);
        }
    }
}
