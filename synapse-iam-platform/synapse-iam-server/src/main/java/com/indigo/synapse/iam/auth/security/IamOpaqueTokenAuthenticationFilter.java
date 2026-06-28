package com.indigo.synapse.iam.auth.security;

import com.indigo.synapse.iam.auth.AuthorizationSnapshot;
import com.indigo.synapse.iam.auth.application.AuthorizationSnapshotValidator;
import com.indigo.synapse.iam.auth.application.IamAuthInfrastructureException;
import com.indigo.synapse.iam.auth.application.IamTokenInvalidException;
import com.indigo.synapse.security.context.AuthenticatedPrincipal;
import com.indigo.synapse.security.context.internal.PrincipalContextBinder;
import com.indigo.synapse.security.context.internal.PrincipalContextScope;
import com.indigo.synapse.webmvc.exception.WebErrorResponseWriter;
import com.indigo.synapse.webmvc.exception.WebExceptionResponseFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * IAM Servlet Opaque Access Token 验证过滤器。
 */
public class IamOpaqueTokenAuthenticationFilter extends OncePerRequestFilter {

    private final List<String> permitPaths;
    private final AuthorizationSnapshotValidator snapshotValidator;
    private final AuthorizationSnapshotAuthenticationMapper authenticationMapper;
    private final WebExceptionResponseFactory responseFactory;
    private final WebErrorResponseWriter responseWriter;

    public IamOpaqueTokenAuthenticationFilter(
            List<String> permitPaths,
            AuthorizationSnapshotValidator snapshotValidator,
            AuthorizationSnapshotAuthenticationMapper authenticationMapper,
            WebExceptionResponseFactory responseFactory,
            WebErrorResponseWriter responseWriter) {
        this.permitPaths = List.copyOf(permitPaths);
        this.snapshotValidator = snapshotValidator;
        this.authenticationMapper = authenticationMapper;
        this.responseFactory = responseFactory;
        this.responseWriter = responseWriter;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return permitPaths.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = BearerTokenExtractor.extract(request);
        if (token == null) {
            write(response, new IamTokenInvalidException());
            return;
        }
        try {
            AuthorizationSnapshot snapshot = snapshotValidator.validate(token);
            OpaqueSnapshotAuthenticationToken authentication = authenticationMapper.toAuthentication(snapshot);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            AuthenticatedPrincipal principal = authentication.getPrincipal();
            try (PrincipalContextScope ignored = PrincipalContextBinder.bind(principal)) {
                filterChain.doFilter(request, response);
            }
        } catch (IamTokenInvalidException | IamAuthInfrastructureException ex) {
            SecurityContextHolder.clearContext();
            write(response, ex);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void write(HttpServletResponse response, RuntimeException exception) throws IOException {
        responseWriter.write(response, responseFactory.mvc(exception));
    }
}
