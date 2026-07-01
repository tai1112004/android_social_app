ALTER TABLE messages
    MODIFY COLUMN type VARCHAR(20),
    ADD COLUMN reply_to_message_id BIGINT NULL,
    ADD COLUMN reaction VARCHAR(32) NULL,
    ADD COLUMN media_url VARCHAR(1000) NULL,
    ADD CONSTRAINT fk_messages_reply_to
        FOREIGN KEY (reply_to_message_id) REFERENCES messages(id);
