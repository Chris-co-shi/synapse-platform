package com.indigo.synapse.iam.auth.security;

import com.indigo.synapse.iam.auth.AuthorizationSnapshot;
import com.indigo.synapse.security.context.AuthenticatedPrincipal;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 基于 Redis 授权快照建立的 Spring Security Authentication。
 */
public class OpaqueSnapshotAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedPrincipal principal;
    private final AuthorizationSnapshot snapshot;

    public OpaqueSnapshotAuthenticationToken(
            AuthenticatedPrincipal principal,
            AuthorizationSnapshot snapshot,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.snapshot = snapshot;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    public AuthorizationSnapshot snapshot() {
        return snapshot;
    }
}
