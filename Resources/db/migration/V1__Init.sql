CREATE TABLE IF NOT EXISTS lw_bans (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `type` varchar(10) DEFAULT 'IP',
    `name` varchar(64) NOT NULL,
    `bandate` datetime DEFAULT NULL,
    `bannedon` datetime DEFAULT NULL,
    `attempts` int(11) DEFAULT NULL,
    `reason` varchar(200) DEFAULT NULL,
    UNIQUE KEY `lw_bans_name` (`name`)
);

CREATE TABLE IF NOT EXISTS `lw_bounces` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `address` varchar(64) DEFAULT NULL,
    `timereceived` datetime DEFAULT NULL,
    `code` varchar(10) NOT NULL,
    `description` varchar(64) DEFAULT NULL,
    UNIQUE KEY `lw_bounces_address` (`address`)
);

CREATE TABLE IF NOT EXISTS `lw_comment` (
    `comment_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `resource_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `text` mediumtext NOT NULL,
    `date` datetime NOT NULL,
    KEY `lw_comment_user_id` (`user_id`),
    KEY `lw_comment_resource_id` (`resource_id`)
);

CREATE TABLE IF NOT EXISTS `lw_course` (
    `course_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` varchar(50) NOT NULL,
    `options_field1` bigint(20) NOT NULL DEFAULT 1,
    `organisation_id` int(10) unsigned NOT NULL DEFAULT 0,
    `default_group_id` int(10) unsigned NOT NULL DEFAULT 0,
    `wizard_param` varchar(100) DEFAULT NULL,
    `next_x_users_become_moderator` tinyint(3) unsigned NOT NULL DEFAULT 0,
    `welcome_message` text DEFAULT NULL,
    `timestamp_update` timestamp NOT NULL DEFAULT current_timestamp(),
    `timestamp_creation` timestamp NOT NULL DEFAULT current_timestamp(),
    UNIQUE KEY `lw_course_wizard_param` (`wizard_param`),
    KEY `lw_course_organisation_id` (`organisation_id`)
);

