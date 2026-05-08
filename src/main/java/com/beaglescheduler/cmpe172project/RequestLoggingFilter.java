package com.beaglescheduler.cmpe172project;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every inbound HTTP request and its response status + duration.
 * Runs before the security filter chain so even rejected requests are visible.
 */
@Component
@Order(Integer.MIN_VALUE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("HTTP");

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String query = req.getQueryString() != null ? "?" + req.getQueryString() : "";
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = System.currentTimeMillis() - start;
            log.info("{} {}{} -> {} ({}ms)",
                req.getMethod(),
                req.getRequestURI(),
                query,
                res.getStatus(),
                ms);
        }
    }
}
