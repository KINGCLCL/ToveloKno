package backend.backend.repository;

import backend.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 角色数据访问层。
 *
 * 注册普通用户时，需要从数据库中找到 USER 角色并绑定给新用户。
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    // 根据角色名称查询角色，例如 USER、ADMIN。
    Optional<Role> findByRoleName(String roleName);
}
