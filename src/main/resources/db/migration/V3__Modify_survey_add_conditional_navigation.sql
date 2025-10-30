ALTER TABLE `lw_survey_question`
    ADD `exposable` TINYINT(1) UNSIGNED NOT NULL DEFAULT '0' AFTER `required`;
ALTER TABLE `lw_survey_question`
    ADD `required_question_id` INT(10) UNSIGNED NULL AFTER `deleted`;
ALTER TABLE `lw_survey_question`
    ADD `required_answer` VARCHAR(1000) DEFAULT NULL AFTER `required_question_id`;

ALTER TABLE `lw_survey_page`
    ADD `required_question_id` INT(10) UNSIGNED NULL AFTER `resource_id`;
ALTER TABLE `lw_survey_page`
    ADD `required_answer` VARCHAR(1000) DEFAULT NULL AFTER `required_question_id`;

ALTER TABLE `lw_survey_page`
    ADD CONSTRAINT `fk_lw_survey_page_required_question_id`
        FOREIGN KEY (`required_question_id`) REFERENCES `lw_survey_question` (`question_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_survey_question`
    ADD CONSTRAINT `fk_lw_survey_question_required_question_id`
        FOREIGN KEY (`required_question_id`) REFERENCES `lw_survey_question` (`question_id`) ON DELETE SET NULL ON UPDATE CASCADE;
