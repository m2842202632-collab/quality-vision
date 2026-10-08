package com.qualityvision.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：未登录访问 /api/** 一律 401，前端据此跳登录页。
 * 白名单：/api/auth/login、静态资源。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/")) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("uid") != null) {
            return true;
        }
        response.setStatus(401);
        return false;
    }
}
