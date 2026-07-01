package com.yourapp.story.repository;

import com.yourapp.story.entity.Story;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryRepository extends JpaRepository<Story, Long> {
    @Query("SELECT s FROM Story s WHERE s.user.id IN :userIds AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findActiveByUserIds(@Param("userIds") List<Long> userIds, @Param("now") LocalDateTime now);

    Optional<Story> findByIdAndExpiresAtAfter(Long id, LocalDateTime now);

    long countByUserIdAndExpiresAtAfter(Long userId, LocalDateTime now);

    long deleteByExpiresAtBefore(LocalDateTime now);
}