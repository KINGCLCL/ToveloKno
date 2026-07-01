package backend.backend.repository;

import backend.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户数据访问层。
 *
 * JpaRepository 已经提供了基础的增删改查方法，
 * 这里只补充用户模块需要的按用户名、邮箱查询能力。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // 登录时根据用户名查找用户。
    Optional<User> findByUsername(String username);

    // 注册时判断用户名是否已经被占用。
    boolean existsByUsername(String username);

    // 注册时判断邮箱是否已经被占用。
    boolean existsByEmail(String email);

    // 修改资料时判断邮箱是否被其他用户占用。
    boolean existsByEmailAndIdNot(String email, Long id);
}
