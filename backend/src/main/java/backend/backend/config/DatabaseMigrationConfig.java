package backend.backend.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;

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
            createLearningResourceTableIfMissing(jdbcTemplate);
        };
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String columnName, String definition) {
        Boolean exists = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            try (ResultSet columns = connection.getMetaData().getColumns(
                    connection.getCatalog(), null, null, null)) {
                while (columns.next()) {
                    if ("user".equalsIgnoreCase(columns.getString("TABLE_NAME"))
                            && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
                return false;
            }
        });
        if (!Boolean.TRUE.equals(exists)) {
            jdbcTemplate.execute("ALTER TABLE user ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void createLearningResourceTableIfMissing(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS learning_resource (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    name VARCHAR(180) NOT NULL,
                    type VARCHAR(40),
                    mime_type VARCHAR(120),
                    original_filename VARCHAR(260) NOT NULL,
                    stored_filename VARCHAR(260) NOT NULL,
                    file_url VARCHAR(500) NOT NULL,
                    file_size BIGINT NOT NULL,
                    source VARCHAR(80),
                    description VARCHAR(800),
                    favorite BOOLEAN NOT NULL DEFAULT FALSE,
                    progress_percent INT NOT NULL DEFAULT 0,
                    current_page INT NOT NULL DEFAULT 1,
                    total_pages INT NOT NULL DEFAULT 0,
                    learned_minutes INT NOT NULL DEFAULT 0,
                    annotation_count INT NOT NULL DEFAULT 0,
                    annotations_json TEXT,
                    last_studied_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
    }
}
