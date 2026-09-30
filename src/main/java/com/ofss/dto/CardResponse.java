package com.ofss.dto;

import com.ofss.entity.CardStatus;
import com.ofss.entity.CreditCard;
import java.math.BigDecimal;

public record CardResponse(
    Long cardNumber,
    CardStatus cardStatus,
    BigDecimal availableCredit,
    BigDecimal outstandingAmount
) {
    public static CardResponse from(CreditCard card) {
        return new CardResponse(card.getCardNumber(), card.getCardStatus(),
            card.getAvailableCredit(), card.getOutstandingAmount());
    }
}
