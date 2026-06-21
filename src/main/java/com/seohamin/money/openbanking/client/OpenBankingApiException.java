package com.seohamin.money.openbanking.client;

/** 오픈뱅킹 API 호출이 HTTP 오류이거나 rsp_code가 성공(A0000)이 아닐 때 던진다. */
public class OpenBankingApiException extends RuntimeException {

    private final String rspCode;

    public OpenBankingApiException(String message, String rspCode, String rspMessage) {
        super("%s (rsp_code=%s, rsp_message=%s)".formatted(message, rspCode, rspMessage));
        this.rspCode = rspCode;
    }

    public String getRspCode() {
        return rspCode;
    }
}
