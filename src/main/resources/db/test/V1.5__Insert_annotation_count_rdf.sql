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

CREATE TABLE `learnweb_annotations`.`annotation_rdf` (
  `id` int UNSIGNED NOT NULL,
  `user_id` int NOT NULL,
  `rdf_value` longtext NOT NULL
);