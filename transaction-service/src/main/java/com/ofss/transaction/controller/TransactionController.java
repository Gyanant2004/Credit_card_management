package com.ofss.transaction.controller;

import java.net.URI;
import java.util.List;

import com.ofss.transaction.dto.AmountReport;
import com.ofss.transaction.dto.PaymentRequest;
import com.ofss.transaction.entity.Transaction;
import com.ofss.transaction.exception.ApiException;
import com.ofss.transaction.service.PaymentService;
import com.ofss.transaction.service.TransactionReportService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionReportService reports;
    private final PaymentService payments;

    public TransactionController(
            TransactionReportService reports,
            PaymentService payments
    ) {
        this.reports = reports;
        this.payments = payments;
    }

    // Report 4: complete transaction history.
    @GetMapping
    public List<Transaction> history() {
        return reports.history();
    }

    // Retrieve one transaction.
    @GetMapping("/{transactionId}")
    public Transaction getById(@PathVariable Long transactionId) {

        if (transactionId <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Transaction ID must be positive"
            );
        }

        return reports.getById(transactionId);
    }

    // Process and record a payment.
    @PostMapping("/payments")
    public ResponseEntity<Transaction> pay(
            @Valid @RequestBody PaymentRequest request
    ) {
        Transaction transaction = payments.pay(request);

        return ResponseEntity.created(
                URI.create(
                        "/api/transactions/"
                                + transaction.getTransactionId()
                )
        ).body(transaction);
    }

    // Report 11.
    @GetMapping("/reports/purchases/today")
    public AmountReport purchasesToday() {
        return reports.purchasesToday();
    }

    // Report 12.
    @GetMapping("/reports/payments/today")
    public AmountReport paymentsToday() {
        return reports.paymentsToday();
    }

    // Report 18.
    @GetMapping("/reports/purchases/average")
    public AmountReport averagePurchase() {
        return reports.averagePurchase();
    }

    // Report 19.
    @GetMapping("/reports/purchases/largest")
    public Transaction largestPurchase() {
        return reports.largestPurchase();
    }
}