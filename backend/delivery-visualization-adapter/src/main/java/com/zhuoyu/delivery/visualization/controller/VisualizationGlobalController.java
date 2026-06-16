package com.zhuoyu.delivery.visualization.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.visualization.application.VisualizationAdapterApplicationService;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.GlandarReadyModelProjectResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visualization-adapter")
public class VisualizationGlobalController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final VisualizationAdapterApplicationService visualizationAdapterApplicationService;

    public VisualizationGlobalController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        VisualizationAdapterApplicationService visualizationAdapterApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.visualizationAdapterApplicationService = visualizationAdapterApplicationService;
    }

    @GetMapping("/glandar/ready-model-catalog")
    public ApiResponse<List<GlandarReadyModelProjectResponse>> glandarReadyModelCatalog() {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(visualizationAdapterApplicationService.glandarReadyModelCatalog(principal.userId()));
    }
}
