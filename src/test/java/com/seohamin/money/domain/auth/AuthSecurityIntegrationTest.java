package com.seohamin.money.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.seohamin.money.domain.member.entity.Member;
import com.seohamin.money.domain.member.repository.MemberRepository;
import com.seohamin.money.domain.openbanking.entity.LinkedAccount;
import com.seohamin.money.domain.openbanking.repository.LinkedAccountRepository;
import com.seohamin.money.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LinkedAccountRepository accountRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void signupReturnsJwt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "signup@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.refreshTokenExpiresInSeconds").value(1209600));
    }

    @Test
    void loginReturnsJwtForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void refreshTokenIssuesNewTokenPair() throws Exception {
        final String signupResponse = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "refresh@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        final String refreshToken = JsonPath.read(signupResponse, "$.refreshToken");

        final String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        final String renewedRefreshToken = JsonPath.read(refreshResponse, "$.refreshToken");

        assertThat(renewedRefreshToken).isNotEqualTo(refreshToken);
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() throws Exception {
        final Member member = memberRepository.save(Member.builder()
                .email("invalid-refresh@example.com")
                .passwordHash("unused")
                .build());
        final String accessToken = jwtTokenProvider.createAccessToken(member.getId());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refreshTokenCannotAccessProtectedApi() throws Exception {
        final Member member = memberRepository.save(Member.builder()
                .email("refresh-auth@example.com")
                .passwordHash("unused")
                .build());

        mockMvc.perform(get("/api/v1/accounts")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtTokenProvider.createRefreshToken(member.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountsRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void balanceRequiresFintechUseNum() throws Exception {
        final Member member = memberRepository.save(Member.builder()
                .email("balance-required@example.com")
                .passwordHash("unused")
                .build());

        mockMvc.perform(get("/api/v1/accounts/balance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(member.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void balanceRejectsBlankFintechUseNum() throws Exception {
        final Member member = memberRepository.save(Member.builder()
                .email("balance-blank@example.com")
                .passwordHash("unused")
                .build());

        mockMvc.perform(get("/api/v1/accounts/balance")
                        .queryParam("fintechUseNum", " ")
                        .header(HttpHeaders.AUTHORIZATION, bearer(member.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void swaggerDeclaresBearerAuthForAccounts() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                                "$['paths']['/api/v1/accounts']['get']['security'][0]['bearerAuth']")
                        .isArray());
    }

    @Test
    void memberCannotReadAnotherMembersAccounts() throws Exception {
        final Member owner = memberRepository.save(Member.builder()
                .email("owner@example.com")
                .passwordHash("unused")
                .build());
        final Member other = memberRepository.save(Member.builder()
                .email("other@example.com")
                .passwordHash("unused")
                .build());
        accountRepository.save(LinkedAccount.builder()
                .member(owner)
                .userSeqNo("1100000000")
                .fintechUseNum("1990000000000000000001")
                .bankName("테스트은행")
                .build());

        mockMvc.perform(get("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(other.getId())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].fintechUseNum")
                        .value("1990000000000000000001"));
    }

    private String bearer(final Long memberId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(memberId);
    }
}
