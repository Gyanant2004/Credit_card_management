package com.ofss.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ofss.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByCardNumber(String cardNumber);

    List<Transaction> findByMerchantId(Long merchantId);

}