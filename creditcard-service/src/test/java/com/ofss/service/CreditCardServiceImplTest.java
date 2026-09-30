package com.ofss.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.ofss.dto.AmountRequest;
import com.ofss.entity.CardStatus;
import com.ofss.entity.CardType;
import com.ofss.entity.CreditCard;
import com.ofss.exception.CardOperationException;
import com.ofss.repository.CreditCardRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceImplTest {
    @Mock CreditCardRepository repository;
    private CreditCardServiceImpl service;
    private CreditCard card;

    @BeforeEach
    void setUp() {
        service = new CreditCardServiceImpl(repository);
        card = new CreditCard(123456789012L, 1L, CardType.GOLD,
            new BigDecimal("100000.00"), LocalDate.now().plusYears(2));
    }

    @Test
    void purchaseChangesBothBalances() {
        when(repository.findForUpdate(card.getCardNumber())).thenReturn(Optional.of(card));
        when(repository.saveAndFlush(card)).thenReturn(card);

        var result = service.purchase(card.getCardNumber(), new AmountRequest(new BigDecimal("5000.00")));

        assertEquals(new BigDecimal("95000.00"), result.availableCredit());
        assertEquals(new BigDecimal("5000.00"), result.outstandingAmount());
        verify(repository).saveAndFlush(card);
    }

    @Test
    void blockedCardCannotPurchase() {
        card.setCardStatus(CardStatus.BLOCKED);
        when(repository.findForUpdate(card.getCardNumber())).thenReturn(Optional.of(card));

        assertThrows(CardOperationException.class,
            () -> service.purchase(card.getCardNumber(), new AmountRequest(new BigDecimal("5000.00"))));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void insufficientCreditDoesNotChangeBalance() {
        when(repository.findForUpdate(card.getCardNumber())).thenReturn(Optional.of(card));

        assertThrows(CardOperationException.class,
            () -> service.purchase(card.getCardNumber(), new AmountRequest(new BigDecimal("100001.00"))));
        assertEquals(new BigDecimal("100000.00"), card.getAvailableCredit());
        verify(repository, never()).saveAndFlush(any());
    }
}
