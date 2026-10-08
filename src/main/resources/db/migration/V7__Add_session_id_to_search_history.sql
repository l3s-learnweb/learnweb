-- The search history is grouped by session, which was only available from the `searching` (5) entries of lw_user_log
ALTER TABLE `lw_search_history` ADD COLUMN `session_id` CHAR(32) DEFAULT NULL AFTER `user_id`;
-- the sessions are always loaded by user, see SearchHistoryDao.findSessions
CREATE INDEX `lw_search_history_user_session` ON `lw_search_history` (`user_id`, `session_id`, `created_at`);

-- lw_user_log isn't indexed by target_id, the derived table is materialized once instead of scanning the log per search
-- (UPDATE ... JOIN would do the same, but isn't supported by H2, which runs the migrations in tests)
UPDATE `lw_search_history` h
SET `session_id` = (
    SELECT l.`session_id` FROM (
        SELECT `target_id`, `user_id`, MIN(`session_id`) AS `session_id` FROM `lw_user_log`
        WHERE `action` = 5 GROUP BY `target_id`, `user_id`
    ) l
    WHERE l.`target_id` = h.`search_id` AND l.`user_id` = h.`user_id`
);

-- The target_id of `searching` (5) entries of lw_user_log is the search_id of lw_search_history
ALTER TABLE `lw_user_log_action` MODIFY
    `target` ENUM ('NONE','RESOURCE_ID','GROUP_ID','USER_ID','FORUM_TOPIC_ID','FORUM_POST_ID','COURSE_ID','FOLDER_ID','OTHER','SEARCH_ID') NOT NULL;

UPDATE `lw_user_log_action` SET `target` = 'SEARCH_ID' WHERE `action` = 5;

-- the actions of the search results are stored in lw_search_history_action instead
UPDATE `lw_user_log_action` SET `name` = 'unused1' WHERE `action` = 46;
UPDATE `lw_user_log_action` SET `name` = 'unused8' WHERE `action` = 47;
