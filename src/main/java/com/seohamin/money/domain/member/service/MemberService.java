package com.seohamin.money.domain.member.service;

import com.seohamin.money.domain.member.dto.MemberResponseDto;
import com.seohamin.money.domain.member.dto.UpdateEmailRequest;
import com.seohamin.money.domain.member.dto.UpdatePasswordRequest;
import com.seohamin.money.domain.member.entity.Member;
import com.seohamin.money.domain.member.repository.MemberRepository;
import com.seohamin.money.global.exception.CustomException;
import com.seohamin.money.global.exception.constants.ExceptionCode;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MemberResponseDto getMember(final Long memberId) {
        return MemberResponseDto.of(findMember(memberId));
    }

    @Transactional
    public void updateEmail(final Long memberId, final UpdateEmailRequest request) {
        final Member member = findMember(memberId);
        final String email = normalizeEmail(request.email());
        if (member.getEmail().equals(email)) {
            return;
        }
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(ExceptionCode.EMAIL_ALREADY_EXISTS);
        }
        member.updateEmail(email);
    }

    @Transactional
    public void updatePassword(final Long memberId, final UpdatePasswordRequest request) {
        final Member member = findMember(memberId);
        if (!passwordEncoder.matches(request.currentPassword(), member.getPasswordHash())) {
            throw new CustomException(ExceptionCode.INVALID_CURRENT_PASSWORD);
        }
        member.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void withdraw(final Long memberId) {
        final Member member = findMember(memberId);
        memberRepository.delete(member);
    }

    private Member findMember(final Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new CustomException(ExceptionCode.MEMBER_NOT_FOUND));
    }

    private static String normalizeEmail(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
