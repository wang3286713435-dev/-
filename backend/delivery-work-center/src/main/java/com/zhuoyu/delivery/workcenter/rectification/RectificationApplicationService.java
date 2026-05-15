package com.zhuoyu.delivery.workcenter.rectification;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import com.zhuoyu.delivery.workcenter.delivery.DeliveryBindingRepository;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.RectificationRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.RectificationResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ResolveRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RectificationApplicationService {

    private static final String MODULE_CODE = "work-center";

    private final DeliveryBindingRepository deliveryBindingRepository;
    private final AuditLogApplicationService auditLogApplicationService;

    public RectificationApplicationService(
        DeliveryBindingRepository deliveryBindingRepository,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.deliveryBindingRepository = deliveryBindingRepository;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public List<RectificationResponse> list(Long projectId, String status) {
        return deliveryBindingRepository.findRectificationsByProject(projectId, status);
    }

    public RectificationResponse detail(Long projectId, Long rectificationId) {
        return requireRectification(projectId, rectificationId);
    }

    @Transactional
    public RectificationResponse update(Long userId, Long projectId, Long rectificationId, RectificationRequest request) {
        requireRectification(projectId, rectificationId);
        deliveryBindingRepository.updateRectification(projectId, rectificationId,
            request.title(), request.description(), request.severity(),
            request.assigneeUserId(), request.dueDate(), userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.rectification.update", "RECTIFICATION",
            String.valueOf(rectificationId), userId, Map.of());
        return requireRectification(projectId, rectificationId);
    }

    @Transactional
    public RectificationResponse resolve(Long userId, Long projectId, Long rectificationId, ResolveRequest request) {
        var rect = requireRectification(projectId, rectificationId);
        String currentStatus = rect.status();
        if (!List.of("OPEN", "IN_PROGRESS", "REOPENED").contains(currentStatus)) {
            throw new BusinessException("WORK_RECTIFICATION_INVALID_STATUS",
                "当前整改状态不允许标记已处理: " + currentStatus, HttpStatus.CONFLICT);
        }
        deliveryBindingRepository.updateRectificationStatus(projectId, rectificationId, "RESOLVED", request.resolutionNote(), userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.rectification.resolve", "RECTIFICATION",
            String.valueOf(rectificationId), userId, Map.of("from", currentStatus, "to", "RESOLVED"));
        return requireRectification(projectId, rectificationId);
    }

    @Transactional
    public RectificationResponse close(Long userId, Long projectId, Long rectificationId) {
        var rect = requireRectification(projectId, rectificationId);
        String currentStatus = rect.status();
        if (!"RESOLVED".equals(currentStatus)) {
            throw new BusinessException("WORK_RECTIFICATION_INVALID_STATUS",
                "只有已处理状态的整改项才能关闭: " + currentStatus, HttpStatus.CONFLICT);
        }
        deliveryBindingRepository.updateRectificationStatus(projectId, rectificationId, "CLOSED", null, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.rectification.close", "RECTIFICATION",
            String.valueOf(rectificationId), userId, Map.of("from", currentStatus, "to", "CLOSED"));
        return requireRectification(projectId, rectificationId);
    }

    @Transactional
    public RectificationResponse reopen(Long userId, Long projectId, Long rectificationId) {
        var rect = requireRectification(projectId, rectificationId);
        String currentStatus = rect.status();
        if (!"CLOSED".equals(currentStatus)) {
            throw new BusinessException("WORK_RECTIFICATION_INVALID_STATUS",
                "只有已关闭状态的整改项才能重新打开: " + currentStatus, HttpStatus.CONFLICT);
        }
        deliveryBindingRepository.updateRectificationStatus(projectId, rectificationId, "REOPENED", null, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.rectification.reopen", "RECTIFICATION",
            String.valueOf(rectificationId), userId, Map.of("from", currentStatus, "to", "REOPENED"));
        return requireRectification(projectId, rectificationId);
    }

    private RectificationResponse requireRectification(Long projectId, Long rectificationId) {
        return deliveryBindingRepository.findRectificationById(projectId, rectificationId)
            .orElseThrow(() -> new BusinessException("WORK_RECTIFICATION_NOT_FOUND", "整改项不存在", HttpStatus.NOT_FOUND));
    }
}
