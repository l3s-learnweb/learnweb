CREATE SCHEMA IF NOT EXISTS `learnweb_large`;

CREATE TABLE IF NOT EXISTS `learnweb_large`.`speechrepository_video` (
    `id` INT(11) NOT NULL PRIMARY KEY,
    `title` VARCHAR(1000) NOT NULL,
    `url` VARCHAR(1000) NOT NULL,
    `rights` VARCHAR(1000) NOT NULL,
    `date` VARCHAR(1000) NOT NULL,
    `description` VARCHAR(1000) NOT NULL,
    `notes` VARCHAR(2000) DEFAULT NULL,
    `image_link` VARCHAR(1000) NOT NULL,
    `video_link` VARCHAR(1000) NOT NULL,
    `duration` INT(11) NOT NULL,
    `language` VARCHAR(1000) NOT NULL,
    `level` VARCHAR(1000) DEFAULT NULL,
    `use` VARCHAR(1000) DEFAULT NULL,
    `type` VARCHAR(1000) DEFAULT NULL,
    `domains` VARCHAR(1000) DEFAULT NULL,
    `terminology` TEXT DEFAULT NULL,
    `learnweb_resource_id` INT(10) UNSIGNED NOT NULL DEFAULT 0,
    KEY `speechrepository_video_learnweb_resource_id` (`learnweb_resource_id`)
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`ted_transcripts_lang_mapping` (
    `language_code` CHAR(10) NOT NULL,
    `language` CHAR(25) NOT NULL,
    PRIMARY KEY (`language_code`, `language`)
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`ted_transcripts_paragraphs` (
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `language` CHAR(10) NOT NULL,
    `starttime` INT(10) UNSIGNED NOT NULL,
    `paragraph` LONGTEXT NOT NULL,
    KEY `ted_transcripts_paragraphs_resource_id` (`resource_id`, `language`)
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`ted_video` (
    `ted_id` INT(10) UNSIGNED NOT NULL DEFAULT 0 PRIMARY KEY,
    `resource_id` INT(10) UNSIGNED NOT NULL DEFAULT 0,
    `title` VARCHAR(200) NOT NULL,
    `description` MEDIUMTEXT NOT NULL,
    `slug` VARCHAR(200) NOT NULL,
    `viewed_count` INT(10) UNSIGNED NOT NULL,
    `published_at` TIMESTAMP NULL DEFAULT NULL,
    `talk_updated_at` TIMESTAMP NULL DEFAULT NULL,
    `photo1_url` VARCHAR(255) DEFAULT NULL,
    `photo1_width` SMALLINT(6) UNSIGNED NOT NULL DEFAULT 0,
    `photo1_height` SMALLINT(6) UNSIGNED NOT NULL DEFAULT 0,
    `photo2_url` VARCHAR(255) DEFAULT NULL,
    `photo2_width` SMALLINT(6) NOT NULL DEFAULT 0,
    `photo2_height` SMALLINT(6) NOT NULL DEFAULT 0,
    `tags` MEDIUMTEXT NOT NULL,
    `duration` SMALLINT(6) UNSIGNED NOT NULL DEFAULT 0,
    `json` MEDIUMTEXT DEFAULT NULL,
    KEY `ted_video_slug` (`slug`)
);
