package com.zhuoyu.delivery.datasteward.storage;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageProviderHealthResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/storage")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/providers/health")
    public ApiResponse<List<StorageProviderHealthResponse>> providerHealth() {
        return ApiResponse.success(storageService.providerHealth());
    }
}
