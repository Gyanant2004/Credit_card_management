package com.ofss.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ofss.dto.AmountRequest;
import com.ofss.dto.CardDetails;
import com.ofss.dto.PurchaseRequest;
import com.ofss.entity.Merchant;
import com.ofss.entity.Transaction;
import com.ofss.exception.PurchaseException;
import com.ofss.exception.ResourceNotFoundException;
import com.ofss.repository.MerchantRepository;
import com.ofss.repository.TransactionRepository;

@Service
public class PurchaseService {

    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final RestClient cardServiceClient;

    public PurchaseService(
            MerchantRepository merchantRepository,
            TransactionRepository transactionRepository,
            RestClient cardServiceClient) {

        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
        this.cardServiceClient = cardServiceClient;
    }

    public Transaction makePurchase(PurchaseRequest request) {

        // =====================================================
        // 1. Check whether merchant exists
        // =====================================================

        Merchant merchant = merchantRepository.findById(
                request.getMerchantId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Merchant not found with ID: "
                                + request.getMerchantId()
                )
        );

        // =====================================================
        // 2. Prepare transaction object
        // =====================================================

        Transaction transaction = new Transaction();

        transaction.setCardNumber(
                String.valueOf(request.getCardNumber())
        );

        transaction.setTransactionType("PURCHASE");
        transaction.setAmount(request.getPurchaseAmount());
        transaction.setMerchantId(merchant.getMerchantId());
        transaction.setTransactionDate(LocalDateTime.now());

        // =====================================================
        // 3. Get card details from Card Service
        // =====================================================

        CardDetails card;

        try {

            card = cardServiceClient
                    .get()
                    .uri(
                            "/api/cards/{cardNumber}",
                            request.getCardNumber()
                    )
                    .retrieve()
                    .body(CardDetails.class);

        } catch (RestClientException ex) {

            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);

            throw new PurchaseException(
                    "Unable to communicate with Card Service"
            );
        }

        // =====================================================
        // 4. Make sure card information was received
        // =====================================================

        if (card == null) {

            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);

            throw new PurchaseException(
                    "Card information could not be retrieved"
            );
        }

        // =====================================================
        // 5. Check card status
        // =====================================================

        if (!"ACTIVE".equalsIgnoreCase(card.getCardStatus())) {

            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);

            throw new PurchaseException(
                    "Purchase is not allowed. Card is blocked."
            );
        }

        // =====================================================
        // 6. Check available credit
        // =====================================================

        if (card.getAvailableCredit()
                .compareTo(request.getPurchaseAmount()) < 0) {

            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);

            throw new PurchaseException(
                    "Insufficient available credit"
            );
        }

        // =====================================================
        // 7. Tell Card Service to actually perform purchase
        // =====================================================

        try {

            AmountRequest amountRequest =
                    new AmountRequest(
                            request.getPurchaseAmount()
                    );

            cardServiceClient
                    .post()
                    .uri(
                            "/api/cards/{cardNumber}/purchase",
                            request.getCardNumber()
                    )
                    .body(amountRequest)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException ex) {

            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);

            throw new PurchaseException(
                    "Card update failed. Purchase was not completed."
            );
        }

        // =====================================================
        // 8. Record successful purchase
        // =====================================================

        transaction.setStatus("SUCCESS");

        return transactionRepository.save(transaction);
    }

    public Transaction getTransactionById(Long transactionId) {

        return transactionRepository.findById(transactionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Transaction not found with ID: "
                                        + transactionId));
    }

    public List<Transaction> getAllTransactions() {

        return transactionRepository.findAll();
    }

    public List<Transaction> getTransactionsByCard(
            String cardNumber) {

        return transactionRepository.findByCardNumber(cardNumber);
    }

    public List<Transaction> getTransactionsByMerchant(
            Long merchantId) {

        return transactionRepository.findByMerchantId(merchantId);
    }
}