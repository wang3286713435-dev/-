package com.zhuoyu.delivery.shared.api;

import com.zhuoyu.delivery.shared.trace.TraceIdHolder;
import java.time.Instant;

public record ApiResponse<T>(
    String code,
    String message,
    T data,
    String traceId,
    Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "success", data, TraceIdHolder.getTraceId(), Instant.now());
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null, TraceIdHolder.getTraceId(), Instant.now());
    }
}
