ALTER TABLE `lw_resource` ADD
    `website_view_tab` ENUM ('SCREENSHOT', 'ARCHIVED', 'LIVE') NOT NULL DEFAULT 'SCREENSHOT' AFTER `metadata`;
