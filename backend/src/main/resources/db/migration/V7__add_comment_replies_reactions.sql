ALTER TABLE `post_comments`
    ADD COLUMN `reply_to_comment_id` BIGINT NULL AFTER `user_id`;

ALTER TABLE `post_comments`
    ADD CONSTRAINT `fk_post_comments_reply_to`
        FOREIGN KEY (`reply_to_comment_id`) REFERENCES `post_comments`(`id`)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

CREATE TABLE IF NOT EXISTS `post_comment_reactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `comment_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `reaction` VARCHAR(32) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_post_comment_reactions_comment_user` (`comment_id`, `user_id`),
    CONSTRAINT `fk_post_comment_reactions_comment`
        FOREIGN KEY (`comment_id`) REFERENCES `post_comments`(`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT `fk_post_comment_reactions_user`
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;