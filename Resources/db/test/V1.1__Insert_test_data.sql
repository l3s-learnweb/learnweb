INSERT INTO `lw_bans` (`id`, `type`, `name`, `bandate`, `bannedon`, `attempts`, `reason`) VALUES
    (1, 'IP', '78.237.96.161', '2024-05-11 13:22:43', '2021-01-23 04:57:48', 0, 'SQL injection (test)'),
    (2, 'IP', '222.184.105.9', '2025-07-09 16:04:28', '2020-12-21 22:02:47', 0, 'SQL injection (test)'),
    (3, 'IP', '50.152.129.184', '2027-03-11 20:13:24', '2020-04-19 10:12:47', 0, 'SQL injection (test)'),
    (4, 'IP', '9.174.42.119', '2023-04-17 19:25:22', '2020-12-20 11:06:33', 0, 'SQL injection (test)'),
    (5, 'IP', '174.178.53.245', '2030-09-13 01:09:49', '2020-07-14 19:19:57', 0, 'SQL injection (test)'),
    (6, 'IP', '7.155.235.6', '2030-02-09 20:52:22', '2020-09-08 00:57:36', 0, 'SQL injection (test)'),
    (7, 'IP', '111.114.109.179', '2029-10-01 00:15:13', '2021-01-31 13:08:10', 0, 'SQL injection (test)'),
    (8, 'IP', '166.27.62.166', '2028-07-02 15:51:27', '2020-08-24 02:24:51', 0, 'SQL injection (test)'),
    (9, 'IP', '179.243.153.100', '2022-08-28 20:58:18', '2020-05-21 10:50:15', 0, 'SQL injection (test)'),
    (10, 'IP', '128.23.35.138', '2027-12-19 04:41:09', '2020-03-10 00:18:46', 0, 'SQL injection (test)');

INSERT INTO `lw_bounces` (`id`, `address`, `timereceived`, `code`, `description`) VALUES
    (1, 'plehner@example.org', '2018-10-20 18:26:36', '4.1.2', 'Transient Persistent Failure: Bad destination system address'),
    (2, 'lstrosin@example.org', '2020-12-08 18:07:59', '5.4.1', 'Permanent Failure: No answer from host'),
    (3, 'sandy69@example.net', '2020-11-28 18:33:35', '5.4.1', 'Permanent Failure: No answer from host'),
    (4, 'larue.murazik@example.net', '2020-10-27 09:39:38', '5.1.1', 'Permanent Failure: Bad destination mailbox address'),
    (5, NULL, '2020-07-12 21:11:02', '5.0.0', 'Permanent Failure: Unspecified mailing error'),
    (6, 'bryana88@example.com', '2019-05-20 14:48:54', '5.4.1', 'Permanent Failure: No answer from host'),
    (7, 'hans15@example.net', '2020-06-15 18:25:12', '5.1.10', 'Permanent Failure: Unspecified mailing error'),
    (8, NULL, '2019-09-30 17:23:22', '5.0.0', 'Permanent Failure: Unspecified mailing error'),
    (9, NULL, '2020-10-12 11:55:59', '5.0.0', 'Permanent Failure: Unspecified mailing error'),
    (10, 'nitzsche.jarod@example.org', '2019-03-23 18:16:24', '4.1.2', 'Transient Persistent Failure: Bad destination system address');

INSERT INTO `lw_requests` (`id`, `IP`, `requests`, `logins`, `usernames`, `time`) VALUES
    (1, '33.143.226.138', 4, 2, '[admin]', '2019-06-16 05:00:00'),
    (2, '180.108.249.51', 1, 0, '', '2018-12-05 20:00:00'),
    (3, '40.77.167.200', 2, 0, '', '2018-06-30 06:00:00'),
    (4, '105.37.46.171', 105, 1, '[user2]', '2018-04-21 12:00:00'),
    (5, '66.220.146.29', 1, 0, '', '2018-04-04 16:00:00'),
    (6, '212.189.140.217', 37, 0, '', '2018-12-20 18:00:00'),
    (7, '51.75.160.7', 1, 0, '', '2020-01-27 20:00:00'),
    (8, '176.35.136.222', 40, 1, '[user3]', '2018-05-10 13:00:00'),
    (9, '207.84.18.203', 1, 0, '', '2020-01-30 18:00:00'),
    (10, '151.45.185.174', 24, 0, '', '2018-03-27 18:00:00');

