package com.seohamin.money.domain.member.controller;

import com.seohamin.money.domain.member.dto.UpdateEmailRequest;
import com.seohamin.money.domain.member.dto.UpdatePasswordRequest;
import com.seohamin.money.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @PatchMapping("/me/email")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> updateEmail(
            @AuthenticationPrincipal final Long memberId,
            @Valid @RequestBody final UpdateEmailRequest request
    ) {
        memberService.updateEmail(memberId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal final Long memberId,
            @Valid @RequestBody final UpdatePasswordRequest request
    ) {
        memberService.updatePassword(memberId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal final Long memberId) {
        memberService.withdraw(memberId);
        return ResponseEntity.noContent().build();
    }
}
