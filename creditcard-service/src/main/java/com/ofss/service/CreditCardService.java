package com.ofss.service;

import com.ofss.dto.AmountRequest;
import com.ofss.dto.BalanceResponse;
import com.ofss.dto.CardResponse;
import com.ofss.dto.IssueCardRequest;
import com.ofss.dto.UpdateCardRequest;
import java.util.List;
import com.ofss.dto.CardUsageResponse;


public interface CreditCardService {
    CardResponse issue(IssueCardRequest request);
    CardResponse get(Long cardNumber);
    List<CardResponse> list(Long customerId);
    List<CardResponse> blockedCards();
    List<CardResponse> cardsBelowTwentyPercent();
    List<CardUsageResponse> mostUsedCards();
    List<CardUsageResponse> leastUsedCards();
    CardResponse update(Long cardNumber, UpdateCardRequest request);
    CardResponse block(Long cardNumber);
    CardResponse unblock(Long cardNumber);
    BalanceResponse purchase(Long cardNumber, AmountRequest request);
    BalanceResponse payment(Long cardNumber, AmountRequest request);
}