INSERT INTO `lw_organisation` (`organisation_id`, `is_default`, `title`, `logo`, `logout_page`, `welcome_page`, `welcome_message`, `options_field1`, `default_search_text`, `default_search_image`, `default_search_video`, `default_language`, `language_variant`, `banner_image_file_id`, `css_file`, `glossary_languages`) VALUES
(1, 1, 'Public', NULL, '/lw/index.jsf', '/lw/myhome/welcome.jsf', 'Hello world', 3118, 'bing', 'flickr', 'youtube', 'en', '', NULL, NULL, 'en,de,it');

INSERT INTO `lw_course` (`course_id`, `title`, `options_field1`, `organisation_id`, `default_group_id`, `wizard_param`, `next_x_users_become_moderator`, `welcome_message`, `timestamp_update`, `timestamp_creation`) VALUES
    (2, 'Public', 17, 1, NULL, 'default', 0, '', '2019-11-27 20:38:07', '2019-11-27 20:38:07');

INSERT INTO `lw_user` (`user_id`, `deleted`, `username`, `password`, `hashing`, `email`, `email_confirmation_token`, `is_email_confirmed`, `organisation_id`, `image_file_id`, `gender`, `dateofbirth`, `address`, `profession`, `additionalinformation`, `interest`, `student_identifier`, `phone`, `is_admin`, `is_moderator`, `registration_date`, `preferences`, `credits`, `fullname`, `affiliation`, `accept_terms_and_conditions`, `preferred_notification_frequency`, `time_zone`, `language`, `guides`) VALUES
    (1, 0, 'admin', '1000:77bec387609669c5d506e52f199ec7e1865547ea7d7c0400:554bbf88a705defdaa4e6dedd7508479825e830fb46eccef3b5da807a24634d46172e3b77abc34aefae8b0afc0ba9ac828cf5f139b40e1a560ae31d14a8526e71c0cf4d1ac66e217116c929c8eec27bfa9a7ebcb2d566ff2fcbcdb8438b1d10454f5dc2e7bf946fc9021e4e445810897561b04007fe0a497584d71c5045396c6', 'PBKDF2', 'lwadmin@maildrop.cc', NULL, 1, 1, NULL, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, '2021-02-18 11:27:19', 'ACED0005737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000037400135345415243485F534552564943455F5445585474000462696E677400145345415243485F534552564943455F494D414745740006666C69636B727400145345415243485F534552564943455F564944454F740007796F757475626578', NULL, NULL, NULL, 0, 'NEVER', 'Europe/Berlin', 'de_DE', 0),
    (2, 0, 'moderator', '1000:3a553d401c756bcb84143b0ddb680ef3cf97873a31c21745:5a86a53888650e2254e5d3cb855dcc2f6dea6d5f4f4a07ef9d476d9f76c59345e4c356adfadd09f1336e98c28a54c5f1f5507f67bd7fb3c7e37ee59208b2792d826aeef8616238a2d21730d26102e8e22677e55e48d2b54067c3ee2d2df4ec6f31150e2bd162c336cdf85840c68fdd51a70dffcb0202e3d75e054dd23e636b33', 'PBKDF2', 'lwmoderator@maildrop.cc', NULL, 1, 1, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 1, '2021-02-18 11:32:33', 'ACED0005737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000037400135345415243485F534552564943455F5445585474000462696E677400145345415243485F534552564943455F494D414745740006666C69636B727400145345415243485F534552564943455F564944454F740007796F757475626578', NULL, NULL, NULL, 0, 'NEVER', 'Europe/Berlin', 'de_DE', 0),
    (3, 0, 'user1', '1000:8e77da66b674585a4e3f667a3dc1b0d7c9e705e3572418ae:61dfab2273c03e7c0195802e5d2add7a8074813263283950c98bb3bcae3c7ac8722abb6492314221513af2eaceb3b26b89d15fbcaae04ced608be227a4e22f6d18a29d66c1d8843e4dcfa07ebea4890adb8e4b897aefb57767b52fbc33cb610e764b61c8dd4e8f556a08fb594183e319fd98152d01b48ba7930286805a5df1f5', 'PBKDF2', 'lwuser1@maildrop.cc', NULL, 1, 1, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, '2021-02-18 11:39:52', 'ACED0005737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000037400135345415243485F534552564943455F5445585474000462696E677400145345415243485F534552564943455F494D414745740006666C69636B727400145345415243485F534552564943455F564944454F740007796F757475626578', NULL, NULL, NULL, 0, 'NEVER', 'Europe/Berlin', 'de_DE', 0),
    (4, 0, 'user2', '1000:bbaa7180cc37c52582b5fc3e7618912eebc33493923207ce:09f245d048227c89da8707bb2bc6f0abd6c765522e13c6820611dc5266878a4b6de00df728dd9afd2459a7b70faba624b0e2148a5b23e24776b31b7585492bb1809e5af3bd5dace09c2e360b6822259e898c60ef51b2e0c077e33c85e8c02282590e345897e56db975a91e662135c7870ebad5a7a5e2b86769f79f299b497b44', 'PBKDF2', 'lwuser2@maildrop.cc', NULL, 1, 1, NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, '2021-02-18 11:40:16', 'ACED0005737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000037400135345415243485F534552564943455F5445585474000462696E677400145345415243485F534552564943455F494D414745740006666C69636B727400145345415243485F534552564943455F564944454F740007796F757475626578', NULL, NULL, NULL, 0, 'NEVER', 'Europe/Berlin', 'de_DE', 0),
    (5, 0, 'user3', '1000:accfa373ff62cc05695a7b364110c14ab61ab0338ca06f66:66fc462266aac17f9f8e3574927fd31a4f95d1838a60efa83932dcf49a6a2ddc54dd7065a8d912e0984bb01a5c2764f8fdd8d4eda170d0de2a26d3bf2395c6dae7a69586335a16896733cf598e09131e57f2ff352e535327ec7530614ac6922d23a2b01b7bc34bb3eddae23631816d93076aa775ed2abf4237a6d54c8c567c5e', 'PBKDF2', 'lwuser3@maildrop.cc', NULL, 1, 1, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, '2021-02-18 11:40:37', 'ACED0005737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000037400135345415243485F534552564943455F5445585474000462696E677400145345415243485F534552564943455F494D414745740006666C69636B727400145345415243485F534552564943455F564944454F740007796F757475626578', NULL, NULL, NULL, 0, 'NEVER', 'Europe/Berlin', 'de_DE', 0),
    (6, 0, 'user4', '1000:0b7790e65eed579deaa4d229049a6b97aed34fe24dc8e6de:3239a44cbe9f26aa38e5be816a2a34a548220f7c917948f057e965071fad1727feda4595c31911098ef974026bc00aa8a14e73db345c5ceaec624a9edd42c9668efff19aaf2e0303e09b6c34c000894bf65af3bcb60166c5c1ffc43186cf384b0b9dc0a24db591d05d6a8c42471b074aafbe41532154b54fbdab7977c71387b7', 'PBKDF2', 'lwuser4@maildrop.cc', NULL, 1, 1, NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, '2021-02-18 11:40:58', 'ACED0005737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000037400135345415243485F534552564943455F5445585474000462696E677400145345415243485F534552564943455F494D414745740006666C69636B727400145345415243485F534552564943455F564944454F740007796F757475626578', NULL, NULL, NULL, 0, 'NEVER', 'Europe/Berlin', 'de_DE', 0),
    (7, 0, 'user5', '1000:be4fe54fdd9843078afc2b9e27e5f05e25a3d115b9173a31:499deb70b8712b1fff2783217c4c99619c5556c5fd92b7eda3d82f9cf310adc6bfbaf82c74474a09747452cfa740bd89a0c5aabf75b1f1508d037e412b012218b12acab81c5a5fff4481809e8f6a17c852818f169101067bc655759cf18a31e1e8381ca34936e0eddb294c5e7e6298b06666450966ebd4aec3d7a25ee74a32ff', 'PBKDF2', 'lwuser5@maildrop.cc', NULL, 1, 1, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, '2021-02-18 11:41:25', 'ACED0005737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000037400135345415243485F534552564943455F5445585474000462696E677400145345415243485F534552564943455F494D414745740006666C69636B727400145345415243485F534552564943455F564944454F740007796F757475626578', NULL, NULL, NULL, 0, 'NEVER', 'Europe/Berlin', 'de_DE', 0),
    (8, 0, 'user6', '1000:19adf5142f00d8dfb6e3e6fcbe2c7dce79aa6c7364be6b46:2310ae1d576e4c13bf1acb17c0de7c2407821c32e349965bf93dce5b52de6068adba0028165268da58e4a3eac6875b627915ef75d72565cadcc84059862373bab3caadd954e717ab4c6873a95b5b3c1651b090e94e9eac73690a2672ae09a062bf53b3002c261ee1b10a96898df5bde8df1f99e150288c85bdf7ee767c8f9d04', 'PBKDF2', 'lwuser6@maildrop.cc', 'd1odhAONB4hJGr5m0blDGz8AMM7rIoWb', 0, 1, NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, '2021-02-18 12:10:58', 'ACED0005737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000037400135345415243485F534552564943455F5445585474000462696E677400145345415243485F534552564943455F494D414745740006666C69636B727400145345415243485F534552564943455F564944454F740007796F757475626578', NULL, NULL, NULL, 0, 'NEVER', 'Europe/Berlin', 'de_DE', 0),
    (9, 0, 'user7', '1000:61e3cc7f68333a6c8b9a56789a3620b881c4542296d679cd:840fb7e4d75bfdf6f9478689a56d30dd2c739305eaef2f283769eebff7f01fc144e652c8bbaeee0b46e64c026fbe79c6a1b39647632f7f4a6edf48180f20157f14b283b932340825d370470dd36b4ec2a3e560962cdc34e328bd83fd88063a9f7d6869cdfa12eb93b27c918551b510c5f4d45a7a36de448a6291d05c8dcb85e4', 'PBKDF2', 'lwuser7@maildrop.cc', 'JR2rIiQlRxRGDItiY1MaGpdNKAWVZsZ9', 0, 1, NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, '2021-02-18 12:11:40', 'ACED0005737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000037400135345415243485F534552564943455F5445585474000462696E677400145345415243485F534552564943455F494D414745740006666C69636B727400145345415243485F534552564943455F564944454F740007796F757475626578', NULL, NULL, NULL, 0, 'NEVER', 'Europe/Berlin', 'de_DE', 0),
    (10, 0, 'user8', '1000:5949e4b6c7484f98747bd0cb802b6c602bf278cdf932edde:407f2a05a45502a2e5da881b9686ab9a4b64b0d79a5a5a5c25e8ad133e9a25be9ed7062055dd32617c6ea09a7e81651353fdf73ef3b8b73ae92ec63e03447a20beaf1a0d13b4b0fbef0a9d2f44b3762b9a96203ae512f5f21ea926e3955e290bd3800d1fcedc4925dca2f4da5b3b77bf81bc3d985e27435ea82e1fffe38ffddd', 'PBKDF2', 'lwuser8@maildrop.cc', 'AtkMrcE6MNCRDG67KxolSV6peYykoyaA', 0, 1, NULL, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, '2021-02-18 12:12:07', 'ACED0005737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C770800000010000000037400135345415243485F534552564943455F5445585474000462696E677400145345415243485F534552564943455F494D414745740006666C69636B727400145345415243485F534552564943455F564944454F740007796F757475626578', NULL, NULL, NULL, 0, 'NEVER', 'Europe/Berlin', 'de_DE', 0);

