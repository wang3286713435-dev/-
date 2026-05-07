package com.zhuoyu.delivery.core.auth.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.auth.domain.TokenClaims;
import com.zhuoyu.delivery.core.auth.dto.LoginRequest;
import com.zhuoyu.delivery.core.auth.dto.RefreshTokenRequest;
import com.zhuoyu.delivery.core.auth.dto.SessionTokenResponse;
import com.zhuoyu.delivery.core.auth.infrastructure.JwtTokenProvider;
import com.zhuoyu.delivery.core.project.application.ProjectAccessApplicationService;
import com.zhuoyu.delivery.core.user.repository.UserAccountRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public SessionTokenResponse login(LoginRequest request) {
        var user = userAccountRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException("CORE_AUTH_INVALID_CREDENTIAL", "用户名或密码错误", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BusinessException("CORE_AUTH_DISABLED", "当前账号已停用", HttpStatus.FORBIDDEN);
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new BusinessException("CORE_AUTH_INVALID_CREDENTIAL", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        List<com.zhuoyu.delivery.core.project.domain.AccessibleProject> projects =
            projectAccessApplicationService.listAccessibleProjects(user.id());
        if (projects.isEmpty()) {
            throw new BusinessException("CORE_PROJECT_NOT_FOUND", "当前账号未绑定任何项目", HttpStatus.FORBIDDEN);
        }
        Long currentProjectId = projects.getFirst().id();
        SessionTokenResponse response = jwtTokenProvider.issueTokens(user.id(), user.username(), currentProjectId);
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

    public SessionTokenResponse refresh(RefreshTokenRequest request) {
        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parseRefreshToken(request.refreshToken());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException("CORE_AUTH_INVALID_TOKEN", "refresh token 无效或已过期", HttpStatus.UNAUTHORIZED);
        }
        var user = userAccountRepository.findById(claims.userId())
            .orElseThrow(() -> new BusinessException("CORE_AUTH_UNAUTHORIZED", "当前用户不存在", HttpStatus.UNAUTHORIZED));
        Long projectId = claims.currentProjectId();
        if (projectId != null) {
            projectAccessApplicationService.requireAccessibleProject(user.id(), projectId);
        }
        return jwtTokenProvider.issueTokens(user.id(), user.username(), projectId);
    }

    public SessionTokenResponse switchProject(Long userId, Long projectId) {
        var user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("CORE_AUTH_UNAUTHORIZED", "当前用户不存在", HttpStatus.UNAUTHORIZED));
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
}
