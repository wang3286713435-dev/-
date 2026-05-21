package com.zhuoyu.delivery.core.auth.application;

import com.zhuoyu.delivery.core.auth.domain.AuthenticatedPrincipal;
import com.zhuoyu.delivery.core.user.repository.UserAccountRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityPrincipalAccessor {

    private final UserAccountRepository userAccountRepository;

    public SecurityPrincipalAccessor(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public AuthenticatedPrincipal requireCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            throw new BusinessException("CORE_AUTH_UNAUTHORIZED", "请先登录", HttpStatus.UNAUTHORIZED);
        }
        var user = userAccountRepository.findById(principal.userId())
            .orElseThrow(() -> new BusinessException("CORE_AUTH_UNAUTHORIZED", "当前用户不存在", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BusinessException("CORE_AUTH_DISABLED", "当前账号已停用", HttpStatus.FORBIDDEN);
        }
        return principal;
    }
}
