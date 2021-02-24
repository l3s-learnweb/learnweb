CREATE TABLE IF NOT EXISTS `lw_bans` (
    `addr` varchar(64) NOT NULL PRIMARY KEY,
    `expires` datetime DEFAULT NULL,
    `attempts` int(11) DEFAULT NULL,
    `reason` varchar(200) DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE IF NOT EXISTS `lw_bounces` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'TODO: rename to bounce_id',
    `address` varchar(64) DEFAULT NULL,
    `timereceived` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'TODO: rename to received',
    `code` varchar(10) NOT NULL,
    `description` varchar(64) DEFAULT NULL,
    UNIQUE KEY `lw_bounces_address` (`address`)
);

CREATE TABLE IF NOT EXISTS `lw_comment` (
    `comment_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `resource_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `text` mediumtext NOT NULL,
    `date` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'TODO: rename to created_at'
);

CREATE TABLE IF NOT EXISTS `lw_course` (
    `course_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` varchar(50) NOT NULL,
    `options_field1` bigint(20) NOT NULL DEFAULT 1,
    `organisation_id` int(10) unsigned NOT NULL,
    `default_group_id` int(10) unsigned DEFAULT NULL,
    `wizard_param` varchar(100) DEFAULT NULL,
    `next_x_users_become_moderator` tinyint(3) unsigned NOT NULL DEFAULT 0,
    `welcome_message` text DEFAULT NULL,
    `timestamp_update` timestamp NOT NULL DEFAULT current_timestamp() COMMENT 'TODO: rename to updated_at',
    `timestamp_creation` timestamp NOT NULL DEFAULT current_timestamp() COMMENT 'TODO: rename to created_at',
    UNIQUE KEY `lw_course_wizard_param` (`wizard_param`)
);

CREATE TABLE IF NOT EXISTS `lw_file` (
    `file_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `resource_id` int(10) unsigned DEFAULT NULL,
    `resource_file_number` smallint(5) unsigned NOT NULL,
    `name` varchar(255) NOT NULL,
    `mime_type` varchar(255) NOT NULL,
    `log_actived` tinyint(1) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `missing` tinyint(1) NOT NULL DEFAULT 0,
    `doc_key` varchar(40) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `lw_forum_post` (
    `post_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `topic_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `text` text NOT NULL,
    `category` varchar(70) DEFAULT '',
    `post_time` timestamp NULL DEFAULT NULL,
    `post_edit_time` timestamp NULL DEFAULT NULL,
    `post_edit_count` smallint(11) unsigned NOT NULL DEFAULT 0,
    `post_edit_user_id` int(10) unsigned DEFAULT NULL,
    KEY `lw_forum_post_topic_id` (`topic_id`, `post_time`)
);

CREATE TABLE IF NOT EXISTS `lw_forum_topic` (
    `topic_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` int(10) unsigned NOT NULL,
    `deleted` tinyint(3) unsigned NOT NULL DEFAULT 0,
    `topic_title` varchar(100) NOT NULL DEFAULT '',
    `user_id` int(10) unsigned NOT NULL,
    `topic_time` timestamp NULL DEFAULT NULL,
    `topic_views` int(11) DEFAULT 1,
    `topic_replies` int(11) DEFAULT 0,
    `topic_last_post_id` int(10) unsigned DEFAULT NULL,
    `topic_last_post_time` timestamp NULL DEFAULT NULL,
    `topic_last_post_user_id` int(10) unsigned DEFAULT NULL,
    KEY `lw_forum_topic_group_id` (`group_id`, `deleted`, `topic_last_post_time`)
);

CREATE TABLE IF NOT EXISTS `lw_forum_topic_user` (
    `topic_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `last_visit` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`topic_id`, `user_id`)
) COMMENT ='saves when a user has last seen a forum topic';

