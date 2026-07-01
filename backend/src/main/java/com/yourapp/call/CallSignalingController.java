package com.yourapp.call;

import com.yourapp.call.dto.CallSignalMessage;
import com.yourapp.call.entity.CallSessionSnapshot;
import com.yourapp.call.service.CallSessionService;
import com.yourapp.call.service.FcmCallService;
import com.yourapp.notification.NotificationService;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CallSignalingController {

    private final CallSessionService callSessionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final FcmCallService fcmCallService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public CallSignalingController(CallSessionService callSessionService,
                                   SimpMessagingTemplate messagingTemplate,
                                   FcmCallService fcmCallService,
                                   NotificationService notificationService,
                                   UserRepository userRepository) {
        this.callSessionService = callSessionService;
        this.messagingTemplate = messagingTemplate;
        this.fcmCallService = fcmCallService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @MessageMapping("/call.initiate")
    public void initiateCall(CallSignalMessage message, Principal principal) {
        startIncomingCall(message, principal);
    }

    @PostMapping("/api/calls/initiate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> initiateCallRest(@RequestBody CallSignalMessage message, Principal principal) {
        CallSignalMessage event = startIncomingCall(message, principal);
        return ResponseEntity.ok(ok("OK", event));
    }

    private CallSignalMessage startIncomingCall(CallSignalMessage message, Principal principal) {
        User caller = extractUser(principal);
        if (message == null || message.getCalleeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "calleeId is required");
        }
        User callee = userRepository.findById(message.getCalleeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Callee not found"));
        if (!StringUtils.hasText(message.getSessionId())) {
            message.setSessionId(UUID.randomUUID().toString());
        }
        message.setCallerId(caller.getId());
        message.setCallerName(displayName(caller));
        message.setCallerAvatarUrl(caller.getAvatarUrl());
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }

        CallSessionSnapshot snapshot = callSessionService.startCall(
                message.getSessionId(),
                caller.getId(),
                callee.getId(),
                displayName(caller),
                caller.getAvatarUrl(),
                displayName(callee),
                message.getSdp());

        CallSignalMessage event = copy(message);
        event.setType("INCOMING_CALL");
        event.setSdp(snapshot.getOfferSdp());
        sendToUser(callee.getId(), event);

        notificationService.sendToUser(callee.getId(), "INCOMING_CALL", "Cuoc goi moi",
                displayName(caller) + " dang goi cho ban", caller,
                notificationService.data(
                        "type", "INCOMING_CALL",
                        "callSessionId", message.getSessionId(),
                        "callerId", caller.getId(),
                        "callerName", displayName(caller),
                        "callerAvatar", caller.getAvatarUrl()));

        fcmCallService.sendIncomingCallNotification(callee, caller, message.getSessionId());
        return event;
    }
    @MessageMapping("/call.answer")
    public void answerCall(CallSignalMessage message, Principal principal) {
        answerCallInternal(message, principal);
    }

    @PostMapping("/api/calls/answer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> answerCallRest(@RequestBody CallSignalMessage message, Principal principal) {
        CallSignalMessage event = answerCallInternal(message, principal);
        return ResponseEntity.ok(ok("OK", event));
    }

    private CallSignalMessage answerCallInternal(CallSignalMessage message, Principal principal) {
        User callee = extractUser(principal);
        requireSession(message);
        CallSessionSnapshot existing = callSessionService.findSnapshot(message.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call session not found"));
        if (!callee.getId().equals(existing.getCalleeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not call participant");
        }
        CallSessionSnapshot snapshot = callSessionService.recordAnswer(message.getSessionId(), message.getSdp())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call session not found"));
        CallSignalMessage event = copy(message);
        event.setType("CALL_ANSWERED");
        event.setSdp(snapshot.getAnswerSdp());
        sendToUser(snapshot.getCallerId(), event);
        return event;
    }
    @MessageMapping("/call.ice-candidate")
    public void iceCandidate(CallSignalMessage message, Principal principal) {
        iceCandidateInternal(message, principal);
    }

    @PostMapping("/api/calls/ice-candidate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> iceCandidateRest(@RequestBody CallSignalMessage message, Principal principal) {
        CallSignalMessage event = iceCandidateInternal(message, principal);
        return ResponseEntity.ok(ok("OK", event));
    }

    private CallSignalMessage iceCandidateInternal(CallSignalMessage message, Principal principal) {
        User sender = extractUser(principal);
        requireSession(message);
        CallSessionSnapshot snapshot = callSessionService.findSnapshot(message.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call session not found"));
        if (!isParticipant(sender.getId(), snapshot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not call participant");
        }
        callSessionService.recordCandidate(
                message.getSessionId(),
                message.getCandidate(),
                message.getSdpMid(),
                message.getSdpMLineIndex(),
                sender.getId());
        Long peerId = peerId(sender.getId(), snapshot);
        CallSignalMessage event = copy(message);
        event.setType("ICE_CANDIDATE");
        event.setCallerId(sender.getId());
        event.setCallerName(displayName(sender));
        sendToUser(peerId, event);
        return event;
    }

    @MessageMapping("/call.reject")
    public void rejectCall(CallSignalMessage message, Principal principal) {
        User actor = extractUser(principal);
        requireSession(message);
        CallSessionSnapshot existing = callSessionService.findSnapshot(message.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call session not found"));
        if (!isParticipant(actor.getId(), existing)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not call participant");
        }
        CallSessionSnapshot snapshot = callSessionService.reject(message.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call session not found"));
        CallSignalMessage event = copy(message);
        event.setType("CALL_REJECTED");
        Long peer = peerId(actor.getId(), snapshot);
        sendToUser(peer, event);
        fcmCallService.sendCallEndedNotification(userRepository.findById(peer).orElse(null), message.getSessionId());
    }

    @MessageMapping("/call.end")
    public void endCall(CallSignalMessage message, Principal principal) {
        endCallInternal(message, principal);
    }

    @PostMapping("/api/calls/end")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> endCallRest(@RequestBody CallSignalMessage message, Principal principal) {
        CallSignalMessage event = endCallInternal(message, principal);
        return ResponseEntity.ok(ok("OK", event));
    }

    private CallSignalMessage endCallInternal(CallSignalMessage message, Principal principal) {
        User actor = extractUser(principal);
        requireSession(message);
        CallSessionSnapshot existing = callSessionService.findSnapshot(message.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call session not found"));
        if (!isParticipant(actor.getId(), existing)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not call participant");
        }
        CallSessionSnapshot snapshot = callSessionService.end(message.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call session not found"));
        CallSignalMessage event = copy(message);
        event.setType("CALL_ENDED");
        Long peer = peerId(actor.getId(), snapshot);
        sendToUser(peer, event);
        fcmCallService.sendCallEndedNotification(userRepository.findById(peer).orElse(null), message.getSessionId());
        return event;
    }
    private void requireSession(CallSignalMessage message) {
        if (message == null || !StringUtils.hasText(message.getSessionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required");
        }
    }

    private boolean isParticipant(Long userId, CallSessionSnapshot snapshot) {
        return snapshot != null
                && (userId.equals(snapshot.getCallerId()) || userId.equals(snapshot.getCalleeId()));
    }

    private Long peerId(Long actorId, CallSessionSnapshot snapshot) {
        if (snapshot.getCallerId() != null && snapshot.getCallerId().equals(actorId)) {
            return snapshot.getCalleeId();
        }
        return snapshot.getCallerId();
    }

    private void sendToUser(Long userId, CallSignalMessage message) {
        if (userId == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/call/" + userId, message);
    }

    private CallSignalMessage copy(CallSignalMessage source) {
        CallSignalMessage message = new CallSignalMessage();
        message.setType(source.getType());
        message.setCallerId(source.getCallerId());
        message.setCalleeId(source.getCalleeId());
        message.setSessionId(source.getSessionId());
        message.setSdp(source.getSdp());
        message.setCandidate(source.getCandidate());
        message.setSdpMid(source.getSdpMid());
        message.setSdpMLineIndex(source.getSdpMLineIndex());
        message.setTimestamp(source.getTimestamp());
        message.setCallerName(source.getCallerName());
        message.setCallerAvatarUrl(source.getCallerAvatarUrl());
        return message;
    }

    private Map<String, Object> ok(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("error", null);
        return response;
    }

    private User extractUser(Principal principal) {
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof User user) {
            return user;
        }
        throw new AccessDeniedException("Unauthenticated websocket connection");
    }

    private String displayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getUsername();
    }
}
