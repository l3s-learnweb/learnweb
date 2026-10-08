-- Replace the polymorphic `target_id` with one column per context id. Several can be set per row (e.g. a resource, its folder and its group).
-- There are intentionally no foreign keys: log entries have to outlive the entities they reference.
ALTER TABLE `lw_user_log` ADD (
    `resource_id` INT(10) UNSIGNED DEFAULT NULL,
    `folder_id` INT(10) UNSIGNED DEFAULT NULL,
    `topic_id` INT(10) UNSIGNED DEFAULT NULL,
    `post_id` INT(10) UNSIGNED DEFAULT NULL,
    `course_id` INT(10) UNSIGNED DEFAULT NULL,
    `target_user_id` INT(10) UNSIGNED DEFAULT NULL,
    `search_id` INT(10) UNSIGNED DEFAULT NULL
);

-- Backfill from `target_id` in a single pass. The action ids are a frozen snapshot of Action.java, `lw_user_log_action` is not reliable.
UPDATE `lw_user_log` SET
    -- tagging, rating, commenting, opening, deleting, adding, deleting comment, survey save, edit, survey submit, thumb rating, downloading,
    -- glossary open/entry/term actions, thumbnail update, metadata, office edit, lock rejected/interrupted, move
    `resource_id` = CASE WHEN `action` IN (0, 1, 2, 3, 14, 15, 17, 18, 19, 20, 21, 32, 37, 39, 40, 41, 42, 43, 44, 45, 49, 50, 54, 59, 60, 66) THEN `target_id` END,
    -- folder opening, deleting, adding, editing, moving
    `folder_id` = CASE WHEN `action` IN (29, 31, 35, 36, 65) THEN `target_id` END,
    -- forum topic added, post added (both logged the topic id)
    `topic_id` = CASE WHEN `action` IN (11, 55) THEN `target_id` END,
    -- forum post deleted
    `post_id` = CASE WHEN `action` = 16 THEN `target_id` END,
    -- course delete, anonymize
    `course_id` = CASE WHEN `action` IN (57, 58) THEN `target_id` END,
    -- changing profile, moderator login, soft/hard user delete
    `target_user_id` = CASE WHEN `action` IN (13, 56, 63, 64) THEN `target_id` END,
    -- searching, search result clicked, saved
    `search_id` = CASE WHEN `action` IN (5, 46, 47) THEN `target_id` END
WHERE `target_id` > 0
  AND `action` IN (0, 1, 2, 3, 14, 15, 17, 18, 19, 20, 21, 32, 37, 39, 40, 41, 42, 43, 44, 45, 49, 50, 54, 59, 60, 66,
                   29, 31, 35, 36, 65, 11, 55, 16, 57, 58, 13, 56, 63, 64, 5, 46, 47);

CREATE INDEX `lw_user_log_resource_id` ON `lw_user_log` (`resource_id`);

-- The target type of an action is now given by the column it is stored in.
ALTER TABLE `lw_user_log_action` DROP COLUMN `target`;

-- `target_id` is no longer written or read. It is kept until the backfill is verified on production and will be dropped in a later migration.
