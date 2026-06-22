package com.zhuoyu.delivery.visualization.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.visualization.application.VisualizationAdapterApplicationService;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.GlandarReadyModelProjectResponse;
import com.zhuoyu.delivery.visualization.engine.GlandarStationClient;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visualization-adapter")
public class VisualizationGlobalController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final VisualizationAdapterApplicationService visualizationAdapterApplicationService;
    private final GlandarStationClient glandarStationClient;

    public VisualizationGlobalController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        VisualizationAdapterApplicationService visualizationAdapterApplicationService,
        GlandarStationClient glandarStationClient
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.visualizationAdapterApplicationService = visualizationAdapterApplicationService;
        this.glandarStationClient = glandarStationClient;
    }

    @GetMapping("/glandar/ready-model-catalog")
    public ApiResponse<List<GlandarReadyModelProjectResponse>> glandarReadyModelCatalog() {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(visualizationAdapterApplicationService.glandarReadyModelCatalog(principal.userId()));
    }

    @GetMapping("/glandar/static/**")
    public ResponseEntity<byte[]> glandarStaticAsset(HttpServletRequest request) {
        String prefix = request.getContextPath() + "/api/visualization-adapter/glandar/static/";
        String requestUri = request.getRequestURI();
        String relativePath = requestUri.startsWith(prefix) ? requestUri.substring(prefix.length()) : "";
        var asset = glandarStationClient.fetchStaticAsset(relativePath);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(asset.contentType()))
            .cacheControl(CacheControl.noCache())
            .header("X-Content-Type-Options", "nosniff")
            .body(asset.body());
    }
}
