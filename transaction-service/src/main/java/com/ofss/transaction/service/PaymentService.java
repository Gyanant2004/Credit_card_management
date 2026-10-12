package com.ofss.transaction.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import com.ofss.transaction.dto.AmountRequest;
import com.ofss.transaction.dto.BalanceResponse;
import com.ofss.transaction.dto.PaymentRequest;
import com.ofss.transaction.entity.Transaction;
import com.ofss.transaction.exception.ApiException;
import com.ofss.transaction.repository.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class PaymentService {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentService.class);

    private final TransactionRepository repository;
    private final RestClient cardClient;
    private final ZoneId businessZone;

    public PaymentService(
            TransactionRepository repository,
            @LoadBalanced RestClient.Builder builder,
            @Value("${app.card-service-url}") String cardServiceUrl,
            @Value("${app.business-zone}") String businessZone
    ) {
        this.repository = repository;

        this.cardClient = builder.clone()
                .baseUrl(cardServiceUrl)
                .build();

        this.businessZone = ZoneId.of(businessZone);
    }

    public Transaction pay(PaymentRequest request) {

        // Unknown cards cannot be recorded because of the foreign key.
        verifyCardExists(request.cardNumber());

        String reference = UUID.randomUUID().toString();

        BalanceResponse response;

        try {
            response = cardClient.post()
                    .uri(
                            "/api/cards/{cardNumber}/payment",
                            request.cardNumber()
                    )
                    .body(new AmountRequest(request.amount()))
                    .retrieve()
                    .body(BalanceResponse.class);

        } catch (RestClientResponseException ex) {

            int status = ex.getStatusCode().value();

            if (status == 404) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Card not found: " + request.cardNumber()
                );
            }

            // Current card service returns 409 for business rejection.
            if (status == 400 || status == 409) {

                Transaction failed =
                        saveOutcome(request, "FAILED", reference);

                throw new ApiException(
                        status == 409
                                ? HttpStatus.CONFLICT
                                : HttpStatus.BAD_REQUEST,
                        "Card service rejected payment. Failed transaction ID: "
                                + failed.getTransactionId()
                                + ". Check card status and outstanding balance."
                );
            }

            throw uncertain(request, reference, ex);

        } catch (RestClientException ex) {

            // A timeout may happen after the balance update commits.
            throw uncertain(request, reference, ex);
        }

        if (response == null
                || !response.success()
                || !request.cardNumber().equals(response.cardNumber())) {

            throw uncertain(request, reference, null);
        }

        return saveOutcome(request, "SUCCESS", reference);
    }

    private void verifyCardExists(Long cardNumber) {

        try {
            cardClient.get()
                    .uri("/api/cards/{cardNumber}", cardNumber)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException ex) {

            if (ex.getStatusCode().value() == 404) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Card not found: " + cardNumber
                );
            }

            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to verify card. Payment was not submitted."
            );

        } catch (RestClientException ex) {

            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to reach card service. Payment was not submitted."
            );
        }
    }

    private Transaction saveOutcome(
            PaymentRequest request,
            String status,
            String reference
    ) {
        Transaction transaction = new Transaction();

        transaction.setCardNumber(
                String.valueOf(request.cardNumber())
        );
        transaction.setTransactionType("PAYMENT");
        transaction.setAmount(request.amount());
        transaction.setMerchantId(null);
        transaction.setTransactionDate(
                LocalDateTime.now(businessZone)
        );
        transaction.setStatus(status);

        try {
            return repository.saveAndFlush(transaction);

        } catch (DataAccessException ex) {

            log.error(
                    "Payment recording failed. reference={}, card={}, "
                            + "amount={}, confirmedStatus={}",
                    reference,
                    request.cardNumber(),
                    request.amount(),
                    status,
                    ex
            );

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Payment outcome was " + status
                            + ", but its transaction record could not be saved. "
                            + "Do not repeat payment; reconcile reference: "
                            + reference
            );
        }
    }

    private ApiException uncertain(
            PaymentRequest request,
            String reference,
            Exception cause
    ) {
        log.error(
                "Payment outcome requires reconciliation. reference={}, "
                        + "card={}, amount={}",
                reference,
                request.cardNumber(),
                request.amount(),
                cause
        );

        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "Payment outcome is unknown. Do not repeat payment "
                        + "until checked. Reference: " + reference
        );
    }
}