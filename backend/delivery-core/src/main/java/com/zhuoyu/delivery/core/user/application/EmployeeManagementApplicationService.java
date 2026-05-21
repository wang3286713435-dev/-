package com.zhuoyu.delivery.core.user.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.rbac.application.PermissionApplicationService;
import com.zhuoyu.delivery.core.user.dto.AssignableProjectResponse;
import com.zhuoyu.delivery.core.user.dto.EmployeeDetailResponse;
import com.zhuoyu.delivery.core.user.dto.EmployeeProjectRoleItem;
import com.zhuoyu.delivery.core.user.dto.EmployeeProjectRoleUpdateRequest;
import com.zhuoyu.delivery.core.user.dto.EmployeeStatusUpdateRequest;
import com.zhuoyu.delivery.core.user.dto.EmployeeSummaryResponse;
import com.zhuoyu.delivery.core.user.dto.ProjectRoleOptionResponse;
import com.zhuoyu.delivery.core.user.repository.EmployeeManagementRepository;
import com.zhuoyu.delivery.core.user.repository.EmployeeManagementRepository.EmployeeBaseRecord;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeManagementApplicationService {

    private static final Set<String> MANAGEMENT_PERMISSIONS = Set.of("CORE_USER_MANAGE", "CORE_PROJECT_ROLE_MANAGE");
    private static final Set<String> ALLOWED_PROJECT_ROLES = Set.of("PROJECT_VIEWER", "DELIVERY_ENGINEER", "PROJECT_ADMIN");
    private static final Set<String> ALLOWED_USER_STATUS = Set.of("ACTIVE", "DISABLED");

    private final EmployeeManagementRepository employeeManagementRepository;
    private final PermissionApplicationService permissionApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public EmployeeManagementApplicationService(
        EmployeeManagementRepository employeeManagementRepository,
        PermissionApplicationService permissionApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.employeeManagementRepository = employeeManagementRepository;
        this.permissionApplicationService = permissionApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public List<EmployeeSummaryResponse> listEmployees(Long operatorId, String keyword, String status) {
        requireManagementPermission(operatorId);
        String normalizedStatus = normalizeStatusFilter(status);
        return employeeManagementRepository.searchEmployees(keyword, normalizedStatus);
    }

    public EmployeeDetailResponse getEmployee(Long operatorId, Long userId) {
        requireManagementPermission(operatorId);
        return getEmployeeDetail(userId);
    }

    @Transactional
    public EmployeeDetailResponse updateStatus(Long operatorId, Long userId, EmployeeStatusUpdateRequest request) {
        requireManagementPermission(operatorId);
        if (operatorId.equals(userId)) {
            throw new BusinessException("CORE_USER_SELF_DISABLE_FORBIDDEN", "不能停用自己的账号", HttpStatus.FORBIDDEN);
        }
        String status = normalizeRequired(request.status(), "账号状态不能为空");
        if (!ALLOWED_USER_STATUS.contains(status)) {
            throw new BusinessException("CORE_USER_STATUS_INVALID", "账号状态只能是 ACTIVE 或 DISABLED", HttpStatus.BAD_REQUEST);
        }
        EmployeeBaseRecord user = requireEmployee(userId);
        employeeManagementRepository.updateUserStatus(userId, status, operatorId);
        auditLogApplicationService.record(
            null,
            "core.user.status.update",
            "USER",
            String.valueOf(userId),
            operatorId,
            Map.of("username", user.username(), "fromStatus", user.status(), "toStatus", status)
        );
        return getEmployeeDetail(userId);
    }

    @Transactional
    public void deleteEmployee(Long operatorId, Long userId) {
        requireManagementPermission(operatorId);
        if (operatorId.equals(userId)) {
            throw new BusinessException("CORE_USER_SELF_DELETE_FORBIDDEN", "不能删除自己的账号", HttpStatus.FORBIDDEN);
        }
        EmployeeBaseRecord user = requireEmployee(userId);
        employeeManagementRepository.softDeleteUser(userId, operatorId);
        auditLogApplicationService.record(
            null,
            "core.user.delete",
            "USER",
            String.valueOf(userId),
            operatorId,
            Map.of("username", user.username(), "phoneNumber", user.phoneNumber() == null ? "" : user.phoneNumber())
        );
    }

    @Transactional
    public EmployeeDetailResponse updateProjectRoles(
        Long operatorId,
        Long userId,
        EmployeeProjectRoleUpdateRequest request
    ) {
        requireManagementPermission(operatorId);
        EmployeeBaseRecord user = requireEmployee(userId);
        List<EmployeeProjectRoleItem> assignments = request.assignments() == null
            ? List.of()
            : request.assignments().stream()
                .map(item -> new EmployeeProjectRoleItem(
                    item.projectId(),
                    normalizeRequired(item.roleCode(), "请选择项目角色")
                ))
                .toList();
        if (operatorId.equals(userId) && assignments.isEmpty()) {
            throw new BusinessException("CORE_PROJECT_ROLE_SELF_CLEAR_FORBIDDEN", "不能清空自己的全部项目权限", HttpStatus.FORBIDDEN);
        }
        validateAssignments(operatorId, assignments);
        Map<String, Long> roleIds = employeeManagementRepository.findRoleIds(ALLOWED_PROJECT_ROLES);
        if (!roleIds.keySet().containsAll(ALLOWED_PROJECT_ROLES)) {
            throw new BusinessException("CORE_PROJECT_ROLE_CONFIG_INVALID", "项目角色配置不完整", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        employeeManagementRepository.replaceProjectRoles(userId, assignments, roleIds, operatorId);
        auditLogApplicationService.record(
            null,
            "core.user.project_roles.update",
            "USER",
            String.valueOf(userId),
            operatorId,
            Map.of(
                "username", user.username(),
                "assignmentCount", assignments.size(),
                "projectIds", assignments.stream().map(EmployeeProjectRoleItem::projectId).toList()
            )
        );
        return getEmployeeDetail(userId);
    }

    public List<AssignableProjectResponse> listAssignableProjects(Long operatorId) {
        requireManagementPermission(operatorId);
        return employeeManagementRepository.findAssignableProjects(operatorId);
    }

    public List<ProjectRoleOptionResponse> listProjectRoleOptions(Long operatorId) {
        requireManagementPermission(operatorId);
        return List.of(
            new ProjectRoleOptionResponse("PROJECT_VIEWER", "查看者", "只读查看项目、目录和交付状态"),
            new ProjectRoleOptionResponse("DELIVERY_ENGINEER", "交付工程师", "可参与交付，并维护真实 NAS 文件资源目录"),
            new ProjectRoleOptionResponse("PROJECT_ADMIN", "项目管理员", "可管理项目数据、真实 NAS 资产和员工授权")
        );
    }

    private void requireManagementPermission(Long operatorId) {
        List<String> permissions = permissionApplicationService.listPermissionCodes(operatorId);
        boolean allowed = permissions.stream().anyMatch(MANAGEMENT_PERMISSIONS::contains);
        if (!allowed) {
            throw new BusinessException("CORE_USER_MANAGE_FORBIDDEN", "当前账号无权管理员工权限", HttpStatus.FORBIDDEN);
        }
    }

    private EmployeeDetailResponse getEmployeeDetail(Long userId) {
        EmployeeBaseRecord user = requireEmployee(userId);
        return new EmployeeDetailResponse(
            user.userId(),
            user.username(),
            user.phoneNumber(),
            user.displayName(),
            user.departmentName(),
            user.status(),
            user.lastLoginAt(),
            user.createdAt(),
            user.updatedAt(),
            employeeManagementRepository.findProjectRoleAssignments(userId)
        );
    }

    private EmployeeBaseRecord requireEmployee(Long userId) {
        return employeeManagementRepository.findEmployeeBase(userId)
            .orElseThrow(() -> new BusinessException("CORE_USER_NOT_FOUND", "员工账号不存在", HttpStatus.NOT_FOUND));
    }

    private void validateAssignments(Long operatorId, List<EmployeeProjectRoleItem> assignments) {
        Set<Long> assignableProjectIds = new LinkedHashSet<>(employeeManagementRepository.findAssignableProjectIds(operatorId));
        Set<Long> seenProjectIds = new LinkedHashSet<>();
        for (EmployeeProjectRoleItem item : assignments) {
            if (item.projectId() == null) {
                throw new BusinessException("CORE_PROJECT_ROLE_PROJECT_REQUIRED", "请选择授权项目", HttpStatus.BAD_REQUEST);
            }
            String roleCode = normalizeRequired(item.roleCode(), "请选择项目角色");
            if (!ALLOWED_PROJECT_ROLES.contains(roleCode)) {
                throw new BusinessException("CORE_PROJECT_ROLE_INVALID", "项目角色不允许授权", HttpStatus.BAD_REQUEST);
            }
            if (!assignableProjectIds.contains(item.projectId())) {
                throw new BusinessException("CORE_PROJECT_ROLE_ASSIGN_FORBIDDEN", "不能授权自己无项目管理员权限的项目", HttpStatus.FORBIDDEN);
            }
            if (!seenProjectIds.add(item.projectId())) {
                throw new BusinessException("CORE_PROJECT_ROLE_DUPLICATED", "同一项目只能选择一个角色", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_USER_STATUS.contains(normalized)) {
            throw new BusinessException("CORE_USER_STATUS_INVALID", "账号状态只能是 ACTIVE 或 DISABLED", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("CORE_REQUEST_INVALID", message, HttpStatus.BAD_REQUEST);
        }
        return value.trim().toUpperCase();
    }
}
