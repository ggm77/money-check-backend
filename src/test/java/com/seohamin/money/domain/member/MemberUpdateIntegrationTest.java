package com.seohamin.money.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.seohamin.money.domain.member.entity.Member;
import com.seohamin.money.domain.member.repository.MemberRepository;
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
class MemberUpdateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void updatesEmail() throws Exception {
        final String accessToken = signup("before@example.com", "password123");

        mockMvc.perform(patch("/api/v1/members/me/email")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "After@Example.com"
                                }
                                """))
                .andExpect(status().isNoContent());

        assertThat(memberRepository.findByEmail("after@example.com")).isPresent();
        assertThat(memberRepository.findByEmail("before@example.com")).isEmpty();
    }

    @Test
    void rejectsDuplicatedEmail() throws Exception {
        signup("existing@example.com", "password123");
        final String accessToken = signup("member@example.com", "password123");

        mockMvc.perform(patch("/api/v1/members/me/email")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "existing@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void updatesPasswordAfterVerifyingCurrentPassword() throws Exception {
        final String accessToken = signup("password-change@example.com", "password123");

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "password123",
                                  "newPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isNoContent());

        login("password-change@example.com", "password123")
                .andExpect(status().isUnauthorized());
        login("password-change@example.com", "newPassword123")
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidCurrentPassword() throws Exception {
        final String accessToken = signup("wrong-password@example.com", "password123");

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "wrongPassword",
                                  "newPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
    }

    @Test
    void updateRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "anonymous@example.com"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String signup(final String email, final String password) throws Exception {
        final String response = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private org.springframework.test.web.servlet.ResultActions login(
            final String email,
            final String password
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password)));
    }

    private static String bearer(final String accessToken) {
        return "Bearer " + accessToken;
    }
}
