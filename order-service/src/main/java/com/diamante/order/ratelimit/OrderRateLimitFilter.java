package com.diamante.order.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "rate-limit.orders.enabled", havingValue = "true", matchIfMissing = true)
public class OrderRateLimitFilter extends OncePerRequestFilter {

    private static final String ORDERS_PATH = "/orders";
    private static final String LIMIT_EXCEEDED_BODY = "{\"error\":\"Too many requests, please try again shortly\"}";

    private final SlidingWindowRateLimiter rateLimiter;

    public OrderRateLimitFilter(@Value("${rate-limit.orders.permits-per-second:20}") int permitsPerSecond) {
        this.rateLimiter = new SlidingWindowRateLimiter(permitsPerSecond, 1000);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !ORDERS_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (rateLimiter.tryAcquire()) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "1");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(LIMIT_EXCEEDED_BODY);
    }
}
