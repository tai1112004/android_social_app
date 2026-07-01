package com.yourapp.call.repository;

import com.yourapp.call.entity.CallHistory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallHistoryRepository extends JpaRepository<CallHistory, Long> {

    Optional<CallHistory> findBySessionId(String sessionId);

    Page<CallHistory> findByCallerIdOrCalleeIdOrderByStartTimeDesc(Long callerId, Long calleeId, Pageable pageable);
}
