package com.workrh.notification.service;

import com.workrh.common.events.LeaveStatusChangedEvent;
import com.workrh.common.events.ThresholdAlertEvent;
import com.workrh.common.events.ThresholdExceededEvent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class NotificationEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${notification.automations.enabled:true}")
    private boolean automationEmailsEnabled;

    @Value("${notification.automations.from-email:no-reply@workrh.app}")
    private String fromEmail;

    @Value("${notification.automations.default-recipient:}")
    private String defaultRecipient;

    @Value("${notification.automations.threshold-recipient:}")
    private String thresholdRecipient;

    @Value("${notification.automations.leave-recipient:}")
    private String leaveRecipient;

    public NotificationEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendThresholdAlert(ThresholdAlertEvent event) {
        return send(
                resolveRecipients(thresholdRecipient),
                "Alerte teletravail " + event.alertStagePercent() + "% du seuil fiscal",
                """
                <p>Bonjour,</p>
                <p>Le collaborateur <strong>#%d</strong> a atteint <strong>%d%%</strong> du seuil fiscal teletravail.</p>
                <p>Consommation actuelle: <strong>%d / %d jours</strong>.</p>
                <p>Equipe WorkRH</p>
                """.formatted(
                        event.employeeId(),
                        event.alertStagePercent(),
                        event.annualUsedDays(),
                        event.annualLimit()
                )
        );
    }

    public boolean sendThresholdExceeded(ThresholdExceededEvent event) {
        return send(
                resolveRecipients(thresholdRecipient),
                "Seuil fiscal teletravail depasse",
                """
                <p>Bonjour,</p>
                <p>Le collaborateur <strong>#%d</strong> a depasse le seuil fiscal teletravail.</p>
                <p>Consommation actuelle: <strong>%d / %d jours</strong>.</p>
                <p>Equipe WorkRH</p>
                """.formatted(
                        event.employeeId(),
                        event.annualUsedDays(),
                        event.annualLimit()
                )
        );
    }

    public boolean sendLeaveApproved(LeaveStatusChangedEvent event) {
        return send(
                resolveRecipients(leaveRecipient),
                "Conge approuve",
                """
                <p>Bonjour,</p>
                <p>Le conge du collaborateur <strong>#%d</strong> a ete approuve.</p>
                <p>Periode: <strong>%s</strong> au <strong>%s</strong>.</p>
                <p>Equipe WorkRH</p>
                """.formatted(
                        event.employeeId(),
                        event.startDate(),
                        event.endDate()
                )
        );
    }

    private boolean send(String[] recipients, String subject, String body) {
        if (!automationEmailsEnabled || mailHost == null || mailHost.isBlank() || recipients.length == 0) {
            return false;
        }

        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(mimeMessage);
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to send automation email", exception);
        }
    }

    private String[] resolveRecipients(String configuredRecipients) {
        String value = configuredRecipients == null || configuredRecipients.isBlank() ? defaultRecipient : configuredRecipients;
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(value.split("[,;]"))
                .map(String::trim)
                .filter(recipient -> !recipient.isBlank())
                .distinct()
                .toArray(String[]::new);
    }
}
