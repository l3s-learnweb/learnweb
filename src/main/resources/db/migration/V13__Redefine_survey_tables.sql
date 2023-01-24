ALTER TABLE `lw_survey_resource_user` RENAME TO `lw_survey_response`;
ALTER TABLE `lw_survey_response` DROP FOREIGN KEY `fk_lw_survey_resource_user_lw_survey_resource`;
ALTER TABLE `lw_survey_response` DROP FOREIGN KEY `fk_lw_survey_resource_user_lw_user`;
ALTER TABLE `lw_survey_response` DROP PRIMARY KEY;
ALTER TABLE `lw_survey_response` ADD COLUMN `response_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE `lw_survey_response` MODIFY `user_id` INT(10) UNSIGNED NULL;
ALTER TABLE `lw_survey_response` ADD CONSTRAINT `fk_lw_survey_response_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE `lw_survey_response` ADD CONSTRAINT `fk_lw_survey_response_lw_user` FOREIGN KEY (`user_id`) REFERENCES `lw_user` (`user_id`) ON UPDATE CASCADE ON DELETE CASCADE;


ALTER TABLE `lw_survey_answer` RENAME TO `lw_survey_response_answer`;
ALTER TABLE `lw_survey_response_answer` DROP FOREIGN KEY `fk_lw_survey_answer_lw_survey_question`;
ALTER TABLE `lw_survey_response_answer` DROP FOREIGN KEY `fk_lw_survey_answer_lw_survey_resource`;
ALTER TABLE `lw_survey_response_answer` DROP FOREIGN KEY `fk_lw_survey_answer_lw_user`;
ALTER TABLE `lw_survey_response_answer` DROP PRIMARY KEY;
ALTER TABLE `lw_survey_response_answer` ADD COLUMN `response_id` INT(10) UNSIGNED NULL FIRST;
ALTER TABLE `lw_survey_response_answer` ADD COLUMN `variant_id` INT(10) UNSIGNED NULL DEFAULT NULL AFTER `question_id`;
UPDATE `lw_survey_response_answer` a SET `response_id` = (SELECT `response_id` FROM `lw_survey_response` r WHERE r.`user_id` = a.`user_id` AND r.`resource_id` = a.`resource_id`) WHERE `response_id` IS NULL;
ALTER TABLE `lw_survey_response_answer` DROP COLUMN `resource_id`;
ALTER TABLE `lw_survey_response_answer` DROP COLUMN `user_id`;
ALTER TABLE `lw_survey_response_answer` MODIFY `response_id` INT(10) UNSIGNED NOT NULL;
ALTER TABLE `lw_survey_response_answer` ADD PRIMARY KEY (`response_id`, `question_id`);

ALTER TABLE `lw_survey_response_answer` ADD CONSTRAINT `fk_lw_survey_response_answer_lw_survey_question` FOREIGN KEY (`question_id`) REFERENCES `lw_survey_question` (`question_id`) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE `lw_survey_response_answer` ADD CONSTRAINT `fk_lw_survey_response_answer_lw_survey_response` FOREIGN KEY (`response_id`) REFERENCES `lw_survey_response` (`response_id`) ON UPDATE CASCADE ON DELETE CASCADE;


ALTER TABLE `lw_survey_question` MODIFY `question_type` ENUM('INPUT_TEXT','INPUT_TEXTAREA','ONE_RADIO','ONE_MENU','ONE_MENU_EDITABLE','MANY_CHECKBOX','MULTIPLE_MENU','FULLWIDTH_HEADER') NOT NULL;
ALTER TABLE `lw_survey_question` MODIFY `question` TEXT NOT NULL;
ALTER TABLE `lw_survey_question` RENAME COLUMN `info` TO `description`;
ALTER TABLE `lw_survey_question` MODIFY `description` VARCHAR(3000) DEFAULT NULL;
ALTER TABLE `lw_survey_question` ADD COLUMN `page_id` INT(10) UNSIGNED NOT NULL DEFAULT '0' AFTER `question_id`;
ALTER TABLE `lw_survey_question` ADD COLUMN `min_length` SMALLINT UNSIGNED NULL DEFAULT NULL AFTER `required`;
ALTER TABLE `lw_survey_question` ADD COLUMN `max_length` SMALLINT UNSIGNED NULL DEFAULT NULL AFTER `min_length`;
ALTER TABLE `lw_survey_question` ADD COLUMN `resource_id` INT(10) UNSIGNED NULL FIRST;
UPDATE lw_survey_question q SET resource_id = (SELECT s.resource_id FROM lw_survey_resource s WHERE s.survey_id = q.survey_id) WHERE q.resource_id IS NULL;
ALTER TABLE `lw_survey_question` MODIFY `resource_id` INT(10) UNSIGNED NOT NULL;

