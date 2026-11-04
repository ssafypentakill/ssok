package com.pentakill.ssok.member.presentation;

import com.pentakill.ssok.member.application.MemberService;
import com.pentakill.ssok.member.domain.Member;
import com.pentakill.ssok.member.infrastructure.security.MemberPrincipal;
import com.pentakill.ssok.member.presentation.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(
            @Valid @RequestBody MemberRequest.Signup request) {
        ShippingAddressDto.Request address = request.getShippingAddress();
        Member member = memberService.signup(request.getEmail(), request.getPassword(),
                address.getRecipientName(), address.getRecipientPhone(),
                address.getPostalCode(), address.getAddress(), address.getAddressDetail());
        // 가입 후 자동 로그인하지 않는다.
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member));
    }

    @PostMapping("/login")
    public MemberResponse login(@Valid @RequestBody MemberRequest.Login body,
                                HttpServletRequest request, HttpServletResponse response) {
        Member member = memberService.authenticate(body.getEmail(), body.getPassword());
        MemberPrincipal principal = new MemberPrincipal(member.getId(), member.getEmail());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));

        // 기존 세션 ID 변경 및 기존 CSRF 토큰 제거 후 인증을 명시적으로 저장한다.
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return MemberResponse.from(member);
    }

    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal MemberPrincipal principal) {
        return MemberResponse.from(memberService.getMember(principal.memberId()));
    }

    /** POST 요청 전에 조회하고, 로그인/로그아웃 후에는 다시 조회한다. */
    @GetMapping("/csrf")
    public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.ok().cacheControl(
                org.springframework.http.CacheControl.noStore()).body(csrfToken);
    }
}
