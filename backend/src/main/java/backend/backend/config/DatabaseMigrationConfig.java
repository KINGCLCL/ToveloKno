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
            addColumnIfMissing(jdbcTemplate, "user", "nickname", "VARCHAR(80)");
            addColumnIfMissing(jdbcTemplate, "user", "bio", "VARCHAR(500)");
            addColumnIfMissing(jdbcTemplate, "user", "profile_background", "VARCHAR(255)");
            createLearningResourceTableIfMissing(jdbcTemplate);
            createHomeBannerTableIfMissing(jdbcTemplate);
            addQuestionCoreColumns(jdbcTemplate);
            addQuestionSoftDeleteColumns(jdbcTemplate);
            addQuestionSourceColumns(jdbcTemplate);
            addStudyPlanTargetColumns(jdbcTemplate);
            createQuestionBankInteractionTablesIfMissing(jdbcTemplate);
            addAnswerRecordColumns(jdbcTemplate);
        };
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String tableName, String columnName, String definition) {
        Boolean exists = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            try (ResultSet columns = connection.getMetaData().getColumns(
                    connection.getCatalog(), null, null, null)) {
                while (columns.next()) {
                    if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                            && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
                return false;
            }
        });
        if (!Boolean.TRUE.equals(exists)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void addQuestionSourceColumns(JdbcTemplate jdbcTemplate) {
        addColumnIfMissing(jdbcTemplate, "question", "source_type", "VARCHAR(40) NULL");
        addColumnIfMissing(jdbcTemplate, "question", "source_resource_id", "BIGINT NULL");
        addColumnIfMissing(jdbcTemplate, "question", "source_resource_name", "VARCHAR(180) NULL");
        addColumnIfMissing(jdbcTemplate, "question", "source_page", "INT NULL");
        addColumnIfMissing(jdbcTemplate, "question", "source_excerpt", "TEXT NULL");
    }

    private void addQuestionCoreColumns(JdbcTemplate jdbcTemplate) {
        addColumnIfMissing(jdbcTemplate, "question", "subject", "VARCHAR(80) NULL");
        addColumnIfMissing(jdbcTemplate, "question", "knowledge_point", "VARCHAR(80) NULL");
        addColumnIfMissing(jdbcTemplate, "question", "status", "VARCHAR(20) NOT NULL DEFAULT 'DRAFT'");
    }

    private void addQuestionSoftDeleteColumns(JdbcTemplate jdbcTemplate) {
        addColumnIfMissing(jdbcTemplate, "question", "deleted", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing(jdbcTemplate, "question", "deleted_at", "TIMESTAMP NULL");
    }

    private void addStudyPlanTargetColumns(JdbcTemplate jdbcTemplate) {
        addColumnIfMissing(jdbcTemplate, "study_plan", "target_type", "VARCHAR(40) NULL");
        addColumnIfMissing(jdbcTemplate, "study_plan", "target_id", "BIGINT NULL");
        addColumnIfMissing(jdbcTemplate, "study_plan", "target_title", "VARCHAR(180) NULL");
    }

    private void addAnswerRecordColumns(JdbcTemplate jdbcTemplate) {
        addColumnIfMissing(jdbcTemplate, "answer_record", "practice_mode", "VARCHAR(30) NOT NULL DEFAULT 'free'");
        addIndexIfMissing(jdbcTemplate, "answer_record", "idx_answer_user_time", "CREATE INDEX idx_answer_user_time ON answer_record (user_id, answered_at)");
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

    private void createHomeBannerTableIfMissing(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS home_banner (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    banner_key VARCHAR(80) NOT NULL UNIQUE,
                    title VARCHAR(150) NOT NULL,
                    intro_text VARCHAR(300),
                    image_url VARCHAR(500),
                    sort_order INT NOT NULL DEFAULT 0,
                    updated_by BIGINT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
    }

    private void createQuestionBankInteractionTablesIfMissing(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS answer_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    question_id BIGINT NOT NULL,
                    user_answer VARCHAR(1000) NOT NULL,
                    is_correct TINYINT NOT NULL DEFAULT 0,
                    practice_mode VARCHAR(30) NOT NULL DEFAULT 'free',
                    answered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_answer_user_time (user_id, answered_at)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wrong_question (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    question_id BIGINT NOT NULL,
                    wrong_count INT NOT NULL DEFAULT 1,
                    mastered TINYINT NOT NULL DEFAULT 0,
                    last_wrong_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    last_reviewed_at DATETIME,
                    UNIQUE KEY uk_wrong_question (user_id, question_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS question_bank_setting (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    bank_name VARCHAR(100) NOT NULL DEFAULT '我的题库',
                    description VARCHAR(500),
                    member_edit TINYINT(1) NOT NULL DEFAULT 1,
                    member_export TINYINT(1) NOT NULL DEFAULT 1,
                    review_required TINYINT(1) NOT NULL DEFAULT 0,
                    practice_count INT NOT NULL DEFAULT 10,
                    default_difficulty VARCHAR(20) NOT NULL DEFAULT '中等',
                    sort_mode VARCHAR(20) NOT NULL DEFAULT '随机排序',
                    show_answer VARCHAR(20) NOT NULL DEFAULT '立即显示',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_question_bank_setting_user (user_id)
                )
                """);
    }

    private void addIndexIfMissing(JdbcTemplate jdbcTemplate, String tableName, String indexName, String createSql) {
        Boolean exists = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            try (ResultSet indexes = connection.getMetaData().getIndexInfo(
                    connection.getCatalog(), null, tableName, false, false)) {
                while (indexes.next()) {
                    if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                        return true;
                    }
                }
                return false;
            }
        });
        if (!Boolean.TRUE.equals(exists)) {
            jdbcTemplate.execute(createSql);
        }
    }
}
