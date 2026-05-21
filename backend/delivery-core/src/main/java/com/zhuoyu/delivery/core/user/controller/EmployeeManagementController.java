package com.zhuoyu.delivery.core.user.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.user.application.EmployeeManagementApplicationService;
import com.zhuoyu.delivery.core.user.dto.AssignableProjectResponse;
import com.zhuoyu.delivery.core.user.dto.EmployeeDetailResponse;
import com.zhuoyu.delivery.core.user.dto.EmployeeProjectRoleUpdateRequest;
import com.zhuoyu.delivery.core.user.dto.EmployeeStatusUpdateRequest;
import com.zhuoyu.delivery.core.user.dto.EmployeeSummaryResponse;
import com.zhuoyu.delivery.core.user.dto.ProjectRoleOptionResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core")
public class EmployeeManagementController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final EmployeeManagementApplicationService employeeManagementApplicationService;

    public EmployeeManagementController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        EmployeeManagementApplicationService employeeManagementApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.employeeManagementApplicationService = employeeManagementApplicationService;
    }

    @GetMapping("/users")
    public ApiResponse<List<EmployeeSummaryResponse>> listEmployees(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(employeeManagementApplicationService.listEmployees(principal.userId(), keyword, status));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<EmployeeDetailResponse> getEmployee(@PathVariable Long userId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(employeeManagementApplicationService.getEmployee(principal.userId(), userId));
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<EmployeeDetailResponse> updateStatus(
        @PathVariable Long userId,
        @Valid @RequestBody EmployeeStatusUpdateRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(employeeManagementApplicationService.updateStatus(principal.userId(), userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteEmployee(@PathVariable Long userId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        employeeManagementApplicationService.deleteEmployee(principal.userId(), userId);
        return ApiResponse.success();
    }

    @PutMapping("/users/{userId}/project-roles")
    public ApiResponse<EmployeeDetailResponse> updateProjectRoles(
        @PathVariable Long userId,
        @Valid @RequestBody EmployeeProjectRoleUpdateRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(employeeManagementApplicationService.updateProjectRoles(principal.userId(), userId, request));
    }

    @GetMapping("/projects/assignable")
    public ApiResponse<List<AssignableProjectResponse>> listAssignableProjects() {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(employeeManagementApplicationService.listAssignableProjects(principal.userId()));
    }

    @GetMapping("/roles/project-assignable")
    public ApiResponse<List<ProjectRoleOptionResponse>> listProjectRoleOptions() {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(employeeManagementApplicationService.listProjectRoleOptions(principal.userId()));
    }
}