ALTER TABLE `lw_survey_question` DROP FOREIGN KEY `fk_lw_survey_question_lw_survey`;
ALTER TABLE `lw_survey_question` DROP COLUMN `survey_id`;
ALTER TABLE `lw_survey_question` DROP COLUMN `answers`;
ALTER TABLE `lw_survey_question` DROP COLUMN `extra`;
ALTER TABLE `lw_survey_question` DROP COLUMN `option`;

ALTER TABLE `lw_survey_question` ADD CONSTRAINT `fk_lw_survey_question_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON UPDATE CASCADE ON DELETE CASCADE;


ALTER TABLE `lw_survey_resource` RENAME TO `lw_survey_page`;
ALTER TABLE `lw_survey_page` DROP CONSTRAINT `fk_lw_survey_resource_lw_resource`;
ALTER TABLE `lw_survey_page` DROP CONSTRAINT `fk_lw_survey_resource_lw_survey`;
ALTER TABLE `lw_survey_page` DROP PRIMARY KEY;
ALTER TABLE `lw_survey_page` DROP COLUMN `open_date`;
ALTER TABLE `lw_survey_page` DROP COLUMN `close_date`;
ALTER TABLE `lw_survey_page` DROP COLUMN `editable`;
ALTER TABLE `lw_survey_page` DROP COLUMN `survey_id`;
ALTER TABLE `lw_survey_page` MODIFY `resource_id` INT(10) UNSIGNED NOT NULL;
ALTER TABLE `lw_survey_page` ADD `page_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY;
ALTER TABLE `lw_survey_page` ADD `deleted` TINYINT(1) UNSIGNED NOT NULL DEFAULT '0';
ALTER TABLE `lw_survey_page` ADD `order` SMALLINT(5) UNSIGNED NOT NULL DEFAULT '0';
ALTER TABLE `lw_survey_page` ADD `title` VARCHAR(500) NULL DEFAULT NULL;
ALTER TABLE `lw_survey_page` ADD `description` VARCHAR(3000) NULL DEFAULT NULL;
ALTER TABLE `lw_survey_page` ADD `sampling` TINYINT(1) UNSIGNED NOT NULL DEFAULT '0';

ALTER TABLE `lw_survey_page` ADD CONSTRAINT `fk_lw_survey_page_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON UPDATE CASCADE ON DELETE CASCADE;
UPDATE lw_survey_question q SET page_id = (SELECT p.page_id FROM lw_survey_page p WHERE p.resource_id = q.resource_id) WHERE q.page_id = 0;
ALTER TABLE `lw_survey_question` ADD CONSTRAINT `fk_lw_survey_question_lw_survey_page` FOREIGN KEY (`page_id`) REFERENCES `lw_survey_page` (`page_id`) ON DELETE CASCADE ON UPDATE CASCADE;


ALTER TABLE `lw_survey_question_option` MODIFY `question_id` INT(10) UNSIGNED NOT NULL;
ALTER TABLE `lw_survey_question_option` RENAME COLUMN `answer_id` TO `option_id`;
ALTER TABLE `lw_survey_question_option` DROP CONSTRAINT `fk_question_id`;
ALTER TABLE `lw_survey_question_option` ADD CONSTRAINT `fk_lw_survey_question_option_lw_survey_question` FOREIGN KEY (`question_id`) REFERENCES `lw_survey_question` (`question_id`) ON DELETE CASCADE ON UPDATE CASCADE;


CREATE TABLE `lw_survey_page_variant` (
    `page_id` INT(10) UNSIGNED NOT NULL,
    `variant_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `description` VARCHAR(3000) NULL DEFAULT NULL,
    `data` VARCHAR(100) NULL DEFAULT NULL
);

ALTER TABLE `lw_survey_page_variant` ADD CONSTRAINT `fk_lw_survey_page_variant_lw_survey_page` FOREIGN KEY (`page_id`) REFERENCES `lw_survey_page` (`page_id`) ON UPDATE CASCADE ON DELETE CASCADE;


DROP TABLE `lw_survey`;
