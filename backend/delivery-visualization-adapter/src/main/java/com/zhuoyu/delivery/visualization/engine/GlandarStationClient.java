package com.zhuoyu.delivery.visualization.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GlandarStationClient {

    private static final int BIM_ENGINE_TYPE = 2;
    private static final String UPLOAD_PATH = "/api/app/model/SplitUploadFile";
    private static final String DIRECT_UPLOAD_PATH = "/api/app/model/upload-file";
    private static final String QUERY_PATH = "/api/app/model/query-model-info";
    private static final long DIRECT_UPLOAD_MAX_BYTES = 64L * 1024L * 1024L;

    private final GlandarEngineSettings settings;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GlandarStationClient(GlandarEngineSettings settings, ObjectMapper objectMapper) {
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public UploadResult upload(UploadCommand command, InputStream source, long sizeBytes) {
        requireReady();
        int chunkSize = settings.uploadChunkSizeBytes();
        if (sizeBytes >= 0 && sizeBytes <= DIRECT_UPLOAD_MAX_BYTES) {
            return uploadDirect(command, source);
        }
        int chunks = Math.max(1, (int) Math.ceil(sizeBytes / (double) chunkSize));
        String inputJson = inputJson(command);
        String lightweightName = command.lightweightName();
        try (InputStream inputStream = source) {
            for (int chunk = 0; chunk < chunks; chunk++) {
                byte[] bytes = inputStream.readNBytes(chunkSize);
                if (bytes.length == 0 && sizeBytes > 0) {
                    throw new BusinessException("ENGINE_UPLOAD_STREAM_ENDED",
                        "轻量化任务提交失败：模型流读取提前结束", HttpStatus.PRECONDITION_FAILED);
                }
                String responseBody = postMultipart(command.fileName(), chunk, chunks, inputJson, bytes);
                JsonNode root = parseJson(responseBody, "ENGINE_UPLOAD_RESPONSE_INVALID");
                if (root.path("code").asInt(0) != 1) {
                    throw new BusinessException("ENGINE_UPLOAD_FAILED",
                        "轻量化任务提交失败：" + safeMessage(root.path("codeMsg").asText()), HttpStatus.PRECONDITION_FAILED);
                }
                String returnedName = root.path("datas").path("lightweightName").asText(null);
                if (hasText(returnedName)) {
                    lightweightName = returnedName.trim();
                }
                if (sizeBytes == 0) {
                    break;
                }
            }
            return new UploadResult(lightweightName, objectMapper.writeValueAsString(Map.of(
                "code", 1,
                "codeMsg", "上传成功",
                "lightweightName", lightweightName,
                "chunks", chunks
            )));
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException("ENGINE_UPLOAD_FAILED", "轻量化任务提交失败：模型流读取失败",
                HttpStatus.PRECONDITION_FAILED);
        }
    }

    public QueryResult query(String lightweightName) {
        requireReady();
        if (!hasText(lightweightName)) {
            throw new BusinessException("ENGINE_TASK_NOT_FOUND", "轻量化任务名为空", HttpStatus.NOT_FOUND);
        }
        String url = settings.stationApiBase() + QUERY_PATH + "?LightweightName="
            + URLEncoder.encode(lightweightName, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Token", settings.stationToken())
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("ENGINE_API_UNREACHABLE", "轻量化任务查询失败：引擎接口不可用",
                    HttpStatus.PRECONDITION_FAILED);
            }
            JsonNode root = parseJson(response.body(), "ENGINE_QUERY_RESPONSE_INVALID");
            if (root.path("code").asInt(0) != 1) {
                throw new BusinessException("ENGINE_TASK_NOT_FOUND",
                    "轻量化任务查询失败：" + safeMessage(root.path("codeMsg").asText()), HttpStatus.NOT_FOUND);
            }
            JsonNode item = firstData(root.path("datas"));
            if (item == null || item.isMissingNode() || item.isNull()) {
                return new QueryResult("RUNNING", 30, null, false, null, "Station 暂未返回任务明细", response.body());
            }
            int stationStatus = item.path("status").asInt(-1);
            String stationDescription = safeMessage(item.path("statusDescription").asText());
            String modelAccessAddress = normalizeModelAccessAddress(lightweightName, item.path("modelAccessAddress").asText(null));
            if (stationStatus == 100) {
                return new QueryResult("READY", 100, modelAccessAddress, hasText(modelAccessAddress),
                    null, defaultText(stationDescription, "轻量化成功"), response.body());
            }
            if (stationStatus < 0) {
                return new QueryResult("FAILED", 0, null, false, "ENGINE_TRANSCODE_FAILED",
                    defaultText(stationDescription, "轻量化转换失败"), response.body());
            }
            return new QueryResult("RUNNING", 60, null, false, null,
                defaultText(stationDescription, "轻量化转换中"), response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException("ENGINE_API_UNREACHABLE", "轻量化任务查询失败：引擎接口不可达",
                HttpStatus.PRECONDITION_FAILED);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("ENGINE_API_UNREACHABLE", "轻量化任务查询被中断",
                HttpStatus.PRECONDITION_FAILED);
        }
    }

    private String postMultipart(String fileName, int chunk, int chunks, String inputJson, byte[] fileBytes) {
        String boundary = "----delivery-glandar-" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, fileName, chunk, chunks, inputJson, fileBytes);
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.stationApiBase() + UPLOAD_PATH))
            .timeout(Duration.ofMinutes(3))
            .header("Token", settings.stationToken())
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("ENGINE_UPLOAD_FAILED", "轻量化任务提交失败：引擎接口返回 HTTP "
                    + response.statusCode(),
                    HttpStatus.PRECONDITION_FAILED);
            }
            return response.body();
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException("ENGINE_API_UNREACHABLE", "轻量化任务提交失败：引擎接口不可达（"
                + exception.getClass().getSimpleName() + "）",
                HttpStatus.PRECONDITION_FAILED);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("ENGINE_UPLOAD_FAILED", "轻量化任务提交被中断",
                HttpStatus.PRECONDITION_FAILED);
        }
    }

    private UploadResult uploadDirect(UploadCommand command, InputStream source) {
        String inputJson = inputJson(command);
        String url = settings.stationApiBase() + DIRECT_UPLOAD_PATH;
        String boundary = "----delivery-glandar-direct-" + UUID.randomUUID();
        try (InputStream inputStream = source) {
            byte[] bytes = inputStream.readAllBytes();
            byte[] body = directMultipartBody(boundary, command.fileName(), inputJson, bytes);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("Token", settings.stationToken())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("ENGINE_UPLOAD_FAILED", "轻量化任务直接提交失败：引擎接口返回 HTTP "
                    + response.statusCode(), HttpStatus.PRECONDITION_FAILED);
            }
            JsonNode root = parseJson(response.body(), "ENGINE_UPLOAD_RESPONSE_INVALID");
            if (root.path("code").asInt(0) != 1) {
                throw new BusinessException("ENGINE_UPLOAD_FAILED",
                    "轻量化任务直接提交失败：" + safeMessage(root.path("codeMsg").asText()), HttpStatus.PRECONDITION_FAILED);
            }
            String returnedName = root.path("datas").path("lightweightName").asText(null);
            return new UploadResult(hasText(returnedName) ? returnedName.trim() : command.lightweightName(), response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException("ENGINE_UPLOAD_FAILED", "轻量化任务直接提交失败：模型流读取失败",
                HttpStatus.PRECONDITION_FAILED);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("ENGINE_UPLOAD_FAILED", "轻量化任务直接提交被中断",
                HttpStatus.PRECONDITION_FAILED);
        }
    }

    private byte[] directMultipartBody(String boundary, String fileName, String inputJson, byte[] fileBytes) {
        StringBuilder builder = new StringBuilder();
        appendField(builder, boundary, "input", inputJson);
        builder.append("--").append(boundary).append("\r\n");
        builder.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            .append(safeFileName(fileName)).append("\"\r\n");
        builder.append("Content-Type: application/octet-stream\r\n\r\n");
        byte[] header = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[header.length + fileBytes.length + footer.length];
        System.arraycopy(header, 0, body, 0, header.length);
        System.arraycopy(fileBytes, 0, body, header.length, fileBytes.length);
        System.arraycopy(footer, 0, body, header.length + fileBytes.length, footer.length);
        return body;
    }

    private byte[] multipartBody(String boundary, String fileName, int chunk, int chunks, String inputJson, byte[] fileBytes) {
        StringBuilder builder = new StringBuilder();
        appendField(builder, boundary, "chunk", String.valueOf(chunk));
        appendField(builder, boundary, "chunks", String.valueOf(chunks));
        appendField(builder, boundary, "input", inputJson);
        builder.append("--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            .append(safeFileName(fileName)).append("\"\r\n")
            .append("Content-Type: application/octet-stream\r\n\r\n");
        byte[] header = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[header.length + fileBytes.length + footer.length];
        System.arraycopy(header, 0, body, 0, header.length);
        System.arraycopy(fileBytes, 0, body, header.length, fileBytes.length);
        System.arraycopy(footer, 0, body, header.length + fileBytes.length, footer.length);
        return body;
    }

    private void appendField(StringBuilder builder, String boundary, String name, String value) {
        builder.append("--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
            .append(value == null ? "" : value)
            .append("\r\n");
    }

    private String inputJson(UploadCommand command) {
        Map<String, Object> config = new HashMap<>();
        config.put("style", 1);
        config.put("accuracy", 5);
        config.put("drawing", 0);
        config.put("locationType", 3);
        config.put("unitRatio", 0.001);
        config.put("isInstance", 1);
        config.put("maxCountInstance", 100);
        config.put("engineType", BIM_ENGINE_TYPE);
        config.put("srs", "");
        config.put("srsOrigin", new Object[]{});
        config.put("longitude", 1.9958732553751939);
        config.put("latitude", 0.46945336105306534);
        config.put("transHeight", 0);
        config.put("flipY", 0);
        config.put("faceNumLimit", 1000000);
        config.put("textureLimit", 10240);
        config.put("textureSizePercent", 50);
        config.put("textureQuality", 30);
        config.put("combineTexture", 1);
        config.put("blockRender", 1);
        config.put("useGisCS", 1);
        config.put("materialType", 0);
        config.put("compressionLevel", 10);
        config.put("quantizePositionBits", 20);
        config.put("quantizeNormalBits", 10);
        config.put("quantizeTexcoordBits", 16);
        config.put("ktx2", 0);
        config.put("separateTextures", 0);
        config.put("separate", 0);
        config.put("acs", 0);
        config.put("language", 0);
        config.put("schedule", 0);
        config.put("combineResult", 0);
        config.put("dbTreeType", 1);
        config.put("dbPropertyType", 0);
        config.put("simplification", 0);
        config.put("generateSubMesh", 0);
        config.put("residentRatio", 0.5);
        config.put("correctWinding", 0);
        Map<String, Object> input = new HashMap<>();
        input.put("Name", command.fileName());
        input.put("LightweightName", command.lightweightName());
        input.put("InitiatingUser", "delivery-platform");
        input.put("UniqueCode", command.uniqueCode());
        input.put("Priority", "203");
        input.put("ModelUploadUrl", "");
        input.put("OtherInfo", command.safeOtherInfo());
        input.put("ConfigJson", config);
        try {
            return objectMapper.writeValueAsString(input);
        } catch (IOException exception) {
            throw new BusinessException("ENGINE_INPUT_SERIALIZE_FAILED", "轻量化任务参数生成失败",
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private JsonNode parseJson(String body, String code) {
        try {
            return objectMapper.readTree(defaultText(body, "{}"));
        } catch (IOException exception) {
            throw new BusinessException(code, "轻量化引擎返回内容无法解析", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private JsonNode firstData(JsonNode datas) {
        if (datas == null || datas.isMissingNode() || datas.isNull()) {
            return null;
        }
        if (datas.isArray()) {
            Iterator<JsonNode> iterator = datas.elements();
            return iterator.hasNext() ? iterator.next() : null;
        }
        return datas;
    }

    private String normalizeModelAccessAddress(String lightweightName, String returnedAddress) {
        String address = hasText(returnedAddress)
            ? returnedAddress.trim()
            : settings.stationApiBase() + "/Tools/output/model/" + lightweightName + "/root.glt";
        if (address.contains("127.0.0.1") || address.contains("localhost")) {
            return settings.stationApiBase() + "/Tools/output/model/" + lightweightName + "/root.glt";
        }
        return address;
    }

    private void requireReady() {
        if (!settings.readyForHandshake()) {
            throw new BusinessException("ENGINE_TOKEN_MISSING", "葛兰岱尔引擎配置不完整，不能提交真实转换",
                HttpStatus.PRECONDITION_FAILED);
        }
    }

    private String safeFileName(String fileName) {
        return defaultText(fileName, "model.rvt").replace("\"", "");
    }

    private String safeMessage(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.replace(settings.stationToken(), "***");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    public record UploadCommand(
        String fileName,
        String lightweightName,
        String uniqueCode,
        String safeOtherInfo
    ) {
    }

    public record UploadResult(
        String lightweightName,
        String stationRecordJson
    ) {
    }

    public record QueryResult(
        String status,
        Integer progressPercent,
        String modelAccessAddress,
        Boolean viewerAvailable,
        String errorCode,
        String message,
        String stationRecordJson
    ) {
    }
}
