package com.ofss.dto;

import com.ofss.entity.CardType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IssueCardRequest(
	@NotNull @Positive @Digits(integer = 12, fraction = 0) Long cardNumber,
    @NotNull @Positive Long customerId,
    @NotNull CardType cardType,
    @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal creditLimit,
    @NotNull @Future LocalDate expiryDate
) { }
