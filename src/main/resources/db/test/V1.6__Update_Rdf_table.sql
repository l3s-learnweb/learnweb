ALTER TABLE `learnweb_annotations`.`annotation_rdf` ADD `group_id` INT NOT NULL AFTER `user_id`;
CREATE TABLE `learnweb_annotations`.`annotation_inputStream` (
    `id` int NOT NULL,
    `user_id` int NOT NULL,
    `type` varchar(250) NOT NULL,
    `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    `date_created` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
ALTER TABLE `learnweb_annotations`.`annotation_inputStream`
    ADD PRIMARY KEY (`id`);
ALTER TABLE `learnweb_annotations`.`annotation_inputStream`
    MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=247;

CREATE TABLE IF NOT EXISTS `learnweb_annotations`.`annotation_query_count` (
    `search_id` int NOT NULL,
    `uri_id` int NOT NULL
)