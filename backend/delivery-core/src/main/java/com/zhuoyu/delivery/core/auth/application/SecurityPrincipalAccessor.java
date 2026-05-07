package com.zhuoyu.delivery.core.auth.application;

import com.zhuoyu.delivery.core.auth.domain.AuthenticatedPrincipal;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityPrincipalAccessor {

    public AuthenticatedPrincipal requireCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            throw new BusinessException("CORE_AUTH_UNAUTHORIZED", "请先登录", HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }
}