INSERT INTO `lw_user_course` (`user_id`, `course_id`, `timestamp`) VALUES
    (1, 2, '2021-02-18 11:27:20'),
    (2, 2, '2021-02-18 11:32:34'),
    (3, 2, '2021-02-18 11:39:53'),
    (4, 2, '2021-02-18 11:40:17'),
    (5, 2, '2021-02-18 11:40:38'),
    (6, 2, '2021-02-18 11:40:59'),
    (7, 2, '2021-02-18 11:41:26'),
    (8, 2, '2021-02-18 12:11:00'),
    (9, 2, '2021-02-18 12:11:41'),
    (10, 2, '2021-02-18 12:12:08');

INSERT INTO `lw_message` (`message_id`, `sender_user_id`, `recipient_user_id`, `title`, `text`, `is_seen`, `is_read`, `created_at`) VALUES
    (1, 6, 7, 'Dolorum maxime.', 'Sed eos vitae voluptatem qui et est. Aliquid nam officiis dolore ipsa. Repellat ab tenetur eveniet nostrum.', 0, 1, '2016-02-14 19:34:25'),
    (2, 1, 1, 'Qui eaque rerum.', 'Doloribus aliquid harum cum facere totam est minus. Eveniet aut ab et fugiat dicta quia accusamus. Et consequatur eligendi in.', 1, 0, '2013-06-11 09:07:06'),
    (3, 1, 6, 'Omnis sit dolorum commodi.', 'Eos velit aut rerum ut ullam non. Aut repellat labore earum quasi autem sed libero.', 0, 0, '2019-01-04 11:00:47'),
    (4, 3, 3, 'Labore enim esse.', 'Voluptatem et labore sed est doloribus perferendis. Impedit quia qui iure inventore esse. Dicta optio aut sit est. Reprehenderit quibusdam est illum quia quasi est quis.', 1, 0, '2021-01-26 11:57:36'),
    (5, 2, 3, 'Expedita atque porro vero.', 'Soluta dicta occaecati optio ullam quae quia. Officiis pariatur temporibus et est qui. Repellendus aut maxime dignissimos doloribus sunt. Animi eum voluptatem consequatur expedita.', 0, 0, '2014-02-22 01:11:57'),
    (6, 7, 5, 'Exercitationem culpa voluptatem.', 'Voluptatum aliquid dolores aut aut commodi enim qui. Tempore velit et dolores libero. Officia sunt rerum ex aspernatur reprehenderit.', 0, 1, '2018-03-22 22:36:47'),
    (7, 2, 4, 'Voluptatibus labore assumenda.', 'Sed ea eligendi veniam et fuga est. In blanditiis enim sunt nihil consequuntur. Doloribus porro eveniet tempora qui iure. Aut rerum provident error et consequatur.', 0, 0, '2015-06-19 10:34:35'),
    (8, 4, 3, 'Eos deserunt quae enim.', 'Aliquam corrupti a enim ex vitae. Laboriosam expedita aut et. Fugiat rem ab sit quasi quos hic. Et similique at consequatur eveniet.', 0, 0, '2016-11-23 10:50:05'),
    (9, 1, 2, 'Quia fugiat dolores.', 'Deserunt sunt hic quaerat sint incidunt nemo. Harum autem eos reiciendis ipsum. Non sunt eos accusantium ut non ut.', 0, 0, '2020-11-13 17:17:16'),
    (10, 2, 2, 'Omnis ducimus.', 'Nobis debitis aut vero et delectus. Omnis velit facere voluptatum dolor quisquam. Ut earum excepturi ullam.', 0, 1, '2013-01-14 00:11:40');