CREATE TABLE IF NOT EXISTS `lw_glossary_entry` (
    `entry_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `resource_id` int(10) unsigned NOT NULL,
    `original_entry_id` int(10) unsigned DEFAULT NULL,
    `last_changed_by_user_id` int(10) unsigned DEFAULT NULL,
    `user_id` int(10) unsigned NULL,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `topic_one` varchar(100) DEFAULT NULL,
    `topic_two` varchar(100) DEFAULT NULL,
    `topic_three` varchar(100) DEFAULT NULL,
    `description` varchar(3000) DEFAULT NULL,
    `description_pasted` tinyint(4) NOT NULL DEFAULT 0,
    `imported` tinyint(1) NOT NULL DEFAULT 0,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    KEY `lw_glossary_entry_resource_id` (`resource_id`, `deleted`)
);

CREATE TABLE IF NOT EXISTS `lw_glossary_resource` (
    `resource_id` int(10) unsigned NOT NULL PRIMARY KEY,
    `allowed_languages` varchar(200) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `lw_glossary_term` (
    `term_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `entry_id` int(10) unsigned NOT NULL,
    `original_term_id` int(10) unsigned DEFAULT NULL,
    `last_changed_by_user_id` int(10) unsigned DEFAULT NULL,
    `user_id` int(10) unsigned NULL,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `term` varchar(500) DEFAULT NULL,
    `language` varchar(500) NOT NULL,
    `uses` varchar(500) DEFAULT NULL,
    `pronounciation` varchar(500) DEFAULT NULL,
    `acronym` varchar(500) DEFAULT NULL,
    `source` varchar(500) DEFAULT NULL,
    `phraseology` varchar(4100) DEFAULT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `term_pasted` tinyint(4) NOT NULL DEFAULT 0,
    `pronounciation_pasted` tinyint(4) NOT NULL DEFAULT 0,
    `acronym_pasted` tinyint(4) NOT NULL DEFAULT 0,
    `phraseology_pasted` tinyint(4) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `lw_group` (
    `group_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `title` varchar(150) NOT NULL,
    `description` mediumtext DEFAULT NULL,
    `creation_time` timestamp NOT NULL DEFAULT current_timestamp(),
    `course_id` int(10) unsigned NOT NULL,
    `max_member_count` smallint(6) NOT NULL DEFAULT -1 COMMENT 'number of allowed members; -1 = unlimitted',
    `restriction_forum_category_required` tinyint(1) NOT NULL DEFAULT 0,
    `leader_id` int(10) unsigned NOT NULL,
    `policy_add` enum ('GROUP_MEMBERS','GROUP_LEADER') NOT NULL DEFAULT 'GROUP_MEMBERS',
    `policy_annotate` enum ('ALL_LEARNWEB_USERS','COURSE_MEMBERS','GROUP_MEMBERS','GROUP_LEADER') NOT NULL DEFAULT 'ALL_LEARNWEB_USERS',
    `policy_edit` enum ('GROUP_MEMBERS','GROUP_LEADER','GROUP_LEADER_AND_FILE_OWNER') NOT NULL DEFAULT 'GROUP_MEMBERS',
    `policy_join` enum ('COURSE_MEMBERS','NOBODY','ALL_LEARNWEB_USERS','ORGANISATION_MEMBERS') NOT NULL DEFAULT 'COURSE_MEMBERS',
    `policy_view` enum ('ALL_LEARNWEB_USERS','COURSE_MEMBERS','GROUP_MEMBERS','GROUP_LEADER') NOT NULL DEFAULT 'ALL_LEARNWEB_USERS',
    `hypothesis_link` varchar(255) DEFAULT NULL,
    `hypothesis_token` varchar(255) DEFAULT NULL,
    KEY `lw_group_title` (`title`),
    KEY `lw_group_creation_time` (`creation_time`),
    KEY `lw_group_course` (`deleted`, `group_id`, `policy_join`, `course_id`)
);

CREATE TABLE IF NOT EXISTS `lw_group_folder` (
    `folder_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) unsigned NOT NULL DEFAULT 0,
    `group_id` int(10) unsigned DEFAULT NULL,
    `parent_folder_id` int(10) unsigned DEFAULT NULL,
    `name` varchar(100) NOT NULL,
    `description` text DEFAULT NULL,
    `user_id` int(10) unsigned DEFAULT NULL,
    `last_change` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    KEY `lw_group_folder_group_id` (`group_id`, `parent_folder_id`)
);

