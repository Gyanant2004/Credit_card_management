package com.ofss.service;

import com.ofss.dto.AmountRequest;
import com.ofss.dto.BalanceResponse;
import com.ofss.dto.CardResponse;
import com.ofss.dto.IssueCardRequest;
import com.ofss.dto.UpdateCardRequest;
import java.util.List;

public interface CreditCardService {
    CardResponse issue(IssueCardRequest request);
    CardResponse get(Long cardNumber);
    List<CardResponse> list(Long customerId);
    CardResponse update(Long cardNumber, UpdateCardRequest request);
    CardResponse block(Long cardNumber);
    CardResponse unblock(Long cardNumber);
    BalanceResponse purchase(Long cardNumber, AmountRequest request);
    BalanceResponse payment(Long cardNumber, AmountRequest request);
}
