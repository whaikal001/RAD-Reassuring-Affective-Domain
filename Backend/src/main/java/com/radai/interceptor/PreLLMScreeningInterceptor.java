package com.radai.interceptor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class PreLLMScreeningInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();

        // Only enforce for chat flow AI processing endpoints
        if (!path.startsWith("/api/chat/flow")) {
            return true;
        }

        // Allow authenticated users (registered) to proceed
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()));
        if (!isAnonymous) {
            return true;
        }

        // For anonymous users, expect screening headers from the client
        String screeningCompleted = request.getHeader("X-Screening-Completed");
        String screeningAction = request.getHeader("X-Screening-Action");

        if (!"true".equalsIgnoreCase(screeningCompleted)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"screening_required\",\"message\":\"Please complete the pre-chat screening before starting a conversation.\"}");
            return false;
        }

        // If screening result indicates intervention, block the LLM access
        if (screeningAction != null && screeningAction.equalsIgnoreCase("intervention")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"intervention_required\",\"message\":\"High stress detected. Conversation access blocked for safety.\"}");
            return false;
        }

        // Otherwise allow (allow and prevention both proceed)
        return true;
    }
}

