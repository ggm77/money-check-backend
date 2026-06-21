package com.seohamin.money.openbanking.web.dto;

import java.util.List;

/** 연동 결과/계좌 목록 응답. */
public record LinkResult(String userSeqNo, List<AccountResponse> accounts) {}
