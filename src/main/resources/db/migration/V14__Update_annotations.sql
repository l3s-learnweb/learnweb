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
    `url` VARCHAR(200) NOT NULL,
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
    `url` VARCHAR(200) NOT NULL,
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

/*=======================Annotation=================================*/
CREATE TABLE IF NOT EXISTS `learnweb_large`.`sl_recognised_entity` (
    `entity_uri` int NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `uri` varchar(250) NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `type` varchar(250) NOT NULL,
    `surface_form` varchar(250) NOT NULL,
    `session_id` varchar(10000) NOT NULL,
    `confidence` double NOT NULL,
    `input_id` text NOT NULL,
    `created_at` timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`sl_shared_object` ( /* TODO: redesign, flatten object and split into entity, link, entity_user_weight */
    `id` int UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `user_id` int NOT NULL,
    `group_id` int NOT NULL,
    `application` varchar(250) NOT NULL,
    `shared_object` varchar(10000) NOT NULL,
    `created_at` date NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`sl_rdf` (
    `id` int UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `user_id` int NOT NULL,
    `group_id` int NOT NULL,
    `rdf_value` longtext NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`sl_input_string` (
    `id` int NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `user_id` int NOT NULL,
    `type` varchar(250) NOT NULL,
    `content` longtext NOT NULL,
    `date_created` date NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_large`.`sl_search_entity` (
    `search_id` int NOT NULL,
    `entity_uri` int NOT NULL
);

