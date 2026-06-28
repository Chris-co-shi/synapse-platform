package com.indigo.synapse.iam.controller;

import com.indigo.synapse.iam.auth.CurrentUserResponse;
import com.indigo.synapse.iam.auth.LoginDTO;
import com.indigo.synapse.iam.auth.LogoutRequest;
import com.indigo.synapse.iam.auth.RefreshTokenRequest;
import com.indigo.synapse.iam.auth.TokenPairResponse;
import com.indigo.synapse.iam.auth.application.AuthApplicationService;
import com.indigo.synapse.iam.auth.security.BearerTokenExtractor;
import com.indigo.synapse.security.context.AuthenticatedPrincipal;
import com.indigo.synapse.security.context.AuthenticatedUser;
import com.indigo.synapse.security.context.CurrentPrincipalContext;
import com.indigo.synapse.web.core.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 史偕成
 * @date 2026/06/25 13:14
 **/
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return Opaque Token 响应
     */
    @PostMapping("/login")
    public Result<TokenPairResponse> login(@RequestBody LoginDTO request) {
        return Result.success(authApplicationService.login(request));
    }

    /**
     * Refresh Token rotation。
     *
     * @param request 刷新请求
     * @return 新 Opaque Token 响应
     */
    @PostMapping("/refresh")
    public Result<TokenPairResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return Result.success(authApplicationService.refresh(request));
    }

    /**
     * 当前用户。
     *
     * @return 当前用户响应
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        AuthenticatedPrincipal principal = CurrentPrincipalContext.currentPrincipal()
                .orElseThrow(IllegalStateException::new);
        if (!(principal instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("current principal is not a user");
        }
        return Result.success(authApplicationService.currentUser(user));
    }

    /**
     * 退出当前会话。
     *
     * @param request HTTP 请求
     * @param body 退出请求
     * @return 空响应
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, @RequestBody(required = false) LogoutRequest body) {
        String accessToken = BearerTokenExtractor.extract(request.getHeader(HttpHeaders.AUTHORIZATION));
        authApplicationService.logout(accessToken, body);
        return Result.success();
    }
}
