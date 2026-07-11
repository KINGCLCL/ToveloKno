package backend.backend.service;

import backend.backend.auth.AuthTokenService;
import backend.backend.auth.AuthenticatedUser;
import backend.backend.common.ForbiddenException;
import backend.backend.dto.ChangePasswordRequest;
import backend.backend.dto.LoginRequest;
import backend.backend.dto.LoginResponse;
import backend.backend.dto.ProfileImageUploadResponse;
import backend.backend.dto.RegisterRequest;
import backend.backend.dto.UpdateUserProfileRequest;
import backend.backend.dto.UserResponse;
import backend.backend.entity.Role;
import backend.backend.entity.User;
import backend.backend.repository.RoleRepository;
import backend.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 用户模块业务层。
 *
 * Controller 只负责接收请求和返回响应；注册、登录、权限校验、密码加密、
 * 操作日志记录等规则都放在 Service 中，便于后续复用和测试。
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenService authTokenService;
    private final OperationLogService operationLogService;
    private final Path uploadRoot;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuthTokenService authTokenService,
            OperationLogService operationLogService,
            @Value("${app.upload.profile-dir:uploads/profile}") String uploadDir) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authTokenService = authTokenService;
        this.operationLogService = operationLogService;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 注册普通用户。
     *
     * 主要步骤：清洗输入、检查用户名/邮箱重复、加密密码、绑定默认 USER 角色、记录操作日志。
     */
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

    /**
     * 用户登录。
     *
     * 校验用户名、密码和账号状态，通过后生成 token 并记录登录日志。
     */
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

    /**
     * 查询指定用户资料。
     *
     * 普通用户只能查自己，管理员可以查任意用户。
     */
    public UserResponse getUserProfile(Long userId, AuthenticatedUser currentUser) {
        ensureSelfOrAdmin(userId, currentUser);
        return toResponse(findUserById(userId));
    }

    /**
     * 查询当前登录用户资料。
     */
    public UserResponse getCurrentUserProfile(AuthenticatedUser currentUser) {
        return toResponse(findUserById(currentUser.getId()));
    }

    /**
     * 修改指定用户资料。
     *
     * 当前只开放邮箱和头像修改；用户名仍作为登录身份，不在这里改。
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateUserProfileRequest request, AuthenticatedUser currentUser) {
        ensureSelfOrAdmin(userId, currentUser);
        User user = findUserById(userId);
        String email = normalizeEmail(request.getEmail());

        if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new IllegalArgumentException("邮箱已被使用");
        }

        user.setEmail(email);
        user.setNickname(normalizeText(request.getNickname()));
        user.setBio(normalizeText(request.getBio()));
        user.setAvatar(normalizeText(request.getAvatar()));
        user.setProfileBackground(normalizeText(request.getProfileBackground()));
        user.setProfileSignature(normalizeText(request.getProfileSignature()));
        user.setProfileCoverText(normalizeText(request.getProfileCoverText()));
        user.setProfileAge(normalizeText(request.getProfileAge()));
        user.setProfileOccupation(normalizeText(request.getProfileOccupation()));
        user.setProfilePhone(normalizeText(request.getProfilePhone()));
        user.setProfileQq(normalizeText(request.getProfileQq()));

        operationLogService.record(currentUser.getId(), "USER_UPDATE_PROFILE", "修改用户资料：" + user.getUsername());
        return toResponse(userRepository.save(user));
    }

    /**
     * 上传当前用户的头像或个人主页背景图，并把 URL 写入用户资料。
     */
    @Transactional
    public ProfileImageUploadResponse uploadCurrentUserProfileImage(
            AuthenticatedUser currentUser,
            MultipartFile file,
            String type) {
        User user = findUserById(currentUser.getId());
        String url = saveProfileImage(file);
        String normalizedType = type == null ? "avatar" : type.trim().toLowerCase(Locale.ROOT);

        if ("background".equals(normalizedType)) {
            user.setProfileBackground(url);
        } else if ("avatar".equals(normalizedType)) {
            user.setAvatar(url);
        } else {
            throw new IllegalArgumentException("图片类型只能是 avatar 或 background");
        }

        User savedUser = userRepository.save(user);
        operationLogService.record(currentUser.getId(), "USER_UPLOAD_PROFILE_IMAGE", "上传个人主页图片：" + normalizedType);
        return new ProfileImageUploadResponse(url, toResponse(savedUser));
    }

    /**
     * 修改当前登录用户资料。
     */
    @Transactional
    public UserResponse updateCurrentUserProfile(AuthenticatedUser currentUser, UpdateUserProfileRequest request) {
        return updateProfile(currentUser.getId(), request, currentUser);
    }

    /**
     * 修改指定用户密码。
     *
     * 先校验旧密码，再保存新密码的 BCrypt 加密结果。
     */
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

    /**
     * 修改当前登录用户密码。
     */
    @Transactional
    public void changeCurrentUserPassword(AuthenticatedUser currentUser, ChangePasswordRequest request) {
        changePassword(currentUser.getId(), request, currentUser);
    }

    /**
     * 给其他业务 Service 复用的用户实体查询方法。
     *
     * 例如学习计划、错题本模块需要校验 user_id 是否存在时，可以调用它。
     */
    public User findUserEntityById(Long userId) {
        return findUserById(userId);
    }

    /**
     * 将 User 实体转换成安全的响应对象。
     *
     * 注意：这里不会返回 password 字段。
     */
    public UserResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .sorted()
                .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getBio(),
                user.getAvatar(),
                user.getProfileBackground(),
                user.getProfileSignature(),
                user.getProfileCoverText(),
                user.getProfileAge(),
                user.getProfileOccupation(),
                user.getProfilePhone(),
                user.getProfileQq(),
                user.getStatus(),
                roles,
                user.getCreatedAt()
        );
    }

    private String saveProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("只能上传图片文件");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("图片不能超过 5MB");
        }

        String extension = extensionFrom(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(uploadRoot);
            file.transferTo(uploadRoot.resolve(filename));
        } catch (IOException exception) {
            throw new IllegalStateException("图片保存失败");
        }
        return "/uploads/profile/" + filename;
    }

    private String extensionFrom(String originalFilename, String contentType) {
        Set<String> allowed = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                String extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
                if (allowed.contains(extension)) {
                    return extension;
                }
            }
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    // 限制普通用户只能操作自己的数据；管理员角色可以操作其他用户数据。
    private void ensureSelfOrAdmin(Long targetUserId, AuthenticatedUser currentUser) {
        if (!currentUser.getId().equals(targetUserId) && !currentUser.hasRole("ADMIN")) {
            throw new ForbiddenException("只能操作自己的数据");
        }
    }

    // 统一封装用户不存在时的错误提示。
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    // 把空邮箱统一处理成 null，避免数据库里出现没有意义的空字符串。
    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim();
    }

    // 把可选文本字段里的空字符串统一处理成 null。
    private String normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }
}
