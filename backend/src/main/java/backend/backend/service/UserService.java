package backend.backend.service;

import backend.backend.auth.AuthTokenService;
import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.ForbiddenException;
import backend.backend.dto.ChangePasswordRequest;
import backend.backend.dto.LoginRequest;
import backend.backend.dto.LoginResponse;
import backend.backend.dto.RegisterRequest;
import backend.backend.dto.UpdateUserProfileRequest;
import backend.backend.dto.UserResponse;
import backend.backend.entity.Role;
import backend.backend.entity.User;
import backend.backend.repository.RoleRepository;
import backend.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenService authTokenService;
    private final OperationLogService operationLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuthTokenService authTokenService,
            OperationLogService operationLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authTokenService = authTokenService;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被使用");
        }

        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new IllegalStateException("默认用户角色不存在，请先执行数据库初始化脚本"));

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(email);
        user.getRoles().add(userRole);

        User savedUser = userRepository.save(user);
        operationLogService.record(savedUser.getId(), "USER_REGISTER", "用户注册：" + savedUser.getUsername());
        return toResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }

        operationLogService.record(user.getId(), "USER_LOGIN", "用户登录：" + user.getUsername());
        return new LoginResponse(authTokenService.createToken(user), "Bearer", toResponse(user));
    }

    public UserResponse getUserProfile(Long userId, AuthenticatedUser currentUser) {
        ensureSelfOrAdmin(userId, currentUser);
        return toResponse(findUserById(userId));
    }

    public UserResponse getCurrentUserProfile(AuthenticatedUser currentUser) {
        return toResponse(findUserById(currentUser.getId()));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateUserProfileRequest request, AuthenticatedUser currentUser) {
        ensureSelfOrAdmin(userId, currentUser);
        User user = findUserById(userId);
        String email = normalizeEmail(request.getEmail());

        if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new IllegalArgumentException("邮箱已被使用");
        }

        user.setEmail(email);
        user.setAvatar(normalizeText(request.getAvatar()));

        operationLogService.record(currentUser.getId(), "USER_UPDATE_PROFILE", "修改用户资料：" + user.getUsername());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateCurrentUserProfile(AuthenticatedUser currentUser, UpdateUserProfileRequest request) {
        return updateProfile(currentUser.getId(), request, currentUser);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request, AuthenticatedUser currentUser) {
        ensureSelfOrAdmin(userId, currentUser);
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        operationLogService.record(currentUser.getId(), "USER_CHANGE_PASSWORD", "修改密码：" + user.getUsername());
    }

    @Transactional
    public void changeCurrentUserPassword(AuthenticatedUser currentUser, ChangePasswordRequest request) {
        changePassword(currentUser.getId(), request, currentUser);
    }

    public User findUserEntityById(Long userId) {
        return findUserById(userId);
    }

    public UserResponse toResponse(User user) {
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

    private void ensureSelfOrAdmin(Long targetUserId, AuthenticatedUser currentUser) {
        if (!currentUser.getId().equals(targetUserId) && !currentUser.hasRole("ADMIN")) {
            throw new ForbiddenException("只能操作自己的数据");
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim();
    }

    private String normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }
}
