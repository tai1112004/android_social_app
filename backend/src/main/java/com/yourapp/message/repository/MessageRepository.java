package com.yourapp.message.repository;

import com.yourapp.message.entity.Message;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdAndDeletedFalseOrderBySentAtAsc(Long conversationId);
    Message findTopByConversationIdAndDeletedFalseOrderBySentAtDesc(Long conversationId);
    long countByConversationIdAndDeletedFalseAndSentAtAfterAndSenderIdNot(Long conversationId,
                                                                         LocalDateTime sentAt,
                                                                         Long senderId);
}