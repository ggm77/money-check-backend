package com.seohamin.money.global.exception.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "필요한 값이 비어있습니다."),
    INVALID_OAUTH_STATE(HttpStatus.BAD_REQUEST, "유효하지 않은 OAuth state 입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token 입니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),

    OPENBANKING_NOT_LINKED(HttpStatus.NOT_FOUND, "연동된 오픈뱅킹 계좌/토큰이 없습니다."),
    OPENBANKING_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 다른 사용자에게 연동된 오픈뱅킹 계정입니다."),

    OPENBANKING_API_ERROR(HttpStatus.BAD_GATEWAY, "오픈뱅킹 API 호출에 실패했습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 에러가 발생했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
