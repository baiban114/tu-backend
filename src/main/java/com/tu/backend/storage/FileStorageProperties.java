package com.tu.backend.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public class FileStorageProperties {

    private boolean enabled = true;
    private String s3Endpoint = "http://localhost:9000";
    private String s3Region = "us-east-1";
    private String s3AccessKey = "tuadmin";
    private String s3SecretKey = "tu123456";
    private String s3Bucket = "tu-files";
    private boolean s3PathStyle = true;
    private long maxFileSize = 20L * 1024 * 1024;
    private long maxPdfFileSize = 200L * 1024 * 1024;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public void setS3Endpoint(String s3Endpoint) {
        this.s3Endpoint = s3Endpoint;
    }

    public String getS3Region() {
        return s3Region;
    }

    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    public String getS3AccessKey() {
        return s3AccessKey;
    }

    public void setS3AccessKey(String s3AccessKey) {
        this.s3AccessKey = s3AccessKey;
    }

    public String getS3SecretKey() {
        return s3SecretKey;
    }

    public void setS3SecretKey(String s3SecretKey) {
        this.s3SecretKey = s3SecretKey;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public boolean isS3PathStyle() {
        return s3PathStyle;
    }

    public void setS3PathStyle(boolean s3PathStyle) {
        this.s3PathStyle = s3PathStyle;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxPdfFileSize() {
        return maxPdfFileSize;
    }

    public void setMaxPdfFileSize(long maxPdfFileSize) {
        this.maxPdfFileSize = maxPdfFileSize;
    }
}