CREATE TABLE IF NOT EXISTS `lw_file` (
    `file_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(4) NOT NULL DEFAULT 0,
    `resource_id` int(10) NOT NULL,
    `resource_file_number` smallint(5) unsigned NOT NULL,
    `name` varchar(255) NOT NULL,
    `mime_type` varchar(255) NOT NULL,
    `log_actived` tinyint(4) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `missing` tinyint(4) NOT NULL DEFAULT 0,
    `doc_key` varchar(40) DEFAULT NULL,
    KEY `lw_file_resource_id` (`resource_id`)
);

CREATE TABLE IF NOT EXISTS `lw_forum_post` (
    `post_id` int(11) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(4) NOT NULL DEFAULT 0,
    `topic_id` int(11) unsigned NOT NULL,
    `user_id` int(11) unsigned NOT NULL,
    `text` text NOT NULL,
    `category` varchar(70) DEFAULT '',
    `post_time` timestamp NULL DEFAULT NULL,
    `post_edit_time` timestamp NULL DEFAULT NULL,
    `post_edit_count` smallint(11) unsigned NOT NULL DEFAULT 0,
    `post_edit_user_id` int(10) unsigned NOT NULL,
    KEY `lw_forum_post_user_id` (`user_id`),
    KEY `lw_forum_post_topic_id` (`topic_id`, `post_time`)
);

CREATE TABLE IF NOT EXISTS `lw_forum_topic` (
    `topic_id` int(11) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` int(11) unsigned NOT NULL,
    `deleted` tinyint(3) unsigned NOT NULL DEFAULT 0,
    `topic_title` varchar(100) NOT NULL DEFAULT '',
    `user_id` int(11) unsigned NOT NULL DEFAULT 0,
    `topic_time` timestamp NULL DEFAULT NULL,
    `topic_views` int(11) DEFAULT 1,
    `topic_replies` int(11) DEFAULT 0,
    `topic_last_post_id` int(11) unsigned NOT NULL DEFAULT 0,
    `topic_last_post_time` timestamp NOT NULL,
    `topic_last_post_user_id` int(10) unsigned NOT NULL,
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
    `resource_id` int(10) NOT NULL,
    `original_entry_id` bigint(10) NOT NULL DEFAULT 0 COMMENT 'is set to -1 if the entry was imported from a file',
    `last_changed_by_user_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `deleted` tinyint(4) NOT NULL DEFAULT 0,
    `topic_one` varchar(100) DEFAULT NULL,
    `topic_two` varchar(100) DEFAULT NULL,
    `topic_three` varchar(100) DEFAULT NULL,
    `description` varchar(3000) DEFAULT NULL,
    `description_pasted` tinyint(4) NOT NULL DEFAULT 0,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    KEY `lw_glossary_entry_resource_id` (`resource_id`, `deleted`)
);

CREATE TABLE IF NOT EXISTS `lw_glossary_resource` (
    `resource_id` int(10) NOT NULL PRIMARY KEY,
    `allowed_languages` varchar(200) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `lw_glossary_term` (
    `term_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `entry_id` int(10) unsigned NOT NULL,
    `original_term_id` int(11) NOT NULL DEFAULT 0,
    `last_changed_by_user_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `deleted` tinyint(4) NOT NULL DEFAULT 0,
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
    `phraseology_pasted` tinyint(4) NOT NULL DEFAULT 0,
    KEY `lw_glossary_term_entry_id` (`entry_id`)
);

CREATE TABLE IF NOT EXISTS `lw_group` (
    `group_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(4) NOT NULL DEFAULT 0,
    `title` varchar(150) NOT NULL,
    `description` mediumtext DEFAULT NULL,
    `creation_time` timestamp NOT NULL DEFAULT current_timestamp(),
    `course_id` int(10) NOT NULL DEFAULT -1,
    `max_member_count` smallint(6) NOT NULL DEFAULT -1 COMMENT 'number of allowed members; -1 = unlimitted',
    `restriction_forum_category_required` tinyint(1) NOT NULL DEFAULT 0,
    `leader_id` int(10) unsigned NOT NULL,
    `group_category_id` int(10) unsigned NOT NULL DEFAULT 0,
    `policy_add` enum ('GROUP_MEMBERS','GROUP_LEADER') NOT NULL DEFAULT 'GROUP_MEMBERS',
    `policy_annotate` enum ('ALL_LEARNWEB_USERS','COURSE_MEMBERS','GROUP_MEMBERS','GROUP_LEADER') NOT NULL DEFAULT 'ALL_LEARNWEB_USERS',
    `policy_edit` enum ('GROUP_MEMBERS','GROUP_LEADER','GROUP_LEADER_AND_FILE_OWNER') NOT NULL DEFAULT 'GROUP_MEMBERS',
    `policy_join` enum ('COURSE_MEMBERS','NOBODY','ALL_LEARNWEB_USERS','ORGANISATION_MEMBERS') NOT NULL DEFAULT 'COURSE_MEMBERS',
    `policy_view` enum ('ALL_LEARNWEB_USERS','COURSE_MEMBERS','GROUP_MEMBERS','GROUP_LEADER') NOT NULL DEFAULT 'ALL_LEARNWEB_USERS',
    `hypothesis_link` varchar(255) DEFAULT NULL,
    `hypothesis_token` varchar(255) DEFAULT NULL,
    KEY `lw_group_title` (`title`),
    KEY `lw_group_creation_time` (`creation_time`),
    KEY `lw_group_course` (`deleted`, `group_id`, `policy_join`, `course_id`),
    KEY `lw_group_course_id` (`course_id`)
);

