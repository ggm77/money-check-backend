package com.seohamin.money.openbanking.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.seohamin.money.config.OpenBankingProperties;
import com.seohamin.money.openbanking.client.dto.BalanceApiResponse;
import com.seohamin.money.openbanking.client.dto.TokenResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenBankingClientTest {

    private static final OpenBankingProperties PROPS = new OpenBankingProperties(
            "http://localhost:9999",
            "test-client-id",
            "test-client-secret",
            "http://localhost:8080/api/openbanking/callback",
            "M202500001",
            "login inquiry");

    private MockRestServiceServer server;
    private OpenBankingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPS.baseUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenBankingClient(builder.build(), PROPS);
    }

    @Test
    void issueToken_parsesAccessTokenAndUserSeqNo() {
        server.expect(requestTo("http://localhost:9999/oauth/2.0/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {
                          "access_token": "aaa.bbb.ccc",
                          "token_type": "Bearer",
                          "expires_in": 7776000,
                          "refresh_token": "rrr",
                          "scope": "login inquiry",
                          "user_seq_no": "1100000000"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        TokenResponse token = client.issueToken("auth-code-123");

        assertThat(token.accessToken()).isEqualTo("aaa.bbb.ccc");
        assertThat(token.userSeqNo()).isEqualTo("1100000000");
        assertThat(token.expiresIn()).isEqualTo(7776000L);
        server.verify();
    }

    @Test
    void getBalance_parsesBalanceFields() {
        server.expect(requestTo(Matchers.startsWith("http://localhost:9999/v2.0/account/balance/fin_num")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "api_tran_id": "abc",
                          "rsp_code": "A0000",
                          "rsp_message": "성공",
                          "bank_name": "오픈은행",
                          "balance_amt": "1000000",
                          "available_amt": "999000",
                          "account_type": "1",
                          "product_name": "자유입출금"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        BalanceApiResponse response =
                client.getBalance("1990000000000000000000", "M202500001U000000001", "20260101120000", "test-token");

        assertThat(response.rspCode()).isEqualTo("A0000");
        assertThat(response.balanceAmt()).isEqualTo("1000000");
        assertThat(response.bankName()).isEqualTo("오픈은행");
        server.verify();
    }

    @Test
    void getBalance_throwsOnNonSuccessRspCode() {
        server.expect(requestTo(Matchers.startsWith("http://localhost:9999/v2.0/account/balance/fin_num")))
                .andRespond(withSuccess(
                        """
                        { "rsp_code": "A0003", "rsp_message": "거래 실패" }
                        """,
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                        client.getBalance("1990000000000000000000", "M202500001U000000001", "20260101120000", "test-token"))
                .isInstanceOf(OpenBankingApiException.class)
                .hasMessageContaining("A0003");
    }
}
