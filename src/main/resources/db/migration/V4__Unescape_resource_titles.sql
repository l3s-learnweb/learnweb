-- Resource titles were stored HTML-escaped, they are plain text now and escaped on output.
-- Only these entities occur in the stored titles, bare ampersands (e.g. "A&E") are already plain text and stay untouched.
UPDATE `lw_resource`
SET `title` = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`title`,
    '&nbsp;', ' '), '&quot;', '"'), '&uuml;', 'ü'), '&lt;', '<'), '&gt;', '>'), '&amp;', '&'))
WHERE `title` LIKE '%&%;%';

-- The same for the resource titles logged by deleting_resource (14), edit_resource (19) and move_resource (66)
UPDATE `lw_user_log`
SET `params` = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`params`,
    '&nbsp;', ' '), '&quot;', '"'), '&uuml;', 'ü'), '&lt;', '<'), '&gt;', '>'), '&amp;', '&'))
WHERE `action` IN (14, 19, 66) AND `params` LIKE '%&%;%';

-- Titles of web search results were stored as received from Interweb (with markup and entities), they are plain text now.
-- The tags are removed first, so that decoded entities (e.g. "&lt;b&gt;") stay text.
UPDATE `lw_search_history_resource`
SET `title` = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REGEXP_REPLACE(`title`, '<[a-zA-Z/!][^>]*>', ''),
    '&quot;', '"'), '&#39;', ''''), '&lt;', '<'), '&gt;', '>'), '&amp;', '&'))
WHERE `title` LIKE '%&%;%' OR `title` LIKE '%<%';
