package com.zhuoyu.delivery.core.user.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.user.dto.AssignableProjectResponse;
import com.zhuoyu.delivery.core.user.dto.EmployeeCreateRequest;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeManagementApplicationService {

    private static final String SUPER_ADMIN_USERNAME = "admin";
    private static final Set<String> ALLOWED_PROJECT_ROLES = Set.of("PROJECT_VIEWER", "DELIVERY_ENGINEER", "PROJECT_ADMIN");
    private static final Set<String> ALLOWED_USER_STATUS = Set.of("ACTIVE", "DISABLED");

    private final EmployeeManagementRepository employeeManagementRepository;
    private final AuditLogApplicationService auditLogApplicationService;
    private final PasswordEncoder passwordEncoder;

    public EmployeeManagementApplicationService(
        EmployeeManagementRepository employeeManagementRepository,
        AuditLogApplicationService auditLogApplicationService,
        PasswordEncoder passwordEncoder
    ) {
        this.employeeManagementRepository = employeeManagementRepository;
        this.auditLogApplicationService = auditLogApplicationService;
        this.passwordEncoder = passwordEncoder;
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
    public EmployeeDetailResponse createEmployee(Long operatorId, EmployeeCreateRequest request) {
        requireManagementPermission(operatorId);
        String username = normalizeUsername(request.username());
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        String displayName = request.displayName().trim();
        String departmentName = request.departmentName() == null ? null : request.departmentName().trim();
        Long userId;
        try {
            userId = employeeManagementRepository.insertEmployee(
                username,
                phoneNumber,
                passwordEncoder.encode(request.password()),
                displayName,
                departmentName,
                operatorId
            );
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("CORE_AUTH_ACCOUNT_DUPLICATED", "用户名或手机号已存在", HttpStatus.CONFLICT);
        }
        auditLogApplicationService.record(
            null,
            "core.user.create",
            "USER",
            String.valueOf(userId),
            operatorId,
            Map.of("username", username, "phoneNumber", phoneNumber, "projectAuthorized", false)
        );
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
        return employeeManagementRepository.findAllActiveProjects();
    }

    public List<ProjectRoleOptionResponse> listProjectRoleOptions(Long operatorId) {
        requireManagementPermission(operatorId);
        return List.of(
            new ProjectRoleOptionResponse("PROJECT_VIEWER", "查看者", "只读查看项目、目录和交付状态"),
            new ProjectRoleOptionResponse("DELIVERY_ENGINEER", "交付工程师", "可参与交付，并维护真实 NAS 文件资源目录"),
            new ProjectRoleOptionResponse("PROJECT_ADMIN", "项目管理员", "可管理项目数据和真实 NAS 资产，不包含员工账号权限")
        );
    }

    private void requireManagementPermission(Long operatorId) {
        EmployeeBaseRecord operator = requireEmployee(operatorId);
        if (!SUPER_ADMIN_USERNAME.equalsIgnoreCase(operator.username())) {
            throw new BusinessException("CORE_USER_MANAGE_FORBIDDEN", "仅超级管理员可管理员工账号和项目权限", HttpStatus.FORBIDDEN);
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
        Set<Long> assignableProjectIds = new LinkedHashSet<>(employeeManagementRepository.findAllActiveProjectIds());
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
                throw new BusinessException("CORE_PROJECT_ROLE_ASSIGN_FORBIDDEN", "只能授权当前可用项目", HttpStatus.FORBIDDEN);
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

    private String normalizePhoneNumber(String value) {
        String phoneNumber = normalizeRequired(value, "请输入手机号");
        if (!phoneNumber.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("CORE_AUTH_PHONE_INVALID", "请输入有效的手机号", HttpStatus.BAD_REQUEST);
        }
        return phoneNumber;
    }

    private String normalizeUsername(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("CORE_REQUEST_INVALID", "请输入用户名", HttpStatus.BAD_REQUEST);
        }
        String username = value.trim();
        if (!username.matches("^[A-Za-z][A-Za-z0-9._-]{2,31}$")) {
            throw new BusinessException(
                "CORE_AUTH_USERNAME_INVALID",
                "用户名需以字母开头，支持字母、数字、点、下划线和短横线，长度 3-32 位",
                HttpStatus.BAD_REQUEST
            );
        }
        return username;
    }
}
