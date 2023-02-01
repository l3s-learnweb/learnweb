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