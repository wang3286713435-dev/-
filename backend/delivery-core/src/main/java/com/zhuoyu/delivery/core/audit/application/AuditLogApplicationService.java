package com.zhuoyu.delivery.core.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuoyu.delivery.core.audit.dto.AuditLogResponse;
import com.zhuoyu.delivery.core.audit.repository.AuditLogRepository;
import com.zhuoyu.delivery.shared.trace.TraceIdHolder;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuditLogApplicationService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogApplicationService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void record(
        Long projectId,
        String actionCode,
        String targetType,
        String targetId,
        Long operatorId,
        Map<String, Object> details
    ) {
        record(projectId, "core", actionCode, targetType, targetId, operatorId, details);
    }

    public void record(
        Long projectId,
        String moduleCode,
        String actionCode,
        String targetType,
        String targetId,
        Long operatorId,
        Map<String, Object> details
    ) {
        auditLogRepository.insert(
            projectId,
            moduleCode,
            actionCode,
            targetType,
            targetId,
            operatorId,
            TraceIdHolder.getTraceId(),
            toJson(details)
        );
    }

    public List<AuditLogResponse> listLatest(Long projectId, String moduleCode, int limit) {
        String normalizedModule = moduleCode == null || moduleCode.isBlank() ? null : moduleCode.trim();
        return auditLogRepository.findLatest(projectId, normalizedModule, limit);
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
