package backend.backend.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 轻量补齐开发库字段，避免已有 MySQL 表缺少新个人主页字段时启动失败。
 */
@Configuration
public class DatabaseMigrationConfig {

    @Bean
    public ApplicationRunner ensureUserProfileColumns(JdbcTemplate jdbcTemplate) {
        return args -> {
            addColumnIfMissing(jdbcTemplate, "nickname", "VARCHAR(80)");
            addColumnIfMissing(jdbcTemplate, "bio", "VARCHAR(500)");
            addColumnIfMissing(jdbcTemplate, "profile_background", "VARCHAR(255)");
        };
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'user'
                  AND column_name = ?
                """,
                Integer.class,
                columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE user ADD COLUMN " + columnName + " " + definition);
        }
    }
}
