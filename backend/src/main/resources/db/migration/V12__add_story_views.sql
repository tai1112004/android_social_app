CREATE TABLE IF NOT EXISTS `story_views` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `story_id` BIGINT NOT NULL,
    `viewer_id` BIGINT NOT NULL,
    `viewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_story_views_story_viewer` (`story_id`, `viewer_id`),
    CONSTRAINT `fk_story_views_story`
        FOREIGN KEY (`story_id`) REFERENCES `stories`(`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT `fk_story_views_viewer`
        FOREIGN KEY (`viewer_id`) REFERENCES `users`(`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
