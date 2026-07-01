package com.yourapp.post.repository;

import com.yourapp.post.entity.PostCommentReaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentReactionRepository extends JpaRepository<PostCommentReaction, Long> {
    long countByCommentId(Long commentId);
    Optional<PostCommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}