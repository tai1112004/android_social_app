package com.yourapp.call.service;

import com.yourapp.call.entity.CallHistory;
import com.yourapp.call.entity.CallSessionSnapshot;
import com.yourapp.call.entity.CallSessionSnapshot.CallSignalCandidate;
import com.yourapp.call.repository.CallHistoryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallSessionService {

    public static final long RING_TIMEOUT_SECONDS = 30L;

    private final Map<String, CallSessionState> sessions = new ConcurrentHashMap<>();
    private final CallHistoryRepository callHistoryRepository;

    public CallSessionService(CallHistoryRepository callHistoryRepository) {
        this.callHistoryRepository = callHistoryRepository;
    }

    @Transactional
    public CallSessionSnapshot startCall(String sessionId,
                                         Long callerId,
                                         Long calleeId,
                                         String callerName,
                                         String callerAvatarUrl,
                                         String calleeName,
                                         String offerSdp) {
        CallSessionState state = sessions.computeIfAbsent(sessionId, key -> new CallSessionState());
        state.sessionId = sessionId;
        state.callerId = callerId;
        state.calleeId = calleeId;
        state.callerName = callerName;
        state.callerAvatarUrl = callerAvatarUrl;
        state.calleeName = calleeName;
        state.offerSdp = offerSdp;
        state.status = "RINGING";
        if (state.startedAt == null) {
            state.startedAt = LocalDateTime.now();
        }
        state.timeoutSeconds = RING_TIMEOUT_SECONDS;
        return snapshot(state);
    }

    @Transactional
    public Optional<CallSessionSnapshot> recordAnswer(String sessionId, String answerSdp) {
        CallSessionState state = sessions.get(sessionId);
        if (state == null) {
            return Optional.empty();
        }
        state.answerSdp = answerSdp;
        state.answeredAt = LocalDateTime.now();
        state.status = "ACTIVE";
        return Optional.of(snapshot(state));
    }

    @Transactional
    public Optional<CallSessionSnapshot> recordCandidate(String sessionId, String candidate, String sdpMid, Integer sdpMLineIndex, Long senderId) {
        CallSessionState state = sessions.get(sessionId);
        if (state == null) {
            return Optional.empty();
        }
        CallSignalCandidate signalCandidate = new CallSignalCandidate();
        signalCandidate.setCandidate(candidate);
        signalCandidate.setSdpMid(sdpMid);
        signalCandidate.setSdpMLineIndex(sdpMLineIndex);
        signalCandidate.setSenderId(senderId);
        state.pendingCandidates.add(signalCandidate);
        return Optional.of(snapshot(state));
    }

    @Transactional
    public Optional<CallSessionSnapshot> reject(String sessionId) {
        CallSessionState state = sessions.remove(sessionId);
        if (state == null) {
            return Optional.empty();
        }
        state.status = "REJECTED";
        state.endedAt = LocalDateTime.now();
        saveHistory(state);
        return Optional.of(snapshot(state));
    }

    @Transactional
    public Optional<CallSessionSnapshot> end(String sessionId) {
        CallSessionState state = sessions.remove(sessionId);
        if (state == null) {
            return Optional.empty();
        }
        if (state.answeredAt == null) {
            state.status = "NO_ANSWER";
        } else {
            state.status = "COMPLETED";
        }
        state.endedAt = LocalDateTime.now();
        saveHistory(state);
        return Optional.of(snapshot(state));
    }

    @Transactional(readOnly = true)
    public Optional<CallSessionSnapshot> findSnapshot(String sessionId) {
        CallSessionState state = sessions.get(sessionId);
        return state == null ? Optional.empty() : Optional.of(snapshot(state));
    }

    @Transactional(readOnly = true)
    public Optional<CallSessionSnapshot> findIncomingRinging(Long calleeId) {
        if (calleeId == null) {
            return Optional.empty();
        }
        for (CallSessionState state : sessions.values()) {
            if ("RINGING".equals(state.status) && calleeId.equals(state.calleeId)) {
                return Optional.of(snapshot(state));
            }
        }
        return Optional.empty();
    }

    @Scheduled(fixedDelay = 5000)
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expired = new ArrayList<>();
        for (CallSessionState state : sessions.values()) {
            if ("RINGING".equals(state.status)
                    && state.startedAt != null
                    && Duration.between(state.startedAt, now).getSeconds() >= RING_TIMEOUT_SECONDS) {
                state.status = "NO_ANSWER";
                state.endedAt = now;
                saveHistory(state);
                expired.add(state.sessionId);
            }
        }
        for (String sessionId : expired) {
            sessions.remove(sessionId);
        }
    }

    private void saveHistory(CallSessionState state) {
        CallHistory history = new CallHistory();
        history.setSessionId(state.sessionId);
        history.setCallerId(state.callerId);
        history.setCalleeId(state.calleeId);
        history.setCallerName(state.callerName);
        history.setCalleeName(state.calleeName);
        history.setStartTime(state.startedAt != null ? state.startedAt : LocalDateTime.now());
        history.setEndTime(state.endedAt != null ? state.endedAt : LocalDateTime.now());
        history.setDurationSeconds((int) Math.max(0, Duration.between(history.getStartTime(), history.getEndTime()).getSeconds()));
        history.setStatus(state.status);
        callHistoryRepository.findBySessionId(state.sessionId).ifPresent(existing -> history.setId(existing.getId()));
        callHistoryRepository.save(history);
    }

    private CallSessionSnapshot snapshot(CallSessionState state) {
        CallSessionSnapshot snapshot = new CallSessionSnapshot();
        snapshot.setSessionId(state.sessionId);
        snapshot.setCallerId(state.callerId);
        snapshot.setCalleeId(state.calleeId);
        snapshot.setCallerName(state.callerName);
        snapshot.setCallerAvatarUrl(state.callerAvatarUrl);
        snapshot.setCalleeName(state.calleeName);
        snapshot.setOfferSdp(state.offerSdp);
        snapshot.setAnswerSdp(state.answerSdp);
        snapshot.setStatus(state.status);
        snapshot.setStartedAt(state.startedAt);
        snapshot.setAnsweredAt(state.answeredAt);
        snapshot.setEndedAt(state.endedAt);
        snapshot.setTimeoutSeconds(state.timeoutSeconds);
        snapshot.setPendingCandidates(state.pendingCandidates);
        return snapshot;
    }

    private static class CallSessionState {
        private String sessionId;
        private Long callerId;
        private Long calleeId;
        private String callerName;
        private String callerAvatarUrl;
        private String calleeName;
        private String offerSdp;
        private String answerSdp;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime answeredAt;
        private LocalDateTime endedAt;
        private long timeoutSeconds;
        private final List<CallSignalCandidate> pendingCandidates = new ArrayList<>();
    }
}
