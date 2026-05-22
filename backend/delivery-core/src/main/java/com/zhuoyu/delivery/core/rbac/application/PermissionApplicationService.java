package com.zhuoyu.delivery.core.rbac.application;

import com.zhuoyu.delivery.core.rbac.repository.PermissionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PermissionApplicationService {

    private final PermissionRepository permissionRepository;

    public PermissionApplicationService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public List<String> listPermissionCodes(Long userId, Long projectId) {
        return permissionRepository.findByUserAndProject(userId, projectId).stream()
            .map(permission -> permission.code())
            .toList();
    }

    public List<String> listPermissionCodes(Long userId) {
        return permissionRepository.findByUser(userId).stream()
            .map(permission -> permission.code())
            .toList();
    }
}
