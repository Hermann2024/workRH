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

    @Value("${notification.support.admin-email:}")
    private String adminEmail;

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
            return false;
        }
    }

    public boolean sendAdminNotification(SupportTicket ticket) {
        if (mailHost == null || mailHost.isBlank() || adminEmail == null || adminEmail.isBlank()) {
            return false;
        }

        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject(buildAdminSubject(ticket));
            helper.setText(buildAdminBody(ticket), true);
            mailSender.send(mimeMessage);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean sendResolution(SupportTicket ticket) {
        if (mailHost == null || mailHost.isBlank() || ticket.getRequesterEmail() == null || ticket.getRequesterEmail().isBlank()) {
            return false;
        }

        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(ticket.getRequesterEmail());
            helper.setSubject("Support WorkRH - ticket #" + ticket.getId() + " resolu");
            helper.setText(buildResolutionBody(ticket), true);
            mailSender.send(mimeMessage);
            return true;
        } catch (Exception exception) {
            return false;
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

    private String buildAdminSubject(SupportTicket ticket) {
        String prefix = "PRIORITY".equals(ticket.getPriority().name()) ? "[URGENT] " : "";
        return prefix + "WorkRH - nouveau ticket support #" + ticket.getId();
    }

    private String buildAdminBody(SupportTicket ticket) {
        return """
                <p>Nouveau ticket support WorkRH.</p>
                <p><strong>Reference :</strong> #%d</p>
                <p><strong>Tenant :</strong> %s</p>
                <p><strong>Priorite :</strong> %s</p>
                <p><strong>Categorie :</strong> %s</p>
                <p><strong>Demandeur :</strong> %s &lt;%s&gt;</p>
                <p><strong>Telephone :</strong> %s</p>
                <p><strong>Sujet :</strong> %s</p>
                <pre style="white-space: pre-wrap; font-family: Arial, sans-serif;">%s</pre>
                """.formatted(
                ticket.getId(),
                ticket.getTenantId(),
                ticket.getPriority().name(),
                ticket.getCategory().name(),
                defaultValue(ticket.getRequesterName(), "non renseigne"),
                defaultValue(ticket.getRequesterEmail(), "non renseigne"),
                defaultValue(ticket.getPhoneNumber(), "non renseigne"),
                ticket.getSubject(),
                ticket.getMessage()
        );
    }

    private String buildResolutionBody(SupportTicket ticket) {
        return """
                <p>Bonjour %s,</p>
                <p>Votre ticket support <strong>#%d</strong> a ete traite et cloture.</p>
                <p><strong>Sujet :</strong> %s</p>
                <p><strong>Reponse WorkRH :</strong></p>
                <pre style="white-space: pre-wrap; font-family: Arial, sans-serif;">%s</pre>
                <p>Equipe WorkRH</p>
                """.formatted(
                defaultValue(ticket.getRequesterName(), "client"),
                ticket.getId(),
                ticket.getSubject(),
                defaultValue(ticket.getResolutionMessage(), "Le probleme a ete resolu.")
        );
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
