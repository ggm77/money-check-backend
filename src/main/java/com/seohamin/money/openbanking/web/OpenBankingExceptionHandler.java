package com.seohamin.money.openbanking.web;

import com.seohamin.money.openbanking.client.OpenBankingApiException;
import com.seohamin.money.openbanking.service.OpenBankingNotLinkedException;
import com.seohamin.money.openbanking.web.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 오픈뱅킹 관련 예외를 일관된 ApiError(JSON)로 변환한다. */
@RestControllerAdvice
public class OpenBankingExceptionHandler {

    /** 연동(토큰/계좌)이 없으면 404. */
    @ExceptionHandler(OpenBankingNotLinkedException.class)
    public ResponseEntity<ApiError> handleNotLinked(OpenBankingNotLinkedException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("not_linked", e.getMessage(), null));
    }

    /** 업스트림(KFTC) 호출 오류는 502로 전달하고 rsp_code를 노출. */
    @ExceptionHandler(OpenBankingApiException.class)
    public ResponseEntity<ApiError> handleUpstream(OpenBankingApiException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("openbanking_error", e.getMessage(), e.getRspCode()));
    }
}
