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
}
