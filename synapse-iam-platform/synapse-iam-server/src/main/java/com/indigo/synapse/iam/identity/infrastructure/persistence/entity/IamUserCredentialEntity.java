package com.indigo.synapse.iam.identity.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.indigo.synapse.iam.identity.domain.model.UserCredential;
import com.indigo.synapse.mybatisplus.entity.VersionedEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * IAM 用户认证凭据 MyBatis-Plus 持久化实体。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@TableName("iam_user_credential")
public class IamUserCredentialEntity extends VersionedEntity {

    private String userId;

    @TableField("password_hash")
    private String credentialHash;

    @TableField("password_changed_at")
    private Instant changedAt;

    private Integer failedAttempts;

    private Instant lockedUntil;

    /**
     * 从领域模型创建持久化实体。
     *
     * @param source 领域模型
     * @return 持久化实体
     */
    public static IamUserCredentialEntity fromDomain(UserCredential source) {
        IamUserCredentialEntity entity = new IamUserCredentialEntity();
        entity.setId(source.id());
        entity.setUserId(source.userId());
        entity.setCredentialHash(source.credentialHash());
        entity.setChangedAt(source.changedAt());
        entity.setFailedAttempts(source.failedAttempts());
        entity.setLockedUntil(source.lockedUntil());
        entity.setRevision(source.revision());
        entity.setCreatedAt(source.createdAt());
        entity.setUpdatedAt(source.updatedAt());
        return entity;
    }

    /**
     * 转换为领域模型。
     *
     * @return 领域模型
     */
    public UserCredential toDomain() {
        return new UserCredential(getId(), userId, credentialHash, changedAt, failedAttempts,
                lockedUntil, getRevision(), getCreatedAt(), getUpdatedAt());
    }
}
