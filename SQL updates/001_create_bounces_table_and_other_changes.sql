CREATE TABLE lw_bounces (
  id INT NOT NULL AUTO_INCREMENT,
  address varchar(64) NOT NULL UNIQUE,
  timereceived datetime DEFAULT NULL,
  code varchar(10) NOT NULL,
  description varchar(64),
  PRIMARY KEY (id)
) ;

ALTER TABLE `lw_group` ADD `hypothesis_link` VARCHAR( 255 ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL ,
ADD `hypothesis_token` VARCHAR( 255 ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL ;

ALTER TABLE `lw_course` CHANGE `welcome_message` `welcome_message` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL ;

ALTER TABLE `lw_organisation` ADD `banner_color` CHAR( 7 ) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL ,
ADD `banner_image_file_id` INT UNSIGNED NOT NULL ,
ADD `glossary_languages` VARCHAR( 1000 ) CHARACTER SET armscii8 COLLATE armscii8_general_ci NOT NULL ;

ALTER TABLE `lw_organisation` CHANGE `title` `title` VARCHAR( 60 ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL ,
CHANGE `welcome_message` `welcome_message` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL ;

ALTER TABLE `lw_organisation` CHANGE `banner_color` `banner_color` CHAR( 7 ) CHARACTER SET ascii COLLATE ascii_general_ci NULL ,
CHANGE `banner_image_file_id` `banner_image_file_id` INT( 10 ) UNSIGNED NOT NULL DEFAULT '0',
CHANGE `glossary_languages` `glossary_languages` VARCHAR( 1000 ) CHARACTER SET armscii8 COLLATE armscii8_general_ci NULL ;

ALTER TABLE `lw_organisation` ADD `language_variant` VARCHAR( 10 ) NULL AFTER `default_language` ;

ALTER TABLE `lw_organisation` ADD `logout_page` VARCHAR( 255 ) CHARACTER SET ascii COLLATE ascii_general_ci NULL COMMENT 'page to show after logout' AFTER `logo` ;
ALTER TABLE `lw_organisation` CHANGE `welcome_page` `welcome_page` VARCHAR( 255 ) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL ;

ALTER TABLE `lw_organisation` CHANGE `default_search_text` `default_search_text` VARCHAR( 16 ) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL DEFAULT 'bing',
CHANGE `default_search_image` `default_search_image` VARCHAR( 16 ) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL DEFAULT 'flickr',
CHANGE `default_search_video` `default_search_video` VARCHAR( 16 ) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL DEFAULT 'youtube',
CHANGE `default_language` `default_language` CHAR( 2 ) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL ,
CHANGE `language_variant` `language_variant` VARCHAR( 10 ) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL ,
CHANGE `glossary_languages` `glossary_languages` VARCHAR( 1000 ) CHARACTER SET ascii COLLATE ascii_general_ci NULL DEFAULT NULL ;

ALTER TABLE `lw_organisation` ADD `css_file` VARCHAR( 100 ) CHARACTER SET ascii COLLATE ascii_general_ci NULL AFTER `banner_image_file_id` ;

ALTER TABLE `lw_organisation` DROP `banner_color`;