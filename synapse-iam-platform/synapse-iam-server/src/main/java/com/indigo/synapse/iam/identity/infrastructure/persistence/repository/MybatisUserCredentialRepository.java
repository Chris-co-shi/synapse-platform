package com.indigo.synapse.iam.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.indigo.synapse.iam.identity.domain.model.UserCredential;
import com.indigo.synapse.iam.identity.domain.repository.UserCredentialRepository;
import com.indigo.synapse.iam.identity.infrastructure.persistence.entity.IamUserCredentialEntity;
import com.indigo.synapse.iam.identity.infrastructure.persistence.mapper.IamUserCredentialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的用户认证凭据仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class MybatisUserCredentialRepository implements UserCredentialRepository {

    private final IamUserCredentialMapper credentialMapper;

    @Override
    public UserCredential save(UserCredential source) {
        IamUserCredentialEntity entity = IamUserCredentialEntity.fromDomain(source);
        if (source.id() == null || source.id().isBlank()) {
            credentialMapper.insert(entity);
            return entity.toDomain();
        }
        int updated = credentialMapper.updateById(entity);
        if (updated != 1) {
            throw new OptimisticLockingFailureException(
                    "IAM authentication material update rejected because the persisted revision changed: " + source.id());
        }
        return entity.toDomain();
    }

    @Override
    public Optional<UserCredential> findByUserId(String userId) {
        IamUserCredentialEntity entity = credentialMapper.selectOne(
                Wrappers.<IamUserCredentialEntity>lambdaQuery()
                        .eq(IamUserCredentialEntity::getUserId, userId));
        return Optional.ofNullable(entity).map(IamUserCredentialEntity::toDomain);
    }
}
