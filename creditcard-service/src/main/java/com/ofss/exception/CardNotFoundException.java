package com.ofss.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String cardNumber) {
        super("Card not found: " + cardNumber);
    }
}
