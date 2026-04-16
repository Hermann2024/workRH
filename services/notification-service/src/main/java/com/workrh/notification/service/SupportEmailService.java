package com.workrh.notification.service;

import com.workrh.notification.domain.SupportTicket;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SupportEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${notification.support.from-email:support@workrh.app}")
    private String fromEmail;

    public SupportEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendAcknowledgement(SupportTicket ticket) {
        if (mailHost == null || mailHost.isBlank() || ticket.getRequesterEmail() == null || ticket.getRequesterEmail().isBlank()) {
            return false;
        }

        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(ticket.getRequesterEmail());
            helper.setSubject("Support WorkRH - ticket #" + ticket.getId());
            helper.setText(buildBody(ticket), true);
            mailSender.send(mimeMessage);
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to send support acknowledgement", exception);
        }
    }

    private String buildBody(SupportTicket ticket) {
        return """
                <p>Bonjour %s,</p>
                <p>Votre demande de support a bien ete enregistree sous la reference <strong>#%d</strong>.</p>
                <p><strong>Sujet :</strong> %s</p>
                <p><strong>Priorite :</strong> %s</p>
                <p>Notre equipe reviendra vers vous rapidement.</p>
                <p>Equipe WorkRH</p>
                """.formatted(
                defaultValue(ticket.getRequesterName(), "client"),
                ticket.getId(),
                ticket.getSubject(),
                ticket.getPriority().name()
        );
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
