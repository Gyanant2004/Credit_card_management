package com.ofss.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        boolean success,
        Long cardNumber,
        BigDecimal availableCredit,
        BigDecimal outstandingAmount
) {
}