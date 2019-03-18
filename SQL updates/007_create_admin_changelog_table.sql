CREATE TABLE `admin_changelog` (
   `changelog_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
   `title` VARCHAR(500) NOT NULL,
   `message` VARCHAR(5000) NULL DEFAULT NULL,
   `user_id` INT UNSIGNED NOT NULL,
   `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY (`changelog_id`)
) COLLATE='utf8mb4_general_ci';
