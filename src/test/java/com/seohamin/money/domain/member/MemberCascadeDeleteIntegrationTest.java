package com.seohamin.money.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seohamin.money.domain.member.entity.Member;
import com.seohamin.money.domain.member.repository.MemberRepository;
import com.seohamin.money.domain.openbanking.entity.LinkedAccount;
import com.seohamin.money.domain.openbanking.entity.OpenBankingToken;
import com.seohamin.money.domain.openbanking.repository.LinkedAccountRepository;
import com.seohamin.money.domain.openbanking.repository.OpenBankingTokenRepository;
import com.seohamin.money.global.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberCascadeDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LinkedAccountRepository accountRepository;

    @Autowired
    private OpenBankingTokenRepository tokenRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void withdrawingMemberDeletesAccountsAndOpenBankingToken() throws Exception {
        final Member member = memberRepository.save(Member.builder()
                .email("cascade-delete@example.com")
                .passwordHash("password-hash")
                .build());
        final LinkedAccount account = accountRepository.save(LinkedAccount.builder()
                .member(member)
                .userSeqNo("1100000000")
                .fintechUseNum("1990000000000000000009")
                .bankName("테스트은행")
                .build());
        final OpenBankingToken token = tokenRepository.save(OpenBankingToken.builder()
                .member(member)
                .userSeqNo("1100000000")
                .accessToken("open-banking-access-token")
                .refreshToken("open-banking-refresh-token")
                .tokenType("Bearer")
                .scope("login inquiry")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/members/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtTokenProvider.createAccessToken(member.getId())))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        assertThat(memberRepository.existsById(member.getId())).isFalse();
        assertThat(accountRepository.existsById(account.getId())).isFalse();
        assertThat(tokenRepository.existsById(token.getId())).isFalse();
    }

    @Test
    void withdrawalRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me"))
                .andExpect(status().isUnauthorized());
    }
}
