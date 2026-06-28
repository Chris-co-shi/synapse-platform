package com.indigo.synapse.iam.auth.application;

import com.indigo.synapse.iam.auth.AuthorizationSnapshot;
import com.indigo.synapse.iam.auth.OpaqueTokenDigest;
import com.indigo.synapse.iam.auth.infrastructure.redis.AuthorizationSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Opaque Access Token 授权快照验证器。
 */
@Component
@RequiredArgsConstructor
public class AuthorizationSnapshotValidator {

    private final AuthorizationSnapshotRepository snapshotRepository;
    private final IamAuthProperties properties;
    private final Clock clock = Clock.systemUTC();

    /**
     * 验证原始 Access Token 并返回 Redis 授权快照。
     *
     * @param accessToken 原始 Access Token
     * @return 授权快照
     */
    public AuthorizationSnapshot validate(String accessToken) {
        String digest = OpaqueTokenDigest.sha256Hex(accessToken);
        AuthorizationSnapshot snapshot = snapshotRepository.findByDigest(digest)
                .orElseThrow(IamTokenInvalidException::new);
        if (!digest.equals(snapshot.tokenDigest())) {
            throw new IamTokenInvalidException();
        }
        if (!snapshot.activeAt(clock.instant())) {
            throw new IamTokenInvalidException();
        }
        if (!snapshot.matches(properties.getIssuer(), properties.getAudiences())) {
            throw new IamTokenInvalidException();
        }
        return snapshot;
    }
}
