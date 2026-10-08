-- Group searches were stored in lw_search_history, but never shown there, see SearchHistoryDao.
-- What was searched in which group is kept in lw_user_log as `group_resource_search` (34), which was logged on each query change anyway.
-- Only the searches missing there are added, once per user, group, query and day.
-- LogDao shortens params longer than 250 chars at a word boundary and appends '...', so a logged long query matches by its prefix.
-- A query without a space in its first 247 chars is logged as just '...', which matches nothing.
INSERT INTO `lw_user_log` (`user_id`, `session_id`, `action`, `target_id`, `params`, `created_at`, `group_id`)
SELECT h.`user_id`, 'search_history', 34, h.`group_id`, h.`params`, MIN(h.`created_at`), h.`group_id`
FROM (
    SELECT `user_id`, `group_id`, `query`, `created_at`,
        CASE WHEN CHAR_LENGTH(`query`) > 250 THEN CONCAT(LEFT(`query`, 247), '...') ELSE `query` END AS `params`
    FROM `lw_search_history`
    WHERE `group_id` IS NOT NULL AND `user_id` IS NOT NULL
) h
WHERE NOT EXISTS (
    SELECT 1 FROM `lw_user_log` l
    WHERE l.`group_id` = h.`group_id` AND l.`action` = 34 AND l.`user_id` = h.`user_id`
      AND CAST(l.`created_at` AS DATE) = CAST(h.`created_at` AS DATE)
      AND (l.`params` = h.`query`
        OR (CHAR_LENGTH(h.`query`) > 250 AND CHAR_LENGTH(l.`params`) > 3 AND CONCAT(LEFT(h.`query`, CHAR_LENGTH(l.`params`) - 3), '...') = l.`params`))
)
GROUP BY h.`user_id`, h.`group_id`, h.`params`, CAST(h.`created_at` AS DATE);

-- the stored results and actions are deleted by cascade
DELETE FROM `lw_search_history` WHERE `group_id` IS NOT NULL;

ALTER TABLE `lw_search_history` DROP FOREIGN KEY `fk_lw_search_history_lw_group`;
ALTER TABLE `lw_search_history` DROP COLUMN `group_id`;
