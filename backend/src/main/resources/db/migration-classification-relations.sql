-- 分类关系模块升级脚本
-- 仅用于已经执行过旧版 schema.sql 的数据库；每个数据库只执行一次。

USE tovelokno_db;

ALTER TABLE category
    DROP INDEX uk_category_name_parent,
    ADD COLUMN active TINYINT(1) NOT NULL DEFAULT 1 AFTER parent_id,
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER active,
    ADD KEY idx_category_owner_parent (created_by, parent_id, sort_order, name);

ALTER TABLE tag
    DROP INDEX name,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD KEY idx_tag_owner_name (created_by, name);
