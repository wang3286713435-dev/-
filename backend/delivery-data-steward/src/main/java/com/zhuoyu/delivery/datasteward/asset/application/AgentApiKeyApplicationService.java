package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentApiKeyCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentApiKeyCreateResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentApiKeyResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AgentApiKeyRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentApiKeyApplicationService {

    private static final String KEY_PREFIX = "dpk_";

    private final AgentApiKeyRepository apiKeyRepository;
    private final BimAssetRepository bimAssetRepository;
    private final EventApplicationService eventApplicationService;

    public AgentApiKeyApplicationService(
        AgentApiKeyRepository apiKeyRepository,
        BimAssetRepository bimAssetRepository,
        EventApplicationService eventApplicationService
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.bimAssetRepository = bimAssetRepository;
        this.eventApplicationService = eventApplicationService;
    }

    @Transactional
    public AgentApiKeyCreateResponse createKey(Long userId, AgentApiKeyCreateRequest request) {
        String scopeType = request.scopeType();

        if ("ALL_PROJECTS".equals(scopeType)) {
            if (!apiKeyRepository.hasProjectAdminRoleOnAllActiveProjects(userId)) {
                throw new BusinessException("AGENT_KEY_ALL_PROJECTS_FORBIDDEN",
                    "只有平台管理员可以创建 ALL_PROJECTS API Key", HttpStatus.FORBIDDEN);
            }
        } else if ("SPECIFIC_PROJECTS".equals(scopeType)) {
            if (request.projectIds() == null || request.projectIds().isEmpty()) {
                throw new BusinessException("AGENT_KEY_PROJECTS_REQUIRED",
                    "SPECIFIC_PROJECTS 授权范围必须指定至少一个项目", HttpStatus.BAD_REQUEST);
            }
            // Verify user has access to all specified projects
            List<Long> accessibleProjectIds = bimAssetRepository.listProjects(userId, null).stream()
                .map(p -> p.projectId()).toList();
            Set<Long> accessible = Set.copyOf(accessibleProjectIds);
            for (Long pid : request.projectIds()) {
                if (!accessible.contains(pid)) {
                    throw new BusinessException("AGENT_KEY_PROJECT_FORBIDDEN",
                        "无权为项目 " + pid + " 创建 API Key", HttpStatus.FORBIDDEN);
                }
            }
        } else {
            throw new BusinessException("AGENT_KEY_INVALID_SCOPE",
                "无效的授权范围: " + scopeType, HttpStatus.BAD_REQUEST);
        }

        // Generate key
        String plainKey = generatePlainKey();
        String keyPrefix = plainKey.substring(0, 8);
        String keyHash = sha256(plainKey);

        Long apiKeyId = apiKeyRepository.insert(
            request.keyName(), keyPrefix, keyHash, scopeType,
            request.expiresAt(), userId, request.remark());

        // Insert project associations
        if ("SPECIFIC_PROJECTS".equals(scopeType)) {
            for (Long pid : request.projectIds()) {
                apiKeyRepository.insertProject(apiKeyId, pid);
            }
        }

        // Audit event
        eventApplicationService.record("AGENT_KEY", null, "AGENT_API_KEY",
            String.valueOf(apiKeyId), "create", userId, "API",
            "创建 Agent API Key: " + request.keyName() + " scope=" + scopeType, null);

        return new AgentApiKeyCreateResponse(
            apiKeyId, request.keyName(), keyPrefix, plainKey,
            "ACTIVE", scopeType, request.projectIds(),
            request.expiresAt(), request.remark(), Instant.now());
    }

    public List<AgentApiKeyResponse> listKeys(Long userId) {
        List<AgentApiKeyResponse> keys = apiKeyRepository.listByUser(userId);
        // Populate project IDs for each key
        for (int i = 0; i < keys.size(); i++) {
            AgentApiKeyResponse key = keys.get(i);
            List<Long> projectIds = apiKeyRepository.findAuthorizedProjectIds(key.id());
            keys.set(i, new AgentApiKeyResponse(
                key.id(), key.keyName(), key.keyPrefix(), key.status(),
                key.scopeType(), projectIds, key.expiresAt(), key.lastUsedAt(),
                key.lastUsedIp(), key.createdBy(), key.revokedBy(), key.revokedAt(),
                key.remark(), key.createdAt(), key.updatedAt()));
        }
        return keys;
    }

    public AgentApiKeyResponse getKey(Long userId, Long keyId) {
        AgentApiKeyResponse key = apiKeyRepository.findById(keyId)
            .orElseThrow(() -> new BusinessException("AGENT_KEY_NOT_FOUND",
                "API Key 不存在", HttpStatus.NOT_FOUND));
        // Only the creator can view the key
        if (!key.createdBy().equals(userId)) {
            throw new BusinessException("AGENT_KEY_FORBIDDEN",
                "无权查看该 API Key", HttpStatus.FORBIDDEN);
        }
        List<Long> projectIds = apiKeyRepository.findAuthorizedProjectIds(keyId);
        return new AgentApiKeyResponse(
            key.id(), key.keyName(), key.keyPrefix(), key.status(),
            key.scopeType(), projectIds, key.expiresAt(), key.lastUsedAt(),
            key.lastUsedIp(), key.createdBy(), key.revokedBy(), key.revokedAt(),
            key.remark(), key.createdAt(), key.updatedAt());
    }

    @Transactional
    public void revokeKey(Long userId, Long keyId) {
        AgentApiKeyResponse key = apiKeyRepository.findById(keyId)
            .orElseThrow(() -> new BusinessException("AGENT_KEY_NOT_FOUND",
                "API Key 不存在", HttpStatus.NOT_FOUND));
        if (!key.createdBy().equals(userId)) {
            throw new BusinessException("AGENT_KEY_FORBIDDEN",
                "无权撤销该 API Key", HttpStatus.FORBIDDEN);
        }
        if (!apiKeyRepository.revoke(keyId, userId)) {
            throw new BusinessException("AGENT_KEY_ALREADY_REVOKED",
                "API Key 已被撤销或已过期", HttpStatus.BAD_REQUEST);
        }
        eventApplicationService.record("AGENT_KEY", null, "AGENT_API_KEY",
            String.valueOf(keyId), "revoke", userId, "API",
            "撤销 Agent API Key: " + key.keyName(), null);
    }

    public boolean validateKey(String plainKey) {
        if (plainKey == null || plainKey.isBlank()) return false;
        String keyHash = sha256(plainKey);
        return apiKeyRepository.findIdByKeyHash(keyHash)
            .map(apiKeyRepository::isValidKey)
            .orElse(false);
    }

    public Long findKeyIdByHash(String keyHash) {
        return apiKeyRepository.findIdByKeyHash(keyHash).orElse(null);
    }

    public String hashKey(String plainKey) {
        return sha256(plainKey);
    }

    public void touchLastUsed(Long keyId, String ip) {
        apiKeyRepository.updateLastUsed(keyId, ip);
    }

    private String generatePlainKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return KEY_PREFIX + HexFormat.of().formatHex(bytes);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
