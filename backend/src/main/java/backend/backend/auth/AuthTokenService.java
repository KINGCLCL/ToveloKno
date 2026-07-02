package backend.backend.auth;

import backend.backend.common.UnauthorizedException;
import backend.backend.entity.Role;
import backend.backend.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责生成和解析登录 token。
 *
 * 当前实现使用 JWT 的三段式结构，并用 HMAC-SHA256 签名。
 * 这样项目不用先引入完整 Spring Security，也能满足团队开发阶段的登录态需求。
 */
@Service
public class AuthTokenService {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    /**
     * tokenSecret 和过期时间来自 application.properties。
     *
     * 部署到正式环境前，tokenSecret 一定要换成更长、更随机的值。
     */
    public AuthTokenService(
            ObjectMapper objectMapper,
            @Value("${app.auth.token-secret:tovelokno-dev-secret-change-before-deploy}") String tokenSecret,
            @Value("${app.auth.token-expiration-hours:24}") long expirationHours) {
        this.objectMapper = objectMapper;
        this.secret = tokenSecret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationHours * 3600;
    }

    /**
     * 登录成功后生成 token。
     *
     * payload 里只放用户 id、用户名、角色和过期时间，不放密码、邮箱等敏感字段。
     */
    public String createToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .sorted()
                .toList();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getId());
        payload.put("username", user.getUsername());
        payload.put("roles", roles);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.getEpochSecond() + expirationSeconds);

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signingInput = headerPart + "." + payloadPart;

        return signingInput + "." + sign(signingInput);
    }

    /**
     * 解析并校验 token。
     *
     * 校验内容包括格式、签名和过期时间；通过后返回当前登录用户的轻量身份信息。
     */
    public AuthenticatedUser parseToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new UnauthorizedException("登录状态无效");
        }

        String signingInput = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(sign(signingInput).getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("登录状态无效");
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        long expiresAt = readLong(payload, "exp");
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new UnauthorizedException("登录已过期，请重新登录");
        }

        Long userId = readLong(payload, "sub");
        String username = String.valueOf(payload.get("username"));
        List<String> roles = readStringList(payload.get("roles"));
        return new AuthenticatedUser(userId, username, roles);
    }

    // JSON -> Base64URL，用来生成 JWT 的 header 和 payload。
    private String encodeJson(Map<String, Object> value) {
        try {
            return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Token生成失败", exception);
        }
    }

    // Base64URL -> JSON，用来读取 token 里的 payload。
    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] json = DECODER.decode(value);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new UnauthorizedException("登录状态无效");
        }
    }

    // 对 header.payload 做 HMAC 签名，防止 token 被前端或第三方篡改。
    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Token签名失败", exception);
        }
    }

    // ObjectMapper 反序列化数字时可能得到不同 Number 子类，这里统一转成 long。
    private long readLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new UnauthorizedException("登录状态无效");
    }

    // token 里角色字段异常时按空角色处理，避免解析阶段出现类型转换错误。
    private List<String> readStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(String::valueOf)
                .toList();
    }
}
