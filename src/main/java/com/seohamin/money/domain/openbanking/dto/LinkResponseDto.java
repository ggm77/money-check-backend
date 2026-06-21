package com.seohamin.money.domain.openbanking.dto;

import java.util.List;

/** 연동 결과/계좌 목록 응답. */
public record LinkResponseDto(String userSeqNo, List<AccountResponseDto> accounts) {}
