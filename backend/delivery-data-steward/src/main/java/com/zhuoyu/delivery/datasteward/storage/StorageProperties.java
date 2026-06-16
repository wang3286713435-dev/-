package com.zhuoyu.delivery.datasteward.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "delivery.storage")
public class StorageProperties {

    private ObjectStorageProvider minio = new ObjectStorageProvider();
    private ObjectStorageProvider s3Compatible = new ObjectStorageProvider();
    private ReadPolicy readPolicy = new ReadPolicy();

    public ObjectStorageProvider getMinio() {
        return minio;
    }

    public void setMinio(ObjectStorageProvider minio) {
        this.minio = minio;
    }

    public ObjectStorageProvider getS3Compatible() {
        return s3Compatible;
    }

    public void setS3Compatible(ObjectStorageProvider s3Compatible) {
        this.s3Compatible = s3Compatible;
    }

    public ReadPolicy getReadPolicy() {
        return readPolicy;
    }

    public void setReadPolicy(ReadPolicy readPolicy) {
        this.readPolicy = readPolicy;
    }

    @PostConstruct
    void loadPrivateLocalMinioEnv() {
        Path envFile = privateMinioEnvFile();
        if (envFile == null || !Files.isRegularFile(envFile)) {
            return;
        }
        Map<String, String> values = parseEnvFile(envFile);
        overlayMinio(values);
    }

    private Path privateMinioEnvFile() {
        String explicit = firstText(
            System.getProperty("delivery.local-minio-env-file"),
            System.getenv("DELIVERY_LOCAL_MINIO_ENV_FILE")
        );
        if (explicit != null) {
            return Path.of(explicit);
        }
        String home = System.getProperty("user.home");
        List<Path> candidates = List.of(
            Path.of("../tmp/local-env/nas-minio.env"),
            Path.of("tmp/local-env/nas-minio.env"),
            home == null ? Path.of(".zhuoyu-delivery/minio.env") : Path.of(home, ".zhuoyu-delivery/minio.env")
        );
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Map<String, String> parseEnvFile(Path envFile) {
        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String rawLine : Files.readAllLines(envFile)) {
                String line = rawLine == null ? "" : rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }
                int split = line.indexOf('=');
                if (split <= 0) {
                    continue;
                }
                String key = line.substring(0, split).trim();
                String value = unquote(line.substring(split + 1).trim());
                if (!key.isEmpty() && usable(value)) {
                    values.put(key, value);
                }
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return values;
    }

    private void overlayMinio(Map<String, String> values) {
        if (values.isEmpty()) {
            return;
        }
        String enabled = values.get("DELIVERY_MINIO_ENABLED");
        if (enabled != null) {
            minio.setEnabled(Boolean.parseBoolean(enabled));
        }
        String endpoint = values.get("DELIVERY_MINIO_ENDPOINT");
        if (endpoint != null) {
            minio.setEndpoint(endpoint);
        }
        String accessKey = values.get("DELIVERY_MINIO_ACCESS_KEY");
        if (accessKey != null) {
            minio.setAccessKey(accessKey);
        }
        String secretKey = values.get("DELIVERY_MINIO_SECRET_KEY");
        if (secretKey != null) {
            minio.setSecretKey(secretKey);
        }
        String bucket = values.get("DELIVERY_MINIO_DEFAULT_BUCKET");
        if (bucket != null) {
            minio.setDefaultBucket(bucket);
        }
    }

    private String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private boolean usable(String value) {
        String text = firstText(value);
        if (text == null) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return !normalized.startsWith("<") && !normalized.endsWith(">");
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public static class ObjectStorageProvider {
        private boolean enabled;
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String defaultBucket;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getDefaultBucket() {
            return defaultBucket;
        }

        public void setDefaultBucket(String defaultBucket) {
            this.defaultBucket = defaultBucket;
        }
    }

    public static class ReadPolicy {
        private boolean objectFirstEnabled = true;
        private boolean nasFallbackEnabled = false;

        public boolean isObjectFirstEnabled() {
            return objectFirstEnabled;
        }

        public void setObjectFirstEnabled(boolean objectFirstEnabled) {
            this.objectFirstEnabled = objectFirstEnabled;
        }

        public boolean isNasFallbackEnabled() {
            return nasFallbackEnabled;
        }

        public void setNasFallbackEnabled(boolean nasFallbackEnabled) {
            this.nasFallbackEnabled = nasFallbackEnabled;
        }
    }
}