CREATE TABLE IF NOT EXISTS `lw_group_folder` (
    `folder_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) unsigned DEFAULT 0,
    `group_id` int(10) unsigned NOT NULL,
    `parent_folder_id` int(10) unsigned NOT NULL DEFAULT 0,
    `name` varchar(100) NOT NULL,
    `description` text DEFAULT NULL,
    `user_id` int(11) NOT NULL DEFAULT -1,
    `last_change` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    KEY `lw_group_folder_group_id` (`group_id`, `parent_folder_id`)
);

CREATE TABLE IF NOT EXISTS `lw_group_user` (
    `group_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `join_time` timestamp NOT NULL DEFAULT current_timestamp(),
    `last_visit` int(10) unsigned NOT NULL DEFAULT 0,
    `notification_frequency` enum ('NEVER','DAILY','WEEKLY','MONTHLY') NOT NULL DEFAULT 'NEVER',
    PRIMARY KEY (`group_id`, `user_id`),
    KEY `lw_group_user_user_id` (`user_id`)
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
    `title` varchar(60) NOT NULL,
    `logo` longtext DEFAULT NULL,
    `logout_page` varchar(255) DEFAULT NULL COMMENT 'page to show after logout',
    `welcome_page` varchar(255) DEFAULT NULL,
    `welcome_message` text DEFAULT NULL,
    `options_field1` bigint(20) NOT NULL DEFAULT 0,
    `default_search_text` varchar(16) NOT NULL DEFAULT 'bing',
    `default_search_image` varchar(16) NOT NULL DEFAULT 'flickr',
    `default_search_video` varchar(16) NOT NULL DEFAULT 'youtube',
    `default_language` char(2) DEFAULT NULL,
    `language_variant` varchar(10) DEFAULT NULL,
    `banner_image_file_id` int(10) unsigned NOT NULL DEFAULT 0,
    `css_file` varchar(100) DEFAULT NULL,
    `glossary_languages` varchar(1000) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `lw_requests` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `IP` varchar(64) NOT NULL,
    `requests` int(11) DEFAULT NULL,
    `logins` int(11) DEFAULT NULL,
    `usernames` varchar(512) DEFAULT NULL,
    `time` datetime NOT NULL
);

CREATE TABLE IF NOT EXISTS `lw_resource` (
    `resource_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(4) NOT NULL DEFAULT 0,
    `group_id` int(10) unsigned NOT NULL DEFAULT 0,
    `folder_id` int(10) unsigned NOT NULL DEFAULT 0,
    `title` varchar(1000) NOT NULL,
    `description` mediumtext NOT NULL,
    `url` varchar(4000) DEFAULT NULL,
    `storage_type` tinyint(4) NOT NULL,
    `rights` tinyint(4) NOT NULL,
    `source` varchar(255) NOT NULL,
    `language` varchar(200) DEFAULT NULL,
    `author` varchar(255) DEFAULT NULL,
    `type` varchar(255) NOT NULL,
    `format` varchar(255) NOT NULL,
    `duration` int(10) unsigned NOT NULL DEFAULT 0,
    `owner_user_id` int(11) NOT NULL,
    `id_at_service` varchar(100) DEFAULT NULL,
    `rating` int(11) NOT NULL,
    `rate_number` int(11) NOT NULL,
    `filename` varchar(200) DEFAULT NULL,
    `file_url` varchar(1000) DEFAULT NULL,
    `max_image_url` varchar(1000) DEFAULT NULL,
    `query` varchar(1000) DEFAULT NULL,
    `original_resource_id` int(10) unsigned NOT NULL DEFAULT 0,
    `machine_description` longtext DEFAULT NULL,
    `access` varchar(1) DEFAULT NULL,
    `thumbnail0_url` varchar(255) DEFAULT NULL,
    `thumbnail0_file_id` int(10) unsigned NOT NULL DEFAULT 0,
    `thumbnail0_width` smallint(5) unsigned NOT NULL DEFAULT 0,
    `thumbnail0_height` smallint(5) unsigned NOT NULL DEFAULT 0,
    `thumbnail1_url` varchar(255) DEFAULT NULL,
    `thumbnail1_file_id` int(10) unsigned NOT NULL DEFAULT 0,
    `thumbnail1_width` smallint(5) unsigned NOT NULL DEFAULT 0,
    `thumbnail1_height` smallint(5) unsigned NOT NULL DEFAULT 0,
    `thumbnail2_url` varchar(255) DEFAULT NULL,
    `thumbnail2_file_id` int(10) unsigned NOT NULL DEFAULT 0,
    `thumbnail2_width` smallint(5) unsigned NOT NULL DEFAULT 0,
    `thumbnail2_height` smallint(5) unsigned NOT NULL DEFAULT 0,
    `thumbnail3_url` varchar(255) DEFAULT NULL,
    `thumbnail3_file_id` int(10) unsigned NOT NULL DEFAULT 0,
    `thumbnail3_width` smallint(5) unsigned NOT NULL DEFAULT 0,
    `thumbnail3_height` smallint(5) unsigned NOT NULL DEFAULT 0,
    `thumbnail4_url` varchar(255) DEFAULT NULL,
    `thumbnail4_file_id` int(10) unsigned NOT NULL DEFAULT 0,
    `thumbnail4_width` smallint(5) unsigned NOT NULL DEFAULT 0,
    `thumbnail4_height` smallint(5) unsigned NOT NULL DEFAULT 0,
    `embeddedRaw` mediumtext DEFAULT NULL,
    `transcript` longtext DEFAULT NULL,
    `read_only_transcript` tinyint(4) NOT NULL DEFAULT 0,
    `online_status` enum ('UNKNOWN','ONLINE','OFFLINE','PROCESSING') NOT NULL DEFAULT 'UNKNOWN' COMMENT '0=unknown',
    `restricted` tinyint(4) NOT NULL DEFAULT 0,
    `resource_timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `creation_date` datetime DEFAULT NULL,
    `metadata` blob DEFAULT NULL,
    KEY `lw_resource_type` (`type`),
    KEY `lw_resource_storage_type` (`storage_type`, `deleted`),
    KEY `lw_resource_url` (`url`),
    KEY `lw_resource_owner_user_id` (`owner_user_id`, `deleted`),
    KEY `lw_resource_folder_id` (`folder_id`),
    KEY `lw_resource_group_id` (`group_id`)
);

