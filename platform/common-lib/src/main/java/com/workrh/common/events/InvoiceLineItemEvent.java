package com.workrh.common.events;

import java.math.BigDecimal;

public record InvoiceLineItemEvent(
        String description,
        int quantity,
        BigDecimal unitAmount,
        BigDecimal totalAmount
) {
}
