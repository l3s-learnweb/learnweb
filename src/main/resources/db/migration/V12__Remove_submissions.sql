DROP TABLE IF EXISTS `lw_submission_resource`;
DROP TABLE IF EXISTS `lw_submission_status`;
DROP TABLE IF EXISTS `lw_submission`;

UPDATE `lw_resource` SET `policy_view` = 'DEFAULT_RIGHTS' WHERE `policy_view` = 'SUBMISSION_READABLE';

ALTER TABLE `lw_resource` MODIFY
    `policy_view` ENUM ('DEFAULT_RIGHTS','OWNER_READABLE','LEARNWEB_READABLE','WORLD_READABLE') NOT NULL DEFAULT 'DEFAULT_RIGHTS';
