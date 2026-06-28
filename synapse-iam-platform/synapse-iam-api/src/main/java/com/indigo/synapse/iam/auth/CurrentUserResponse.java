package com.indigo.synapse.iam.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 当前用户接口响应。
 *
 * @param userId 用户主键
 * @param username 用户名
 * @param displayName 展示名称
 * @param tenantId 租户标识
 * @param roles 角色快照
 * @param permissions 权限快照
 */
public record CurrentUserResponse(
        String userId,
        String username,
        String displayName,
        String tenantId,
        Set<String> roles,
        Set<String> permissions
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
