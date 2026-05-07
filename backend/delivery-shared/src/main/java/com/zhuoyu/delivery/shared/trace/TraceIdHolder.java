package com.zhuoyu.delivery.shared.trace;

import org.slf4j.MDC;

public final class TraceIdHolder {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceIdHolder() {
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }
}
