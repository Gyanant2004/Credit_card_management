package com.ofss.transaction.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.ofss.transaction.dto.AmountReport;
import com.ofss.transaction.entity.Transaction;
import com.ofss.transaction.exception.ApiException;
import com.ofss.transaction.repository.TransactionRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionReportService {

    private final TransactionRepository repository;
    private final ZoneId businessZone;

    public TransactionReportService(
            TransactionRepository repository,
            @Value("${app.business-zone}") String businessZone
    ) {
        this.repository = repository;
        this.businessZone = ZoneId.of(businessZone);
    }

    // Report 4: complete history, including SUCCESS and FAILED.
    public List<Transaction> history() {
        return repository
                .findAllByOrderByTransactionDateDescTransactionIdDesc();
    }

    public Transaction getById(Long transactionId) {
        return repository.findById(transactionId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found: " + transactionId
                ));
    }

    // Report 11: today's successful purchase total.
    public AmountReport purchasesToday() {
        return today(
                "PURCHASE",
                "Total purchase amount today"
        );
    }

    // Report 12: today's successful payment total.
    public AmountReport paymentsToday() {
        return today(
                "PAYMENT",
                "Total payment amount today"
        );
    }

    // Report 18: average of all successful purchases.
    public AmountReport averagePurchase() {
        return new AmountReport(
                "Average purchase transaction amount",
                null,
                businessZone.getId(),
                money(repository.averageSuccessfulPurchase())
        );
    }

    // Report 19: largest successful purchase.
    public Transaction largestPurchase() {
        return repository
                .findFirstByTransactionTypeAndStatusOrderByAmountDescTransactionIdAsc(
                        "PURCHASE",
                        "SUCCESS"
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "No successful purchase transactions found"
                ));
    }

    private AmountReport today(String type, String reportName) {

        LocalDate date = LocalDate.now(businessZone);

        BigDecimal total = repository.totalForPeriod(
                type,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );

        return new AmountReport(
                reportName,
                date,
                businessZone.getId(),
                money(total)
        );
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}