-- 题库练习、统计与设置模块升级脚本
-- 仅用于已经初始化过的数据库；每个数据库执行一次。

USE tovelokno_db;

ALTER TABLE answer_record
    MODIFY COLUMN user_answer VARCHAR(1000) NOT NULL,
    ADD COLUMN practice_mode VARCHAR(30) NOT NULL DEFAULT 'free' AFTER is_correct,
    ADD KEY idx_answer_user_time (user_id, answered_at);

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
    UNIQUE KEY uk_question_bank_setting_user (user_id),
    CONSTRAINT fk_question_bank_setting_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
