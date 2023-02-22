CREATE SCHEMA IF NOT EXISTS `learnweb_annotations`;

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_count` (
    `uri_id` int NOT NULL PRIMARY KEY AUTO_INCREMENT,
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
    `id` int UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `user_id` int NOT NULL,
    `group_id` int NOT NULL,
    `application` varchar(250) NOT NULL,
    `shared_object` varchar(10000) NOT NULL,
    `created_at` date NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_rdf` (
    `id` int UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `user_id` int NOT NULL,
    `group_id` int NOT NULL,
    `rdf_value` longtext NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_input_stream` (
    `id` int NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `user_id` int NOT NULL,
    `type` varchar(250) NOT NULL,
    `content` longtext NOT NULL,
    `date_created` date NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_query_count` (
    `search_id` int NOT NULL,
    `uri_id` int NOT NULL
)