CREATE TABLE IF NOT EXISTS `lw_resource_archiveurl` (
    `resource_id` int(10) unsigned DEFAULT NULL,
    `archive_url` varchar(600) DEFAULT NULL,
    `timestamp` timestamp NULL DEFAULT NULL,
    KEY `lw_resource_archiveurl_resource_id` (`resource_id`)
);

CREATE TABLE IF NOT EXISTS `lw_resource_history` (
    `resource_history_id` int(11) NOT NULL AUTO_INCREMENT,
    `resource_id` int(11) NOT NULL,
    `user_id` int(11) NOT NULL,
    `file_id` int(11) NOT NULL,
    `prev_file_id` int(11) NOT NULL,
    `changes_file_id` int(11) NOT NULL,
    `server_version` varchar(20) NOT NULL,
    `document_created` varchar(20) NOT NULL,
    `document_key` varchar(20) NOT NULL,
    `document_version` int(11) NOT NULL,
    `document_changes` mediumtext DEFAULT NULL,
    PRIMARY KEY (`resource_history_id`)
);

CREATE TABLE IF NOT EXISTS `lw_resource_rating` (
    `resource_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `rating` tinyint(4) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`resource_id`, `user_id`),
    KEY `lw_resource_rating_user_id` (`user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_resource_tag` (
    `resource_id` int(10) unsigned NOT NULL,
    `tag_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
    KEY `lw_resource_tag_resource_id` (`resource_id`),
    KEY `lw_resource_tag_tag_id` (`tag_id`),
    KEY `lw_resource_tag_user_id` (`user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_submission` (
    `submission_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(4) NOT NULL DEFAULT 0,
    `course_id` int(10) unsigned NOT NULL,
    `title` varchar(100) DEFAULT NULL,
    `description` varchar(1000) DEFAULT NULL,
    `open_datetime` datetime DEFAULT NULL,
    `close_datetime` datetime DEFAULT NULL,
    `number_of_resources` int(10) NOT NULL DEFAULT 3,
    `survey_resource_id` int(10) DEFAULT -1 COMMENT 'todo change to unsigned',
    KEY `lw_submission_course_id` (`course_id`, `deleted`)
);

CREATE TABLE IF NOT EXISTS `lw_submission_resource` (
    `submission_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `resource_id` int(10) unsigned NOT NULL,
    PRIMARY KEY (`submission_id`, `user_id`, `resource_id`),
    KEY `lw_submission_resource_resource_id` (`resource_id`)
);

CREATE TABLE IF NOT EXISTS `lw_submission_status` (
    `submission_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `submitted` tinyint(4) NOT NULL DEFAULT 1,
    `survey_resource_id` int(10) unsigned NOT NULL DEFAULT 0,
    PRIMARY KEY (`submission_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_survey` (
    `survey_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `organization_id` int(10) unsigned NOT NULL,
    `title` varchar(100) NOT NULL,
    `description` varchar(1000) NOT NULL,
    `user_id` int(10) NOT NULL COMMENT 'the user who created this template',
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `public_template` tinyint(1) NOT NULL DEFAULT 0,
    KEY `lw_survey_organization_id` (`organization_id`)
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
    `required` tinyint(1) NOT NULL DEFAULT 0,
    KEY `lw_survey_question_survey_id` (`survey_id`),
    CONSTRAINT `lw_survey_question_ibfk_1` FOREIGN KEY (`survey_id`) REFERENCES `lw_survey` (`survey_id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `lw_survey_resource` (
    `resource_id` int(10) unsigned NOT NULL PRIMARY KEY,
    `survey_id` int(10) unsigned NOT NULL,
    `open_date` datetime DEFAULT NULL,
    `close_date` datetime DEFAULT NULL,
    `editable` tinyint(1) NOT NULL DEFAULT 0,
    KEY `lw_survey_resource_survey_id` (`survey_id`)
);

CREATE TABLE IF NOT EXISTS `lw_survey_answer` (
    `resource_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `question_id` int(10) unsigned NOT NULL,
    `answer` varchar(6000) DEFAULT NULL COMMENT 'delimited by ||| if multi valued',
    PRIMARY KEY (`resource_id`, `user_id`, `question_id`),
    KEY `lw_survey_answer_question_id` (`question_id`),
    CONSTRAINT `lw_survey_answer_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `lw_survey_question` (`question_id`),
    CONSTRAINT `lw_survey_answer_ibfk_2` FOREIGN KEY (`resource_id`) REFERENCES `lw_survey_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `lw_survey_question_option` (
    `answer_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `question_id` int(10) unsigned NOT NULL,
    `value` varchar(1000) DEFAULT NULL,
    KEY `FK_question_id` (`question_id`),
    CONSTRAINT `FK_question_id` FOREIGN KEY (`question_id`) REFERENCES `lw_survey_question` (`question_id`)
);

CREATE TABLE IF NOT EXISTS `lw_survey_resource_user` (
    `resource_id` int(11) unsigned NOT NULL,
    `user_id` int(11) unsigned NOT NULL,
    `submitted` tinyint(4) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
    PRIMARY KEY (`resource_id`, `user_id`),
    KEY `lw_survey_resource_user_ibfk_2` (`user_id`),
    CONSTRAINT `lw_survey_resource_user_ibfk_1` FOREIGN KEY (`resource_id`) REFERENCES `lw_survey_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE
) COMMENT ='Contains submission status for a particular user';

CREATE TABLE IF NOT EXISTS `lw_tag` (
    `tag_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` varchar(230) NOT NULL,
    KEY `lw_tag_name` (`name`)
);

CREATE TABLE IF NOT EXISTS `lw_thumb` (
    `resource_id` int(10) unsigned NOT NULL,
    `user_id` int(10) unsigned NOT NULL,
    `direction` tinyint(4) NOT NULL,
    PRIMARY KEY (`resource_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `lw_transcript_actions` (
    `user_id` int(10) NOT NULL,
    `resource_id` int(10) NOT NULL,
    `words_selected` longtext NOT NULL,
    `user_annotation` mediumtext NOT NULL,
    `action` char(25) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
);

CREATE TABLE IF NOT EXISTS `lw_transcript_final_sel` (
    `resource_id` int(10) NOT NULL,
    `possibility` int(10) NOT NULL,
    `words_selected` longtext NOT NULL,
    `tag_ids` text NOT NULL,
    `start_offset` int(10) NOT NULL,
    `end_offset` int(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS `lw_transcript_final_tags` (
    `tag_id` int(10) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `tag` mediumtext NOT NULL,
    `abstract` tinyint(4) NOT NULL
);

CREATE TABLE IF NOT EXISTS `lw_transcript_selections` (
    `resource_id` int(10) NOT NULL,
    `words_selected` longtext NOT NULL,
    `user_annotation` mediumtext NOT NULL,
    `ua_corrected` mediumtext DEFAULT NULL,
    `start_offset` int(10) NOT NULL,
    `end_offset` int(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS `lw_transcript_summary` (
    `user_id` int(10) NOT NULL,
    `resource_id` int(10) NOT NULL,
    `summary_type` enum ('SHORT','LONG','DETAILED') NOT NULL,
    `summary_text` mediumtext NOT NULL,
    PRIMARY KEY (`user_id`, `resource_id`, `summary_type`)
);

CREATE TABLE IF NOT EXISTS `lw_user` (
    `user_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(4) NOT NULL DEFAULT 0,
    `username` varchar(50) NOT NULL,
    `password` varchar(512) DEFAULT NULL,
    `hashing` enum ('EMPTY','MD5','PBKDF2') NOT NULL DEFAULT 'MD5',
    `email` varchar(250) DEFAULT NULL,
    `email_confirmation_token` varchar(32) DEFAULT NULL,
    `is_email_confirmed` tinyint(1) unsigned NOT NULL DEFAULT 1,
    `organisation_id` int(10) unsigned NOT NULL DEFAULT 0,
    `image_file_id` int(10) unsigned NOT NULL DEFAULT 0,
    `gender` tinyint(4) NOT NULL DEFAULT 0,
    `dateofbirth` date DEFAULT NULL,
    `address` varchar(255) DEFAULT NULL,
    `profession` varchar(105) DEFAULT NULL,
    `additionalinformation` varchar(255) DEFAULT NULL,
    `interest` varchar(255) DEFAULT NULL,
    `student_identifier` varchar(50) DEFAULT NULL,
    `phone` varchar(255) DEFAULT NULL,
    `is_admin` tinyint(4) NOT NULL DEFAULT 0,
    `is_moderator` tinyint(4) NOT NULL DEFAULT 0,
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
);

CREATE TABLE IF NOT EXISTS `lw_user_course` (
    `user_id` int(10) unsigned NOT NULL,
    `course_id` int(10) unsigned NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
    KEY `lw_user_course_user_id` (`user_id`),
    KEY `lw_user_course_course_id` (`course_id`)
);

CREATE TABLE IF NOT EXISTS `lw_user_log` (
    `log_entry_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` int(10) unsigned NOT NULL,
    `session_id` char(32) NOT NULL,
    `action` tinyint(3) unsigned NOT NULL,
    `target_id` int(11) NOT NULL,
    `params` varchar(255) NOT NULL,
    `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
    `group_id` int(10) unsigned NOT NULL,
    KEY `lw_user_log_session_id` (`session_id`),
    KEY `lw_user_log_user_id` (`user_id`),
    KEY `lw_user_log_group_id` (`group_id`, `action`, `user_id`)
) ENGINE = MyISAM
 ;

CREATE TABLE IF NOT EXISTS `lw_user_log_action` (
    `action` tinyint(4) NOT NULL PRIMARY KEY,
    `name` varchar(100) NOT NULL,
    `target` enum ('NONE','RESOURCE_ID','GROUP_ID','USER_ID','FORUM_TOPIC_ID','FORUM_POST_ID','COURSE_ID','FOLDER_ID','OTHER') NOT NULL,
    `category` enum ('OTHER','RESOURCE','FOLDER','GROUP','GLOSSARY','SURVEY','SEARCH','USER','FORUM','MODERATOR') NOT NULL
) COMMENT ='This table is only used for debugging. Is not automatically synced with Action.java';

CREATE TABLE IF NOT EXISTS `lw_user_token` (
    `token_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `user_id` int(10) unsigned NOT NULL,
    `type` enum ('grant') NOT NULL,
    `token` varchar(128) NOT NULL,
    `created_at` datetime NOT NULL DEFAULT current_timestamp(),
    PRIMARY KEY (`token_id`),
    KEY `lw_user_token_FK_lw_user_token_lw_user` (`user_id`),
    CONSTRAINT `FK_lw_user_token_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS `message` (
    `message_id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `from_user` int(11) NOT NULL,
    `to_user` int(11) NOT NULL,
    `m_title` varchar(2550) NOT NULL,
    `m_text` longtext NOT NULL,
    `m_seen` tinyint(1) NOT NULL,
    `m_read` tinyint(1) NOT NULL,
    `m_time` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    KEY `message_to_user` (`to_user`, `m_seen`)
);

CREATE TABLE IF NOT EXISTS `speechrepository_video` (
    `id` int(11) NOT NULL,
    `title` varchar(1000) NOT NULL,
    `url` varchar(1000) NOT NULL,
    `rights` varchar(1000) NOT NULL,
    `date` varchar(1000) NOT NULL,
    `description` varchar(1000) NOT NULL,
    `notes` varchar(2000) DEFAULT NULL,
    `image_link` varchar(1000) NOT NULL,
    `video_link` varchar(1000) NOT NULL,
    `duration` int(11) NOT NULL,
    `language` varchar(1000) NOT NULL,
    `level` varchar(1000) DEFAULT NULL,
    `use` varchar(1000) DEFAULT NULL,
    `type` varchar(1000) DEFAULT NULL,
    `domains` varchar(1000) DEFAULT NULL,
    `terminology` text DEFAULT NULL,
    `learnweb_resource_id` int(10) unsigned NOT NULL DEFAULT 0,
    KEY `speechrepository_video_learnweb_resource_id` (`learnweb_resource_id`)
);

CREATE TABLE IF NOT EXISTS `ted_transcripts_lang_mapping` (
    `language_code` char(10) NOT NULL,
    `language` char(25) NOT NULL,
    UNIQUE KEY `ted_transcripts_lang_mapping_language_code` (`language_code`, `language`)
);

CREATE TABLE IF NOT EXISTS `ted_transcripts_paragraphs` (
    `resource_id` int(10) unsigned NOT NULL,
    `language` char(10) NOT NULL,
    `starttime` int(10) unsigned NOT NULL,
    `paragraph` longtext NOT NULL,
    KEY `ted_transcripts_paragraphs_resource_id` (`resource_id`, `language`)
);

CREATE TABLE IF NOT EXISTS `ted_video` (
    `ted_id` int(10) unsigned NOT NULL DEFAULT 0 PRIMARY KEY,
    `resource_id` int(10) unsigned NOT NULL DEFAULT 0,
    `title` varchar(200) NOT NULL,
    `description` mediumtext NOT NULL,
    `slug` varchar(200) NOT NULL,
    `viewed_count` int(10) unsigned NOT NULL,
    `published_at` timestamp NULL DEFAULT NULL,
    `talk_updated_at` timestamp NULL DEFAULT NULL,
    `photo1_url` varchar(255) DEFAULT NULL,
    `photo1_width` smallint(6) unsigned NOT NULL DEFAULT 0,
    `photo1_height` smallint(6) unsigned NOT NULL DEFAULT 0,
    `photo2_url` varchar(255) DEFAULT NULL,
    `photo2_width` smallint(6) NOT NULL DEFAULT 0,
    `photo2_height` smallint(6) NOT NULL DEFAULT 0,
    `tags` mediumtext NOT NULL,
    `duration` smallint(6) unsigned NOT NULL DEFAULT 0,
    `json` mediumtext DEFAULT NULL,
    KEY `ted_video_slug` (`slug`)
);

CREATE TABLE IF NOT EXISTS `wb_url` (
    `url_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `url` varchar(2000) NOT NULL,
    `first_capture` timestamp NULL DEFAULT NULL,
    `last_capture` timestamp NULL DEFAULT NULL,
    `all_captures_fetched` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1 when all captures have been loaded into wb_url_capture; 0 else',
    `update_time` timestamp NOT NULL DEFAULT current_timestamp(),
    KEY `wb_url_url` (`url`)
);

CREATE TABLE IF NOT EXISTS `wb_url_capture` (
    `url_id` bigint(20) NOT NULL,
    `timestamp` timestamp NULL DEFAULT NULL,
    KEY `wb_url_capture_url_id` (`url_id`)
);
