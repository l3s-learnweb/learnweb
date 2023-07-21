/* ================= `learnweb_large` schema ================= */
CREATE SCHEMA IF NOT EXISTS `learnweb_large`;

CREATE TABLE IF NOT EXISTS `learnweb_large`.`sl_suggested_query` (
    `search_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `reference_query` VARCHAR(255) NOT NULL,
    `query` VARCHAR(255) NOT NULL,
    `source` VARCHAR(255) NOT NULL,
    `index` INT(10) NOT NULL,
    `options` TEXT DEFAULT NULL,
    `graph` TEXT DEFAULT NULL,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()
);
