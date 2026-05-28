package com.zhuoyu.delivery.visualization.engine;

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
            environment.getProperty("delivery.bim.engine.provider")
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
        return trimToNull(firstNonBlank(
            environment.getProperty("GLANDAR_STATION_API_BASE"),
            environment.getProperty("delivery.bim.engine.glandar.station-api-base")
        ));
    }

    public String stationWebBase() {
        return trimToNull(firstNonBlank(
            environment.getProperty("GLANDAR_STATION_WEB_BASE"),
            environment.getProperty("delivery.bim.engine.glandar.station-web-base")
        ));
    }

    public boolean tokenConfigured() {
        return trimToNull(firstNonBlank(
            environment.getProperty("GLANDAR_TOKEN"),
            environment.getProperty("delivery.bim.engine.glandar.token")
        )) != null;
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

    private String firstNonBlank(String first, String second) {
        String value = trimToNull(first);
        return value != null ? value : trimToNull(second);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
