ALTER TABLE `lw_file` MODIFY
    `type` ENUM ('SYSTEM_FILE','ORGANISATION_BANNER','PROFILE_PICTURE','THUMBNAIL_SMALL','THUMBNAIL_MEDIUM','THUMBNAIL_LARGE','MAIN','ORIGINAL','GLOSSARY','DOC_HISTORY','DOC_CHANGES') NOT NULL;

ALTER TABLE `lw_glossary_entry` ADD
    `pictures_count` SMALLINT(6) UNSIGNED NOT NULL DEFAULT 0 AFTER `imported`;

CREATE TABLE IF NOT EXISTS `lw_glossary_entry_file` (
    `entry_id` INT(10) UNSIGNED NOT NULL,
    `file_id` INT(10) UNSIGNED NOT NULL,
    PRIMARY KEY (`entry_id`, `file_id`)
);

ALTER TABLE `lw_glossary_entry_file` ADD CONSTRAINT `fk_lw_glossary_entry_file_lw_glossary_entry` FOREIGN KEY (`entry_id`) REFERENCES `lw_glossary_entry` (`entry_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_entry_file` ADD CONSTRAINT `fk_lw_glossary_entry_file_lw_file` FOREIGN KEY (`file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE;
