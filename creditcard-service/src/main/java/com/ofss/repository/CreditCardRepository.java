package com.ofss.repository;

import com.ofss.entity.CreditCard;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ofss.entity.CardStatus;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {
    List<CreditCard> findByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CreditCard c where c.cardNumber = :cardNumber")
    Optional<CreditCard> findForUpdate(@Param("cardNumber") Long cardNumber);
    boolean existsByCardNumber(Long cardNumber);
    Optional<CreditCard> findByCardNumber(Long cardNumber);
    
 // Report 13
    List<CreditCard> findByCardStatus(CardStatus status);

    // Report 14: strictly below 20%
    @Query("select c from CreditCard c " +
           "where c.availableCredit < c.creditLimit * 0.20")
    List<CreditCard> findCardsBelowTwentyPercent();

    // Reports 9 and 10: count only successful, recorded purchases
    @Query(value = """
        SELECT t.CARD_NUMBER, COUNT(*)
        FROM TRANSACTION t
        WHERE t.TRANSACTION_TYPE = 'PURCHASE'
          AND t.STATUS = 'SUCCESS'
        GROUP BY t.CARD_NUMBER
        """, nativeQuery = true)
    List<Object[]> countSuccessfulPurchasesByCard();
}
