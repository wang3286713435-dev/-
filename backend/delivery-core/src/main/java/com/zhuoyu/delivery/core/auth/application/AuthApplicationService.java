package com.zhuoyu.delivery.core.auth.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.auth.domain.TokenClaims;
import com.zhuoyu.delivery.core.auth.dto.LoginRequest;
import com.zhuoyu.delivery.core.auth.dto.RefreshTokenRequest;
import com.zhuoyu.delivery.core.auth.dto.RegisterRequest;
import com.zhuoyu.delivery.core.auth.dto.RegisterResponse;
import com.zhuoyu.delivery.core.auth.dto.SessionTokenResponse;
import com.zhuoyu.delivery.core.auth.infrastructure.JwtTokenProvider;
import com.zhuoyu.delivery.core.project.application.ProjectAccessApplicationService;
import com.zhuoyu.delivery.core.user.repository.UserAccountRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private final UserAccountRepository userAccountRepository;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogApplicationService auditLogApplicationService;

    public AuthApplicationService(
        UserAccountRepository userAccountRepository,
        ProjectAccessApplicationService projectAccessApplicationService,
        JwtTokenProvider jwtTokenProvider,
        PasswordEncoder passwordEncoder,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        String username = normalizeOptionalUsername(request.username(), phoneNumber);
        userAccountRepository.findByUsername(username).ifPresent(existing -> {
            throw new BusinessException("CORE_AUTH_USERNAME_DUPLICATED", "用户名已存在", HttpStatus.CONFLICT);
        });
        userAccountRepository.findByUsername(phoneNumber).ifPresent(existing -> {
            throw new BusinessException("CORE_AUTH_PHONE_DUPLICATED", "手机号已注册", HttpStatus.CONFLICT);
        });
        userAccountRepository.findByPhoneNumber(phoneNumber).ifPresent(existing -> {
            throw new BusinessException("CORE_AUTH_PHONE_DUPLICATED", "手机号已注册", HttpStatus.CONFLICT);
        });
        String displayName = request.displayName().trim();
        String departmentName = request.departmentName() == null ? null : request.departmentName().trim();
        Long userId;
        try {
            userId = userAccountRepository.insertRegisteredEmployee(
                username,
                phoneNumber,
                passwordEncoder.encode(request.password()),
                displayName,
                departmentName
            );
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("CORE_AUTH_ACCOUNT_DUPLICATED", "用户名或手机号已存在", HttpStatus.CONFLICT);
        }
        auditLogApplicationService.record(
            null,
            "core.auth.register",
            "USER",
            String.valueOf(userId),
            userId,
            Map.of("username", username, "phoneNumber", phoneNumber, "projectAuthorized", false)
        );
        return new RegisterResponse(userId, username, phoneNumber, displayName, departmentName, "ACTIVE", false);
    }

    @Transactional
    public SessionTokenResponse login(LoginRequest request) {
        String loginName = normalizeLoginName(request.username());
        var user = userAccountRepository.findByUsernameOrPhoneNumber(loginName)
            .orElseThrow(() -> new BusinessException("CORE_AUTH_INVALID_CREDENTIAL", "用户名或密码错误", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BusinessException("CORE_AUTH_DISABLED", "当前账号已停用", HttpStatus.FORBIDDEN);
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new BusinessException("CORE_AUTH_INVALID_CREDENTIAL", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        List<com.zhuoyu.delivery.core.project.domain.AccessibleProject> projects =
            projectAccessApplicationService.listAccessibleProjects(user.id());
        Long currentProjectId = projects.isEmpty() ? null : projects.getFirst().id();
        SessionTokenResponse response = jwtTokenProvider.issueTokens(user.id(), user.username(), currentProjectId);
        userAccountRepository.updateLastLoginAt(user.id());
        auditLogApplicationService.record(
            currentProjectId,
            "core.auth.login",
            "USER",
            String.valueOf(user.id()),
            user.id(),
            Map.of("username", user.username())
        );
        return response;
    }

    @Transactional
    public SessionTokenResponse refresh(RefreshTokenRequest request) {
        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parseRefreshToken(request.refreshToken());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException("CORE_AUTH_INVALID_TOKEN", "refresh token 无效或已过期", HttpStatus.UNAUTHORIZED);
        }
        var user = userAccountRepository.findById(claims.userId())
            .orElseThrow(() -> new BusinessException("CORE_AUTH_UNAUTHORIZED", "当前用户不存在", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BusinessException("CORE_AUTH_DISABLED", "当前账号已停用", HttpStatus.FORBIDDEN);
        }
        List<com.zhuoyu.delivery.core.project.domain.AccessibleProject> projects =
            projectAccessApplicationService.listAccessibleProjects(user.id());
        Long claimProjectId = claims.currentProjectId();
        Long projectId;
        if (projects.isEmpty()) {
            projectId = null;
        } else if (claimProjectId == null) {
            projectId = projects.getFirst().id();
        } else {
            projectId = projects.stream()
                .filter(project -> project.id().equals(claimProjectId))
                .findFirst()
                .map(com.zhuoyu.delivery.core.project.domain.AccessibleProject::id)
                .orElse(projects.getFirst().id());
        }
        return jwtTokenProvider.issueTokens(user.id(), user.username(), projectId);
    }

    public SessionTokenResponse switchProject(Long userId, Long projectId) {
        var user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("CORE_AUTH_UNAUTHORIZED", "当前用户不存在", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BusinessException("CORE_AUTH_DISABLED", "当前账号已停用", HttpStatus.FORBIDDEN);
        }
        var project = projectAccessApplicationService.requireAccessibleProject(userId, projectId);
        SessionTokenResponse response = jwtTokenProvider.issueTokens(user.id(), user.username(), project.id());
        auditLogApplicationService.record(
            project.id(),
            "core.project.switch",
            "PROJECT",
            String.valueOf(project.id()),
            user.id(),
            Map.of("projectCode", project.code(), "projectName", project.name())
        );
        return response;
    }

    public void logout(Long userId, Long currentProjectId) {
        auditLogApplicationService.record(
            currentProjectId,
            "core.auth.logout",
            "USER",
            String.valueOf(userId),
            userId,
            Map.of()
        );
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber == null ? "" : phoneNumber.trim();
    }

    private String normalizeLoginName(String username) {
        return username == null ? "" : username.trim();
    }

    private String normalizeOptionalUsername(String username, String fallbackPhoneNumber) {
        if (username == null || username.isBlank()) {
            return fallbackPhoneNumber;
        }
        String normalized = username.trim();
        if (!normalized.matches("^[A-Za-z][A-Za-z0-9._-]{2,31}$")) {
            throw new BusinessException(
                "CORE_AUTH_USERNAME_INVALID",
                "用户名需以字母开头，支持字母、数字、点、下划线和短横线，长度 3-32 位",
                HttpStatus.BAD_REQUEST
            );
        }
        return normalized;
    }
}
