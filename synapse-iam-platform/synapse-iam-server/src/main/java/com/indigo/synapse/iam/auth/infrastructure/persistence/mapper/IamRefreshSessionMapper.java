package com.indigo.synapse.iam.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.indigo.synapse.iam.auth.infrastructure.persistence.entity.IamRefreshSessionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * IAM Refresh Token 会话 Mapper。
 */
@Mapper
public interface IamRefreshSessionMapper extends BaseMapper<IamRefreshSessionEntity> {
}
