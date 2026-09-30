package com.ofss.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ofss.dto.PurchaseRequest;
import com.ofss.entity.Transaction;
import com.ofss.service.PurchaseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<Transaction> makePurchase(
            @Valid @RequestBody PurchaseRequest request) {

        Transaction transaction =
                purchaseService.makePurchase(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transaction);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransaction(
            @PathVariable Long transactionId) {

        return ResponseEntity.ok(
                purchaseService.getTransactionById(
                        transactionId));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {

        return ResponseEntity.ok(
                purchaseService.getAllTransactions());
    }

    @GetMapping("/card/{cardNumber}")
    public ResponseEntity<List<Transaction>> getByCard(
            @PathVariable String cardNumber) {

        return ResponseEntity.ok(
                purchaseService.getTransactionsByCard(
                        cardNumber));
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<Transaction>> getByMerchant(
            @PathVariable Long merchantId) {

        return ResponseEntity.ok(
                purchaseService.getTransactionsByMerchant(
                        merchantId));
    }
}
