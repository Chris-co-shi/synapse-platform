package com.indigo.synapse.iam.identity.domain.repository;

import com.indigo.synapse.iam.identity.domain.model.UserCredential;

import java.util.Optional;

/**
 * 用户认证凭据仓储端口。
 */
public interface UserCredentialRepository {

    /**
     * 保存新增或已存在凭据。
     *
     * @param credential 凭据模型
     * @return 保存后的凭据模型
     */
    UserCredential save(UserCredential credential);

    /**
     * 按用户主键查询凭据。
     *
     * @param userId 用户主键
     * @return 凭据，不存在时为空
     */
    Optional<UserCredential> findByUserId(String userId);
}
