package com.indigo.synapse.iam.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.indigo.synapse.iam.identity.infrastructure.persistence.entity.IamUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * IAM 用户 MyBatis-Plus Mapper。
 */
@Mapper
public interface IamUserMapper extends BaseMapper<IamUserEntity> {
}
