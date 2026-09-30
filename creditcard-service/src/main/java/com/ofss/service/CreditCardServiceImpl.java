package com.ofss.service;

import com.ofss.dto.AmountRequest;
import com.ofss.dto.BalanceResponse;
import com.ofss.dto.CardResponse;
import com.ofss.dto.IssueCardRequest;
import com.ofss.dto.UpdateCardRequest;
import com.ofss.entity.CardStatus;
import com.ofss.entity.CreditCard;
import com.ofss.exception.CardNotFoundException;
import com.ofss.exception.CardOperationException;
import com.ofss.repository.CreditCardRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditCardServiceImpl implements CreditCardService {
    private final CreditCardRepository repository;

    public CreditCardServiceImpl(CreditCardRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CardResponse issue(IssueCardRequest request) {
        if (repository.existsByCardNumber(request.cardNumber())) {
            throw new CardOperationException("Card number already exists");
        }
        CreditCard card = new CreditCard(request.cardNumber(), request.customerId(),
            request.cardType(), request.creditLimit(), request.expiryDate());
        return CardResponse.from(repository.saveAndFlush(card));
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse get(Long cardNumber) {
        return CardResponse.from(repository.findByCardNumber(cardNumber)
            .orElseThrow(() ->
                new CardNotFoundException(String.valueOf(cardNumber))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> list(Long customerId) {
        List<CreditCard> cards = customerId == null
            ? repository.findAll() : repository.findByCustomerId(customerId);
        return cards.stream().map(CardResponse::from).toList();
    }

    @Override
    @Transactional
    public CardResponse update(Long cardNumber, UpdateCardRequest request) {
        CreditCard card = locked(cardNumber);
        BigDecimal limitChange = request.creditLimit().subtract(card.getCreditLimit());
        if (card.getAvailableCredit().add(limitChange).signum() < 0) {
            throw new CardOperationException("New credit limit is below the amount already used");
        }
        card.updateDetails(request.cardType(), request.creditLimit(), request.expiryDate());
        return CardResponse.from(repository.saveAndFlush(card));
    }

    @Override
    @Transactional
    public CardResponse block(Long cardNumber) {
        CreditCard card = locked(cardNumber);
        card.setCardStatus(CardStatus.BLOCKED);
        return CardResponse.from(repository.saveAndFlush(card));
    }

    @Override
    @Transactional
    public CardResponse unblock(Long cardNumber) {
        CreditCard card = locked(cardNumber);
        card.setCardStatus(CardStatus.ACTIVE);
        return CardResponse.from(repository.saveAndFlush(card));
    }

    @Override
    @Transactional
    public BalanceResponse purchase(Long cardNumber, AmountRequest request) {
        CreditCard card = locked(cardNumber);
        requireActive(card);
        if (card.getAvailableCredit().compareTo(request.amount()) < 0) {
            throw new CardOperationException("Insufficient available credit");
        }
        card.applyPurchase(request.amount());
        return BalanceResponse.from(repository.saveAndFlush(card));
    }

    @Override
    @Transactional
    public BalanceResponse payment(Long cardNumber, AmountRequest request) {
        CreditCard card = locked(cardNumber);
        requireActive(card);
        if (card.getOutstandingAmount().compareTo(request.amount()) < 0) {
            throw new CardOperationException("Payment exceeds outstanding amount");
        }
        card.applyPayment(request.amount());
        return BalanceResponse.from(repository.saveAndFlush(card));
    }

    private CreditCard locked(Long cardNumber) {
        return repository.findForUpdate(cardNumber)
            .orElseThrow(() -> new CardNotFoundException(String.valueOf(cardNumber)));
    }

    private void requireActive(CreditCard card) {
        if (card.getCardStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException("Card is blocked");
        }
    }
}
