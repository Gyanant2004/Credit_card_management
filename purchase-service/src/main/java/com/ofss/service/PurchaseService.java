package com.ofss.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import com.ofss.dto.AmountRequest;
import com.ofss.dto.BalanceResponse;
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
	private final RestClient.Builder cardServiceClientBuilder;

	public PurchaseService(
	        MerchantRepository merchantRepository,
	        TransactionRepository transactionRepository,
	        @LoadBalanced RestClient.Builder cardServiceClientBuilder) {

	    this.merchantRepository = merchantRepository;
	    this.transactionRepository = transactionRepository;
	    this.cardServiceClientBuilder = cardServiceClientBuilder;
	}

    // =========================================================
    // CREATE PURCHASE
    // =========================================================

    public Transaction makePurchase(PurchaseRequest request) {

        // -----------------------------------------------------
        // 1. Check whether merchant exists
        // -----------------------------------------------------

        Merchant merchant = merchantRepository.findById(
                request.getMerchantId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Merchant not found with ID: "
                                + request.getMerchantId()
                )
        );

        // -----------------------------------------------------
        // 2. Prepare transaction object
        // -----------------------------------------------------

        Transaction transaction = new Transaction();

        transaction.setCardNumber(
                String.valueOf(request.getCardNumber())
        );

        transaction.setTransactionType("PURCHASE");
        transaction.setAmount(request.getPurchaseAmount());
        transaction.setMerchantId(merchant.getMerchantId());
        transaction.setTransactionDate(LocalDateTime.now());

        // -----------------------------------------------------
        // 3. Get card details from Credit Card Service
        //    through Eureka + LoadBalancer
        // -----------------------------------------------------

        CardDetails card;

        try {

            card = cardServiceClientBuilder
                    .build()
                    .get()
                    .uri(
                            "http://CREDIT-CARD-SERVICE/api/cards/{cardNumber}",
                            request.getCardNumber()
                    )
                    .retrieve()
                    .body(CardDetails.class);

        } catch (RestClientResponseException ex) {

            // Card does not exist.
            // Do not save failed transaction because
            // TRANSACTION.CARD_NUMBER references CREDIT_CARD.

            if (ex.getStatusCode().value() == 404) {

                throw new ResourceNotFoundException(
                        "Card not found with number: "
                                + request.getCardNumber()
                );
            }

            // Other HTTP error returned by Card Service.

            saveFailedTransaction(transaction);

            throw new PurchaseException(
                    "Card Service returned an error: "
                            + ex.getStatusCode().value()
            );

        } catch (RestClientException ex) {

            // Card Service could not be reached.

            saveFailedTransaction(transaction);

            throw new PurchaseException(
                    "Unable to communicate with Card Service"
            );
        }

        // -----------------------------------------------------
        // 4. Check card response
        // -----------------------------------------------------

        if (card == null) {

            saveFailedTransaction(transaction);

            throw new PurchaseException(
                    "Card information could not be retrieved"
            );
        }

        // -----------------------------------------------------
        // 5. Check whether card is ACTIVE
        // -----------------------------------------------------

        if (!"ACTIVE".equalsIgnoreCase(card.getCardStatus())) {

            saveFailedTransaction(transaction);

            throw new PurchaseException(
                    "Purchase is not allowed. Card is blocked."
            );
        }

        // -----------------------------------------------------
        // 6. Check available credit
        // -----------------------------------------------------

        if (card.getAvailableCredit()
                .compareTo(request.getPurchaseAmount()) < 0) {

            saveFailedTransaction(transaction);

            throw new PurchaseException(
                    "Insufficient available credit"
            );
        }

        // -----------------------------------------------------
        // 7. Ask Credit Card Service to perform purchase
        //    through Eureka + LoadBalancer
        // -----------------------------------------------------

        try {

            AmountRequest amountRequest =
                    new AmountRequest(
                            request.getPurchaseAmount()
                    );

            BalanceResponse balanceResponse =
                    cardServiceClientBuilder
                            .build()
                            .post()
                            .uri(
                                    "http://CREDIT-CARD-SERVICE/api/cards/{cardNumber}/purchase",
                                    request.getCardNumber()
                            )
                            .body(amountRequest)
                            .retrieve()
                            .body(BalanceResponse.class);

            // -------------------------------------------------
            // 8. Check Card Service response
            // -------------------------------------------------

            if (balanceResponse == null
                    || !balanceResponse.success()) {

                saveFailedTransaction(transaction);

                throw new PurchaseException(
                        "Card Service could not complete the purchase"
                );
            }

        } catch (RestClientResponseException ex) {

            // Card disappeared between GET and POST.

            if (ex.getStatusCode().value() == 404) {

                throw new ResourceNotFoundException(
                        "Card not found with number: "
                                + request.getCardNumber()
                );
            }

            // Other HTTP error from Card Service.

            saveFailedTransaction(transaction);

            throw new PurchaseException(
                    "Card purchase operation failed: "
                            + ex.getStatusCode().value()
            );

        } catch (RestClientException ex) {

            // Communication error.

            saveFailedTransaction(transaction);

            throw new PurchaseException(
                    "Card update failed. Purchase was not completed."
            );
        }

        // -----------------------------------------------------
        // 9. Record successful purchase
        // -----------------------------------------------------

        transaction.setStatus("SUCCESS");

        return transactionRepository.save(transaction);
    }

    // =========================================================
    // GET TRANSACTION BY ID
    // =========================================================

    public Transaction getTransactionById(Long transactionId) {

        return transactionRepository.findById(transactionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Transaction not found with ID: "
                                        + transactionId
                        )
                );
    }

    // =========================================================
    // GET ALL TRANSACTIONS
    // =========================================================

    public List<Transaction> getAllTransactions() {

        return transactionRepository.findAll();
    }

    // =========================================================
    // GET TRANSACTIONS BY CARD
    // =========================================================

    public List<Transaction> getTransactionsByCard(
            String cardNumber) {

        return transactionRepository.findByCardNumber(
                cardNumber
        );
    }

    // =========================================================
    // GET TRANSACTIONS BY MERCHANT
    // =========================================================

    public List<Transaction> getTransactionsByMerchant(
            Long merchantId) {

        return transactionRepository.findByMerchantId(
                merchantId
        );
    }

    // =========================================================
    // SAVE FAILED TRANSACTION
    // =========================================================

    private void saveFailedTransaction(
            Transaction transaction) {

        transaction.setStatus("FAILED");

        transactionRepository.save(transaction);
    }
}