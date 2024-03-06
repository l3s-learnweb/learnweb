ALTER TABLE `lw_survey_page`
    MODIFY `resource_id` INT(10) UNSIGNED NULL;

ALTER TABLE `lw_survey_question` DROP CONSTRAINT `fk_lw_survey_question_lw_resource`;
ALTER TABLE `lw_survey_question` DROP COLUMN `resource_id`;

ALTER TABLE `lw_survey_question`
    MODIFY `page_id` INT(10) UNSIGNED NOT NULL;

ALTER TABLE `lw_survey_question`
    ADD COLUMN `placeholder` VARCHAR(3000) NULL DEFAULT NULL AFTER `description`;

ALTER TABLE `lw_survey_question_option`
    MODIFY `question_id` INT(10) UNSIGNED NOT NULL;

ALTER TABLE `lw_survey_response`
    MODIFY `resource_id` INT(10) UNSIGNED NULL;

ALTER TABLE `lw_survey_response`
    ADD COLUMN `message_id` BIGINT UNSIGNED NULL DEFAULT NULL AFTER `resource_id`;
