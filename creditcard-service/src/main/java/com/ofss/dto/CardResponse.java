package com.ofss.dto;

import com.ofss.entity.CardStatus;
import com.ofss.entity.CardType;
import com.ofss.entity.CreditCard;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CardResponse(
    Long cardNumber,
    CardType cardType,
    BigDecimal creditLimit,
    LocalDate expiryDate,
    CardStatus cardStatus,
    BigDecimal availableCredit,
    BigDecimal outstandingAmount
) {
    public static CardResponse from(CreditCard card) {
        return new CardResponse(
            card.getCardNumber(),
            card.getCardType(),
            card.getCreditLimit(),
            card.getExpiryDate(),
            card.getCardStatus(),
            card.getAvailableCredit(),
            card.getOutstandingAmount()
        );
    }
}