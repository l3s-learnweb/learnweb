ALTER TABLE `lw_group_folder`
  ADD COLUMN `deleted` TINYINT(1) UNSIGNED NULL DEFAULT '0' AFTER `folder_id`;
