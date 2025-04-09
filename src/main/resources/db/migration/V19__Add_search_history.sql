CREATE TABLE IF NOT EXISTS `lw_search_history` (
    `search_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT(10) UNSIGNED DEFAULT NULL,
    `query` VARCHAR(255) NOT NULL,
    `mode` ENUM ('text','image','video','group') NOT NULL,
    `group_id` INT(10) UNSIGNED DEFAULT NULL COMMENT 'id of a learnweb group, if mode is group',
    `service` ENUM ('bing','flickr','giphy','youtube','vimeo','ipernity','ted','tedx','loro','yovisto','learnweb','archiveit','teded','factcheck','desktop','internet','slideshare','speechrepository') DEFAULT NULL,
    `language` CHAR(5) DEFAULT NULL,
    `filters` VARCHAR(3072) DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE IF NOT EXISTS `lw_search_history_resource` (
    `search_id` INT(10) UNSIGNED NOT NULL,
    `rank` SMALLINT(5) UNSIGNED NOT NULL,
    `resource_id` INT(10) UNSIGNED DEFAULT NULL COMMENT 'id of a learnweb resource NULL otherwise',
    `url` VARCHAR(3072) DEFAULT NULL COMMENT 'null if learnweb resource',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'null if learnweb resource',
    `description` VARCHAR(3072) DEFAULT NULL COMMENT 'null if learnweb resource',
    `thumbnail_url` VARCHAR(3072) DEFAULT NULL,
    `thumbnail_height` SMALLINT(5) UNSIGNED DEFAULT NULL,
    `thumbnail_width` SMALLINT(5) UNSIGNED DEFAULT NULL,
    PRIMARY KEY (`search_id`, `rank`),
    KEY `lw_search_history_resource_search_id` (`search_id`)
);

CREATE TABLE IF NOT EXISTS `lw_search_history_action` (
    `search_id` INT(10) UNSIGNED NOT NULL,
    `rank` SMALLINT(5) UNSIGNED NOT NULL,
    `action` ENUM ('resource_clicked','resource_saved') NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    PRIMARY KEY (`search_id`, `rank`, `action`),
    KEY `lw_search_history_action_search_id` (`search_id`)
);

ALTER TABLE `lw_search_history` ADD CONSTRAINT `fk_lw_search_history_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON DELETE CASCADE;
ALTER TABLE `lw_search_history` ADD CONSTRAINT `fk_lw_search_history_lw_group` FOREIGN KEY (`group_id`) REFERENCES `lw_group` (`group_id`) ON DELETE CASCADE;
ALTER TABLE `lw_search_history_resource` ADD CONSTRAINT `fk_lw_search_history_resource_lw_search_history` FOREIGN KEY (`search_id`) REFERENCES `lw_search_history` (`search_id`) ON DELETE CASCADE;
ALTER TABLE `lw_search_history_resource` ADD CONSTRAINT `fk_lw_search_history_resource_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE;
ALTER TABLE `lw_search_history_action` ADD CONSTRAINT `fk_lw_search_history_action_lw_search_history` FOREIGN KEY (`search_id`) REFERENCES `lw_search_history` (`search_id`) ON DELETE CASCADE;
