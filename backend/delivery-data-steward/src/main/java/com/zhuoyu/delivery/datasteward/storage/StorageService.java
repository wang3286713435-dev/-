package com.zhuoyu.delivery.datasteward.storage;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileStorageStatusResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageProviderHealthResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageProviderReadinessResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import okhttp3.OkHttpClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StorageService {

    private final StorageProperties storageProperties;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StorageService(StorageProperties storageProperties, NamedParameterJdbcTemplate jdbcTemplate) {
        this.storageProperties = storageProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureReadable(FileAssetResponse file) {
        StorageReference reference = referenceFor(file);
        if ("NAS".equals(reference.provider())) {
            resolveNasPath(reference);
            return;
        }
        ObjectStorageProvider provider = providerFor(reference.provider());
        try {
            provider.client().statObject(StatObjectArgs.builder()
                .bucket(reference.bucket())
                .object(reference.objectKey())
                .build());
        } catch (Exception exception) {
            throw new BusinessException("ASSET_FILE_NOT_READABLE",
                "对象存储文件不存在或不可读取", HttpStatus.PRECONDITION_FAILED);
        }
    }

    public StoredResource openReadable(FileAssetResponse file) {
        StorageReference reference = referenceFor(file);
        if ("NAS".equals(reference.provider())) {
            Path path = resolveNasPath(reference);
            return new StoredResource(
                new FileSystemResource(path),
                detectNasContentType(path, file),
                readableSize(path),
                reference.provider()
            );
        }
        ObjectStorageProvider provider = providerFor(reference.provider());
        try {
            StatObjectResponse stat = provider.client().statObject(StatObjectArgs.builder()
                .bucket(reference.bucket())
                .object(reference.objectKey())
                .build());
            return new StoredResource(
                new InputStreamResource(provider.client().getObject(GetObjectArgs.builder()
                    .bucket(reference.bucket())
                    .object(reference.objectKey())
                    .build())),
                contentTypeFrom(stat.contentType(), file),
                stat.size(),
                reference.provider()
            );
        } catch (Exception exception) {
            throw new BusinessException("ASSET_FILE_NOT_READABLE",
                "对象存储文件不存在或不可读取", HttpStatus.PRECONDITION_FAILED);
        }
    }

    public ObjectMirrorResult mirrorNasFileToObject(FileAssetResponse file, String targetProvider) {
        String providerCode = normalizeTargetProvider(targetProvider);
        ObjectStorageProvider provider = providerFor(providerCode);
        String bucket = defaultBucket(providerCode);
        StorageReference sourceReference = sourceReferenceForMigration(file);
        Path sourcePath = resolveNasPath(sourceReference);
        Long sizeBytes = readableSize(sourcePath);
        String checksum = sha256File(sourcePath);
        String contentType = detectNasContentType(sourcePath, file);
        String objectKey = stableObjectKey(file, checksum);
        try {
            ensureBucket(provider.client(), bucket);
            try (InputStream inputStream = Files.newInputStream(sourcePath)) {
                provider.client().putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(inputStream, sizeBytes, -1)
                    .build());
            }
            StatObjectResponse stat = provider.client().statObject(StatObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build());
            if (stat.size() != sizeBytes) {
                throw new BusinessException("STORAGE_MIGRATION_VERIFY_FAILED",
                    "对象存储校验失败：文件大小不一致", HttpStatus.PRECONDITION_FAILED);
            }
            Instant verifiedAt = Instant.now();
            return new ObjectMirrorResult(
                providerCode,
                bucket,
                objectKey,
                stat.etag(),
                checksum,
                contentType,
                sizeBytes,
                "NAS",
                sha256Text(sourceReference.rawReference()),
                sha256Text(sourcePath.toString()),
                verifiedAt
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("STORAGE_MIGRATION_UPLOAD_FAILED",
                "对象存储镜像上传失败", HttpStatus.PRECONDITION_FAILED);
        }
    }

    public ObjectWriteResult writeUploadToObject(
        Long projectId,
        String assetUuid,
        String fileName,
        String checksum,
        String contentType,
        Long sizeBytes,
        InputStream inputStream,
        String targetProvider
    ) {
        String providerCode = normalizeTargetProvider(targetProvider);
        ObjectStorageProvider provider = providerFor(providerCode);
        String bucket = defaultBucket(providerCode);
        String objectKey = stableUploadObjectKey(projectId, assetUuid, checksum, fileName);
        String safeContentType = hasText(contentType) ? contentType : contentTypeFromFileName(fileName);
        try (InputStream uploadStream = inputStream) {
            ensureBucket(provider.client(), bucket);
            provider.client().putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .contentType(safeContentType)
                .stream(uploadStream, sizeBytes == null ? -1 : sizeBytes, -1)
                .build());
            StatObjectResponse stat = provider.client().statObject(StatObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build());
            if (sizeBytes != null && stat.size() != sizeBytes) {
                throw new BusinessException("OBJECT_UPLOAD_VERIFY_FAILED",
                    "对象存储写入校验失败：文件大小不一致", HttpStatus.PRECONDITION_FAILED);
            }
            Instant verifiedAt = Instant.now();
            return new ObjectWriteResult(
                providerCode,
                bucket,
                objectKey,
                stat.etag(),
                checksum,
                safeContentType,
                stat.size(),
                sha256Text("USER_UPLOAD:" + assetUuid),
                verifiedAt
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("OBJECT_UPLOAD_FAILED",
                "对象存储暂不可用，新增文件未写入", HttpStatus.PRECONDITION_FAILED);
        }
    }

    public List<StorageProviderHealthResponse> providerHealth() {
        return List.of(
            new StorageProviderHealthResponse("NAS", "NAS 文件源", true, true, true, false, null),
            providerHealth("MINIO", "MinIO 对象存储", storageProperties.getMinio()),
            providerHealth("S3_COMPATIBLE", "S3-compatible 对象存储", storageProperties.getS3Compatible())
        );
    }

    public StorageProviderReadinessResponse minioReadiness() {
        return providerReadiness("MINIO", storageProperties.getMinio());
    }

    public FileStorageStatusResponse fileStorageStatus(FileAssetResponse file) {
        StoredObjectStatus objectStatus = activeObjectStatus(file.fileId());
        if (objectStatus != null) {
            return new FileStorageStatusResponse(
                file.fileId(),
                file.assetUuid(),
                file.projectId(),
                safeStorageState(objectStatus.storageState(), "OBJECT_STORED"),
                normalizeProvider(objectStatus.provider(), file.storageProvider(), file.storagePath()),
                true,
                hasText(objectStatus.checksum()) || hasText(file.checksum()),
                objectStatus.lastVerifiedAt(),
                safeMigrationStatus(objectStatus.migrationStatus(), "COMPLETED"),
                "文件已有对象存储版本；平台只返回状态，不暴露底层对象定位信息。"
            );
        }
        MigrationStatus migrationStatus = latestMigrationStatus(file.fileId());
        if (migrationStatus != null && "FAILED".equalsIgnoreCase(migrationStatus.migrationStatus())) {
            return new FileStorageStatusResponse(
                file.fileId(),
                file.assetUuid(),
                file.projectId(),
                "MIGRATION_FAILED",
                normalizeProvider(file.storageProvider(), null, file.storagePath()),
                false,
                hasText(file.checksum()),
                migrationStatus.lastVerifiedAt(),
                "FAILED",
                "对象存储迁移失败，请查看受控任务记录；不会返回底层路径。"
            );
        }
        if (migrationStatus != null) {
            return new FileStorageStatusResponse(
                file.fileId(),
                file.assetUuid(),
                file.projectId(),
                "MIGRATION_PENDING",
                normalizeProvider(file.storageProvider(), null, file.storagePath()),
                false,
                hasText(file.checksum()),
                migrationStatus.lastVerifiedAt(),
                safeMigrationStatus(migrationStatus.migrationStatus(), "PENDING"),
                "对象存储迁移尚未完成；当前仍按受控存储访问策略处理。"
            );
        }
        String activeProvider = normalizeProvider(file.storageProvider(), null, file.storagePath());
        boolean objectStored = isObjectProvider(activeProvider);
        return new FileStorageStatusResponse(
            file.fileId(),
            file.assetUuid(),
            file.projectId(),
            objectStored ? "OBJECT_STORED" : "NAS_ONLY",
            activeProvider,
            objectStored,
            hasText(file.checksum()),
            file.lastSeenAt(),
            objectStored ? "COMPLETED" : "NOT_STARTED",
            objectStored
                ? "文件当前指向对象存储；平台不返回底层对象定位信息。"
                : "文件当前仍为 NAS 源文件；对象存储镜像尚未建立。"
        );
    }

    private StorageProviderHealthResponse providerHealth(
        String code,
        String displayName,
        StorageProperties.ObjectStorageProvider config
    ) {
        boolean configured = config != null
            && config.isEnabled()
            && hasText(config.getEndpoint())
            && hasText(config.getAccessKey())
            && hasText(config.getSecretKey());
        if (!configured) {
            return new StorageProviderHealthResponse(code, displayName, false, false, true, false,
                "对象存储未配置或未启用");
        }
        try {
            minioClient(config).listBuckets();
            return new StorageProviderHealthResponse(code, displayName, true, true, true, false, null);
        } catch (Exception exception) {
            return new StorageProviderHealthResponse(code, displayName, true, false, true, false,
                "对象存储当前不可用或凭证不可用");
        }
    }

    private StorageProviderReadinessResponse providerReadiness(
        String providerCode,
        StorageProperties.ObjectStorageProvider config
    ) {
        String endpointType = endpointType(config == null ? null : config.getEndpoint());
        boolean configured = config != null
            && config.isEnabled()
            && hasText(config.getEndpoint())
            && hasText(config.getAccessKey())
            && hasText(config.getSecretKey())
            && hasText(config.getDefaultBucket());
        if (!configured) {
            return new StorageProviderReadinessResponse(
                providerCode,
                false,
                false,
                false,
                false,
                endpointType,
                "NOT_CONFIGURED",
                "对象存储未完整配置，不能启动真实项目对象化。"
            );
        }
        boolean reachable = false;
        boolean readable = false;
        boolean writable = false;
        try {
            MinioClient client = minioClient(config);
            String bucket = defaultBucket(providerCode);
            ensureBucket(client, bucket);
            reachable = true;
            readable = true;

            String smokeObjectKey = "smoke/readiness/" + UUID.randomUUID() + ".txt";
            byte[] payload = ("readiness:" + Instant.now()).getBytes(StandardCharsets.UTF_8);
            client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(smokeObjectKey)
                .contentType("text/plain")
                .stream(new ByteArrayInputStream(payload), payload.length, -1)
                .build());
            StatObjectResponse stat = client.statObject(StatObjectArgs.builder()
                .bucket(bucket)
                .object(smokeObjectKey)
                .build());
            writable = stat.size() == payload.length;
            client.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(smokeObjectKey)
                .build());
        } catch (Exception exception) {
            if (!reachable) {
                return new StorageProviderReadinessResponse(
                    providerCode,
                    true,
                    false,
                    false,
                    false,
                    endpointType,
                    "UNREACHABLE",
                    "对象存储当前不可达或凭证不可用。"
                );
            }
        }
        String readinessStatus;
        String message;
        if ("LOCAL_DEV_MINIO".equals(endpointType)) {
            readinessStatus = "LOCAL_DEV_ONLY";
            message = "本机开发对象存储可用，但尚未确认 NAS 侧 MinIO，不能启动真实全项目对象化。";
        } else if (!writable) {
            readinessStatus = "WRITE_UNAVAILABLE";
            message = "对象存储可访问，但写入探测未通过。";
        } else if ("NAS_SIDE_MINIO".equals(endpointType)) {
            readinessStatus = "READY";
            message = "NAS 侧 MinIO 已通过只读/写入探测，可用于 M3G 后续受控对象化。";
        } else {
            readinessStatus = "READY";
            message = "对象存储已通过探测，但 endpoint 类型未能自动归类，请运维确认后再启动真实对象化。";
        }
        return new StorageProviderReadinessResponse(
            providerCode,
            true,
            reachable,
            readable,
            writable,
            endpointType,
            readinessStatus,
            message
        );
    }

    private StorageReference referenceFor(FileAssetResponse file) {
        StorageReference activeObjectReference = activeObjectReference(file.fileId());
        if (activeObjectReference != null) {
            return activeObjectReference;
        }
        return sourceReferenceForMigration(file);
    }

    private StorageReference sourceReferenceForMigration(FileAssetResponse file) {
        String storagePath = text(file.storagePath());
        if (storagePath == null) {
            throw new BusinessException("ASSET_FILE_PATH_INVALID", "文件缺少存储引用", HttpStatus.PRECONDITION_FAILED);
        }
        String lower = storagePath.toLowerCase(Locale.ROOT);
        if (lower.startsWith("minio://")) {
            ObjectLocation location = parseObjectLocation(storagePath.substring("minio://".length()));
            return new StorageReference("MINIO", location.bucket(), location.objectKey(), storagePath);
        }
        if (lower.startsWith("s3://") || lower.startsWith("oss://")) {
            String value = lower.startsWith("s3://")
                ? storagePath.substring("s3://".length())
                : storagePath.substring("oss://".length());
            ObjectLocation location = parseObjectLocation(value);
            return new StorageReference("S3_COMPATIBLE", location.bucket(), location.objectKey(), storagePath);
        }
        if (lower.startsWith("nas://") || storagePath.startsWith("/")) {
            return new StorageReference("NAS", null, null, storagePath);
        }
        throw new BusinessException("ASSET_FILE_PATH_INVALID", "存储引用格式不受支持", HttpStatus.PRECONDITION_FAILED);
    }

    private StorageReference activeObjectReference(Long fileId) {
        List<StorageReference> rows = jdbcTemplate.query("""
            SELECT so.provider, so.bucket, so.object_key
            FROM data_file_object_versions fov
            JOIN data_storage_objects so ON so.id = fov.storage_object_id AND so.deleted = 0
            WHERE fov.file_id = :fileId
              AND fov.active = 1
              AND fov.deleted = 0
              AND fov.storage_state = 'OBJECT_STORED'
            ORDER BY fov.id DESC
            LIMIT 1
            """, new MapSqlParameterSource("fileId", fileId), (rs, rowNum) -> new StorageReference(
            normalizeProvider(rs.getString("provider"), null, null),
            rs.getString("bucket"),
            rs.getString("object_key"),
            null
        ));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private ObjectLocation parseObjectLocation(String value) {
        String normalized = text(value);
        if (normalized == null || normalized.startsWith("/") || normalized.contains("..")) {
            throw new BusinessException("ASSET_FILE_PATH_INVALID", "对象存储引用格式不受支持", HttpStatus.PRECONDITION_FAILED);
        }
        int slash = normalized.indexOf('/');
        if (slash <= 0 || slash == normalized.length() - 1) {
            throw new BusinessException("ASSET_FILE_PATH_INVALID", "对象存储引用格式不受支持", HttpStatus.PRECONDITION_FAILED);
        }
        return new ObjectLocation(normalized.substring(0, slash), normalized.substring(slash + 1));
    }

    private ObjectStorageProvider providerFor(String providerCode) {
        StorageProperties.ObjectStorageProvider config = switch (providerCode) {
            case "MINIO" -> storageProperties.getMinio();
            case "S3_COMPATIBLE" -> storageProperties.getS3Compatible();
            default -> null;
        };
        if (config == null || !config.isEnabled() || !hasText(config.getEndpoint())
            || !hasText(config.getAccessKey()) || !hasText(config.getSecretKey())) {
            throw new BusinessException("STORAGE_PROVIDER_UNAVAILABLE",
                "对象存储提供方未配置或未启用", HttpStatus.PRECONDITION_FAILED);
        }
        return new ObjectStorageProvider(providerCode, minioClient(config));
    }

    private String defaultBucket(String providerCode) {
        StorageProperties.ObjectStorageProvider config = switch (providerCode) {
            case "MINIO" -> storageProperties.getMinio();
            case "S3_COMPATIBLE" -> storageProperties.getS3Compatible();
            default -> null;
        };
        String bucket = config == null ? null : text(config.getDefaultBucket());
        if (bucket == null) {
            throw new BusinessException("STORAGE_PROVIDER_BUCKET_REQUIRED",
                "对象存储默认空间未配置", HttpStatus.PRECONDITION_FAILED);
        }
        return bucket;
    }

    private void ensureBucket(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private MinioClient minioClient(StorageProperties.ObjectStorageProvider config) {
        return MinioClient.builder()
            .endpoint(config.getEndpoint())
            .credentials(config.getAccessKey(), config.getSecretKey())
            .httpClient(new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .writeTimeout(Duration.ofSeconds(30))
                .callTimeout(Duration.ofSeconds(45))
                .build())
            .build();
    }

    private String endpointType(String endpoint) {
        String value = text(endpoint);
        if (value == null) {
            return "UNKNOWN";
        }
        try {
            String host = URI.create(value).getHost();
            if (host == null || host.isBlank()) {
                return "UNKNOWN";
            }
            String normalized = host.toLowerCase(Locale.ROOT);
            if ("localhost".equals(normalized)
                || "0.0.0.0".equals(normalized)
                || "host.docker.internal".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || normalized.startsWith("127.")
                || "::1".equals(normalized)) {
                return "LOCAL_DEV_MINIO";
            }
            if (normalized.startsWith("192.168.")
                || normalized.startsWith("10.")
                || private172(normalized)) {
                return "NAS_SIDE_MINIO";
            }
            return "UNKNOWN";
        } catch (IllegalArgumentException exception) {
            return "UNKNOWN";
        }
    }

    private boolean private172(String host) {
        if (!host.startsWith("172.")) {
            return false;
        }
        String[] parts = host.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private Path resolveNasPath(StorageReference reference) {
        String rawPath = reference.rawReference();
        if (rawPath == null) {
            throw new BusinessException("ASSET_FILE_PATH_INVALID", "文件缺少存储引用", HttpStatus.PRECONDITION_FAILED);
        }
        if (rawPath.startsWith("nas://")) {
            rawPath = rawPath.substring("nas://".length());
            if (!rawPath.startsWith("/")) {
                rawPath = "/" + rawPath;
            }
        }
        if (!rawPath.startsWith("/")) {
            throw new BusinessException("ASSET_FILE_PATH_INVALID", "NAS 存储引用格式不受支持", HttpStatus.PRECONDITION_FAILED);
        }
        Path path = Paths.get(rawPath).normalize().toAbsolutePath();
        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new BusinessException("ASSET_FILE_NOT_READABLE", "文件不存在或不可读取", HttpStatus.PRECONDITION_FAILED);
        }
        return path;
    }

    private String detectNasContentType(Path path, FileAssetResponse file) {
        try {
            String probed = Files.probeContentType(path);
            if (hasText(probed)) {
                return probed;
            }
        } catch (IOException ignored) {
            // Fall back by extension below.
        }
        return contentTypeFrom(null, file);
    }

    private Long readableSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            throw new BusinessException("ASSET_FILE_NOT_READABLE", "文件大小读取失败", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private String contentTypeFrom(String contentType, FileAssetResponse file) {
        if (hasText(contentType)) {
            return contentType;
        }
        return contentTypeFromFileName(file.fileName());
    }

    private String contentTypeFromFileName(String fileName) {
        return switch (extensionOf(fileName)) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".bmp" -> "image/bmp";
            case ".svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }

    private StoredObjectStatus activeObjectStatus(Long fileId) {
        List<StoredObjectStatus> rows = jdbcTemplate.query("""
            SELECT so.provider, fov.storage_state, fov.migration_status,
                   COALESCE(fov.checksum, so.checksum) AS checksum,
                   COALESCE(fov.last_verified_at, so.last_verified_at) AS last_verified_at
            FROM data_file_object_versions fov
            JOIN data_storage_objects so ON so.id = fov.storage_object_id AND so.deleted = 0
            WHERE fov.file_id = :fileId
              AND fov.active = 1
              AND fov.deleted = 0
            ORDER BY fov.id DESC
            LIMIT 1
            """, new MapSqlParameterSource("fileId", fileId), (rs, rowNum) -> {
            Timestamp verifiedAt = rs.getTimestamp("last_verified_at");
            return new StoredObjectStatus(
                rs.getString("provider"),
                rs.getString("storage_state"),
                rs.getString("migration_status"),
                rs.getString("checksum"),
                verifiedAt == null ? null : verifiedAt.toInstant()
            );
        });
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private MigrationStatus latestMigrationStatus(Long fileId) {
        List<MigrationStatus> rows = jdbcTemplate.query("""
            SELECT migration_status, last_verified_at
            FROM data_object_migration_tasks
            WHERE file_id = :fileId
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """, new MapSqlParameterSource("fileId", fileId), (rs, rowNum) -> {
            Timestamp verifiedAt = rs.getTimestamp("last_verified_at");
            return new MigrationStatus(
                rs.getString("migration_status"),
                verifiedAt == null ? null : verifiedAt.toInstant()
            );
        });
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private String normalizeProvider(String primary, String fallback, String storagePath) {
        String lower = storagePath == null ? "" : storagePath.toLowerCase(Locale.ROOT);
        if (lower.startsWith("minio://")) return "MINIO";
        if (lower.startsWith("s3://") || lower.startsWith("oss://")) return "S3_COMPATIBLE";
        String provider = text(primary);
        if (provider == null) provider = text(fallback);
        if (provider != null) {
            String normalized = provider.toUpperCase(Locale.ROOT);
            if ("S3".equals(normalized) || "OSS".equals(normalized)) return "S3_COMPATIBLE";
            if ("MINIO".equals(normalized) || "S3_COMPATIBLE".equals(normalized) || "NAS".equals(normalized)) return normalized;
        }
        return "NAS";
    }

    private String normalizeTargetProvider(String value) {
        String normalized = text(value);
        if (normalized == null) return "MINIO";
        normalized = normalized.toUpperCase(Locale.ROOT);
        if ("S3".equals(normalized) || "OSS".equals(normalized)) return "S3_COMPATIBLE";
        if (!List.of("MINIO", "S3_COMPATIBLE").contains(normalized)) {
            throw new BusinessException("STORAGE_MIGRATION_TARGET_PROVIDER_INVALID",
                "对象存储目标只能是 MINIO 或 S3_COMPATIBLE", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String safeStorageState(String value, String fallback) {
        String normalized = text(value);
        if (normalized == null) return fallback;
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "NAS_ONLY", "MIGRATION_PENDING", "OBJECT_STORED", "MIGRATION_FAILED" -> normalized.toUpperCase(Locale.ROOT);
            case "MIGRATION_PARTIAL" -> "MIGRATION_PENDING";
            default -> fallback;
        };
    }

    private String safeMigrationStatus(String value, String fallback) {
        String normalized = text(value);
        if (normalized == null) return fallback;
        return normalized.toUpperCase(Locale.ROOT);
    }

    private boolean isObjectProvider(String provider) {
        return "MINIO".equals(provider) || "S3_COMPATIBLE".equals(provider);
    }

    private String extensionOf(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String stableObjectKey(FileAssetResponse file, String checksum) {
        String safeName = file.fileName() == null || file.fileName().isBlank()
            ? "file-" + file.fileId()
            : file.fileName().replaceAll("[\\\\/\\p{Cntrl}]+", "_");
        return "projects/" + file.projectId() + "/files/" + file.fileId() + "/" + checksum + "/" + safeName;
    }

    private String stableUploadObjectKey(Long projectId, String assetUuid, String checksum, String fileName) {
        String safeName = fileName == null || fileName.isBlank()
            ? "upload-" + assetUuid
            : fileName.replaceAll("[\\\\/\\p{Cntrl}]+", "_");
        return "projects/" + projectId + "/uploads/" + assetUuid + "/" + checksum + "/" + safeName;
    }

    private String sha256File(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return hex(digest.digest());
        } catch (IOException exception) {
            throw new BusinessException("ASSET_FILE_NOT_READABLE", "文件不存在或不可读取", HttpStatus.PRECONDITION_FAILED);
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException("STORAGE_MIGRATION_CHECKSUM_FAILED",
                "文件校验值计算失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String sha256Text(String value) {
        if (value == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return hex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException("STORAGE_MIGRATION_CHECKSUM_FAILED",
                "文件校验值计算失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record StoredResource(Resource resource, String contentType, Long contentLength, String provider) {
    }

    public record ObjectMirrorResult(
        String provider,
        String bucket,
        String objectKey,
        String etag,
        String checksum,
        String contentType,
        Long sizeBytes,
        String sourceProvider,
        String sourceUriDigest,
        String sourcePathDigest,
        Instant verifiedAt
    ) {
    }

    public record ObjectWriteResult(
        String provider,
        String bucket,
        String objectKey,
        String etag,
        String checksum,
        String contentType,
        Long sizeBytes,
        String sourceUriDigest,
        Instant verifiedAt
    ) {
    }

    private record StorageReference(String provider, String bucket, String objectKey, String rawReference) {
    }

    private record ObjectLocation(String bucket, String objectKey) {
    }

    private record ObjectStorageProvider(String providerCode, MinioClient client) {
    }

    private record StoredObjectStatus(
        String provider,
        String storageState,
        String migrationStatus,
        String checksum,
        Instant lastVerifiedAt
    ) {
    }

    private record MigrationStatus(String migrationStatus, Instant lastVerifiedAt) {
    }
}
