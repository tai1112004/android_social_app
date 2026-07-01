package com.yourapp.conversation.repository;

import com.yourapp.conversation.entity.Conversation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for Conversation entity.
 * Provides query to find all conversations a user belongs to
 * via the conversation_members join table.
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Find all conversations of a given user (by userId), ordered by creation date DESC.
     * Joins Conversation with ConversationMember on conversation id.
     */
    @Query("SELECT c FROM Conversation c JOIN ConversationMember cm " +
           "ON c.id = cm.conversation.id WHERE cm.user.id = :userId " +
           "ORDER BY c.createdAt DESC")
    List<Conversation> findByUserId(@Param("userId") Long userId);
    @Query("SELECT c FROM Conversation c JOIN ConversationMember cm1 " +
           "ON c.id = cm1.conversation.id JOIN ConversationMember cm2 " +
           "ON c.id = cm2.conversation.id WHERE cm1.user.id = :userId1 " +
           "AND cm2.user.id = :userId2 AND c.type = 'PRIVATE'")
    List<Conversation> findPrivateConversationBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
