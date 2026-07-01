package com.yourapp.story;

import com.yourapp.story.repository.StoryRepository;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryMaintenanceService {

    private final StoryRepository storyRepository;

    public StoryMaintenanceService(StoryRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    @Transactional
    @Scheduled(fixedDelay = 600000)
    public void purgeExpiredStories() {
        storyRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}