CREATE TABLE IF NOT EXISTS `lw_group_user` (
    `group_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `join_time` timestamp NOT NULL DEFAULT current_timestamp(),
    `last_visit` int(10) unsigned NOT NULL DEFAULT 0 COMMENT 'TODO: change type to timestamp',
    `notification_frequency` enum ('NEVER','DAILY','WEEKLY','MONTHLY') NOT NULL DEFAULT 'NEVER',
    PRIMARY KEY (`group_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_message` (
    `message_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `sender_user_id` int(10) unsigned NOT NULL,
    `recipient_user_id` int(10) unsigned NOT NULL,
    `title` varchar(2550) NOT NULL,
    `text` longtext NOT NULL,
    `is_seen` tinyint(1) NOT NULL,
    `is_read` tinyint(1) NOT NULL,
    `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
    KEY `lw_message_recipient_user_id` (`recipient_user_id`, `is_seen`)
);

CREATE TABLE IF NOT EXISTS `lw_news` (
    `news_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` int(10) unsigned NOT NULL,
    `hidden` tinyint(1) NOT NULL DEFAULT 0,
    `title` varchar(500) NOT NULL,
    `message` varchar(5000) DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE IF NOT EXISTS `lw_organisation` (
    `organisation_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `is_default` tinyint(1) DEFAULT NULL,
    `title` varchar(60) NOT NULL,
    `logo` longtext DEFAULT NULL,
    `welcome_page` varchar(255) DEFAULT NULL,
    `welcome_message` text DEFAULT NULL,
    `options_field1` bigint(20) NOT NULL DEFAULT 0,
    `default_search_text` varchar(16) NOT NULL DEFAULT 'bing',
    `default_search_image` varchar(16) NOT NULL DEFAULT 'flickr',
    `default_search_video` varchar(16) NOT NULL DEFAULT 'youtube',
    `default_language` char(2) DEFAULT NULL,
    `language_variant` varchar(10) DEFAULT NULL,
    `banner_image_file_id` int(10) unsigned DEFAULT NULL,
    `css_file` varchar(100) DEFAULT NULL,
    `glossary_languages` varchar(1000) DEFAULT NULL,
    UNIQUE KEY `lw_organisation_is_default` (`is_default`)
);

CREATE TABLE IF NOT EXISTS `lw_requests` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'TODO: rename to request_id',
    `IP` varchar(64) NOT NULL,
    `requests` int(11) DEFAULT NULL,
    `logins` int(11) DEFAULT NULL,
    `usernames` varchar(512) DEFAULT NULL,
    `time` datetime NOT NULL
);

CREATE TABLE IF NOT EXISTS `lw_resource` (
    `resource_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `group_id` int(10) unsigned DEFAULT NULL,
    `folder_id` int(10) unsigned DEFAULT NULL,
    `title` varchar(1000) NOT NULL,
    `description` mediumtext NOT NULL,
    `url` varchar(4000) DEFAULT NULL,
    `storage_type` tinyint(1) NOT NULL,
    `rights` tinyint(1) NOT NULL,
    `source` varchar(255) NOT NULL,
    `language` varchar(200) DEFAULT NULL,
    `author` varchar(255) DEFAULT NULL,
    `type` varchar(255) NOT NULL,
    `format` varchar(255) NOT NULL,
    `duration` int(10) unsigned DEFAULT NULL,
    `owner_user_id` int(10) unsigned NOT NULL,
    `id_at_service` varchar(100) DEFAULT NULL,
    `rating` int(11) NOT NULL,
    `rate_number` int(11) NOT NULL,
    `filename` varchar(200) DEFAULT NULL,
    `file_url` varchar(1000) DEFAULT NULL,
    `max_image_url` varchar(1000) DEFAULT NULL,
    `query` varchar(1000) DEFAULT NULL,
    `original_resource_id` int(10) unsigned DEFAULT NULL,
    `machine_description` longtext DEFAULT NULL,
    `access` varchar(1) DEFAULT NULL,
    `thumbnail0_url` varchar(255) DEFAULT NULL,
    `thumbnail0_file_id` int(10) unsigned DEFAULT NULL,
    `thumbnail0_width` smallint(5) unsigned DEFAULT NULL,
    `thumbnail0_height` smallint(5) unsigned DEFAULT NULL,
    `thumbnail1_url` varchar(255) DEFAULT NULL,
    `thumbnail1_file_id` int(10) unsigned DEFAULT NULL,
    `thumbnail1_width` smallint(5) unsigned DEFAULT NULL,
    `thumbnail1_height` smallint(5) unsigned DEFAULT NULL,
    `thumbnail2_url` varchar(255) DEFAULT NULL,
    `thumbnail2_file_id` int(10) unsigned DEFAULT NULL,
    `thumbnail2_width` smallint(5) unsigned DEFAULT NULL,
    `thumbnail2_height` smallint(5) unsigned DEFAULT NULL,
    `thumbnail3_url` varchar(255) DEFAULT NULL,
    `thumbnail3_file_id` int(10) unsigned DEFAULT NULL,
    `thumbnail3_width` smallint(5) unsigned DEFAULT NULL,
    `thumbnail3_height` smallint(5) unsigned DEFAULT NULL,
    `thumbnail4_url` varchar(255) DEFAULT NULL,
    `thumbnail4_file_id` int(10) unsigned DEFAULT NULL,
    `thumbnail4_width` smallint(5) unsigned DEFAULT NULL,
    `thumbnail4_height` smallint(5) unsigned DEFAULT NULL,
    `embeddedRaw` mediumtext DEFAULT NULL,
    `transcript` longtext DEFAULT NULL,
    `read_only_transcript` tinyint(1) NOT NULL DEFAULT 0,
    `online_status` enum ('UNKNOWN','ONLINE','OFFLINE','PROCESSING') NOT NULL DEFAULT 'UNKNOWN' COMMENT '0=unknown',
    `restricted` tinyint(1) NOT NULL DEFAULT 0,
    `resource_timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `creation_date` datetime NOT NULL DEFAULT current_timestamp(),
    `metadata` blob DEFAULT NULL,
    KEY `lw_resource_type` (`type`),
    KEY `lw_resource_storage_type` (`storage_type`, `deleted`),
    KEY `lw_resource_url` (`url`),
    KEY `lw_resource_owner_user_id` (`owner_user_id`, `deleted`)
);

