ALTER TABLE `lw_search_history` MODIFY
    `query` VARCHAR(512) NOT NULL;

ALTER TABLE `lw_forum_post` MODIFY
    `category` VARCHAR(255) DEFAULT '';

ALTER TABLE `lw_bans` MODIFY
    `reason` VARCHAR(255) DEFAULT NULL;

ALTER TABLE `lw_bounces` MODIFY
    `description` VARCHAR(255) DEFAULT NULL;

ALTER TABLE `lw_course` MODIFY
    `title` VARCHAR(255) NOT NULL;
ALTER TABLE `lw_course` MODIFY
    `reg_wizard` VARCHAR(255) DEFAULT NULL;

ALTER TABLE `lw_forum_topic` MODIFY
    `title` VARCHAR(100) NOT NULL DEFAULT '';

ALTER TABLE `lw_glossary_entry` MODIFY
    `topic_one` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `lw_glossary_entry` MODIFY
    `topic_two` VARCHAR(255) DEFAULT NULL;

ALTER TABLE `lw_glossary_resource` MODIFY
    `allowed_languages` VARCHAR(255) DEFAULT NULL;

ALTER TABLE `lw_group` MODIFY
    `title` VARCHAR(255) NOT NULL;

ALTER TABLE `lw_group_folder` MODIFY
    `title` VARCHAR(255) NOT NULL;

ALTER TABLE `lw_organisation` MODIFY
    `title` VARCHAR(255) NOT NULL;
