package com.seohamin.money.openbanking.web.dto;

/** 사용자인증(authorize) URL 응답. 이 URL을 브라우저 주소창에서 열어 동의를 진행한다. */
public record AuthorizeUrlResponse(String authorizeUrl) {}
