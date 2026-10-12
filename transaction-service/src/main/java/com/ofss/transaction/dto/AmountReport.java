package com.ofss.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AmountReport(
        String report,
        LocalDate date,
        String timezone,
        BigDecimal amount
) {
}