INSERT INTO `lw_news` (`news_id`, `user_id`, `hidden`, `title`, `message`, `created_at`) VALUES
    (1, 2, 1, 'Dolores eum illum neque.', 'Omnis tempore et deserunt. Quia qui rerum qui eum commodi sint non. Porro in enim nam quia quo dolores nulla.', '2017-03-03 00:01:02'),
    (2, 2, 1, 'Aut sunt cupiditate eum.', 'Aliquid soluta qui corporis. Doloribus in quia fugiat quia non. Saepe reprehenderit error similique consequatur tempora. Facilis consequatur amet voluptas adipisci et et. Qui voluptatum dolores enim est.', '2012-08-26 18:55:38'),
    (3, 2, 0, 'Aut nemo alias est.', 'Vel officia consectetur esse molestiae quia fugiat atque non. Velit rerum tempora mollitia laborum id. Facilis voluptatem quo laboriosam veritatis. Rem voluptas ex et nostrum. Voluptatibus perferendis error a repudiandae debitis non. Doloremque eaque sed nesciunt in rerum debitis voluptas.', '2013-01-15 12:51:21'),
    (4, 1, 0, 'Facilis quisquam praesentium cum consequatur.', 'Sint voluptatem sed quis occaecati iure temporibus consequuntur. Consequuntur ut et earum et laboriosam. Ipsam est fuga omnis. Adipisci quo vero et vero. Veniam esse hic ab explicabo et.', '2014-04-07 02:50:31'),
    (5, 3, 1, 'Aut possimus autem ut.', 'Quod inventore doloremque qui numquam sed ad facere. Vero quis eos explicabo. Laborum placeat eos corrupti et qui. Qui aut dolorum fugiat est excepturi. Ut doloribus sit pariatur praesentium eligendi rerum harum. Impedit maxime repellat omnis sed odit dolorem.', '2016-11-21 08:55:07'),
    (6, 2, 1, 'Sapiente velit dolorem saepe non.', 'Voluptatem et velit explicabo. Quod iste nihil dolorem quo saepe ipsum. Commodi eaque nihil officia vero. Ea ab incidunt et. Rerum reiciendis velit doloremque quos atque velit quia. Iure fugiat sed ut molestiae esse sed.', '2020-12-11 03:18:24'),
    (7, 2, 0, 'Quo qui eos aliquid iure.', 'Modi incidunt voluptatum delectus natus. Velit ut impedit illo ut magni quis quasi. Amet explicabo autem sint excepturi vel nihil id. Fugit sint ea dolor aliquam a. Autem hic aut laboriosam voluptates. Accusantium maiores temporibus minima vero autem est fuga voluptatem.', '2015-09-09 12:07:19'),
    (8, 1, 1, 'Aliquam quos animi.', 'Harum qui ut ipsa magnam. Velit sit asperiores qui. Velit et maxime vero dolorem nam veniam. Illum sit ipsa tempora consectetur perferendis quidem perspiciatis. Sit et commodi quia aut est.', '2017-05-29 23:00:25'),
    (9, 1, 0, 'Omnis dolorem sit est.', 'Quas ut vitae doloremque doloribus quasi deserunt. Excepturi voluptatem ipsa nihil ut maiores ipsum corrupti. Similique molestiae dolorem rem sit. Molestiae tempore nisi illum. Ut dolor velit officia doloribus nostrum eos incidunt alias. Accusantium eum sit alias suscipit ad. Molestias amet voluptatem velit.', '2013-05-16 03:13:04'),
    (10, 1, 1, 'Deserunt commodi necessitatibus deserunt voluptatibus.', 'Molestias quibusdam a nemo et. Dolores earum possimus maiores est. Non qui quas quaerat odio veniam placeat quidem molestias. Non voluptate incidunt rerum odio enim commodi. Neque dolor nulla quo dolore.', '2019-08-20 13:21:46');

