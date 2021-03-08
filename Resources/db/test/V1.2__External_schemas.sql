/* ================= `learnweb_annotations` schema ================= */

CREATE TABLE IF NOT EXISTS `learnweb_annotations.document` (
    `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` text DEFAULT NULL,
    `web_uri` text DEFAULT NULL,
    `updated` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created` datetime NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations.annotation` (
    `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` int(10) UNSIGNED DEFAULT NULL,
    `group_id` int(10) UNSIGNED NOT NULL DEFAULT 0,
    `document_id` int(10) UNSIGNED NOT NULL,
    `text` text DEFAULT NULL,
    `quote` mediumtext DEFAULT NULL,
    `text_rendered` text DEFAULT NULL,
    `tags` text DEFAULT NULL,
    `shared` tinyint(1) NOT NULL DEFAULT 0,
    `target_uri` text DEFAULT NULL,
    `target_uri_normalized` text DEFAULT NULL,
    `target_selectors` text DEFAULT NULL,
    `references` text DEFAULT NULL,
    `extra` mediumtext DEFAULT NULL,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `updated` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created` datetime NOT NULL DEFAULT current_timestamp(),
    KEY `annotation_user_id` (`user_id`),
    KEY `annotation_group_id` (`group_id`),
    KEY `annotation_updated` (`updated`),
    KEY `annotation_document` (`document_id`),
    /*KEY `tags` (`tags`),*/
    CONSTRAINT `annotation_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `learnweb_annotations.document` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations.annotation_moderation` (
    `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `annotation_id` int(10) UNSIGNED NOT NULL,
    `updated` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created` datetime NOT NULL DEFAULT current_timestamp(),
    UNIQUE KEY `annotation_moderation_annotation` (`annotation_id`),
    CONSTRAINT `annotation_moderation_ibfk_1` FOREIGN KEY (`annotation_id`) REFERENCES `learnweb_annotations.annotation` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations.document_meta` (
    `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `document_id` int(10) UNSIGNED NOT NULL,
    `type` text NOT NULL,
    `value` text NOT NULL,
    `claimant` mediumtext NOT NULL,
    `claimant_normalized` mediumtext NOT NULL,
    `updated` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created` datetime NOT NULL DEFAULT current_timestamp(),
    /*UNIQUE KEY `document_claimant_normalized` (`claimant_normalized`, `type`),*/
    KEY `document_meta_document` (`document_id`),
    KEY `document_meta_updated` (`updated`),
    CONSTRAINT `document_meta_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `learnweb_annotations.document` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations.document_uri` (
    `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `document_id` int(10) UNSIGNED NOT NULL,
    `type` mediumtext NOT NULL DEFAULT '',
    `content_type` mediumtext NOT NULL DEFAULT '',
    `uri` text NOT NULL,
    `uri_normalized` text NOT NULL,
    `claimant` mediumtext NOT NULL,
    `claimant_normalized` mediumtext NOT NULL,
    `updated` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created` datetime NOT NULL DEFAULT current_timestamp(),
    /*UNIQUE KEY `document_uri_claimant_normalized` (`claimant_normalized`, `uri_normalized`, `type`, `content_type`),*/
    KEY `document_uri_document` (`document_id`),
    /*KEY `document_uri_uri_normalized` (`uri_normalized`),*/
    KEY `document_uri_updated` (`updated`),
    CONSTRAINT `document_uri_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `learnweb_annotations.document` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations.flag` (
    `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `annotation_id` int(10) UNSIGNED NOT NULL,
    `user_id` int(10) UNSIGNED NOT NULL,
    `updated` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created` datetime NOT NULL DEFAULT current_timestamp(),
    UNIQUE KEY `flag_annotation_id` (`annotation_id`, `user_id`),
    KEY `flag_user_id` (`user_id`),
    CONSTRAINT `flag_ibfk_1` FOREIGN KEY (`annotation_id`) REFERENCES `learnweb_annotations.annotation` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
);

/* ================= `tracker` schema ================= */

CREATE TABLE IF NOT EXISTS `tracker.account` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    `last_seen` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `updated_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `created_at` timestamp NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE IF NOT EXISTS `tracker.client` (
    `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` varchar(64) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `api_key` varchar(32) NOT NULL,
    `is_tracked` bit(1) NOT NULL DEFAULT '1',
    `overlay_path` varchar(255) DEFAULT NULL,
    `updated_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `created_at` timestamp NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE IF NOT EXISTS `tracker.account_client` (
    `account_id` bigint(20) NOT NULL,
    `client_id` bigint(20) UNSIGNED NOT NULL,
    PRIMARY KEY (`account_id`, `client_id`),
    CONSTRAINT `fk_account_client_account_id` FOREIGN KEY (`account_id`) REFERENCES `tracker.account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_account_client_client_id` FOREIGN KEY (`client_id`) REFERENCES `tracker.client` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `tracker.role` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `role` varchar(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS `tracker.account_role` (
    `account_id` bigint(20) NOT NULL,
    `role_id` bigint(20) NOT NULL,
    PRIMARY KEY (`account_id`, `role_id`),
    CONSTRAINT `fk_account_role_account_id` FOREIGN KEY (`account_id`) REFERENCES `tracker.account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_account_role_role_id` FOREIGN KEY (`role_id`) REFERENCES `tracker.role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `tracker.website_class` (
    `id` int(11) UNSIGNED NOT NULL PRIMARY KEY,
    `website` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS `tracker.track` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `uuid` varchar(36) NOT NULL,
    `client_id` bigint(20) UNSIGNED NOT NULL,
    `status` enum ('OPEN','CLOSED','PROCESSED') NOT NULL DEFAULT 'OPEN',
    `url` varchar(2000) NOT NULL,
    `url_domain` varchar(255) DEFAULT NULL,
    `title` varchar(5000) NOT NULL,
    `referrer` varchar(2000) DEFAULT NULL,
    `agent` varchar(2000) NOT NULL,
    `viewport_width` int(10) DEFAULT NULL,
    `viewport_height` int(10) DEFAULT NULL,
    `user_ip` varchar(46) NOT NULL,
    `external_user_id` varchar(128) DEFAULT NULL,
    `external_session_id` varchar(128) DEFAULT NULL,
    `closed_by_timeout` tinyint(1) NOT NULL DEFAULT 0,
    `updated_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `statistic_version` int(10) DEFAULT NULL,
    `total_events` int(10) DEFAULT NULL,
    `time_stay` int(10) DEFAULT NULL,
    `time_active` int(10) DEFAULT NULL,
    `clicks` int(10) DEFAULT NULL,
    `keypress` int(10) DEFAULT NULL,
    `copy` int(10) DEFAULT NULL,
    `paste` int(10) DEFAULT NULL,
    `website_class_id` int(11) UNSIGNED DEFAULT NULL,
    KEY `track_uuid` (`uuid`),
    KEY `track_external_client_id` (`external_user_id`, `external_session_id`),
    CONSTRAINT `fk_track_client_id` FOREIGN KEY (`client_id`) REFERENCES `tracker.client` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_track_website_class_id` FOREIGN KEY (`website_class_id`) REFERENCES `tracker.website_class` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `tracker.event` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `track_id` bigint(20) NOT NULL,
    `type` enum ('focus','blur','online','offline','resize','scroll','click','auxclick','mousemove','keypress','change','beforeunload','copy','paste','custom') NOT NULL,
    `type_custom` varchar(64) DEFAULT NULL,
    `x` int(10) DEFAULT NULL,
    `y` int(10) DEFAULT NULL,
    `target` text DEFAULT NULL,
    `value` text DEFAULT NULL,
    `time` int(10) NOT NULL,
    KEY `event_type` (`type`),
    KEY `event_time` (`time`),
    KEY `event_type_custom` (`type_custom`),
    CONSTRAINT `fk_event_track_id` FOREIGN KEY (`track_id`) REFERENCES `tracker.track` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
