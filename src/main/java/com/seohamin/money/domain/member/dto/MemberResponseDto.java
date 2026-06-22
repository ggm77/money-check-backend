package com.seohamin.money.domain.member.dto;

import com.seohamin.money.domain.member.entity.Member;
import java.time.Instant;

public record MemberResponseDto(
        Long id,
        String email,
        Instant createdAt
) {

    public static MemberResponseDto of(final Member member) {
        return new MemberResponseDto(
                member.getId(),
                member.getEmail(),
                member.getCreatedAt());
    }
}
