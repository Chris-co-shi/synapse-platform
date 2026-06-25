package com.indigo.synapse.iam.identity.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.indigo.synapse.iam.identity.domain.model.UserCredential;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * IAM 用户认证凭据 MyBatis-Plus 持久化实体。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("iam_user_credential")
public class IamUserCredentialEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    @TableField("password_hash")
    private String credentialHash;

    @TableField("password_changed_at")
    private Instant changedAt;

    private Integer failedAttempts;

    private Instant lockedUntil;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

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
        entity.setVersion(source.version());
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
        return new UserCredential(id, userId, credentialHash, changedAt, failedAttempts,
                lockedUntil, version, createdAt, updatedAt);
    }
}
