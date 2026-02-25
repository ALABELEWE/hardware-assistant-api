package com.hardwareassistant.hardware_assistant_api.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_HOUR = 5;
    private static final long WINDOW_MS = 60 * 60 * 1000L;

    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStart = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String userEmail = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName() : request.getRemoteAddr();

        long now = System.currentTimeMillis();
        windowStart.putIfAbsent(userEmail, now);
        requestCounts.putIfAbsent(userEmail, new AtomicInteger(0));

        // Reset window if expired
        if (now - windowStart.get(userEmail) > WINDOW_MS) {
            windowStart.put(userEmail, now);
            requestCounts.get(userEmail).set(0);
        }

        int count = requestCounts.get(userEmail).incrementAndGet();
        if (count > MAX_REQUESTS_PER_HOUR) {
            log.warn("RATE LIMIT exceeded for user: {}", userEmail);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\": \"Rate limit exceeded. Max 5 analyses per hour.\"}");
            return;
        }

        chain.doFilter(req, res);
    }
}