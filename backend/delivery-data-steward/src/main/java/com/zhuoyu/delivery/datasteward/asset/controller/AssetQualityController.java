package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.application.AssetQualityApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetQualityOverviewResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/assets/quality")
public class AssetQualityController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final AssetQualityApplicationService assetQualityApplicationService;

    public AssetQualityController(SecurityPrincipalAccessor securityPrincipalAccessor,
                                  AssetQualityApplicationService assetQualityApplicationService) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.assetQualityApplicationService = assetQualityApplicationService;
    }

    @GetMapping("/overview")
    public ApiResponse<AssetQualityOverviewResponse> getOverview(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) String assetSource
    ) {
        Long userId = securityPrincipalAccessor.requireCurrentPrincipal().userId();
        return ApiResponse.success(assetQualityApplicationService.getOverview(userId, projectId, assetSource));
    }
}
