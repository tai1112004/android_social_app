ALTER TABLE `messages`
    ADD COLUMN `reply_preview` VARCHAR(1000) NULL AFTER `reply_to_message_id`;
