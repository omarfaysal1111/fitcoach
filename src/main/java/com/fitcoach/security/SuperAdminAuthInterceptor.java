package com.fitcoach.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Base64;

@Component
public class SuperAdminAuthInterceptor implements HandlerInterceptor {

    public static final String ADMIN_EMAIL    = "omarfaysaladmin@admin.co";
    public static final String ADMIN_PASSWORD = "x6-4C37M";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        if (!isAuthorized(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error("Invalid admin credentials"));
            return false;
        }
        return true;
    }

    public boolean isAuthorized(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Basic ")) return false;
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6)));
            String[] parts = decoded.split(":", 2);
            return parts.length == 2
                    && ADMIN_EMAIL.equals(parts[0])
                    && ADMIN_PASSWORD.equals(parts[1]);
        } catch (Exception e) {
            return false;
        }
    }
}
