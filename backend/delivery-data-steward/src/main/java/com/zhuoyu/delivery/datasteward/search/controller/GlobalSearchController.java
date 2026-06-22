package com.zhuoyu.delivery.datasteward.search.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.search.application.GlobalSearchApplicationService;
import com.zhuoyu.delivery.datasteward.search.dto.GlobalSearchDtos.GlobalSearchResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/search")
public class GlobalSearchController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final GlobalSearchApplicationService globalSearchApplicationService;

    public GlobalSearchController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        GlobalSearchApplicationService globalSearchApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.globalSearchApplicationService = globalSearchApplicationService;
    }

    @GetMapping("/global")
    public ApiResponse<GlobalSearchResponse> globalSearch(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) Integer limit
    ) {
        Long userId = securityPrincipalAccessor.requireCurrentPrincipal().userId();
        return ApiResponse.success(globalSearchApplicationService.search(userId, keyword, projectId, limit));
    }
}
