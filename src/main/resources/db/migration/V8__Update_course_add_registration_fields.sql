ALTER TABLE `lw_course` ADD `reg_type` ENUM ('PUBLIC','SPECIFIC','HIDDEN','CLOSED') NOT NULL DEFAULT 'HIDDEN' AFTER `title`;
ALTER TABLE `lw_course` ADD `reg_description` VARCHAR(1000) DEFAULT NULL AFTER `reg_type`;
ALTER TABLE `lw_course` ADD `reg_icon_file_id` INT(10) UNSIGNED DEFAULT NULL AFTER `reg_description`;
ALTER TABLE `lw_course` RENAME COLUMN `wizard_param` TO `reg_wizard`;

ALTER TABLE `lw_organisation` RENAME COLUMN `registration_message` TO `terms_and_conditions`;

ALTER TABLE `lw_course` ADD CONSTRAINT `fk_lw_course_lw_file` FOREIGN KEY (`reg_icon_file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `lw_file` MODIFY
    `type` ENUM ('SYSTEM_FILE','ORGANISATION_BANNER','COURSE_PICTURE','PROFILE_PICTURE','GROUP_PICTURE','THUMBNAIL_SMALL','THUMBNAIL_MEDIUM','THUMBNAIL_LARGE','MAIN','ORIGINAL','GLOSSARY','DOC_HISTORY','DOC_CHANGES') NOT NULL;
