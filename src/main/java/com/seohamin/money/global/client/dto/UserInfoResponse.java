package com.seohamin.money.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * {@code GET /v2.0/user/me} (사용자정보조회) 응답. res_list에 등록 계좌별 fintech_use_num이 담긴다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserInfoResponse(
        @JsonProperty("rsp_code") String rspCode,
        @JsonProperty("rsp_message") String rspMessage,
        @JsonProperty("user_seq_no") String userSeqNo,
        @JsonProperty("user_name") String userName,
        @JsonProperty("res_list") List<Account> resList) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Account(
            @JsonProperty("fintech_use_num") String fintechUseNum,
            @JsonProperty("account_alias") String accountAlias,
            @JsonProperty("bank_code_std") String bankCodeStd,
            @JsonProperty("bank_name") String bankName,
            @JsonProperty("account_num_masked") String accountNumMasked,
            @JsonProperty("account_holder_name") String accountHolderName,
            @JsonProperty("inquiry_agree_yn") String inquiryAgreeYn) {}
}
