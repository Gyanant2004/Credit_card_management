package com.ofss.dto;

import com.ofss.entity.CardType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateCardRequest(
    @NotNull CardType cardType,
    @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal creditLimit,
    @NotNull @Future LocalDate expiryDate
) { }
