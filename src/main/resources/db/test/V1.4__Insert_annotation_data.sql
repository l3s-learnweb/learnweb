CREATE SCHEMA IF NOT EXISTS `learnweb_annotations`;

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation` (
    `user_id` INT(10) UNSIGNED NOT NULL,
    `search_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` INT(10) UNSIGNED DEFAULT 0,
    `query` VARCHAR(250) NOT NULL,
    `text` VARCHAR(250) NOT NULL,
    `quote` VARCHAR(250) NOT NULL,
    `target_uri` VARCHAR(250) NOT NULL,
    `target_uri_normalized` VARCHAR(250) NOT NULL,
    `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    `frequency` INT(10) UNSIGNED NOT NULL
);