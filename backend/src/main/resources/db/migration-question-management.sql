-- 题目管理模块升级脚本
-- 仅用于已经执行过旧版 schema.sql 的数据库；全新数据库不需要再执行本文件。

USE tovelokno_db;

ALTER TABLE question
    MODIFY COLUMN question_type VARCHAR(30) NOT NULL
        COMMENT 'SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK, SHORT_ANSWER',
    MODIFY COLUMN correct_answer VARCHAR(1000) NOT NULL,
    ADD COLUMN subject VARCHAR(80) NULL AFTER difficulty,
    ADD COLUMN knowledge_point VARCHAR(80) NULL AFTER subject,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' AFTER knowledge_point,
    ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER created_by,
    ADD COLUMN deleted_at DATETIME NULL AFTER deleted;

-- 旧数据统一归为草稿；如需直接展示，可按实际情况改成 PUBLISHED。
UPDATE question
SET status = 'DRAFT'
WHERE status IS NULL OR status = '';

-- 将旧版小写题型编码升级为当前枚举编码。
UPDATE question
SET question_type = CASE LOWER(question_type)
    WHEN 'single_choice' THEN 'SINGLE_CHOICE'
    WHEN 'multiple_choice' THEN 'MULTIPLE_CHOICE'
    WHEN 'true_false' THEN 'TRUE_FALSE'
    WHEN 'fill_blank' THEN 'FILL_BLANK'
    WHEN 'short_answer' THEN 'SHORT_ANSWER'
    ELSE UPPER(question_type)
END;

ALTER TABLE question
    ADD KEY idx_question_owner_deleted_updated (created_by, deleted, updated_at),
    ADD KEY idx_question_filter (created_by, status, question_type, difficulty);
