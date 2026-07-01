ALTER TABLE `users`
    ADD COLUMN IF NOT EXISTS `cover_url` VARCHAR(500) NULL AFTER `avatar_url`,
    ADD COLUMN IF NOT EXISTS `bio` TEXT NULL AFTER `cover_url`,
    ADD COLUMN IF NOT EXISTS `location` VARCHAR(120) NULL AFTER `bio`,
    ADD COLUMN IF NOT EXISTS `website` VARCHAR(255) NULL AFTER `location`;

ALTER TABLE `messages`
    ADD COLUMN IF NOT EXISTS `reply_context_type` VARCHAR(20) NULL AFTER `reply_preview`,
    ADD COLUMN IF NOT EXISTS `reply_context_id` BIGINT NULL AFTER `reply_context_type`,
    ADD COLUMN IF NOT EXISTS `reply_context_author_id` BIGINT NULL AFTER `reply_context_id`,
    ADD COLUMN IF NOT EXISTS `reply_context_author_username` VARCHAR(50) NULL AFTER `reply_context_author_id`,
    ADD COLUMN IF NOT EXISTS `reply_context_author_display_name` VARCHAR(100) NULL AFTER `reply_context_author_username`,
    ADD COLUMN IF NOT EXISTS `reply_context_text` VARCHAR(2000) NULL AFTER `reply_context_author_display_name`,
    ADD COLUMN IF NOT EXISTS `reply_context_media_url` VARCHAR(1000) NULL AFTER `reply_context_text`;