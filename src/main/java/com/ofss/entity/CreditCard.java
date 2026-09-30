package com.ofss.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "CREDITCARD")
public class CreditCard {
    @Id
    
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CARD_ID")
    private Long cardId;
    
    @Column(name = "CARD_NUMBER", precision = 12, nullable = false)
    private Long cardNumber;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "CARD_TYPE", length = 10, nullable = false)
    private CardType cardType;

    @Column(name = "CREDIT_LIMIT", precision = 12, scale = 2, nullable = false)
    private BigDecimal creditLimit;

    @Column(name = "AVAIL_CREDIT", precision = 12, scale = 2, nullable = false)
    private BigDecimal availableCredit;

    @Column(name = "OUTSTANDING_AMT", precision = 12, scale = 2, nullable = false)
    private BigDecimal outstandingAmount;

    @Column(name = "EXPIRY_DATE", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "CARD_STATUS", length = 7, nullable = false)
    private CardStatus cardStatus;

    protected CreditCard() {
    }

    public CreditCard(Long cardNumber, Long customerId, CardType cardType,
                      BigDecimal creditLimit, LocalDate expiryDate) {
        this.cardNumber = cardNumber;
        this.customerId = customerId;
        this.cardType = cardType;
        this.creditLimit = creditLimit;
        this.availableCredit = creditLimit;
        this.outstandingAmount = BigDecimal.ZERO.setScale(2);
        this.expiryDate = expiryDate;
        this.cardStatus = CardStatus.ACTIVE;
    }

    public Long getCardNumber() { return cardNumber; }
    public Long getCustomerId() { return customerId; }
    public CardType getCardType() { return cardType; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public BigDecimal getAvailableCredit() { return availableCredit; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public CardStatus getCardStatus() { return cardStatus; }

    public void setCardStatus(CardStatus cardStatus) { this.cardStatus = cardStatus; }

    public void updateDetails(CardType cardType, BigDecimal newLimit, LocalDate expiryDate) {
        BigDecimal difference = newLimit.subtract(this.creditLimit);
        this.availableCredit = this.availableCredit.add(difference);
        this.creditLimit = newLimit;
        this.cardType = cardType;
        this.expiryDate = expiryDate;
    }

    public void applyPurchase(BigDecimal amount) {
        this.availableCredit = this.availableCredit.subtract(amount);
        this.outstandingAmount = this.outstandingAmount.add(amount);
    }

    public void applyPayment(BigDecimal amount) {
        this.availableCredit = this.availableCredit.add(amount);
        this.outstandingAmount = this.outstandingAmount.subtract(amount);
    }
}
