CREATE TABLE IF NOT EXISTS `lw_resource_annotation` (
    `annotation_id` INT(10) UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `action` CHAR(25) NOT NULL,
    `selection` LONGTEXT NOT NULL,
    `annotation` MEDIUMTEXT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);
