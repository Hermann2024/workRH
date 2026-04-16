package com.workrh.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.workrh.common.tenant.TenantContext;
import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.SubscriptionInvoice;
import com.workrh.subscription.repository.SubscriptionInvoiceRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SubscriptionInvoiceExportServiceTest {

    private final SubscriptionInvoiceRepository subscriptionInvoiceRepository = Mockito.mock(SubscriptionInvoiceRepository.class);
    private final SubscriptionInvoiceExportService exportService = new SubscriptionInvoiceExportService(subscriptionInvoiceRepository);

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldExportAccountingCsvForTenantInvoices() {
        TenantContext.setTenantId("tenant-a");
        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setInvoiceNumber("INV-2026-001");
        invoice.setIssuedAt(Instant.parse("2026-03-01T10:15:30Z"));
        invoice.setCustomerName("Demo Company");
        invoice.setCustomerEmail("billing@demo.com");
        invoice.setPlanCode(PlanCode.PREMIUM);
        invoice.setBillingPeriodStart(LocalDate.of(2026, 3, 1));
        invoice.setBillingPeriodEnd(LocalDate.of(2026, 3, 31));
        invoice.setCurrency("EUR");
        invoice.setTotalAmount(new BigDecimal("399.00"));
        invoice.setApplicationName("WorkRH");

        when(subscriptionInvoiceRepository.findAllByTenantIdOrderByIssuedAtDesc("tenant-a")).thenReturn(List.of(invoice));

        byte[] csv = exportService.exportAccountingCsv(2026, 3);

        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("INV-2026-001");
        assertThat(content).contains("706100");
        assertThat(content).contains("TVA-LU");
    }
}
