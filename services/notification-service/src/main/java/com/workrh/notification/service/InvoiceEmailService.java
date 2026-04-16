package com.workrh.notification.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.workrh.common.events.InvoiceIssuedEvent;
import com.workrh.common.events.InvoiceLineItemEvent;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class InvoiceEmailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;

    @Value("${notification.billing.enabled:true}")
    private boolean billingEmailEnabled;

    @Value("${notification.billing.application-name:WorkRH}")
    private String applicationName;

    @Value("${notification.billing.from-email:no-reply@workrh.app}")
    private String fromEmail;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public InvoiceEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendInvoice(InvoiceIssuedEvent event) {
        if (!billingEmailEnabled || mailHost == null || mailHost.isBlank()) {
            return false;
        }

        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(event.customerEmail());
            helper.setSubject("Votre facture " + applicationName + " " + event.invoiceNumber());
            helper.setText(buildHtmlBody(event), true);
            helper.addAttachment(buildFilename(event), new ByteArrayResource(generatePdf(event)));
            mailSender.send(mimeMessage);
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to send invoice email", exception);
        }
    }

    private String buildFilename(InvoiceIssuedEvent event) {
        return "facture-" + applicationName.toLowerCase(Locale.ROOT) + "-" + event.invoiceNumber() + ".pdf";
    }

    private String buildHtmlBody(InvoiceIssuedEvent event) {
        return """
                <p>Bonjour,</p>
                <p>Merci pour votre achat sur <strong>%s</strong>.</p>
                <p>Votre facture <strong>%s</strong> est jointe a cet email.</p>
                <p>Montant : <strong>%s</strong></p>
                <p>Periode de facturation : %s au %s</p>
                <p>Cordialement,<br>%s</p>
                """.formatted(
                applicationName,
                event.invoiceNumber(),
                formatCurrency(event.totalAmount(), event.currency()),
                formatDate(event.billingPeriodStart()),
                formatDate(event.billingPeriodEnd()),
                applicationName
        );
    }

    private byte[] generatePdf(InvoiceIssuedEvent event) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            Paragraph title = new Paragraph("Facture " + applicationName, titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            title.setSpacingAfter(12);
            document.add(title);

            document.add(new Paragraph("Numero : " + event.invoiceNumber(), sectionFont));
            document.add(new Paragraph("Client : " + defaultValue(event.customerName(), event.customerEmail())));
            document.add(new Paragraph("Email : " + event.customerEmail()));
            document.add(new Paragraph("Plan : " + event.planCode()));
            document.add(new Paragraph("Periode : " + formatDate(event.billingPeriodStart()) + " au " + formatDate(event.billingPeriodEnd())));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[] {4f, 1f, 2f, 2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(8);
            table.addCell(headerCell("Description"));
            table.addCell(headerCell("Qt"));
            table.addCell(headerCell("Unitaire"));
            table.addCell(headerCell("Total"));

            for (InvoiceLineItemEvent item : event.lineItems()) {
                table.addCell(bodyCell(item.description()));
                table.addCell(bodyCell(Integer.toString(item.quantity())));
                table.addCell(bodyCell(formatCurrency(item.unitAmount(), event.currency())));
                table.addCell(bodyCell(formatCurrency(item.totalAmount(), event.currency())));
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total facture : " + formatCurrency(event.totalAmount(), event.currency()), sectionFont));
            document.close();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate invoice PDF", exception);
        }
    }

    private PdfPCell headerCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell bodyCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(6);
        return cell;
    }

    private String formatCurrency(BigDecimal amount, String currency) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        try {
            formatter.setCurrency(java.util.Currency.getInstance(currency.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            // Keep locale default when Stripe returns an unknown currency code.
        }
        return formatter.format(amount);
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