INSERT INTO `lw_user_log` (`log_entry_id`, `user_id`, `session_id`, `action`, `target_id`, `params`, `timestamp`, `group_id`) VALUES
    (1, 1, '42A21BC3D47DA8D2D92340F51A2F87C1', 12, 0, '', '2021-02-18 11:27:21', NULL),
    (2, 1, '42A21BC3D47DA8D2D92340F51A2F87C1', 9, 0, '/lw/user/register.jsf', '2021-02-18 11:27:22', NULL),
    (3, 1, '42A21BC3D47DA8D2D92340F51A2F87C1', 10, 0, '', '2021-02-18 11:27:28', NULL),
    (4, 2, '36A1490DDF869045121048337D1DC88C', 12, 0, '', '2021-02-18 11:32:34', NULL),
    (5, 2, '36A1490DDF869045121048337D1DC88C', 9, 0, '/lw/user/register.jsf', '2021-02-18 11:32:35', NULL),
    (6, 2, '36A1490DDF869045121048337D1DC88C', 10, 0, '', '2021-02-18 11:32:40', NULL),
    (7, 3, 'EA20BD4ADBFF3B71276AB848CFAFD63E', 12, 0, '', '2021-02-18 11:39:53', NULL),
    (8, 3, 'EA20BD4ADBFF3B71276AB848CFAFD63E', 9, 0, '/lw/user/register.jsf', '2021-02-18 11:39:55', NULL),
    (9, 3, 'EA20BD4ADBFF3B71276AB848CFAFD63E', 10, 0, '', '2021-02-18 11:39:58', NULL),
    (10, 4, 'B244C25CDD289EA568671C35F3B72BA3', 12, 0, '', '2021-02-18 11:40:17', NULL),
    (11, 4, 'B244C25CDD289EA568671C35F3B72BA3', 9, 0, '/lw/user/register.jsf', '2021-02-18 11:40:18', NULL),
    (12, 4, 'B244C25CDD289EA568671C35F3B72BA3', 10, 0, '', '2021-02-18 11:40:23', NULL),
    (13, 5, 'EB8BBC317917496A1FAD3318FFFCEE35', 12, 0, '', '2021-02-18 11:40:38', NULL),
    (14, 5, 'EB8BBC317917496A1FAD3318FFFCEE35', 9, 0, '/lw/user/register.jsf', '2021-02-18 11:40:39', NULL),
    (15, 5, 'EB8BBC317917496A1FAD3318FFFCEE35', 10, 0, '', '2021-02-18 11:40:43', NULL),
    (16, 6, '8D400DA685E25E1EEC7B9276C158A0CC', 12, 0, '', '2021-02-18 11:40:59', NULL),
    (17, 6, '8D400DA685E25E1EEC7B9276C158A0CC', 9, 0, '/lw/user/register.jsf', '2021-02-18 11:41:00', NULL),
    (18, 6, '8D400DA685E25E1EEC7B9276C158A0CC', 10, 0, '', '2021-02-18 11:41:03', NULL),
    (19, 7, '7BE8A0CC0E5EC83C8C760ADF75A6B7F7', 12, 0, '', '2021-02-18 11:41:26', NULL),
    (20, 7, '7BE8A0CC0E5EC83C8C760ADF75A6B7F7', 9, 0, '/lw/user/register.jsf', '2021-02-18 11:41:27', NULL),
    (21, 7, '7BE8A0CC0E5EC83C8C760ADF75A6B7F7', 10, 0, '', '2021-02-18 12:09:31', NULL),
    (22, 8, 'DBD7CD45188CBC498492A3B91A585DE6', 12, 0, '', '2021-02-18 12:11:00', NULL),
    (23, 8, 'DBD7CD45188CBC498492A3B91A585DE6', 9, 0, '/lw/user/register.jsf', '2021-02-18 12:11:01', NULL),
    (24, 8, 'DBD7CD45188CBC498492A3B91A585DE6', 10, 0, '', '2021-02-18 12:11:04', NULL),
    (25, 9, '5FFAAD224A2DD55A9380BCA0E18973F5', 12, 0, '', '2021-02-18 12:11:41', NULL),
    (26, 9, '5FFAAD224A2DD55A9380BCA0E18973F5', 9, 0, '/lw/user/register.jsf', '2021-02-18 12:11:42', NULL),
    (27, 9, '5FFAAD224A2DD55A9380BCA0E18973F5', 10, 0, '', '2021-02-18 12:11:46', NULL),
    (28, 10, '4ECFDC6DC72CC9DC74FF4420A95E03C5', 12, 0, '', '2021-02-18 12:12:08', NULL),
    (29, 10, '4ECFDC6DC72CC9DC74FF4420A95E03C5', 9, 0, '/lw/user/register.jsf', '2021-02-18 12:12:09', NULL),
    (30, 10, '4ECFDC6DC72CC9DC74FF4420A95E03C5', 10, 0, '', '2021-02-18 12:12:41', NULL);
