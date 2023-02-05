CREATE SCHEMA IF NOT EXISTS `learnweb_annotations`;

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation` (
    `user_id` INT(10) UNSIGNED NOT NULL,
    `search_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group_id` INT(10) UNSIGNED DEFAULT 0,
    `query` VARCHAR(250) NOT NULL,
    `text` VARCHAR(250) NOT NULL,
    `quote` VARCHAR(250) NOT NULL,
    `target_uri` VARCHAR(250) NOT NULL,
    `target_uri_normalized` VARCHAR(250) NOT NULL,
    `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(),
    `frequency` INT(10) UNSIGNED NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_count` (
    `uri_id` int NOT NULL,
    `uri` varchar(250) NOT NULL,
    `users` varchar(250) NOT NULL,
    `type` varchar(250) NOT NULL,
    `surface_form` varchar(250) NOT NULL,
    `session_id` varchar(10000) NOT NULL,
    `confidence` double NOT NULL,
    `input_id` text NOT NULL,
    `created_at` timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_objects` (
    `id` int UNSIGNED NOT NULL,
    `user_id` int NOT NULL,
    `group_id` int NOT NULL,
    `application` varchar(250) NOT NULL,
    `shared_object` varchar(10000) NOT NULL,
    `created_at` date NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_rdf` (
    `id` int UNSIGNED NOT NULL,
    `user_id` int NOT NULL,
    `rdf_value` longtext NOT NULL
);

ALTER TABLE `learnweb_annotations`.`annotation_rdf` ADD `group_id` INT NOT NULL AFTER `user_id`;
CREATE TABLE `learnweb_annotations`.`annotation_input_stream` (
    `id` int NOT NULL,
    `user_id` int NOT NULL,
    `type` varchar(250) NOT NULL,
    `content` longtext NOT NULL,
    `date_created` date NOT NULL
);
ALTER TABLE `learnweb_annotations`.`annotation_input_stream`
    ADD PRIMARY KEY (`id`);
ALTER TABLE `learnweb_annotations`.`annotation_input_stream`
    MODIFY `id` int NOT NULL AUTO_INCREMENT;

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_query_count` (
    `search_id` int NOT NULL,
    `uri_id` int NOT NULL
)