CREATE TABLE IF NOT EXISTS `lw_resource_archiveurl` (
    `resource_id` int(10) unsigned NOT NULL,
    `archive_url` varchar(600) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE IF NOT EXISTS `lw_resource_history` (
    `resource_history_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `resource_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NULL,
    `file_id` int(10) unsigned NOT NULL,
    `prev_file_id` int(10) unsigned NOT NULL,
    `changes_file_id` int(10) unsigned NOT NULL,
    `server_version` varchar(20) NOT NULL,
    `document_created` varchar(20) NOT NULL,
    `document_key` varchar(20) NOT NULL,
    `document_version` int(11) NOT NULL,
    `document_changes` mediumtext DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `lw_resource_rating` (
    `resource_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `rating` tinyint(1) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`resource_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_resource_tag` (
    `resource_id` int(10) unsigned NOT NULL,
    `tag_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE IF NOT EXISTS `lw_submission` (
    `submission_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `course_id` int(10) unsigned NOT NULL,
    `title` varchar(100) DEFAULT NULL,
    `description` varchar(1000) DEFAULT NULL,
    `open_datetime` datetime DEFAULT NULL,
    `close_datetime` datetime DEFAULT NULL,
    `number_of_resources` int(10) NOT NULL DEFAULT 3,
    `survey_resource_id` int(10) unsigned NOT NULL,
    KEY `lw_submission_course_id` (`course_id`, `deleted`)
);

CREATE TABLE IF NOT EXISTS `lw_submission_resource` (
    `submission_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `resource_id` int(10) unsigned NOT NULL,
    PRIMARY KEY (`submission_id`, `user_id`, `resource_id`)
);

CREATE TABLE IF NOT EXISTS `lw_submission_status` (
    `submission_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `submitted` tinyint(1) NOT NULL DEFAULT 1,
    `survey_resource_id` int(10) unsigned NOT NULL,
    PRIMARY KEY (`submission_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_survey` (
    `survey_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `organization_id` int(10) unsigned NOT NULL,
    `title` varchar(100) NOT NULL,
    `description` varchar(1000) NOT NULL,
    `user_id` int(10) unsigned NOT NULL COMMENT 'the user who created this template',
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `public_template` tinyint(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `lw_survey_answer` (
    `resource_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `question_id` int(10) unsigned NOT NULL,
    `answer` varchar(6000) DEFAULT NULL COMMENT 'delimited by ||| if multi valued',
    PRIMARY KEY (`resource_id`, `user_id`, `question_id`)
);

CREATE TABLE IF NOT EXISTS `lw_survey_question` (
    `question_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `survey_id` int(10) unsigned NOT NULL,
    `order` smallint(5) unsigned DEFAULT NULL,
    `question` varchar(1000) NOT NULL,
    `question_type` enum ('INPUT_TEXT','INPUT_TEXTAREA','ONE_MENU','ONE_MENU_EDITABLE','MULTIPLE_MENU','FULLWIDTH_HEADER','FULLWIDTH_DESCRIPTION','ONE_RADIO','MANY_CHECKBOX') NOT NULL,
    `answers` varchar(6000) DEFAULT NULL COMMENT 'answers separated by ||| delimiter',
    `extra` varchar(1000) DEFAULT NULL,
    `option` varchar(100) DEFAULT NULL COMMENT 'optional value, depends on question_type',
    `info` varchar(1000) DEFAULT NULL COMMENT 'optional description that can be shown as tooltip',
    `required` tinyint(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `lw_survey_question_option` (
    `answer_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `question_id` int(10) unsigned NOT NULL,
    `value` varchar(1000) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `lw_survey_resource` (
    `resource_id` int(10) unsigned NOT NULL PRIMARY KEY,
    `survey_id` int(10) unsigned NOT NULL,
    `open_date` datetime DEFAULT NULL,
    `close_date` datetime DEFAULT NULL,
    `editable` tinyint(1) NOT NULL DEFAULT 0,
    KEY `lw_survey_resource_survey_id` (`survey_id`)
);

CREATE TABLE IF NOT EXISTS `lw_survey_resource_user` (
    `resource_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `submitted` tinyint(1) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
    PRIMARY KEY (`resource_id`, `user_id`)
) COMMENT ='Contains submission status for a particular user';

CREATE TABLE IF NOT EXISTS `lw_tag` (
    `tag_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` varchar(230) NOT NULL,
    KEY `lw_tag_name` (`name`)
);

CREATE TABLE IF NOT EXISTS `lw_thumb` (
    `resource_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `direction` tinyint(1) NOT NULL,
    PRIMARY KEY (`resource_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_transcript_actions` (
    `user_id` int(10) unsigned NOT NULL,
    `resource_id` int(10) unsigned NOT NULL,
    `words_selected` longtext NOT NULL,
    `user_annotation` mediumtext NOT NULL,
    `action` char(25) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) COMMENT 'TODO: rename to lw_transcript_log';

CREATE TABLE IF NOT EXISTS `lw_transcript_selections` (
    `resource_id` int(10) unsigned NOT NULL,
    `words_selected` longtext NOT NULL,
    `user_annotation` mediumtext NOT NULL,
    `ua_corrected` mediumtext DEFAULT NULL,
    `start_offset` int(10) NOT NULL,
    `end_offset` int(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS `lw_transcript_summary` (
    `user_id` int(10) unsigned NOT NULL,
    `resource_id` int(10) unsigned NOT NULL,
    `summary_type` enum ('SHORT','LONG','DETAILED') NOT NULL,
    `summary_text` mediumtext NOT NULL,
    PRIMARY KEY (`user_id`, `resource_id`, `summary_type`)
);

CREATE TABLE IF NOT EXISTS `lw_user` (
    `user_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `username` varchar(50) NOT NULL,
    `password` varchar(512) DEFAULT NULL,
    `hashing` enum ('EMPTY','MD5','PBKDF2') NOT NULL DEFAULT 'MD5',
    `email` varchar(250) DEFAULT NULL,
    `email_confirmation_token` varchar(32) DEFAULT NULL,
    `is_email_confirmed` tinyint(1) unsigned NOT NULL DEFAULT 1,
    `organisation_id` int(10) unsigned NOT NULL,
    `image_file_id` int(10) unsigned DEFAULT NULL,
    `gender` tinyint(1) NOT NULL DEFAULT 0,
    `dateofbirth` date DEFAULT NULL,
    `address` varchar(255) DEFAULT NULL,
    `profession` varchar(105) DEFAULT NULL,
    `additionalinformation` varchar(255) DEFAULT NULL,
    `interest` varchar(255) DEFAULT NULL,
    `student_identifier` varchar(50) DEFAULT NULL,
    `phone` varchar(255) DEFAULT NULL,
    `is_admin` tinyint(1) NOT NULL DEFAULT 0,
    `is_moderator` tinyint(1) NOT NULL DEFAULT 0,
    `registration_date` timestamp NOT NULL DEFAULT current_timestamp(),
    `preferences` blob DEFAULT NULL,
    `credits` varchar(255) DEFAULT NULL,
    `fullname` varchar(105) DEFAULT NULL,
    `affiliation` varchar(105) DEFAULT NULL,
    `accept_terms_and_conditions` tinyint(1) NOT NULL DEFAULT 0,
    `preferred_notification_frequency` enum ('NEVER','DAILY','WEEKLY','MONTHLY') NOT NULL DEFAULT 'NEVER',
    `time_zone` varchar(100) DEFAULT 'Europe/Berlin',
    `language` varchar(10) DEFAULT 'en-UK',
    `guides` bigint(20) NOT NULL DEFAULT 0,
    UNIQUE KEY `lw_user_username` (`username`)
);

CREATE TABLE IF NOT EXISTS `lw_user_auth` (
    `auth_id` bigint(20) unsigned NOT NULL PRIMARY KEY,
    `user_id` int(10) unsigned NOT NULL,
    `token_hash` varchar(64) NOT NULL,
    `expires` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) COMMENT ='Used to store users sessions, used by "Remember for 30 days" feature';

CREATE TABLE IF NOT EXISTS `lw_user_course` (
    `user_id` int(10) unsigned NOT NULL,
    `course_id` int(10) unsigned NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
    PRIMARY KEY (`user_id`, `course_id`)
) COMMENT ='TODO: rename to lw_course_user';

CREATE TABLE IF NOT EXISTS `lw_user_log` (
    `log_entry_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` int(10) unsigned NOT NULL,
    `session_id` char(32) NOT NULL,
    `action` tinyint(3) unsigned NOT NULL,
    `target_id` int(11) DEFAULT NULL,
    `params` varchar(255) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
    `group_id` int(10) unsigned DEFAULT NULL,
    KEY `lw_user_log_session_id` (`session_id`),
    KEY `lw_user_log_group_id` (`group_id`, `action`, `user_id`)
) COMMENT ='TODO: refactor to multiple tables, one for each target_id';

CREATE TABLE IF NOT EXISTS `lw_user_log_action` (
    `action` tinyint(4) NOT NULL PRIMARY KEY,
    `name` varchar(100) NOT NULL,
    `target` enum ('NONE','RESOURCE_ID','GROUP_ID','USER_ID','FORUM_TOPIC_ID','FORUM_POST_ID','COURSE_ID','FOLDER_ID','OTHER') NOT NULL,
    `category` enum ('OTHER','RESOURCE','FOLDER','GROUP','GLOSSARY','SURVEY','SEARCH','USER','FORUM','MODERATOR') NOT NULL
) COMMENT ='This table is only used for debugging. Is not automatically synced with Action.java';

CREATE TABLE IF NOT EXISTS `lw_user_token` (
    `token_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` int(10) unsigned NOT NULL,
    `type` enum ('grant') NOT NULL,
    `token` varchar(128) NOT NULL,
    `created_at` datetime NOT NULL DEFAULT current_timestamp()
);

ALTER TABLE `lw_course` ADD CONSTRAINT `FK_lw_course_lw_group` FOREIGN KEY (`default_group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_course` ADD CONSTRAINT `FK_lw_course_lw_organisation` FOREIGN KEY (`organisation_id`) REFERENCES `lw_organisation` (`organisation_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_comment` ADD CONSTRAINT `FK_lw_comment_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_comment` ADD CONSTRAINT `FK_lw_comment_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_file` ADD CONSTRAINT `FK_lw_file_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `lw_forum_post` ADD CONSTRAINT `FK_lw_forum_post_lw_forum_topic` FOREIGN KEY (`topic_id`) REFERENCES `lw_forum_topic` (`topic_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_forum_post` ADD CONSTRAINT `FK_lw_forum_post_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_forum_post` ADD CONSTRAINT `FK_lw_forum_post_lw_user_edit` FOREIGN KEY (`post_edit_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_forum_topic` ADD CONSTRAINT `FK_lw_forum_topic_lw_forum_post` FOREIGN KEY (`topic_last_post_id`) REFERENCES `lw_forum_post` (`post_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_forum_topic` ADD CONSTRAINT `FK_lw_forum_topic_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_forum_topic` ADD CONSTRAINT `FK_lw_forum_topic_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_forum_topic` ADD CONSTRAINT `FK_lw_forum_topic_lw_user_last_post` FOREIGN KEY (`topic_last_post_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_glossary_entry` ADD CONSTRAINT `FK_lw_glossary_entry_lw_glossary_entry` FOREIGN KEY (`original_entry_id`) REFERENCES `lw_glossary_entry` (`entry_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_entry` ADD CONSTRAINT `FK_lw_glossary_entry_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_entry` ADD CONSTRAINT `FK_lw_glossary_entry_lw_user_changed` FOREIGN KEY (`last_changed_by_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_entry` ADD CONSTRAINT `FK_lw_glossary_entry_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_glossary_resource` ADD CONSTRAINT `FK_lw_glossary_resource_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_glossary_term` ADD CONSTRAINT `FK_lw_glossary_term_lw_glossary_entry` FOREIGN KEY (`entry_id`) REFERENCES `lw_glossary_entry` (`entry_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_term` ADD CONSTRAINT `FK_lw_glossary_term_lw_glossary_term` FOREIGN KEY (`original_term_id`) REFERENCES `lw_glossary_term` (`term_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_term` ADD CONSTRAINT `FK_lw_glossary_term_lw_user_changed` FOREIGN KEY (`last_changed_by_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_term` ADD CONSTRAINT `FK_lw_glossary_term_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_group` ADD CONSTRAINT `FK_lw_group_lw_course` FOREIGN KEY (`course_id`) REFERENCES `lw_course` (`course_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_group` ADD CONSTRAINT `FK_lw_group_lw_user` FOREIGN KEY (`leader_id`) REFERENCES `lw_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `lw_group_folder` ADD CONSTRAINT `FK_lw_group_folder_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_group_folder` ADD CONSTRAINT `FK_lw_group_folder_lw_group_folder` FOREIGN KEY (`parent_folder_id`) REFERENCES `lw_group_folder` (`folder_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_group_folder` ADD CONSTRAINT `FK_lw_group_folder_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_group_user` ADD CONSTRAINT `FK_lw_group_user_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_group_user` ADD CONSTRAINT `FK_lw_group_user_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_message` ADD CONSTRAINT `FK_lw_message_lw_user_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_message` ADD CONSTRAINT `FK_lw_message_lw_user_recipient` FOREIGN KEY (`recipient_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_news` ADD CONSTRAINT `FK_lw_news_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_organisation` ADD CONSTRAINT `FK_lw_organisation_lw_file` FOREIGN KEY (`banner_image_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_resource` ADD CONSTRAINT `FK_lw_resource_lw_file_t0` FOREIGN KEY (`thumbnail0_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `FK_lw_resource_lw_file_t1` FOREIGN KEY (`thumbnail1_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `FK_lw_resource_lw_file_t2` FOREIGN KEY (`thumbnail2_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `FK_lw_resource_lw_file_t3` FOREIGN KEY (`thumbnail3_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `FK_lw_resource_lw_file_t4` FOREIGN KEY (`thumbnail4_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `FK_lw_resource_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `FK_lw_resource_lw_group_folder` FOREIGN KEY (`folder_id`) REFERENCES `lw_group_folder` (`folder_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `FK_lw_resource_lw_resource` FOREIGN KEY (`original_resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_resource` ADD CONSTRAINT `FK_lw_resource_lw_user` FOREIGN KEY (`owner_user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `lw_resource_archiveurl` ADD CONSTRAINT `FK_lw_resource_archiveurl_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_resource_history` ADD CONSTRAINT `FK_lw_resource_history_lw_file` FOREIGN KEY (`file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_history` ADD CONSTRAINT `FK_lw_resource_history_lw_file_prev` FOREIGN KEY (`prev_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_history` ADD CONSTRAINT `FK_lw_resource_history_lw_file_changes` FOREIGN KEY (`changes_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_history` ADD CONSTRAINT `FK_lw_resource_history_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_history` ADD CONSTRAINT `FK_lw_resource_history_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_resource_rating` ADD CONSTRAINT `FK_lw_resource_rating_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_rating` ADD CONSTRAINT `FK_lw_resource_rating_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_resource_tag` ADD CONSTRAINT `FK_lw_resource_tag_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_tag` ADD CONSTRAINT `FK_lw_resource_tag_lw_tag` FOREIGN KEY (`tag_id`) REFERENCES `lw_tag` (`tag_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_resource_tag` ADD CONSTRAINT `FK_lw_resource_tag_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_submission` ADD CONSTRAINT `FK_lw_submission_lw_course` FOREIGN KEY (`course_id`) REFERENCES `lw_course` (`course_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_submission` ADD CONSTRAINT `FK_lw_submission_lw_resource` FOREIGN KEY (`survey_resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_submission_resource` ADD CONSTRAINT `FK_lw_submission_resource_lw_submission` FOREIGN KEY (`submission_id`) REFERENCES `lw_submission` (`submission_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_submission_resource` ADD CONSTRAINT `FK_lw_submission_resource_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_submission_resource` ADD CONSTRAINT `FK_lw_submission_resource_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_submission_status` ADD CONSTRAINT `FK_lw_submission_status_lw_submission` FOREIGN KEY (`submission_id`) REFERENCES `lw_submission` (`submission_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_submission_status` ADD CONSTRAINT `FK_lw_submission_status_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_submission_status` ADD CONSTRAINT `FK_lw_submission_status_lw_resource` FOREIGN KEY (`survey_resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey` ADD CONSTRAINT `FK_lw_survey_lw_organisation` FOREIGN KEY (`organization_id`) REFERENCES `lw_organisation` (`organisation_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey` ADD CONSTRAINT `FK_lw_survey_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey_answer` ADD CONSTRAINT `FK_lw_survey_answer_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey_answer` ADD CONSTRAINT `FK_lw_survey_answer_lw_survey_question` FOREIGN KEY (`question_id`) REFERENCES `lw_survey_question` (`question_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey_answer` ADD CONSTRAINT `FK_lw_survey_answer_lw_survey_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_survey_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey_question` ADD CONSTRAINT `FK_lw_survey_question_lw_survey` FOREIGN KEY (`survey_id`) REFERENCES `lw_survey` (`survey_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey_question_option` ADD CONSTRAINT `FK_question_id` FOREIGN KEY (`question_id`) REFERENCES `lw_survey_question` (`question_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey_resource` ADD CONSTRAINT `FK_lw_survey_resource_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey_resource` ADD CONSTRAINT `FK_lw_survey_resource_lw_survey` FOREIGN KEY (`survey_id`) REFERENCES `lw_survey` (`survey_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_survey_resource_user` ADD CONSTRAINT `FK_lw_survey_resource_user_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey_resource_user` ADD CONSTRAINT `FK_lw_survey_resource_user_lw_survey_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_survey_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_thumb` ADD CONSTRAINT `FK_lw_thumb_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_thumb` ADD CONSTRAINT `FK_lw_thumb_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_transcript_actions` ADD CONSTRAINT `FK_lw_transcript_actions_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_transcript_actions` ADD CONSTRAINT `FK_lw_transcript_actions_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_transcript_selections` ADD CONSTRAINT `FK_lw_transcript_selections_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_transcript_summary` ADD CONSTRAINT `FK_lw_transcript_summary_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_transcript_summary` ADD CONSTRAINT `FK_lw_transcript_summary_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_user` ADD CONSTRAINT `FK_lw_user_lw_file` FOREIGN KEY (`image_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `lw_user` ADD CONSTRAINT `FK_lw_user_lw_organisation` FOREIGN KEY (`organisation_id`) REFERENCES `lw_organisation` (`organisation_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_user_auth` ADD CONSTRAINT `FK_lw_user_auth_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_user_course` ADD CONSTRAINT `FK_lw_user_course_lw_course` FOREIGN KEY (`course_id`) REFERENCES `lw_course` (`course_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_user_course` ADD CONSTRAINT `FK_lw_user_course_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_user_log` ADD CONSTRAINT `FK_lw_user_log_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_user_log` ADD CONSTRAINT `FK_lw_user_log_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `lw_user_token` ADD CONSTRAINT `FK_lw_user_token_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;