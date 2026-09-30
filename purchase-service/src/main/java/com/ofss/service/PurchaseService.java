package com.ofss.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

        // -------------------------------------------------
        // 1. Verify merchant exists
        // -------------------------------------------------

        Merchant merchant = merchantRepository.findById(
                request.getMerchantId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Merchant not found with ID: "
                                + request.getMerchantId()));

        Transaction transaction = new Transaction();

        transaction.setCardNumber(request.getCardNumber());
        transaction.setTransactionType("PURCHASE");
        transaction.setAmount(request.getPurchaseAmount());
        transaction.setMerchantId(merchant.getMerchantId());
        transaction.setTransactionDate(LocalDateTime.now());

        // -------------------------------------------------
        // 2. Get card information from Card Service
        // -------------------------------------------------

        CardDetails card;

        try {

            card = cardServiceClient
                    .get()
                    .uri("/api/cards/{cardNumber}",
                            request.getCardNumber())
                    .retrieve()
                    .body(CardDetails.class);

        } catch (RestClientException ex) {

            transaction.setStatus("FAILED");

            transactionRepository.save(transaction);

            throw new PurchaseException(
                    "Unable to communicate with Card Service");
        }

        // -------------------------------------------------
        // 3. Check card
        // -------------------------------------------------

        if (card == null) {

            transaction.setStatus("FAILED");

            transactionRepository.save(transaction);

            throw new PurchaseException(
                    "Card information could not be retrieved");
        }

        // -------------------------------------------------
        // 4. Check ACTIVE status
        // -------------------------------------------------

        if (!"ACTIVE".equalsIgnoreCase(card.getCardStatus())) {

            transaction.setStatus("FAILED");

            transactionRepository.save(transaction);

            throw new PurchaseException(
                    "Purchase is not allowed. Card is blocked.");
        }

        // -------------------------------------------------
        // 5. Check available credit
        // -------------------------------------------------

        if (card.getAvailableCredit()
                .compareTo(request.getPurchaseAmount()) < 0) {

            transaction.setStatus("FAILED");

            transactionRepository.save(transaction);

            throw new PurchaseException(
                    "Insufficient available credit");
        }

        // -------------------------------------------------
        // 6. Ask Card Service to process card balance
        // -------------------------------------------------

        try {

            cardServiceClient
                    .post()
                    .uri("/api/cards/{cardNumber}/purchase",
                            request.getCardNumber())
                    .body(request.getPurchaseAmount())
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException ex) {

            transaction.setStatus("FAILED");

            transactionRepository.save(transaction);

            throw new PurchaseException(
                    "Card update failed. Purchase was not completed.");
        }

        // -------------------------------------------------
        // 7. Record successful transaction
        // -------------------------------------------------

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