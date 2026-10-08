package com.qualityvision.controller;

import com.qualityvision.domain.SysUser;
import com.qualityvision.dto.ApiResponse;
import com.qualityvision.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** token -> username，演示级会话存储 */
    private static final Map<String, String> SESSIONS = new ConcurrentHashMap<>();

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody Map<String, String> body,
                                                 HttpSession session) {
        try {
            SysUser user = authService.login(body.get("username"), body.get("password"));
            String token = UUID.randomUUID().toString();
            SESSIONS.put(token, user.getUsername());
            session.setAttribute("uid", user.getId());
            session.setAttribute("username", user.getUsername());
            return ApiResponse.ok(Map.of("token", token, "username", user.getUsername()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, String>> me(HttpSession session) {
        Object username = session.getAttribute("username");
        if (username == null) {
            return ApiResponse.fail("未登录");
        }
        return ApiResponse.ok(Map.of("username", String.valueOf(username)));
    }
}
