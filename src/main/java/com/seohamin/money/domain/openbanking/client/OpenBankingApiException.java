package com.seohamin.money.domain.openbanking.client;

import com.seohamin.money.global.exception.CustomException;
import com.seohamin.money.global.exception.constants.ExceptionCode;
import lombok.Getter;

/**
 * 오픈뱅킹 API 호출이 HTTP 오류이거나 rsp_code가 성공(A0000)이 아닐 때 던진다.
 * {@link CustomException}을 상속하므로 전역 핸들러가 OPENBANKING_API_ERROR(502)로 처리하며,
 * rsp_code/rsp_message는 cause 메시지로 보존되어 로그에 남는다.
 */
@Getter
public class OpenBankingApiException extends CustomException {

    private final String rspCode;

    public OpenBankingApiException(final String message, final String rspCode, final String rspMessage) {
        super(
                ExceptionCode.OPENBANKING_API_ERROR,
                new IllegalStateException("%s (rsp_code=%s, rsp_message=%s)".formatted(message, rspCode, rspMessage)));
        this.rspCode = rspCode;
    }
}
