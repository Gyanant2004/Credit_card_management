package com.ofss.repository;

import com.ofss.entity.CreditCard;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {
    List<CreditCard> findByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CreditCard c where c.cardNumber = :cardNumber")
    Optional<CreditCard> findForUpdate(@Param("cardNumber") Long cardNumber);
    boolean existsByCardNumber(Long cardNumber);
    Optional<CreditCard> findByCardNumber(Long cardNumber);
}
