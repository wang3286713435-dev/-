package com.zhuoyu.delivery.visualization.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class GlandarEngineSettings {

    private static final String PROVIDER_MOCK = "MOCK";
    private static final String PROVIDER_GLANDAR = "GLANDAR";

    private final Environment environment;

    public GlandarEngineSettings(Environment environment) {
        this.environment = environment;
    }

    public String provider() {
        String value = firstNonBlank(
            environment.getProperty("BIM_ENGINE_PROVIDER"),
            environment.getProperty("bim.engine.provider"),
            environment.getProperty("delivery.bim.engine.provider"),
            System.getenv("BIM_ENGINE_PROVIDER"),
            localGlandarEnv("BIM_ENGINE_PROVIDER")
        );
        if (value == null) {
            return PROVIDER_MOCK;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return PROVIDER_GLANDAR.equals(normalized) ? PROVIDER_GLANDAR : PROVIDER_MOCK;
    }

    public boolean glandarEnabled() {
        return PROVIDER_GLANDAR.equals(provider());
    }

    public String stationApiBase() {
        return stripTrailingSlash(trimToNull(firstNonBlank(
            environment.getProperty("GLANDAR_STATION_API_BASE"),
            environment.getProperty("glandar.station.api.base"),
            environment.getProperty("delivery.bim.engine.glandar.station-api-base"),
            System.getenv("GLANDAR_STATION_API_BASE"),
            localGlandarEnv("GLANDAR_STATION_API_BASE")
        )));
    }

    public String stationWebBase() {
        return stripTrailingSlash(trimToNull(firstNonBlank(
            environment.getProperty("GLANDAR_STATION_WEB_BASE"),
            environment.getProperty("glandar.station.web.base"),
            environment.getProperty("delivery.bim.engine.glandar.station-web-base"),
            System.getenv("GLANDAR_STATION_WEB_BASE"),
            localGlandarEnv("GLANDAR_STATION_WEB_BASE")
        )));
    }

    public String stationToken() {
        return trimToNull(firstNonBlank(
            environment.getProperty("GLANDAR_TOKEN"),
            environment.getProperty("glandar.token"),
            environment.getProperty("delivery.bim.engine.glandar.token"),
            System.getenv("GLANDAR_TOKEN"),
            localGlandarEnv("GLANDAR_TOKEN")
        ));
    }

    public int uploadChunkSizeBytes() {
        String value = firstNonBlank(
            environment.getProperty("GLANDAR_UPLOAD_CHUNK_SIZE_MB"),
            environment.getProperty("glandar.upload.chunk.size.mb"),
            environment.getProperty("delivery.bim.engine.glandar.upload-chunk-size-mb"),
            System.getenv("GLANDAR_UPLOAD_CHUNK_SIZE_MB"),
            localGlandarEnv("GLANDAR_UPLOAD_CHUNK_SIZE_MB")
        );
        if (value == null) {
            return 2 * 1024 * 1024;
        }
        try {
            int mb = Integer.parseInt(value.trim());
            return Math.min(Math.max(mb, 1), 16) * 1024 * 1024;
        } catch (NumberFormatException ignored) {
            return 2 * 1024 * 1024;
        }
    }

    public boolean tokenConfigured() {
        return stationToken() != null;
    }

    public boolean readyForHandshake() {
        return glandarEnabled()
            && stationApiBase() != null
            && stationWebBase() != null
            && tokenConfigured();
    }

    public List<String> missingConfiguration() {
        List<String> missing = new ArrayList<>();
        if (!glandarEnabled()) {
            missing.add("BIM_ENGINE_PROVIDER 未设置为 GLANDAR");
        }
        if (stationApiBase() == null) {
            missing.add("GLANDAR_STATION_API_BASE 未配置");
        }
        if (stationWebBase() == null) {
            missing.add("GLANDAR_STATION_WEB_BASE 未配置");
        }
        if (!tokenConfigured()) {
            missing.add("葛兰岱尔服务凭据未通过安全环境注入");
        }
        return missing;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String candidate : values) {
            String value = trimToNull(candidate);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String stripTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String localGlandarEnv(String key) {
        Path path = Path.of(System.getProperty("user.home"), ".zhuoyu-delivery", "glandar.env");
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }
                int delimiter = line.indexOf('=');
                if (delimiter <= 0) {
                    continue;
                }
                String name = line.substring(0, delimiter).trim();
                if (!key.equals(name)) {
                    continue;
                }
                return unquote(line.substring(delimiter + 1).trim());
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private String unquote(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
