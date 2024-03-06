ALTER TABLE `lw_resource_rating`
    ADD `type` VARCHAR(100) NOT NULL DEFAULT 'star' AFTER `user_id`;

ALTER TABLE `lw_resource_rating`
    DROP PRIMARY KEY;

ALTER TABLE `lw_resource_rating`
    ADD PRIMARY KEY (`resource_id`, `user_id`, `type`);

UPDATE `lw_resource_rating`
SET `type` = 'star';

INSERT INTO `lw_resource_rating` (`resource_id`, `user_id`, `type`, `rating`, `created_at`)
SELECT `resource_id`, `user_id`, 'thumb', `direction`, `created_at`
FROM `lw_resource_thumb`;

DROP TABLE `lw_resource_thumb`;

ALTER TABLE `lw_resource`
    DROP COLUMN `rating`;

ALTER TABLE `lw_resource`
    DROP COLUMN `rate_number`;
