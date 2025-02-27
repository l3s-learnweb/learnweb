-- Step 1: Add UUID columns to existing tables
ALTER TABLE `learnweb_large`.`sl_query`
    ADD COLUMN `search_uuid` CHAR(36) NULL AFTER `search_id`;

ALTER TABLE `learnweb_large`.`sl_action`
    ADD COLUMN `search_uuid` CHAR(36) NULL AFTER `search_id`;

ALTER TABLE `learnweb_large`.`sl_resource`
    ADD COLUMN `search_uuid` CHAR(36) NULL AFTER `search_id`;

-- Step 2: Drop existing foreign key constraints
ALTER TABLE `learnweb_large`.`sl_action`
    DROP FOREIGN KEY `fk_sl_action_sl_query`;

ALTER TABLE `learnweb_large`.`sl_resource`
    DROP FOREIGN KEY `fk_sl_resource_sl_query`;

-- Step 3: Generate and populate UUID values for sl_query table
UPDATE `learnweb_large`.`sl_query` SET `search_uuid` = UUID();

-- Step 4: Copy UUID values from sl_query to other tables
UPDATE `learnweb_large`.`sl_action` a JOIN `learnweb_large`.`sl_query` q ON a.search_id = q.search_id SET a.search_uuid = q.search_uuid;
UPDATE `learnweb_large`.`sl_resource` r JOIN `learnweb_large`.`sl_query` q ON r.search_id = q.search_id SET r.search_uuid = q.search_uuid;

-- Step 5: Modify primary keys and indexes to use UUID column
ALTER TABLE `learnweb_large`.`sl_query`
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (`search_uuid`);

ALTER TABLE `learnweb_large`.`sl_resource`
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (`search_uuid`, `rank`);

ALTER TABLE `learnweb_large`.`sl_action`
    DROP KEY `sl_action_search_id`,
    ADD KEY `sl_action_search_uuid` (`search_uuid`);

-- Step 6: Add new foreign key constraints with UUID columns
ALTER TABLE `learnweb_large`.`sl_action`
    ADD CONSTRAINT `fk_sl_action_sl_query_uuid`
        FOREIGN KEY (`search_uuid`)
            REFERENCES `learnweb_large`.`sl_query` (`search_uuid`)
            ON DELETE CASCADE;

ALTER TABLE `learnweb_large`.`sl_resource`
    ADD CONSTRAINT `fk_sl_resource_sl_query_uuid`
        FOREIGN KEY (`search_uuid`)
            REFERENCES `learnweb_large`.`sl_query` (`search_uuid`)
            ON DELETE CASCADE;

-- Step 7: Drop old INT columns
# ALTER TABLE `learnweb_large`.`sl_query`
#     DROP COLUMN `search_id`;
#
# ALTER TABLE `learnweb_large`.`sl_action`
#     DROP COLUMN `search_id`;
#
# ALTER TABLE `learnweb_large`.`sl_resource`
#     DROP COLUMN `search_id`;
