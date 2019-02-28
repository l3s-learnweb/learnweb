ALTER TABLE `lw_user`
  ADD COLUMN `hashing` ENUM('MD5','PBKDF2') NOT NULL DEFAULT 'MD5' AFTER `password`;
ALTER TABLE `lw_user`
  CHANGE COLUMN `password` `password` VARCHAR(512) NULL DEFAULT NULL COLLATE 'latin1_general_ci' AFTER `username`;