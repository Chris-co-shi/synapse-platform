package com.indigo.synapse.iam.identity.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.indigo.synapse.mybatisplus.entity.VersionedEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * IAM 用户 MyBatis-Plus 持久化实体。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@TableName("iam_user")
public class IamUserEntity extends VersionedEntity {

    private String username;

    private String normalizedUsername;

    private String displayName;

    private String status;
}
