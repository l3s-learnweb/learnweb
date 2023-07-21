ALTER TABLE `lw_user` ADD
    `preferred_theme` ENUM ('auto','light','dark') NOT NULL DEFAULT 'auto' AFTER `accept_terms_and_conditions`;
