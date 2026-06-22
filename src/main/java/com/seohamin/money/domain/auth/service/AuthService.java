package com.seohamin.money.domain.auth.service;

import com.seohamin.money.domain.auth.dto.AuthTokenResponse;
import com.seohamin.money.domain.auth.dto.LoginRequest;
import com.seohamin.money.domain.auth.dto.RefreshTokenRequest;
import com.seohamin.money.domain.auth.dto.SignupRequest;
import com.seohamin.money.domain.member.entity.Member;
import com.seohamin.money.domain.member.repository.MemberRepository;
import com.seohamin.money.global.exception.CustomException;
import com.seohamin.money.global.exception.constants.ExceptionCode;
import com.seohamin.money.global.security.JwtTokenProvider;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthTokenResponse signup(final SignupRequest request) {
        final String email = normalizeEmail(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(ExceptionCode.EMAIL_ALREADY_EXISTS);
        }

        final Member member = memberRepository.save(Member.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build());
        return issueToken(member);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(final LoginRequest request) {
        final Member member = memberRepository
                .findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new CustomException(ExceptionCode.INVALID_CREDENTIALS);
        }
        return issueToken(member);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse refresh(final RefreshTokenRequest request) {
        final Long memberId = jwtTokenProvider
                .parseRefreshToken(request.refreshToken())
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_REFRESH_TOKEN));
        final Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_REFRESH_TOKEN));
        return issueToken(member);
    }

    private AuthTokenResponse issueToken(final Member member) {
        return AuthTokenResponse.bearer(
                jwtTokenProvider.createAccessToken(member.getId()),
                jwtTokenProvider.createRefreshToken(member.getId()),
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                jwtTokenProvider.getRefreshTokenExpirationSeconds());
    }

    private static String normalizeEmail(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
