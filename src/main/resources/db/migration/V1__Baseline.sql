CREATE TABLE IF NOT EXISTS `lw_bans` (
    `addr` VARCHAR(64) NOT NULL PRIMARY KEY,
    `expires` TIMESTAMP NULL DEFAULT NULL,
    `attempts` INT(11) DEFAULT NULL,
    `reason` VARCHAR(200) DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `lw_bounces` (
    `bounce_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(64) DEFAULT NULL,
    `received` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `code` VARCHAR(10) NOT NULL,
    `description` VARCHAR(100) DEFAULT NULL,
    UNIQUE KEY `lw_bounces_email` (`email`)
);

CREATE TABLE IF NOT EXISTS `lw_comment` (
    `comment_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `text` MEDIUMTEXT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `lw_course` (
    `course_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `organisation_id` INT(10) UNSIGNED NOT NULL,
    `title` VARCHAR(50) NOT NULL,
    `wizard_param` VARCHAR(100) DEFAULT NULL,
    `default_group_id` INT(10) UNSIGNED DEFAULT NULL,
    `next_x_users_become_moderator` TINYINT(3) UNSIGNED NOT NULL DEFAULT 0,
    `welcome_message` TEXT DEFAULT NULL,
    `options_field1` BIGINT(20) NOT NULL DEFAULT 1,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    UNIQUE KEY `lw_course_wizard_param` (`wizard_param`)
);

CREATE TABLE IF NOT EXISTS `lw_course_user` (
    `course_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    PRIMARY KEY (`user_id`, `course_id`)
);

CREATE TABLE IF NOT EXISTS `lw_file` (
    `file_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `type` ENUM ('SYSTEM_FILE','ORGANISATION_BANNER','PROFILE_PICTURE','THUMBNAIL_SMALL','THUMBNAIL_MEDIUM','THUMBNAIL_LARGE','MAIN','ORIGINAL','DOC_HISTORY','DOC_CHANGES') NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(255) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `lw_forum_post` (
    `post_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `topic_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED DEFAULT NULL,
    `text` TEXT NOT NULL,
    `category` VARCHAR(70) DEFAULT '',
    `edit_count` SMALLINT(6) UNSIGNED NOT NULL DEFAULT 0,
    `edit_user_id` INT(10) UNSIGNED DEFAULT NULL,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    KEY `lw_forum_post_topic_id` (`topic_id`, `created_at`)
);

CREATE TABLE IF NOT EXISTS `lw_forum_topic` (
    `topic_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED DEFAULT NULL,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `title` VARCHAR(100) NOT NULL DEFAULT '',
    `views` INT(11) DEFAULT 0,
    `replies` INT(11) DEFAULT 0,
    `last_post_id` INT(10) UNSIGNED DEFAULT NULL,
    `last_post_user_id` INT(10) UNSIGNED DEFAULT NULL,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    KEY `lw_forum_topic_group_id` (`group_id`, `deleted`, `updated_at`)
);

CREATE TABLE IF NOT EXISTS `lw_forum_topic_user` (
    `topic_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `last_visit` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    PRIMARY KEY (`topic_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_glossary_entry` (
    `entry_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `original_entry_id` INT(10) UNSIGNED DEFAULT NULL,
    `edit_user_id` INT(10) UNSIGNED DEFAULT NULL,
    `user_id` INT(10) UNSIGNED DEFAULT NULL,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `topic_one` VARCHAR(100) DEFAULT NULL,
    `topic_two` VARCHAR(100) DEFAULT NULL,
    `topic_three` VARCHAR(100) DEFAULT NULL,
    `description` VARCHAR(3000) DEFAULT NULL,
    `description_pasted` TINYINT(4) NOT NULL DEFAULT 0,
    `imported` BOOLEAN NOT NULL DEFAULT 0,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    KEY `lw_glossary_entry_resource_id` (`resource_id`, `deleted`)
);

CREATE TABLE IF NOT EXISTS `lw_glossary_resource` (
    `resource_id` INT(10) UNSIGNED NOT NULL PRIMARY KEY,
    `allowed_languages` VARCHAR(200) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `lw_glossary_term` (
    `term_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `entry_id` INT(10) UNSIGNED NOT NULL,
    `original_term_id` INT(10) UNSIGNED DEFAULT NULL,
    `edit_user_id` INT(10) UNSIGNED DEFAULT NULL,
    `user_id` INT(10) UNSIGNED DEFAULT NULL,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `term` VARCHAR(500) DEFAULT NULL,
    `term_pasted` TINYINT(4) NOT NULL DEFAULT 0,
    `pronounciation` VARCHAR(500) DEFAULT NULL,
    `pronounciation_pasted` TINYINT(4) NOT NULL DEFAULT 0,
    `acronym` VARCHAR(500) DEFAULT NULL,
    `acronym_pasted` TINYINT(4) NOT NULL DEFAULT 0,
    `phraseology` VARCHAR(4100) DEFAULT NULL,
    `phraseology_pasted` TINYINT(4) NOT NULL DEFAULT 0,
    `language` VARCHAR(500) NOT NULL,
    `uses` VARCHAR(500) DEFAULT NULL,
    `source` VARCHAR(500) DEFAULT NULL,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `lw_group` (
    `group_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `course_id` INT(10) UNSIGNED NOT NULL,
    `leader_id` INT(10) UNSIGNED NOT NULL,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `title` VARCHAR(150) NOT NULL,
    `description` MEDIUMTEXT DEFAULT NULL,
    `max_member_count` SMALLINT(6) NOT NULL DEFAULT -1 COMMENT 'number of allowed members; -1 = unlimitted',
    `restriction_forum_category_required` BOOLEAN NOT NULL DEFAULT 0,
    `policy_add` ENUM ('GROUP_MEMBERS','GROUP_LEADER') NOT NULL DEFAULT 'GROUP_MEMBERS',
    `policy_annotate` ENUM ('ALL_LEARNWEB_USERS','COURSE_MEMBERS','GROUP_MEMBERS','GROUP_LEADER') NOT NULL DEFAULT 'ALL_LEARNWEB_USERS',
    `policy_edit` ENUM ('GROUP_MEMBERS','GROUP_LEADER_AND_FILE_OWNER','GROUP_LEADER') NOT NULL DEFAULT 'GROUP_MEMBERS',
    `policy_join` ENUM ('ALL_LEARNWEB_USERS','ORGANISATION_MEMBERS','COURSE_MEMBERS','NOBODY') NOT NULL DEFAULT 'COURSE_MEMBERS',
    `policy_view` ENUM ('ALL_LEARNWEB_USERS','COURSE_MEMBERS','GROUP_MEMBERS','GROUP_LEADER') NOT NULL DEFAULT 'ALL_LEARNWEB_USERS',
    `hypothesis_link` VARCHAR(255) DEFAULT NULL,
    `hypothesis_token` VARCHAR(255) DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    KEY `lw_group_title` (`title`),
    KEY `lw_group_created_at` (`created_at`),
    KEY `lw_group_course` (`deleted`, `group_id`, `policy_join`, `course_id`)
);

CREATE TABLE IF NOT EXISTS `lw_group_folder` (
    `folder_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` INT(10) UNSIGNED DEFAULT NULL,
    `parent_folder_id` INT(10) UNSIGNED DEFAULT NULL,
    `user_id` INT(10) UNSIGNED DEFAULT NULL,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    KEY `lw_group_folder_group_id` (`group_id`, `parent_folder_id`)
);

CREATE TABLE IF NOT EXISTS `lw_group_user` (
    `group_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `join_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `last_visit` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `notification_frequency` ENUM ('NEVER','DAILY','WEEKLY','MONTHLY') NOT NULL DEFAULT 'NEVER',
    PRIMARY KEY (`group_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_message` (
    `message_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `sender_user_id` INT(10) UNSIGNED NOT NULL,
    `recipient_user_id` INT(10) UNSIGNED NOT NULL,
    `title` VARCHAR(2550) NOT NULL,
    `text` LONGTEXT NOT NULL,
    `seen` BOOLEAN NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    KEY `lw_message_recipient_user_id` (`recipient_user_id`, `seen`)
);

CREATE TABLE IF NOT EXISTS `lw_news` (
    `news_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `hidden` BOOLEAN NOT NULL DEFAULT 0,
    `title` VARCHAR(500) NOT NULL,
    `text` VARCHAR(5000) DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `lw_organisation` (
    `organisation_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(60) NOT NULL,
    `logo` LONGTEXT DEFAULT NULL,
    `welcome_page` VARCHAR(255) DEFAULT NULL,
    `welcome_message` TEXT DEFAULT NULL,
    `options_field1` BIGINT(20) NOT NULL DEFAULT 0,
    `default_search_text` VARCHAR(16) NOT NULL DEFAULT 'bing',
    `default_search_image` VARCHAR(16) NOT NULL DEFAULT 'flickr',
    `default_search_video` VARCHAR(16) NOT NULL DEFAULT 'youtube',
    `default_language` CHAR(2) DEFAULT NULL,
    `language_variant` VARCHAR(10) DEFAULT NULL,
    `banner_image_file_id` INT(10) UNSIGNED DEFAULT NULL,
    `glossary_languages` VARCHAR(1000) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `lw_requests` ( -- TODO: flush data on destroy
    `request_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `addr` VARCHAR(64) NOT NULL,
    `requests` INT(11) DEFAULT NULL,
    `logins` INT(11) DEFAULT NULL,
    `usernames` VARCHAR(512) DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `lw_resource` (
    `resource_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `group_id` INT(10) UNSIGNED DEFAULT NULL,
    `folder_id` INT(10) UNSIGNED DEFAULT NULL,
    `owner_user_id` INT(10) UNSIGNED NOT NULL,
    `title` VARCHAR(1000) NOT NULL,
    `description` MEDIUMTEXT DEFAULT NULL,
    `url` VARCHAR(4000) DEFAULT NULL,
    `storage_type` ENUM ('LEARNWEB','WEB') NOT NULL DEFAULT 'LEARNWEB',
    `policy_view` ENUM ('DEFAULT_RIGHTS','SUBMISSION_READABLE','LEARNWEB_READABLE','WORLD_READABLE') NOT NULL DEFAULT 'DEFAULT_RIGHTS',
    `service` ENUM ('bing','flickr','giphy','youtube','vimeo','ipernity','ted','tedx','loro','yovisto','learnweb','archiveit','teded','factcheck','desktop','internet','slideshare','speechrepository') NOT NULL,
    `language` VARCHAR(200) DEFAULT NULL,
    `author` VARCHAR(255) DEFAULT NULL,
    `type` ENUM ('text','video','image','audio','pdf','website','spreadsheet','presentation','document','file','survey','glossary') NOT NULL,
    `format` VARCHAR(255) DEFAULT NULL,
    `duration` INT(10) UNSIGNED DEFAULT NULL,
    `width` INT(10) UNSIGNED DEFAULT NULL,
    `height` INT(10) UNSIGNED DEFAULT NULL,
    `id_at_service` VARCHAR(100) DEFAULT NULL,
    `rating` INT(11) NOT NULL,
    `rate_number` INT(11) NOT NULL,
    `query` VARCHAR(1000) DEFAULT NULL,
    `max_image_url` VARCHAR(1000) DEFAULT NULL,
    `embedded_url` MEDIUMTEXT DEFAULT NULL,
    `original_resource_id` INT(10) UNSIGNED DEFAULT NULL,
    `machine_description` LONGTEXT DEFAULT NULL,
    `transcript` LONGTEXT DEFAULT NULL,
    `read_only_transcript` BOOLEAN NOT NULL DEFAULT 0,
    `online_status` ENUM ('UNKNOWN','ONLINE','OFFLINE','PROCESSING') NOT NULL DEFAULT 'UNKNOWN',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `metadata` BLOB DEFAULT NULL,
    KEY `lw_resource_type` (`type`),
    KEY `lw_resource_storage_type` (`storage_type`, `deleted`),
    KEY `lw_resource_owner_user_id` (`owner_user_id`, `deleted`)
);

CREATE TABLE IF NOT EXISTS `lw_resource_archiveurl` (
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `archive_url` VARCHAR(600) NOT NULL,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `lw_resource_file` (
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `file_id` INT(10) UNSIGNED NOT NULL,
    PRIMARY KEY (`resource_id`, `file_id`)
);

CREATE TABLE IF NOT EXISTS `lw_resource_history` (
    `resource_history_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED DEFAULT NULL,
    `file_id` INT(10) UNSIGNED NOT NULL,
    `prev_file_id` INT(10) UNSIGNED NOT NULL,
    `changes_file_id` INT(10) UNSIGNED NOT NULL,
    `server_version` VARCHAR(20) NOT NULL,
    `document_created` VARCHAR(20) NOT NULL,
    `document_key` VARCHAR(20) NOT NULL,
    `document_version` INT(11) NOT NULL,
    `document_changes` MEDIUMTEXT DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `lw_resource_rating` (
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `rating` TINYINT(1) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    PRIMARY KEY (`resource_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_resource_tag` (
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `tag_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `lw_resource_thumb` (
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `direction` TINYINT(1) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    PRIMARY KEY (`resource_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_survey` (
    `survey_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `organisation_id` INT(10) UNSIGNED NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` VARCHAR(1000) NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL COMMENT 'the user who created this template',
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `public_template` BOOLEAN NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `lw_survey_answer` (
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `question_id` INT(10) UNSIGNED NOT NULL,
    `answer` VARCHAR(6000) DEFAULT NULL COMMENT 'delimited by ||| if multi valued',
    PRIMARY KEY (`resource_id`, `user_id`, `question_id`)
);

CREATE TABLE IF NOT EXISTS `lw_survey_question` (
    `question_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `survey_id` INT(10) UNSIGNED NOT NULL,
    `order` SMALLINT(5) UNSIGNED DEFAULT NULL,
    `question` VARCHAR(1000) NOT NULL,
    `question_type` ENUM ('INPUT_TEXT','INPUT_TEXTAREA','AUTOCOMPLETE','ONE_MENU','ONE_MENU_EDITABLE','MULTIPLE_MENU','ONE_RADIO','MANY_CHECKBOX','FULLWIDTH_HEADER','FULLWIDTH_DESCRIPTION') NOT NULL,
    `answers` VARCHAR(6000) DEFAULT NULL COMMENT 'answers separated by ||| delimiter',
    `extra` VARCHAR(1000) DEFAULT NULL,
    `option` VARCHAR(100) DEFAULT NULL COMMENT 'optional value, depends on question_type',
    `info` VARCHAR(1000) DEFAULT NULL COMMENT 'optional description that can be shown as tooltip',
    `required` BOOLEAN NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `lw_survey_question_option` (
    `answer_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `question_id` INT(10) UNSIGNED NOT NULL,
    `value` VARCHAR(1000) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `lw_survey_resource` (
    `resource_id` INT(10) UNSIGNED NOT NULL PRIMARY KEY,
    `survey_id` INT(10) UNSIGNED NOT NULL,
    `open_date` TIMESTAMP NULL DEFAULT NULL,
    `close_date` TIMESTAMP NULL DEFAULT NULL,
    `editable` BOOLEAN NOT NULL DEFAULT 0,
    KEY `lw_survey_resource_survey_id` (`survey_id`)
);

CREATE TABLE IF NOT EXISTS `lw_survey_resource_user` (
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `submitted` BOOLEAN NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    PRIMARY KEY (`resource_id`, `user_id`)
) COMMENT = 'Contains survey state for a particular user';

CREATE TABLE IF NOT EXISTS `lw_tag` (
    `tag_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(230) NOT NULL,
    KEY `lw_tag_name` (`name`)
);

CREATE TABLE IF NOT EXISTS `lw_transcript_log` (
    `user_id` INT(10) UNSIGNED NOT NULL,
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `words_selected` LONGTEXT NOT NULL,
    `user_annotation` MEDIUMTEXT NOT NULL,
    `action` CHAR(25) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `lw_transcript_selections` (
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `words_selected` LONGTEXT NOT NULL,
    `user_annotation` MEDIUMTEXT NOT NULL,
    `ua_corrected` MEDIUMTEXT DEFAULT NULL,
    `start_offset` INT(10) NOT NULL,
    `end_offset` INT(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS `lw_transcript_summary` (
    `user_id` INT(10) UNSIGNED NOT NULL,
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `summary_type` ENUM ('SHORT','LONG','DETAILED') NOT NULL,
    `summary_text` MEDIUMTEXT NOT NULL,
    PRIMARY KEY (`user_id`, `resource_id`, `summary_type`)
);

CREATE TABLE IF NOT EXISTS `lw_user` (
    `user_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `organisation_id` INT(10) UNSIGNED NOT NULL,
    `deleted` BOOLEAN NOT NULL DEFAULT 0,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(512) DEFAULT NULL,
    `hashing` ENUM ('EMPTY','MD5','PBKDF2') NOT NULL DEFAULT 'MD5',
    `email` VARCHAR(250) DEFAULT NULL,
    `email_confirmed` BOOLEAN NOT NULL DEFAULT 1,
    `image_file_id` INT(10) UNSIGNED DEFAULT NULL,
    `fullname` VARCHAR(105) DEFAULT NULL,
    `gender` TINYINT(1) NOT NULL DEFAULT 0,
    `birthdate` DATE DEFAULT NULL,
    `address` VARCHAR(255) DEFAULT NULL,
    `profession` VARCHAR(105) DEFAULT NULL,
    `interest` VARCHAR(255) DEFAULT NULL,
    `credits` VARCHAR(255) DEFAULT NULL,
    `affiliation` VARCHAR(105) DEFAULT NULL,
    `student_identifier` VARCHAR(50) DEFAULT NULL,
    `is_admin` BOOLEAN NOT NULL DEFAULT 0,
    `is_moderator` BOOLEAN NOT NULL DEFAULT 0,
    `accept_terms_and_conditions` BOOLEAN NOT NULL DEFAULT 0,
    `preferred_notification_frequency` ENUM ('NEVER','DAILY','WEEKLY','MONTHLY') NOT NULL DEFAULT 'NEVER',
    `time_zone` VARCHAR(100) DEFAULT 'Europe/Berlin',
    `language` VARCHAR(10) DEFAULT 'en-UK',
    `guide_field1` BIGINT(20) NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `preferences` BLOB DEFAULT NULL,
    UNIQUE KEY `lw_user_username` (`username`)
);

CREATE TABLE IF NOT EXISTS `lw_user_log` ( -- TODO: refactor to multiple tables, one for each target_id
    `log_entry_id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `group_id` INT(10) UNSIGNED DEFAULT NULL,
    `session_id` CHAR(32) NOT NULL,
    `action` TINYINT(3) UNSIGNED NOT NULL,
    `target_id` INT(11) DEFAULT NULL,
    `params` VARCHAR(255) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    KEY `lw_user_log_session_id` (`session_id`),
    KEY `lw_user_log_group_id` (`group_id`, `action`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_user_log_action` (
    `action` TINYINT(4) NOT NULL PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `target` ENUM ('NONE','RESOURCE_ID','GROUP_ID','USER_ID','FORUM_TOPIC_ID','FORUM_POST_ID','COURSE_ID','FOLDER_ID','OTHER') NOT NULL,
    `category` ENUM ('OTHER','RESOURCE','FOLDER','GROUP','GLOSSARY','SURVEY','SEARCH','USER','FORUM','MODERATOR') NOT NULL
) COMMENT = 'This table is only used for debugging. Is not automatically synced with Action.java';

CREATE TABLE IF NOT EXISTS `lw_user_token` (
    `token_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `type` ENUM ('GRANT','AUTH','EMAIL_CONFIRMATION','PASSWORD_RESET') NOT NULL,
    `token` VARCHAR(128) NOT NULL,
    `expires` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

ALTER TABLE `lw_course` ADD CONSTRAINT `fk_lw_course_lw_group` FOREIGN KEY (`default_group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_course` ADD CONSTRAINT `fk_lw_course_lw_organisation` FOREIGN KEY (`organisation_id`) REFERENCES `lw_organisation` (`organisation_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_comment` ADD CONSTRAINT `fk_lw_comment_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_comment` ADD CONSTRAINT `fk_lw_comment_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_forum_post` ADD CONSTRAINT `fk_lw_forum_post_lw_forum_topic` FOREIGN KEY (`topic_id`) REFERENCES `lw_forum_topic` (`topic_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_forum_post` ADD CONSTRAINT `fk_lw_forum_post_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_forum_post` ADD CONSTRAINT `fk_lw_forum_post_lw_user_edit` FOREIGN KEY (`edit_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_forum_topic` ADD CONSTRAINT `fk_lw_forum_topic_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_forum_topic` ADD CONSTRAINT `fk_lw_forum_topic_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_forum_topic` ADD CONSTRAINT `fk_lw_forum_topic_last_post` FOREIGN KEY (`last_post_id`) REFERENCES `lw_forum_post` (`post_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_forum_topic` ADD CONSTRAINT `fk_lw_forum_topic_last_post_lw_user` FOREIGN KEY (`last_post_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_glossary_entry` ADD CONSTRAINT `fk_lw_glossary_entry_lw_glossary_entry` FOREIGN KEY (`original_entry_id`) REFERENCES `lw_glossary_entry` (`entry_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_entry` ADD CONSTRAINT `fk_lw_glossary_entry_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_entry` ADD CONSTRAINT `fk_lw_glossary_entry_lw_user_edit` FOREIGN KEY (`edit_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_entry` ADD CONSTRAINT `fk_lw_glossary_entry_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_glossary_resource` ADD CONSTRAINT `fk_lw_glossary_resource_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_glossary_term` ADD CONSTRAINT `fk_lw_glossary_term_lw_glossary_entry` FOREIGN KEY (`entry_id`) REFERENCES `lw_glossary_entry` (`entry_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_term` ADD CONSTRAINT `fk_lw_glossary_term_lw_glossary_term` FOREIGN KEY (`original_term_id`) REFERENCES `lw_glossary_term` (`term_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_term` ADD CONSTRAINT `fk_lw_glossary_term_lw_user_edit` FOREIGN KEY (`edit_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_term` ADD CONSTRAINT `fk_lw_glossary_term_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_group` ADD CONSTRAINT `fk_lw_group_lw_course` FOREIGN KEY (`course_id`) REFERENCES `lw_course` (`course_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_group` ADD CONSTRAINT `fk_lw_group_lw_user` FOREIGN KEY (`leader_id`) REFERENCES `lw_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `lw_group_folder` ADD CONSTRAINT `fk_lw_group_folder_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_group_folder` ADD CONSTRAINT `fk_lw_group_folder_lw_group_folder` FOREIGN KEY (`parent_folder_id`) REFERENCES `lw_group_folder` (`folder_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_group_folder` ADD CONSTRAINT `fk_lw_group_folder_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_group_user` ADD CONSTRAINT `fk_lw_group_user_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_group_user` ADD CONSTRAINT `fk_lw_group_user_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_message` ADD CONSTRAINT `fk_lw_message_lw_user_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_message` ADD CONSTRAINT `fk_lw_message_lw_user_recipient` FOREIGN KEY (`recipient_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_news` ADD CONSTRAINT `fk_lw_news_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_organisation` ADD CONSTRAINT `fk_lw_organisation_lw_file` FOREIGN KEY (`banner_image_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_resource` ADD CONSTRAINT `fk_lw_resource_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `fk_lw_resource_lw_group_folder` FOREIGN KEY (`folder_id`) REFERENCES `lw_group_folder` (`folder_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `fk_lw_resource_lw_resource` FOREIGN KEY (`original_resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `fk_lw_resource_lw_user` FOREIGN KEY (`owner_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `lw_resource_file` ADD CONSTRAINT `fk_lw_resource_file_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_file` ADD CONSTRAINT `fk_lw_resource_file_lw_file` FOREIGN KEY (`file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_resource_archiveurl` ADD CONSTRAINT `fk_lw_resource_archiveurl_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_resource_history` ADD CONSTRAINT `fk_lw_resource_history_lw_file` FOREIGN KEY (`file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_history` ADD CONSTRAINT `fk_lw_resource_history_lw_file_prev` FOREIGN KEY (`prev_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_history` ADD CONSTRAINT `fk_lw_resource_history_lw_file_changes` FOREIGN KEY (`changes_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_history` ADD CONSTRAINT `fk_lw_resource_history_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_history` ADD CONSTRAINT `fk_lw_resource_history_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_resource_rating` ADD CONSTRAINT `fk_lw_resource_rating_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_rating` ADD CONSTRAINT `fk_lw_resource_rating_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_resource_tag` ADD CONSTRAINT `fk_lw_resource_tag_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_tag` ADD CONSTRAINT `fk_lw_resource_tag_lw_tag` FOREIGN KEY (`tag_id`) REFERENCES `lw_tag` (`tag_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_tag` ADD CONSTRAINT `fk_lw_resource_tag_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey` ADD CONSTRAINT `fk_lw_survey_lw_organisation` FOREIGN KEY (`organisation_id`) REFERENCES `lw_organisation` (`organisation_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey` ADD CONSTRAINT `fk_lw_survey_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey_answer` ADD CONSTRAINT `fk_lw_survey_answer_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey_answer` ADD CONSTRAINT `fk_lw_survey_answer_lw_survey_question` FOREIGN KEY (`question_id`) REFERENCES `lw_survey_question` (`question_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey_answer` ADD CONSTRAINT `fk_lw_survey_answer_lw_survey_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_survey_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey_question` ADD CONSTRAINT `fk_lw_survey_question_lw_survey` FOREIGN KEY (`survey_id`) REFERENCES `lw_survey` (`survey_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey_question_option` ADD CONSTRAINT `fk_question_id` FOREIGN KEY (`question_id`) REFERENCES `lw_survey_question` (`question_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey_resource` ADD CONSTRAINT `fk_lw_survey_resource_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey_resource` ADD CONSTRAINT `fk_lw_survey_resource_lw_survey` FOREIGN KEY (`survey_id`) REFERENCES `lw_survey` (`survey_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey_resource_user` ADD CONSTRAINT `fk_lw_survey_resource_user_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey_resource_user` ADD CONSTRAINT `fk_lw_survey_resource_user_lw_survey_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_survey_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_resource_thumb` ADD CONSTRAINT `fk_lw_resource_thumb_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_thumb` ADD CONSTRAINT `fk_lw_resource_thumb_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_transcript_log` ADD CONSTRAINT `fk_lw_transcript_actions_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_transcript_log` ADD CONSTRAINT `fk_lw_transcript_actions_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_transcript_selections` ADD CONSTRAINT `fk_lw_transcript_selections_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_transcript_summary` ADD CONSTRAINT `fk_lw_transcript_summary_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_transcript_summary` ADD CONSTRAINT `fk_lw_transcript_summary_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_user` ADD CONSTRAINT `fk_lw_user_lw_file` FOREIGN KEY (`image_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_user` ADD CONSTRAINT `fk_lw_user_lw_organisation` FOREIGN KEY (`organisation_id`) REFERENCES `lw_organisation` (`organisation_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_course_user` ADD CONSTRAINT `fk_lw_user_course_lw_course` FOREIGN KEY (`course_id`) REFERENCES `lw_course` (`course_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_course_user` ADD CONSTRAINT `fk_lw_user_course_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_user_log` ADD CONSTRAINT `fk_lw_user_log_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_user_log` ADD CONSTRAINT `fk_lw_user_log_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_user_token` ADD CONSTRAINT `fk_lw_user_token_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
