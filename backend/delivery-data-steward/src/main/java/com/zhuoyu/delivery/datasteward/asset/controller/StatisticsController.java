package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.application.StatisticsApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityStatisticsResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/assets/statistics")
public class StatisticsController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final StatisticsApplicationService statisticsApplicationService;

    public StatisticsController(SecurityPrincipalAccessor securityPrincipalAccessor,
                                 StatisticsApplicationService statisticsApplicationService) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.statisticsApplicationService = statisticsApplicationService;
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }

    @GetMapping
    public ApiResponse<CapacityStatisticsResponse> getStatistics(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String assetSource) {
        return ApiResponse.success(
            statisticsApplicationService.getStatistics(currentUserId(), projectId, assetSource));
    }
}
