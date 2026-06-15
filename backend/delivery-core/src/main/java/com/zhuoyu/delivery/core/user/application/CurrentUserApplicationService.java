package com.zhuoyu.delivery.core.user.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.project.application.ProjectAccessApplicationService;
import com.zhuoyu.delivery.core.project.domain.AccessibleProject;
import com.zhuoyu.delivery.core.project.dto.ProjectSummaryResponse;
import com.zhuoyu.delivery.core.rbac.application.PermissionApplicationService;
import com.zhuoyu.delivery.core.user.dto.CurrentUserResponse;
import com.zhuoyu.delivery.core.user.dto.MenuItemResponse;
import com.zhuoyu.delivery.core.user.dto.UserPasswordChangeRequest;
import com.zhuoyu.delivery.core.user.dto.UserProfileUpdateRequest;
import com.zhuoyu.delivery.core.user.repository.UserAccountRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserApplicationService {

    private static final String SUPER_ADMIN_USERNAME = "admin";
    private static final Set<String> MANAGEMENT_PERMISSIONS = Set.of("CORE_USER_MANAGE", "CORE_PROJECT_ROLE_MANAGE");

    private final UserAccountRepository userAccountRepository;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final PermissionApplicationService permissionApplicationService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogApplicationService auditLogApplicationService;

    public CurrentUserApplicationService(
        UserAccountRepository userAccountRepository,
        ProjectAccessApplicationService projectAccessApplicationService,
        PermissionApplicationService permissionApplicationService,
        PasswordEncoder passwordEncoder,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.permissionApplicationService = permissionApplicationService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public CurrentUserResponse getCurrentUser(Long userId, Long currentProjectId) {
        var user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("CORE_AUTH_UNAUTHORIZED", "当前用户不存在", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BusinessException("CORE_AUTH_DISABLED", "当前账号已停用", HttpStatus.FORBIDDEN);
        }
        List<AccessibleProject> projects = projectAccessApplicationService.listAccessibleProjects(userId);
        if (projects.isEmpty()) {
            return new CurrentUserResponse(
                user.id(),
                user.username(),
                user.displayName(),
                user.phoneNumber(),
                user.departmentName(),
                user.lastLoginAt(),
                null,
                List.of(),
                List.of(),
                List.of()
            );
        }
        AccessibleProject currentProject = currentProjectId == null
            ? projects.getFirst()
            : projects.stream()
                .filter(project -> project.id().equals(currentProjectId))
                .findFirst()
                .orElse(projects.getFirst());
        List<String> permissions = normalizeManagementPermissions(
            permissionApplicationService.listPermissionCodes(userId, currentProject.id()),
            isSuperAdmin(user.username())
        );
        List<ProjectSummaryResponse> responses = projects.stream()
            .map(projectAccessApplicationService::toResponse)
            .toList();
        return new CurrentUserResponse(
            user.id(),
            user.username(),
            user.displayName(),
            user.phoneNumber(),
            user.departmentName(),
            user.lastLoginAt(),
            projectAccessApplicationService.toResponse(currentProject),
            responses,
            permissions,
            buildMenus(permissions)
        );
    }

    @Transactional
    public CurrentUserResponse updateProfile(Long userId, Long currentProjectId, UserProfileUpdateRequest request) {
        var user = requireActiveUser(userId);
        String displayName = normalizeRequired(request.displayName(), "请输入姓名");
        String departmentName = request.departmentName() == null ? null : request.departmentName().trim();
        userAccountRepository.updateProfile(user.id(), displayName, departmentName);
        auditLogApplicationService.record(
            currentProjectId,
            "core.user.profile.update",
            "USER",
            String.valueOf(user.id()),
            user.id(),
            Map.of("username", user.username())
        );
        return getCurrentUser(user.id(), currentProjectId);
    }

    @Transactional
    public void changePassword(Long userId, Long currentProjectId, UserPasswordChangeRequest request) {
        var user = requireActiveUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new BusinessException("CORE_USER_PASSWORD_INVALID", "当前密码不正确", HttpStatus.BAD_REQUEST);
        }
        String newPassword = normalizeRequired(request.newPassword(), "请输入新密码");
        if (newPassword.length() < 6 || newPassword.length() > 64) {
            throw new BusinessException("CORE_USER_PASSWORD_INVALID", "新密码长度应为 6-64 位", HttpStatus.BAD_REQUEST);
        }
        userAccountRepository.updatePasswordHash(user.id(), passwordEncoder.encode(newPassword));
        auditLogApplicationService.record(
            currentProjectId,
            "core.user.password.update",
            "USER",
            String.valueOf(user.id()),
            user.id(),
            Map.of("username", user.username())
        );
    }

    private com.zhuoyu.delivery.core.user.domain.UserAccount requireActiveUser(Long userId) {
        var user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("CORE_AUTH_UNAUTHORIZED", "当前用户不存在", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BusinessException("CORE_AUTH_DISABLED", "当前账号已停用", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("CORE_REQUEST_INVALID", message, HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private boolean isSuperAdmin(String username) {
        return SUPER_ADMIN_USERNAME.equalsIgnoreCase(username);
    }

    private List<String> normalizeManagementPermissions(List<String> permissions, boolean superAdmin) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String permission : permissions) {
            if (superAdmin || !MANAGEMENT_PERMISSIONS.contains(permission)) {
                normalized.add(permission);
            }
        }
        if (superAdmin) {
            normalized.addAll(MANAGEMENT_PERMISSIONS);
        }
        return List.copyOf(normalized);
    }

    private List<MenuItemResponse> buildMenus(List<String> permissions) {
        List<MenuItemResponse> menus = new ArrayList<>();
        if (
            permissions.contains("DATA_STEWARD_ASSET_READ")
                || permissions.contains("DATA_STEWARD_FILE_READ")
                || permissions.contains("DATA_STEWARD_MODEL_READ")
                || permissions.contains("DATA_STEWARD_OBJECT_READ")
        ) {
            List<MenuItemResponse> dataStewardChildren = new ArrayList<>();
            if (permissions.contains("DATA_STEWARD_ASSET_READ")) {
                dataStewardChildren.add(
                    new MenuItemResponse("data-steward-assets", "资产总览", "/data-steward/assets", "DataBoard")
                );
                dataStewardChildren.add(
                    new MenuItemResponse("data-steward-scans", "扫描任务", "/data-steward/scans", "Search")
                );
                dataStewardChildren.add(
                    new MenuItemResponse("data-steward-quality", "数据质量", "/data-steward/quality", "Warning")
                );
                dataStewardChildren.add(
                    new MenuItemResponse(
                        "data-steward-nonstandard-directories",
                        "非标准资料治理",
                        "/data-steward/nonstandard-directories",
                        "Warning"
                    )
                );
                dataStewardChildren.add(
                    new MenuItemResponse("data-steward-catalog", "资产目录", "/data-steward/catalog", "FolderOpened")
                );
                dataStewardChildren.add(
                    new MenuItemResponse("data-steward-agent-preview", "Agent预览", "/data-steward/agent-preview", "Monitor")
                );
            }
            if (permissions.contains("DATA_STEWARD_FILE_READ")) {
                dataStewardChildren.add(
                    new MenuItemResponse("data-steward-files", "文件资源", "/data-steward/files", "FolderOpened")
                );
            }
            if (permissions.contains("DATA_STEWARD_MODEL_READ")) {
                dataStewardChildren.add(
                    new MenuItemResponse("data-steward-models", "模型集成", "/data-steward/models", "Box")
                );
            }
            if (permissions.contains("DATA_STEWARD_OBJECT_READ")) {
                dataStewardChildren.add(
                    new MenuItemResponse("data-steward-objects", "管理对象", "/data-steward/objects", "Connection")
                );
            }
            String dataStewardPath = permissions.contains("DATA_STEWARD_ASSET_READ") ? "/data-steward/assets" : "/data-steward/files";
            menus.add(new MenuItemResponse("data-steward", "数据管家", dataStewardPath, "FolderOpened", dataStewardChildren));
        }
        if (
            permissions.contains("DATA_STEWARD_ASSET_READ")
                || permissions.contains("DATA_STEWARD_MODEL_READ")
                || permissions.contains("WORKCENTER_DASHBOARD_VIEW")
                || permissions.contains("VISUALIZATION_WORKBENCH_VIEW")
        ) {
            menus.add(new MenuItemResponse("digital-twin", "BIM协同管理", "/bim-collaboration", "Monitor"));
        }
        if (permissions.contains("WORKCENTER_HOME_VIEW")) {
            menus.add(new MenuItemResponse("home", "首页", "/home", "House"));
        }
        if (
            permissions.contains("MASTERDATA_SECTION_READ")
                || permissions.contains("MASTERDATA_NODE_TYPE_READ")
                || permissions.contains("MASTERDATA_DELIVERABLE_READ")
        ) {
            List<MenuItemResponse> masterDataChildren = new ArrayList<>();
            if (permissions.contains("MASTERDATA_SECTION_READ")) {
                masterDataChildren.add(
                    new MenuItemResponse("master-data-sections", "工程管理部位", "/master-data/sections", "OfficeBuilding")
                );
            }
            if (permissions.contains("MASTERDATA_NODE_TYPE_READ")) {
                masterDataChildren.add(
                    new MenuItemResponse("master-data-node-types", "节点类型", "/master-data/node-types", "Tickets")
                );
            }
            if (permissions.contains("MASTERDATA_DELIVERABLE_READ")) {
                masterDataChildren.add(
                    new MenuItemResponse("master-data-deliverable-standard", "交付物标准", "/master-data/deliverable-standard", "Files")
                );
            }
            menus.add(new MenuItemResponse("master-data", "工程主数据", "/master-data/sections", "Files", masterDataChildren));
        }
        if (
            permissions.contains("WORKCENTER_DELIVERY_READ")
                || permissions.contains("WORKCENTER_DASHBOARD_VIEW")
                || permissions.contains("VISUALIZATION_WORKBENCH_VIEW")
                || permissions.contains("WORKCENTER_RECTIFICATION_READ")
        ) {
            List<MenuItemResponse> workChildren = new ArrayList<>();
            if (permissions.contains("WORKCENTER_DELIVERY_READ")) {
                workChildren.add(new MenuItemResponse("work-document-delivery", "文档交付", "/work/document-delivery", "Document"));
                workChildren.add(new MenuItemResponse("work-drawing-delivery", "图纸交付", "/work/drawing-delivery", "Picture"));
            }
            if (permissions.contains("WORKCENTER_RECTIFICATION_READ")) {
                workChildren.add(new MenuItemResponse("work-rectifications", "整改中心", "/work/rectifications", "Warning"));
            }
            if (permissions.contains("WORKCENTER_DASHBOARD_VIEW")) {
                workChildren.add(new MenuItemResponse("work-dashboard", "BIM协同大屏", "/work/dashboard", "DataBoard"));
            }
            if (permissions.contains("VISUALIZATION_WORKBENCH_VIEW")) {
                workChildren.add(new MenuItemResponse("visualization-workbench", "BIM协同工作台", "/visualization/workbench", "Monitor"));
            }
            menus.add(new MenuItemResponse("work-center", "工作中心", "/work/document-delivery", "DataBoard", workChildren));
        }
        if (permissions.contains("CORE_USER_MANAGE") || permissions.contains("CORE_PROJECT_ROLE_MANAGE")) {
            menus.add(new MenuItemResponse("admin", "管理中心", "/admin/employees", "User", List.of(
                new MenuItemResponse("admin-employees", "员工权限", "/admin/employees", "User")
            )));
        }
        return menus;
    }
}
