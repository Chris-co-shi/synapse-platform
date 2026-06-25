package com.indigo.synapse.iam.identity.domain.repository;

import com.indigo.synapse.iam.identity.domain.model.User;

import java.util.Optional;

/**
 * 用户身份主体仓储端口。
 */
public interface UserRepository {

    /**
     * 保存新增或已存在用户。
     *
     * @param user 用户模型
     * @return 保存后的用户模型
     */
    User save(User user);

    /**
     * 按主键查询用户。
     *
     * @param id 用户主键
     * @return 用户，不存在时为空
     */
    Optional<User> findById(String id);

    /**
     * 按规范化用户名查询用户。
     *
     * @param normalizedUsername 规范化用户名
     * @return 用户，不存在时为空
     */
    Optional<User> findByNormalizedUsername(String normalizedUsername);

    /**
     * 判断规范化用户名是否已经存在。
     *
     * @param normalizedUsername 规范化用户名
     * @return 是否存在
     */
    boolean existsByNormalizedUsername(String normalizedUsername);
}
