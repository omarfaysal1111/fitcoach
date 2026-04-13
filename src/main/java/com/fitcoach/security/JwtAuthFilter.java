package com.fitcoach.security;

import com.fitcoach.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            var claimsOpt = jwtUtil.parseValidClaims(token);
            if (claimsOpt.isPresent()) {
                Claims claims = claimsOpt.get();
                if (claims.getIssuedAt() != null && isSuperseded(claims)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token has been replaced by a newer login");
                    return;
                }
                String email = claims.getSubject();
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSuperseded(Claims claims) {
        Long boundary = userRepository.findByEmail(claims.getSubject())
                .map(u -> u.getJwtIssuedEpochSec())
                .orElse(null);
        if (boundary == null) {
            return false;
        }
        long tokenIatSec = claims.getIssuedAt().toInstant().getEpochSecond();
        return tokenIatSec < boundary;
    }

    /**
     * Uses the last Bearer credential when multiple {@code Authorization} header values are present
     * (duplicate headers or comma-separated list), so the most recently attached token wins.
     */
    private String extractToken(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaders("Authorization");
        if (headers == null || !headers.hasMoreElements()) {
            return null;
        }
        String latest = null;
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            if (!StringUtils.hasText(header)) {
                continue;
            }
            for (String part : header.split(",")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("Bearer ")) {
                    latest = trimmed.substring(7).trim();
                }
            }
        }
        return latest;
    }
}
