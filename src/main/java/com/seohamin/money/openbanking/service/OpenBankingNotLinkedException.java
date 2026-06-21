package com.seohamin.money.openbanking.service;

/** 해당 user_seq_no로 연결된 토큰/계좌가 없을 때 던진다 (콜백 미완료 등). HTTP 404로 매핑. */
public class OpenBankingNotLinkedException extends RuntimeException {

    public OpenBankingNotLinkedException(String message) {
        super(message);
    }
}
