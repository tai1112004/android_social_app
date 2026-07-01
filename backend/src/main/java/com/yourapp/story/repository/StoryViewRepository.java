package com.yourapp.story.repository;

import com.yourapp.story.entity.StoryView;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryViewRepository extends JpaRepository<StoryView, Long> {
    boolean existsByStoryIdAndViewerId(Long storyId, Long viewerId);
    Optional<StoryView> findByStoryIdAndViewerId(Long storyId, Long viewerId);
    List<StoryView> findByStoryIdOrderByViewedAtDesc(Long storyId);
}
