package backend.backend.service;

import backend.backend.dto.LoginRequest;
import backend.backend.dto.RegisterRequest;
import backend.backend.dto.UserResponse;
import backend.backend.entity.Role;
import backend.backend.entity.User;
import backend.backend.repository.RoleRepository;
import backend.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户业务层。
 *
 * Controller 只负责接收请求，真正的注册、登录规则都放在 Service 中，
 * 这样后面无论是网页端还是移动端调用，都能复用同一套业务逻辑。
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // BCrypt 会自动加盐，比直接保存明文密码安全很多。
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * 注册用户。
     *
     * 主要步骤：清理输入、检查重复、加密密码、绑定默认 USER 角色、保存数据库。
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());

        // 用户名必须唯一，否则登录时无法确定是哪一个用户。
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 邮箱允许为空；如果填写了，也要求不能重复。
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被使用");
        }

        // 默认角色来自数据库初始化脚本，缺失时说明数据库还没有准备好。
        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new IllegalStateException("默认用户角色不存在，请先执行数据库初始化脚本"));

        User user = new User();
        user.setUsername(username);
        // 密码入库前必须加密，登录时再用 matches 进行校验。
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(email);
        user.getRoles().add(userRole);

        return toResponse(userRepository.save(user));
    }

    /**
     * 用户登录。
     *
     * 这里先做最小登录闭环：校验用户名、密码和账号状态。
     * 后面如果加入 JWT 或 Session，可以在这里生成登录凭证。
     */
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        // BCrypt 的 matches 会把明文密码和数据库中的加密密码进行安全比较。
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 管理员禁用账号后，用户不能继续登录。
        if (user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }

        return toResponse(user);
    }

    // 把空字符串邮箱统一处理成 null，避免数据库里出现没有意义的空值。
    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim();
    }

    // 把 Entity 转成 Response，控制哪些字段可以返回给前端。
    private UserResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .sorted()
                .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                user.getStatus(),
                roles,
                user.getCreatedAt()
        );
    }
}
