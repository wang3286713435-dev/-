package com.zhuoyu.delivery.workcenter.delivery;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DashboardSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryViewResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryApplicationService {

    private static final String MODULE_CODE = "work-center";

    private final DeliveryBindingRepository deliveryBindingRepository;
    private final AuditLogApplicationService auditLogApplicationService;

    public DeliveryApplicationService(
        DeliveryBindingRepository deliveryBindingRepository,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.deliveryBindingRepository = deliveryBindingRepository;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public DeliveryBindingResponse createBinding(Long userId, Long projectId, DeliveryBindingRequest request) {
        String viewType = normalizeViewType(request.viewType());
        String expectedFileKind = expectedFileKind(viewType);
        if (!deliveryBindingRepository.deliverableTypeExists(projectId, request.deliverableTypeId(), expectedFileKind)) {
            throw new BusinessException("WORK_DELIVERABLE_TYPE_NOT_FOUND", "交付物类型不存在或文件类型不匹配", HttpStatus.NOT_FOUND);
        }
        if (!deliveryBindingRepository.fileExists(projectId, request.fileResourceId(), expectedFileKind)) {
            throw new BusinessException("WORK_FILE_NOT_READY", "文件不存在、类型不匹配或尚未处理完成", HttpStatus.PRECONDITION_FAILED);
        }
        requireSection(projectId, request.sectionNodeId());
        requireObject(projectId, request.managedObjectId());
        if (request.sectionNodeId() == null && request.managedObjectId() == null) {
            throw new BusinessException("WORK_BINDING_TARGET_REQUIRED", "交付绑定必须选择部位或管理对象", HttpStatus.BAD_REQUEST);
        }
        Long bindingId = deliveryBindingRepository.insert(
            projectId,
            viewType,
            request.sectionNodeId(),
            request.managedObjectId(),
            request.deliverableTypeId(),
            request.fileResourceId(),
            defaultString(request.bindingStatus(), "BOUND"),
            defaultString(request.reviewStatus(), "PENDING"),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            blankToNull(request.remark()),
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.delivery-binding.create", "DELIVERY_BINDING",
            String.valueOf(bindingId), userId, Map.of("viewType", viewType));
        return requireBinding(projectId, bindingId);
    }

    public DeliveryViewResponse deliveryView(Long projectId, String viewType) {
        String normalized = normalizeViewType(viewType);
        List<DeliveryBindingResponse> rows = deliveryBindingRepository.findByProjectAndViewType(projectId, normalized);
        int boundCount = (int) rows.stream().filter(row -> "BOUND".equals(row.bindingStatus())).count();
        return new DeliveryViewResponse(projectId, normalized, rows.size(), boundCount, rows);
    }

    public DashboardSummaryResponse dashboardSummary(Long projectId) {
        return deliveryBindingRepository.dashboardSummary(projectId);
    }

    @Transactional
    public void deleteBinding(Long userId, Long projectId, Long bindingId) {
        requireBinding(projectId, bindingId);
        deliveryBindingRepository.markDeleted(projectId, bindingId, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.delivery-binding.delete", "DELIVERY_BINDING",
            String.valueOf(bindingId), userId, Map.of());
    }

    private DeliveryBindingResponse requireBinding(Long projectId, Long bindingId) {
        return deliveryBindingRepository.findByProjectAndId(projectId, bindingId)
            .orElseThrow(() -> new BusinessException("WORK_BINDING_NOT_FOUND", "交付绑定不存在", HttpStatus.NOT_FOUND));
    }

    private void requireSection(Long projectId, Long sectionNodeId) {
        if (sectionNodeId != null && !deliveryBindingRepository.sectionExists(projectId, sectionNodeId)) {
            throw new BusinessException("MASTERDATA_SECTION_NOT_FOUND", "工程管理部位不存在", HttpStatus.NOT_FOUND);
        }
    }

    private void requireObject(Long projectId, Long managedObjectId) {
        if (managedObjectId != null && !deliveryBindingRepository.objectExists(projectId, managedObjectId)) {
            throw new BusinessException("DATA_OBJECT_NOT_FOUND", "管理对象不存在", HttpStatus.NOT_FOUND);
        }
    }

    private String normalizeViewType(String viewType) {
        String normalized = requireText(viewType, "WORK_VIEW_TYPE_REQUIRED", "交付视图类型不能为空").toUpperCase();
        if (!List.of("DOCUMENT", "DRAWING").contains(normalized)) {
            throw new BusinessException("WORK_VIEW_TYPE_INVALID", "交付视图类型只能是 DOCUMENT 或 DRAWING", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String expectedFileKind(String viewType) {
        return "DRAWING".equals(viewType) ? "DRAWING" : "DOCUMENT";
    }

    private String requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
