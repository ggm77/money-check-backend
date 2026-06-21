package com.seohamin.money.domain.openbanking.dto;

import com.seohamin.money.domain.openbanking.client.dto.BalanceApiResponse;

/** 잔액조회 응답. 금액은 long으로 파싱해 제공한다. */
public record BalanceResponseDto(
        String fintechUseNum,
        String bankName,
        Long balanceAmt,
        Long availableAmt,
        String accountType,
        String productName,
        String apiTranId,
        String rspCode,
        String rspMessage) {

    public static BalanceResponseDto of(final BalanceApiResponse response, final String fintechUseNum) {
        return new BalanceResponseDto(
                fintechUseNum,
                response.bankName(),
                parseLong(response.balanceAmt()),
                parseLong(response.availableAmt()),
                response.accountType(),
                response.productName(),
                response.apiTranId(),
                response.rspCode(),
                response.rspMessage());
    }

    private static Long parseLong(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
