package com.seohamin.money.openbanking.web.dto;

import com.seohamin.money.openbanking.domain.LinkedAccount;

/** 연동 계좌 응답. fintech_use_num은 잔액조회 키이므로 그대로 노출(데모). */
public record AccountResponse(
        String fintechUseNum,
        String bankCodeStd,
        String bankName,
        String accountAlias,
        String accountNumMasked,
        String accountHolderName) {

    public static AccountResponse from(LinkedAccount account) {
        return new AccountResponse(
                account.getFintechUseNum(),
                account.getBankCodeStd(),
                account.getBankName(),
                account.getAccountAlias(),
                account.getAccountNumMasked(),
                account.getAccountHolderName());
    }
}
