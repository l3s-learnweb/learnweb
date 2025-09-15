ALTER TABLE `lw_resource`
    MODIFY `service` ENUM ('bing','google','flickr','giphy','youtube','vimeo','ipernity','ted','tedx','loro','yovisto','learnweb','archiveit','teded','factcheck','desktop','internet','slideshare','speechrepository') NOT NULL;

ALTER TABLE `lw_search_history`
    MODIFY `service` ENUM ('bing','google','flickr','giphy','youtube','vimeo','ipernity','ted','tedx','loro','yovisto','learnweb','archiveit','teded','factcheck','desktop','internet','slideshare','speechrepository') DEFAULT NULL;

ALTER TABLE `lw_organisation`
    MODIFY `default_search_text` VARCHAR(16) NOT NULL DEFAULT 'google';

UPDATE `lw_organisation`
SET `default_search_text`='google'
WHERE `default_search_text` = 'bing';
