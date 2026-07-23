package org.wilczewski.substrack.email.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.wilczewski.substrack.common.exception.InternalServerException;
import org.wilczewski.substrack.subscription.api.dto.response.SubscriptionResponse;
import org.wilczewski.substrack.subscription.internal.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SpringTemplateEngine emailTemplateEngine;

    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailService(mailSender, emailTemplateEngine, "reminders@substrack.test");
    }

    @Test
    void sendsConstructedMessageUsingPaymentReminderTemplateContext() {
        LocalDate billingDate = LocalDate.of(2026, 8, 15);
        SubscriptionResponse subscription = subscription("Video Plus");
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(emailTemplateEngine.process(eq("payment-reminder"), contextCaptor.capture()))
                .thenReturn("Your payment is due soon.");

        service.sendPaymentReminder("customer@example.com", subscription, billingDate, 4);

        Context context = contextCaptor.getValue();
        assertEquals("Video Plus", context.getVariable("subscriptionName"));
        assertEquals(4L, context.getVariable("daysBefore"));
        assertEquals(billingDate, context.getVariable("billingDate"));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("reminders@substrack.test", message.getFrom());
        assertArrayEquals(new String[]{"customer@example.com"}, message.getTo());
        assertEquals("Video Plus - Payment reminder", message.getSubject());
        assertEquals("Your payment is due soon.", message.getText());
    }

    @Test
    void wrapsMailExceptionInInternalServerException() {
        SubscriptionResponse subscription = subscription("Video Plus");
        LocalDate billingDate = LocalDate.of(2026, 8, 15);
        MailSendException mailException = new MailSendException("SMTP unavailable");
        when(emailTemplateEngine.process(eq("payment-reminder"), any(Context.class))).thenReturn("Reminder");
        doThrow(mailException).when(mailSender).send(any(SimpleMailMessage.class));

        InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> service.sendPaymentReminder("customer@example.com", subscription, billingDate, 4)
        );

        assertEquals("Failed to send payment reminder to customer@example.com", exception.getMessage());
        assertSame(mailException, exception.getCause());
    }

    private SubscriptionResponse subscription(String name) {
        return new SubscriptionResponse(
                UUID.randomUUID(),
                name,
                UUID.randomUUID(),
                BigDecimal.valueOf(19.99),
                Currency.getInstance("USD"),
                LocalDate.of(2026, 1, 15).atStartOfDay(),
                null,
                1,
                PeriodType.MONTH,
                true,
                false,
                0
        );
    }
}
