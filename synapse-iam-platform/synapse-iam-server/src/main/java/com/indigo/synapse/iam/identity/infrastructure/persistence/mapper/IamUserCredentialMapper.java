package com.indigo.synapse.iam.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.indigo.synapse.iam.identity.infrastructure.persistence.entity.IamUserCredentialEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * IAM 用户认证凭据 MyBatis-Plus Mapper。
 */
@Mapper
public interface IamUserCredentialMapper extends BaseMapper<IamUserCredentialEntity> {
}
