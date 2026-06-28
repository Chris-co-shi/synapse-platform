package com.indigo.synapse.iam.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.indigo.synapse.iam.identity.domain.model.User;
import com.indigo.synapse.iam.identity.domain.model.UserStatus;
import com.indigo.synapse.iam.identity.domain.repository.UserRepository;
import com.indigo.synapse.iam.identity.infrastructure.persistence.entity.IamUserEntity;
import com.indigo.synapse.iam.identity.infrastructure.persistence.mapper.IamUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的用户仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class MybatisUserRepository implements UserRepository {

    private final IamUserMapper userMapper;

    @Override
    public User save(User user) {
        IamUserEntity entity = toEntity(user);
        if (user.id() == null || user.id().isBlank()) {
            userMapper.insert(entity);
            return toDomain(entity);
        }
        int updated = userMapper.updateById(entity);
        if (updated != 1) {
            throw new OptimisticLockingFailureException(
                    "IAM user update rejected because the persisted revision changed: " + user.id());
        }
        return toDomain(entity);
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(MybatisUserRepository::toDomain);
    }

    @Override
    public Optional<User> findByNormalizedUsername(String normalizedUsername) {
        IamUserEntity entity = userMapper.selectOne(Wrappers.<IamUserEntity>lambdaQuery()
                .eq(IamUserEntity::getNormalizedUsername, normalizedUsername));
        return Optional.ofNullable(entity).map(MybatisUserRepository::toDomain);
    }

    @Override
    public boolean existsByNormalizedUsername(String normalizedUsername) {
        return userMapper.exists(Wrappers.<IamUserEntity>lambdaQuery()
                .eq(IamUserEntity::getNormalizedUsername, normalizedUsername));
    }

    private static IamUserEntity toEntity(User user) {
        IamUserEntity entity = new IamUserEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setNormalizedUsername(user.normalizedUsername());
        entity.setDisplayName(user.displayName());
        entity.setStatus(user.status().name());
        entity.setRevision(user.revision());
        entity.setCreatedAt(user.createdAt());
        entity.setUpdatedAt(user.updatedAt());
        return entity;
    }

    private static User toDomain(IamUserEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getNormalizedUsername(),
                entity.getDisplayName(),
                UserStatus.valueOf(entity.getStatus()),
                entity.getRevision(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
