-- The glossary search highlighted matches by wrapping the whole field value of the cached entries in <b></b>, these markers were saved by later edits and copies.
-- The highlighting is rendered on output now. Only one outer marker is removed, the content can start or end with markup of its own.
UPDATE `lw_glossary_entry` SET `topic_one` = SUBSTRING(`topic_one`, 4, CHAR_LENGTH(`topic_one`) - 7) WHERE `topic_one` LIKE '<b>%</b>';
UPDATE `lw_glossary_entry` SET `topic_two` = SUBSTRING(`topic_two`, 4, CHAR_LENGTH(`topic_two`) - 7) WHERE `topic_two` LIKE '<b>%</b>';
UPDATE `lw_glossary_entry` SET `topic_three` = SUBSTRING(`topic_three`, 4, CHAR_LENGTH(`topic_three`) - 7) WHERE `topic_three` LIKE '<b>%</b>';
UPDATE `lw_glossary_entry` SET `description` = SUBSTRING(`description`, 4, CHAR_LENGTH(`description`) - 7) WHERE `description` LIKE '<b>%</b>';

UPDATE `lw_glossary_term` SET `term` = SUBSTRING(`term`, 4, CHAR_LENGTH(`term`) - 7) WHERE `term` LIKE '<b>%</b>';
UPDATE `lw_glossary_term` SET `acronym` = SUBSTRING(`acronym`, 4, CHAR_LENGTH(`acronym`) - 7) WHERE `acronym` LIKE '<b>%</b>';
UPDATE `lw_glossary_term` SET `source` = SUBSTRING(`source`, 4, CHAR_LENGTH(`source`) - 7) WHERE `source` LIKE '<b>%</b>';
UPDATE `lw_glossary_term` SET `phraseology` = SUBSTRING(`phraseology`, 4, CHAR_LENGTH(`phraseology`) - 7) WHERE `phraseology` LIKE '<b>%</b>';
