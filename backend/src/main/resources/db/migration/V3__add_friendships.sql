-- Flyway V3: Add friendships table and last_seen_at to users
-- Friendships: status can be PENDING, ACCEPTED, BLOCKED

ALTER TABLE `users`
    ADD COLUMN `last_seen_at` DATETIME NULL;

CREATE TABLE IF NOT EXISTS `friendships` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `requester_id` BIGINT NOT NULL,
    `addressee_id` BIGINT NOT NULL,
    `status` ENUM('PENDING','ACCEPTED','BLOCKED') NOT NULL DEFAULT 'PENDING',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uq_friendship` UNIQUE (`requester_id`, `addressee_id`),
    CONSTRAINT `fk_friendship_requester`
        FOREIGN KEY (`requester_id`) REFERENCES `users`(`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_friendship_addressee`
        FOREIGN KEY (`addressee_id`) REFERENCES `users`(`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_friendships_addressee` ON `friendships` (`addressee_id`);
CREATE INDEX `idx_friendships_requester` ON `friendships` (`requester_id`);
