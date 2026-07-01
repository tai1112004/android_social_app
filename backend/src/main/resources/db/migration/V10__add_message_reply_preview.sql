ALTER TABLE `messages`
    ADD COLUMN IF NOT EXISTS `reply_preview` VARCHAR(1000) NULL AFTER `reply_to_message_id`;