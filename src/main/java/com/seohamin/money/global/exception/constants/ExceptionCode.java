package com.seohamin.money.global.exception.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "필요한 값이 비어있습니다."),
    INVALID_OAUTH_STATE(HttpStatus.BAD_REQUEST, "유효하지 않은 OAuth state 입니다."),

    OPENBANKING_NOT_LINKED(HttpStatus.NOT_FOUND, "연동된 오픈뱅킹 계좌/토큰이 없습니다."),

    OPENBANKING_API_ERROR(HttpStatus.BAD_GATEWAY, "오픈뱅킹 API 호출에 실패했습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 에러가 발생했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
