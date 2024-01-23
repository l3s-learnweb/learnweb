ALTER TABLE `lw_transcript_log`
    RENAME COLUMN `words_selected` TO `selection`;

ALTER TABLE `lw_transcript_log`
    RENAME COLUMN `user_annotation` TO `annotation`;
