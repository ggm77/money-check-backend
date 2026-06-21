package com.seohamin.money.openbanking.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code GET /v2.0/account/balance/fin_num} (잔액조회) 응답.
 * 금액 필드(balance_amt/available_amt)는 KFTC가 문자열로 내려주므로 String으로 받고 상위에서 파싱한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BalanceApiResponse(
        @JsonProperty("api_tran_id") String apiTranId,
        @JsonProperty("rsp_code") String rspCode,
        @JsonProperty("rsp_message") String rspMessage,
        @JsonProperty("bank_tran_id") String bankTranId,
        @JsonProperty("bank_code_tran") String bankCodeTran,
        @JsonProperty("bank_rsp_code") String bankRspCode,
        @JsonProperty("bank_rsp_message") String bankRspMessage,
        @JsonProperty("bank_name") String bankName,
        @JsonProperty("balance_amt") String balanceAmt,
        @JsonProperty("available_amt") String availableAmt,
        @JsonProperty("account_type") String accountType,
        @JsonProperty("product_name") String productName,
        @JsonProperty("account_issue_date") String accountIssueDate,
        @JsonProperty("maturity_date") String maturityDate,
        @JsonProperty("last_tran_date") String lastTranDate) {}
