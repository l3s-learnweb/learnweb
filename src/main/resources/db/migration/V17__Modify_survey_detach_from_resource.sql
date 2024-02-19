ALTER TABLE `lw_survey_page`
    CHANGE COLUMN `resource_id` `resource_id` INT(10) UNSIGNED NULL AFTER `page_id`;

ALTER TABLE `lw_survey_question`
    DROP COLUMN `resource_id`,
    DROP INDEX `fk_lw_survey_question_lw_resource`,
    DROP FOREIGN KEY `fk_lw_survey_question_lw_resource`;

ALTER TABLE `lw_survey_question`
    CHANGE COLUMN `page_id` `page_id` INT(10) UNSIGNED NOT NULL FIRST;

ALTER TABLE `lw_survey_question`
    ADD COLUMN `placeholder` VARCHAR(3000) NULL DEFAULT NULL AFTER `description`;

ALTER TABLE `lw_survey_question_option`
    CHANGE COLUMN `question_id` `question_id` INT(10) UNSIGNED NOT NULL FIRST;

ALTER TABLE `lw_survey_response`
    CHANGE COLUMN `resource_id` `resource_id` INT(10) UNSIGNED NULL AFTER `response_id`;

ALTER TABLE `lw_survey_response`
    ADD COLUMN `message_id` BIGINT UNSIGNED NULL DEFAULT NULL AFTER `resource_id`;
