package com.ofss.dto;

public record CardUsageResponse(
    CardResponse card,
    long purchaseCount
) {}