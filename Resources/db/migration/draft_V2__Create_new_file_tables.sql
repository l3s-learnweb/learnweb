CREATE TABLE IF NOT EXISTS `new_file` (
    `file_id` int(10) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `deleted` tinyint(1) NOT NULL DEFAULT 0,
    `name` varchar(255) NOT NULL,
    `mime_type` varchar(255) NOT NULL,
    `updated_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `created_at` timestamp NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE IF NOT EXISTS `new_resource_file` (
    `resource_id` int(10) unsigned NOT NULL,
    `type` enum ('main','original','thumbnail_medium','etc') NOT NULL,
    `file_id` int(10) unsigned NOT NULL,
    PRIMARY KEY (`resource_id`, `type`)
);
