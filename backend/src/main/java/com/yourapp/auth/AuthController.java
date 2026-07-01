package com.yourapp.auth;

import com.yourapp.auth.dto.AuthResponse;
import com.yourapp.auth.dto.LoginRequest;
import com.yourapp.auth.dto.RegisterRequest;
import com.yourapp.auth.dto.RefreshRequest;
import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request);
        Map<String, Object> body = createResponse(true, "Registered", Map.of("userId", userId), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        Map<String, Object> body = createResponse(true, "Login successful", authResponse, null);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshRequest request) {
        String accessToken = authService.refresh(request);
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("tokenType", "Bearer");
        Map<String, Object> body = createResponse(true, "Token refreshed", data, null);
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> createResponse(boolean success, String message, Object data, Object error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        response.put("error", error);
        return response;
    }
}
