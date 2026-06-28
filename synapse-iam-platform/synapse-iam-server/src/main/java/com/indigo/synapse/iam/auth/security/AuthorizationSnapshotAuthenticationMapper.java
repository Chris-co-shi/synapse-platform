package com.indigo.synapse.iam.auth.security;

import com.indigo.synapse.iam.auth.AuthorizationSnapshot;
import com.indigo.synapse.iam.auth.TokenPrincipalType;
import com.indigo.synapse.iam.auth.application.IamTokenInvalidException;
import com.indigo.synapse.security.context.AuthenticatedClient;
import com.indigo.synapse.security.context.AuthenticatedPrincipal;
import com.indigo.synapse.security.context.AuthenticatedUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 授权快照到 Framework 认证主体的映射器。
 */
@Component
public class AuthorizationSnapshotAuthenticationMapper {

    /**
     * 创建 Spring Security Authentication。
     *
     * @param snapshot 授权快照
     * @return Authentication
     */
    public OpaqueSnapshotAuthenticationToken toAuthentication(AuthorizationSnapshot snapshot) {
        AuthenticatedPrincipal principal = toPrincipal(snapshot);
        return new OpaqueSnapshotAuthenticationToken(principal, snapshot, authorities(snapshot));
    }

    private AuthenticatedPrincipal toPrincipal(AuthorizationSnapshot snapshot) {
        if (snapshot.principalType() == TokenPrincipalType.USER) {
            return new AuthenticatedUser(
                    snapshot.subjectId(),
                    snapshot.displayName(),
                    snapshot.tenantId(),
                    snapshot.roles(),
                    snapshot.permissions());
        }
        if (snapshot.principalType() == TokenPrincipalType.CLIENT) {
            return new AuthenticatedClient(
                    snapshot.subjectId(),
                    snapshot.displayName(),
                    snapshot.tenantId(),
                    snapshot.roles(),
                    snapshot.permissions());
        }
        throw new IamTokenInvalidException();
    }

    private Collection<? extends GrantedAuthority> authorities(AuthorizationSnapshot snapshot) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        snapshot.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority(prefixed("ROLE_", role))));
        snapshot.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(prefixed("PERM_", permission))));
        return authorities;
    }

    private static String prefixed(String prefix, String value) {
        return value.startsWith(prefix) ? value : prefix + value;
    }
}
