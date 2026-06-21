package com.seohamin.money.domain.openbanking.dto;

import com.seohamin.money.domain.openbanking.entity.LinkedAccount;

/** 연동 계좌 응답. fintech_use_num은 잔액조회 키이므로 그대로 노출(데모). */
public record AccountResponseDto(
        String fintechUseNum,
        String bankCodeStd,
        String bankName,
        String accountAlias,
        String accountNumMasked,
        String accountHolderName) {

    public static AccountResponseDto of(final LinkedAccount account) {
        return new AccountResponseDto(
                account.getFintechUseNum(),
                account.getBankCodeStd(),
                account.getBankName(),
                account.getAccountAlias(),
                account.getAccountNumMasked(),
                account.getAccountHolderName());
    }
}
