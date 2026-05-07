package com.zhuoyu.delivery.shared.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = Optional.ofNullable(request.getHeader(TraceIdHolder.TRACE_ID_HEADER))
            .filter(value -> !value.isBlank())
            .orElseGet(() -> UUID.randomUUID().toString().replace("-", ""));
        TraceIdHolder.setTraceId(traceId);
        response.setHeader(TraceIdHolder.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIdHolder.clear();
        }
    }
}
