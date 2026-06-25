package com.indigo.synapse.iam.identity.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
}
