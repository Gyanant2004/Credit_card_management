package com.ofss.service;

import com.ofss.dto.AmountRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.ofss.dto.CardUsageResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class CreditCardServiceImpl implements CreditCardService {
	
    private static final Logger log = LoggerFactory.getLogger(CreditCardServiceImpl.class);
	
    private final CreditCardRepository repository;
    
    private String maskedCard(Long cardNumber) {
        String number = String.valueOf(cardNumber);
        return "****" + number.substring(Math.max(0, number.length() - 4));
    }

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
        CardStatus previousStatus = card.getCardStatus();

        card.setCardStatus(CardStatus.BLOCKED);

        CardResponse response =
            CardResponse.from(repository.saveAndFlush(card));

        log.info(
            "Card status update flushed: card={}, previousStatus={}, newStatus={}",
            maskedCard(cardNumber),
            previousStatus,
            CardStatus.BLOCKED
        );

        return response;
    }

    @Override
    @Transactional
    public CardResponse unblock(Long cardNumber) {
        CreditCard card = locked(cardNumber);
        CardStatus previousStatus = card.getCardStatus();

        card.setCardStatus(CardStatus.ACTIVE);

        CardResponse response =
            CardResponse.from(repository.saveAndFlush(card));

        log.info(
            "Card status update flushed: card={}, previousStatus={}, newStatus={}",
            maskedCard(cardNumber),
            previousStatus,
            CardStatus.ACTIVE
        );

        return response;
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
    
    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> blockedCards() {
    	 log.info("Blocked cards report requested");

    	    List<CardResponse> cards =
    	        repository.findByCardStatus(CardStatus.BLOCKED)
    	            .stream()
    	            .map(CardResponse::from)
    	            .toList();

    	    log.info("Blocked cards report returned {} cards", cards.size());

    	    return cards;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> cardsBelowTwentyPercent() {
        return repository.findCardsBelowTwentyPercent()
            .stream()
            .map(CardResponse::from)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardUsageResponse> mostUsedCards() {
        return cardsByUsage(true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardUsageResponse> leastUsedCards() {
        return cardsByUsage(false);
    }

    private List<CardUsageResponse> cardsByUsage(boolean mostUsed) {
        // Start with every issued card so unused cards have a count of zero.
        List<CreditCard> cards = repository.findAll();
        if (cards.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> counts = new HashMap<>();

        for (Object[] row : repository.countSuccessfulPurchasesByCard()) {
            Long cardNumber = Long.valueOf(row[0].toString());
            long count = ((Number) row[1]).longValue();
            counts.put(cardNumber, count);
        }

        long target = cards.stream()
            .mapToLong(card -> counts.getOrDefault(card.getCardNumber(), 0L))
            .reduce(mostUsed ? Long.MIN_VALUE : Long.MAX_VALUE,
                    mostUsed ? Math::max : Math::min);

        return cards.stream()
            .filter(card ->
                counts.getOrDefault(card.getCardNumber(), 0L) == target)
            .sorted((a, b) ->
                a.getCardNumber().compareTo(b.getCardNumber()))
            .map(card -> new CardUsageResponse(
                CardResponse.from(card),
                counts.getOrDefault(card.getCardNumber(), 0L)
            ))
            .toList();
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
