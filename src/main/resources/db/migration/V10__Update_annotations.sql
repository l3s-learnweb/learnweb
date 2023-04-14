CREATE SCHEMA IF NOT EXISTS `learnweb_annotations`; /* TODO: move all tables to learnweb_large */

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation` ( /* TODO: remove from migrations */
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

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_count` ( /* TODO: rename to sl_recognised_entity or so... */
    `uri_id` int NOT NULL PRIMARY KEY AUTO_INCREMENT, /* TODO: could be entity_uri */
    `uri` varchar(250) NOT NULL,
    `user_id` INT(10) UNSIGNED NOT NULL,
    `type` varchar(250) NOT NULL,
    `surface_form` varchar(250) NOT NULL,
    `session_id` varchar(10000) NOT NULL,
    `confidence` double NOT NULL,
    `input_id` text NOT NULL,
    `created_at` timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_objects` ( /* TODO: redesign, flatten object and split into entity, link, entity_user_weight */
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

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_input_stream` ( /* TODO: rename it, first it better name */
    `id` int NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `user_id` int NOT NULL,
    `type` varchar(250) NOT NULL,
    `content` longtext NOT NULL,
    `date_created` date NOT NULL
);

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_query_count` ( /* TODO: could be sl_search_entity */
    `search_id` int NOT NULL,
    `uri_id` int NOT NULL /* entity_uri */
    /* both of them can be foreign keys */
)