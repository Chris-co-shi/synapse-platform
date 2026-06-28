package com.indigo.synapse.iam.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * Bearer Token 提取工具。
 */
public final class BearerTokenExtractor {

    private static final String BEARER = "Bearer";

    private BearerTokenExtractor() {
    }

    public static String extract(HttpServletRequest request) {
        return extract(request.getHeader(HttpHeaders.AUTHORIZATION));
    }

    public static String extract(String authorization) {
        if (authorization == null) {
            return null;
        }
        String value = authorization.trim();
        if (value.length() <= BEARER.length()
                || !value.regionMatches(true, 0, BEARER, 0, BEARER.length())
                || !Character.isWhitespace(value.charAt(BEARER.length()))) {
            return null;
        }
        String token = value.substring(BEARER.length()).trim();
        return token.isBlank() ? null : token;
    }
}
