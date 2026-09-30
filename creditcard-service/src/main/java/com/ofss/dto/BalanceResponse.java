package com.ofss.dto;

import com.ofss.entity.CreditCard;
import java.math.BigDecimal;

public record BalanceResponse(
    boolean success,
    Long cardNumber,
    BigDecimal availableCredit,
    BigDecimal outstandingAmount
) {
    public static BalanceResponse from(CreditCard card) {
        return new BalanceResponse(true, card.getCardNumber(),
            card.getAvailableCredit(), card.getOutstandingAmount());
    }
}
