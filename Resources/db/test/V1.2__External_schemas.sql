/* ================= `learnweb_large` schema ================= */
CREATE SCHEMA IF NOT EXISTS `learnweb_large`;

CREATE TABLE IF NOT EXISTS `learnweb_large`.`sl_action` (
    `search_id` INT(10) UNSIGNED NOT NULL,
    `rank` SMALLINT(5) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `action` ENUM ('resource_clicked','resource_saved') NOT NULL,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    KEY `sl_action_search_id` (`search_id`)
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`sl_query` (
    `search_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` INT(10) UNSIGNED DEFAULT 0,
    `query` VARCHAR(250) NOT NULL,
    `mode` ENUM ('text','image','video','group') NOT NULL,
    `service` ENUM ('bing','flickr','giphy','youtube','vimeo','ipernity','ted','tedx','loro','yovisto','learnweb','archiveit','teded','factcheck','desktop','internet','slideshare','speechrepository') NOT NULL,
    `language` CHAR(5) DEFAULT NULL,
    `filters` VARCHAR(1000) DEFAULT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    `learnweb_version` TINYINT(4) NOT NULL DEFAULT 0 COMMENT 'which learnweb instance has inserted this row'
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`sl_resource` (
    `search_id` INT(10) UNSIGNED NOT NULL,
    `rank` SMALLINT(5) UNSIGNED NOT NULL,
    `resource_id` INT(10) UNSIGNED DEFAULT NULL COMMENT 'id of a learnweb resource NULL otherwise',
    `url` VARCHAR(1000) DEFAULT NULL COMMENT 'null if learnweb resource',
    `title` VARCHAR(250) DEFAULT NULL COMMENT 'null if learnweb resource',
    `description` VARCHAR(1000) DEFAULT NULL COMMENT 'null if learnweb resource',
    `thumbnail_url` VARCHAR(1000) DEFAULT NULL,
    `thumbnail_height` SMALLINT(5) UNSIGNED DEFAULT NULL,
    `thumbnail_width` SMALLINT(5) UNSIGNED DEFAULT NULL,
    PRIMARY KEY (`search_id`, `rank`)
);

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

CREATE TABLE IF NOT EXISTS `learnweb_large`.`wb2_url` (
    `url_id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `url` VARCHAR(2000) NOT NULL,
    `first_capture` TIMESTAMP NULL DEFAULT NULL,
    `last_capture` TIMESTAMP NULL DEFAULT NULL,
    `all_captures_fetched` BOOLEAN NOT NULL DEFAULT 0 COMMENT '1 when all captures have been loaded into wb_url_capture; 0 else',
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    KEY `wb2_url_url` (`url`)
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`wb2_url_capture` (
    `url_id` BIGINT(20) NOT NULL,
    `timestamp` TIMESTAMP NULL DEFAULT NULL,
    KEY `wb2_url_capture_url_id` (`url_id`)
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`wb_url` (
    `url_id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `url` VARCHAR(2084) NOT NULL,
    `first_capture` TIMESTAMP NULL DEFAULT NULL,
    `last_capture` TIMESTAMP NULL DEFAULT NULL,
    `all_captures_fetched` BOOLEAN NOT NULL DEFAULT 0 COMMENT '1 when all captures have been loaded into wb_url_capture; 0 else',
    `crawl_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `status_code` SMALLINT(6) NOT NULL DEFAULT -3,
    `status_code_date` TIMESTAMP NOT NULL DEFAULT '1990-01-01 01:00:00',
    UNIQUE KEY `wb_url_url_index` (`url`)
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`wb_url_content` (
    `url_id` BIGINT(20) NOT NULL,
    `status_code` SMALLINT(6) NOT NULL DEFAULT -3,
    `content` LONGTEXT DEFAULT NULL,
    `date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    KEY `wb_url_content_url_id` (`url_id`)
);

ALTER TABLE `learnweb_large`.`sl_action` ADD CONSTRAINT `fk_sl_action_sl_query` FOREIGN KEY (`search_id`) REFERENCES `learnweb_large`.`sl_query` (`search_id`) ON DELETE CASCADE;
ALTER TABLE `learnweb_large`.`sl_resource` ADD CONSTRAINT `fk_sl_resource_sl_query` FOREIGN KEY (`search_id`) REFERENCES `learnweb_large`.`sl_query` (`search_id`) ON DELETE CASCADE;


/* ================= `learnweb_annotations` schema ================= */

CREATE SCHEMA IF NOT EXISTS `learnweb_annotations`;

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`document` (
    `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` TEXT DEFAULT NULL,
    `web_uri` TEXT DEFAULT NULL,
    `updated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    `created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation` (
    `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT(10) UNSIGNED DEFAULT NULL,
    `group_id` INT(10) UNSIGNED NOT NULL DEFAULT 0,
    `document_id` INT(10) UNSIGNED NOT NULL,
    `text` TEXT DEFAULT NULL,
    `quote` MEDIUMTEXT DEFAULT NULL,
    `text_rendered` TEXT DEFAULT NULL,
    `tags` TEXT DEFAULT NULL,
    `shared` BOOLEAN NOT NULL DEFAULT 0,
    `target_uri` TEXT DEFAULT NULL,
    `target_uri_normalized` TEXT DEFAULT NULL,
    `target_selectors` TEXT DEFAULT NULL,
    `references` TEXT DEFAULT NULL,
    `extra` MEDIUMTEXT DEFAULT NULL,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `updated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    `created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    KEY `annotation_user_id` (`user_id`),
    KEY `annotation_group_id` (`group_id`),
    KEY `annotation_updated` (`updated`),
    KEY `annotation_document` (`document_id`),
    /*KEY `annotation_tags` (`tags`),*/
    CONSTRAINT `annotation_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `learnweb_annotations`.`document` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_moderation` (
    `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `annotation_id` INT(10) UNSIGNED NOT NULL,
    `updated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    `created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    UNIQUE KEY `annotation_moderation_annotation` (`annotation_id`),
    CONSTRAINT `annotation_moderation_ibfk_1` FOREIGN KEY (`annotation_id`) REFERENCES `learnweb_annotations`.`annotation` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`document_meta` (
    `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `document_id` INT(10) UNSIGNED NOT NULL,
    `type` TEXT NOT NULL,
    `value` TEXT NOT NULL,
    `claimant` MEDIUMTEXT NOT NULL,
    `claimant_normalized` MEDIUMTEXT NOT NULL,
    `updated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    `created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    /*UNIQUE KEY `document_meta_claimant_normalized` (`claimant_normalized`, `type`),*/
    KEY `document_meta_document` (`document_id`),
    KEY `document_meta_updated` (`updated`),
    CONSTRAINT `document_meta_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `learnweb_annotations`.`document` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`document_uri` (
    `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `document_id` INT(10) UNSIGNED NOT NULL,
    `type` MEDIUMTEXT NOT NULL DEFAULT '',
    `content_type` MEDIUMTEXT NOT NULL DEFAULT '',
    `uri` TEXT NOT NULL,
    `uri_normalized` TEXT NOT NULL,
    `claimant` MEDIUMTEXT NOT NULL,
    `claimant_normalized` MEDIUMTEXT NOT NULL,
    `updated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    `created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    /*UNIQUE KEY `document_uri_claimant_normalized` (`claimant_normalized`, `uri_normalized`, `type`, `content_type`),*/
    KEY `document_uri_document` (`document_id`),
    /*KEY `document_uri_uri_normalized` (`uri_normalized`),*/
    KEY `document_uri_updated` (`updated`),
    CONSTRAINT `document_uri_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `learnweb_annotations`.`document` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`flag` (
    `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `annotation_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `updated` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    `created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    UNIQUE KEY `flag_annotation_id` (`annotation_id`, `user_id`),
    KEY `flag_user_id` (`user_id`),
    CONSTRAINT `flag_ibfk_1` FOREIGN KEY (`annotation_id`) REFERENCES `learnweb_annotations`.`annotation` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
);

/* ================= `tracker` schema ================= */

CREATE SCHEMA IF NOT EXISTS `tracker`;

CREATE TABLE IF NOT EXISTS `tracker`.`account` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `last_seen` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `tracker`.`client` (
    `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `api_key` VARCHAR(32) NOT NULL,
    `is_tracked` BOOLEAN NOT NULL DEFAULT 1,
    `overlay_path` VARCHAR(255) DEFAULT NULL,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `tracker`.`account_client` (
    `account_id` BIGINT(20) NOT NULL,
    `client_id` BIGINT(20) UNSIGNED NOT NULL,
    PRIMARY KEY (`account_id`, `client_id`),
    CONSTRAINT `fk_account_client_account_id` FOREIGN KEY (`account_id`) REFERENCES `tracker`.`account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_account_client_client_id` FOREIGN KEY (`client_id`) REFERENCES `tracker`.`client` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `tracker`.`role` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `role` VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS `tracker`.`account_role` (
    `account_id` BIGINT(20) NOT NULL,
    `role_id` BIGINT(20) NOT NULL,
    PRIMARY KEY (`account_id`, `role_id`),
    CONSTRAINT `fk_account_role_account_id` FOREIGN KEY (`account_id`) REFERENCES `tracker`.`account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_account_role_role_id` FOREIGN KEY (`role_id`) REFERENCES `tracker`.`role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `tracker`.`website_class` (
    `id` INT(11) UNSIGNED NOT NULL PRIMARY KEY,
    `website` VARCHAR(255) NOT NULL,
    `type` VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS `tracker`.`track` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `uuid` VARCHAR(36) NOT NULL,
    `client_id` BIGINT(20) UNSIGNED NOT NULL,
    `status` ENUM ('OPEN','CLOSED','PROCESSED') NOT NULL DEFAULT 'OPEN',
    `url` VARCHAR(2000) NOT NULL,
    `url_domain` VARCHAR(255) DEFAULT NULL,
    `title` VARCHAR(5000) NOT NULL,
    `referrer` VARCHAR(2000) DEFAULT NULL,
    `agent` VARCHAR(2000) NOT NULL,
    `viewport_width` INT(10) DEFAULT NULL,
    `viewport_height` INT(10) DEFAULT NULL,
    `user_ip` VARCHAR(46) NOT NULL,
    `external_user_id` VARCHAR(128) DEFAULT NULL,
    `external_session_id` VARCHAR(128) DEFAULT NULL,
    `closed_by_timeout` BOOLEAN NOT NULL DEFAULT 0,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `statistic_version` INT(10) DEFAULT NULL,
    `total_events` INT(10) DEFAULT NULL,
    `time_stay` INT(10) DEFAULT NULL,
    `time_active` INT(10) DEFAULT NULL,
    `clicks` INT(10) DEFAULT NULL,
    `keypress` INT(10) DEFAULT NULL,
    `copy` INT(10) DEFAULT NULL,
    `paste` INT(10) DEFAULT NULL,
    `website_class_id` INT(11) UNSIGNED DEFAULT NULL,
    KEY `track_uuid` (`uuid`),
    KEY `track_external_client_id` (`external_user_id`, `external_session_id`),
    CONSTRAINT `fk_track_client_id` FOREIGN KEY (`client_id`) REFERENCES `tracker`.`client` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_track_website_class_id` FOREIGN KEY (`website_class_id`) REFERENCES `tracker`.`website_class` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `tracker`.`event` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `track_id` BIGINT(20) NOT NULL,
    `type` ENUM ('focus','blur','online','offline','resize','scroll','click','auxclick','mousemove','keypress','change','beforeunload','copy','paste','custom') NOT NULL,
    `type_custom` VARCHAR(64) DEFAULT NULL,
    `x` INT(10) DEFAULT NULL,
    `y` INT(10) DEFAULT NULL,
    `target` TEXT DEFAULT NULL,
    `value` TEXT DEFAULT NULL,
    `time` INT(10) NOT NULL,
    KEY `event_type` (`type`),
    KEY `event_time` (`time`),
    KEY `event_type_custom` (`type_custom`),
    CONSTRAINT `fk_event_track_id` FOREIGN KEY (`track_id`) REFERENCES `tracker`.`track` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
