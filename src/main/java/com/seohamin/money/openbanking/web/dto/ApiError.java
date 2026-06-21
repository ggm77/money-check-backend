package com.seohamin.money.openbanking.web.dto;

/** 오류 응답 본문. rspCode는 오픈뱅킹 업스트림 오류일 때만 채워진다. */
public record ApiError(String error, String message, String rspCode) {}
