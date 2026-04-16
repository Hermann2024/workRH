package com.workrh.subscription.service;

import com.workrh.common.tenant.TenantContext;
import com.workrh.subscription.domain.SubscriptionInvoice;
import com.workrh.subscription.repository.SubscriptionInvoiceRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionInvoiceExportService {

    private final SubscriptionInvoiceRepository subscriptionInvoiceRepository;

    public SubscriptionInvoiceExportService(SubscriptionInvoiceRepository subscriptionInvoiceRepository) {
        this.subscriptionInvoiceRepository = subscriptionInvoiceRepository;
    }

    public byte[] exportAccountingCsv(Integer year, Integer month) {
        List<SubscriptionInvoice> invoices = subscriptionInvoiceRepository.findAllByTenantIdOrderByIssuedAtDesc(TenantContext.getTenantId()).stream()
                .filter(invoice -> matchesPeriod(invoice, year, month))
                .toList();

        StringBuilder csv = new StringBuilder(
                "invoiceNumber,issuedAt,customerName,customerEmail,planCode,billingPeriodStart,billingPeriodEnd,currency,totalAmount,revenueAccount,taxCode,applicationName\n"
        );
        invoices.forEach(invoice -> csv.append(safe(invoice.getInvoiceNumber())).append(',')
                .append(safe(invoice.getIssuedAt())).append(',')
                .append(safe(invoice.getCustomerName())).append(',')
                .append(safe(invoice.getCustomerEmail())).append(',')
                .append(safe(invoice.getPlanCode())).append(',')
                .append(safe(invoice.getBillingPeriodStart())).append(',')
                .append(safe(invoice.getBillingPeriodEnd())).append(',')
                .append(safe(invoice.getCurrency())).append(',')
                .append(safe(invoice.getTotalAmount())).append(',')
                .append("706100").append(',')
                .append("TVA-LU").append(',')
                .append(safe(invoice.getApplicationName())).append('\n'));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private boolean matchesPeriod(SubscriptionInvoice invoice, Integer year, Integer month) {
        LocalDate start = invoice.getBillingPeriodStart();
        if (start == null) {
            return year == null && month == null;
        }
        if (year != null && start.getYear() != year) {
            return false;
        }
        return month == null || start.getMonthValue() == month;
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }
}
