CREATE TABLE IF NOT EXISTS `lw_organisation_settings` (
    `organisation_id` INT(10) UNSIGNED NOT NULL,
    `setting_key` VARCHAR(255) NOT NULL COLLATE 'latin1_general_ci',
    `setting_value` VARCHAR(255) NOT NULL COLLATE 'latin1_general_ci',
    PRIMARY KEY (`setting_key`, `organisation_id`)
);

ALTER TABLE `lw_organisation_settings` ADD CONSTRAINT `fk_lw_organisation_settings_lw_organisation` FOREIGN KEY (`organisation_id`) REFERENCES `lw_organisation` (`organisation_id`) ON DELETE CASCADE ON UPDATE CASCADE;
