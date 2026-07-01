package com.yourapp.conversation.repository;

import com.yourapp.conversation.entity.ConversationMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for ConversationMember entity.
 * Used for counting members of a conversation.
 */
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    /**
     * Count members belonging to a specific conversation.
     */
    int countByConversationId(Long conversationId);

    /**
     * Check whether a user is a member of a specific conversation.
     */
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    List<ConversationMember> findByConversationId(Long conversationId);

    List<ConversationMember> findByConversationIdOrderByRoleAscJoinedAtAsc(Long conversationId);
}