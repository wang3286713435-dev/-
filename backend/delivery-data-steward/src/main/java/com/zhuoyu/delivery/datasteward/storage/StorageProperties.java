package com.zhuoyu.delivery.datasteward.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "delivery.storage")
public class StorageProperties {

    private ObjectStorageProvider minio = new ObjectStorageProvider();
    private ObjectStorageProvider s3Compatible = new ObjectStorageProvider();

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
}
