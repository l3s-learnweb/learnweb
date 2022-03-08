CREATE TABLE IF NOT EXISTS `lw_glossary_entry_file` (
    `entry_id` INT(10) UNSIGNED NOT NULL,
    `resource_id` INT(10) UNSIGNED NOT NULL,
    `file_id` INT(10) UNSIGNED NOT NULL,
    PRIMARY KEY (`file_id`, `resource_id`, `entry_id`)
);

ALTER TABLE `lw_glossary_entry_file` ADD CONSTRAINT `fk_lw_glossary_entry_file_lw_resource` FOREIGN KEY (`resource_id`) REFERENCES `lw_resource` (`resource_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_entry_file` ADD CONSTRAINT `fk_lw_glossary_entry_file_lw_glossary_entry` FOREIGN KEY (`entry_id`) REFERENCES `lw_glossary_entry` (`entry_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `lw_glossary_entry_file` ADD CONSTRAINT `fk_lw_glossary_entry_file_lw_file` FOREIGN KEY (`file_id`) REFERENCES `lw_file` (`file_id`) ON DELETE CASCADE ON UPDATE CASCADE;