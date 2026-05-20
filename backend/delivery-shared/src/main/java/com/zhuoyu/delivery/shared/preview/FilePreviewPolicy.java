package com.zhuoyu.delivery.shared.preview;

import java.util.List;

public final class FilePreviewPolicy {

    private FilePreviewPolicy() {
    }

    private static final List<String> BROWSER_NATIVE_EXTS = List.of(
        ".pdf", ".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".svg"
    );

    private static final List<String> OFFICE_EXTS = List.of(
        ".doc", ".docx", ".wps", ".xls", ".xlsx", ".ppt", ".pptx"
    );

    private static final List<String> CAD_EXTS = List.of(
        ".dwg", ".dxf", ".dgn"
    );

    private static final List<String> BIM_EXTS = List.of(
        ".rvt", ".ifc", ".nwd", ".nwc", ".glb", ".gltf"
    );

    private static final List<String> ARCHIVE_EXTS = List.of(
        ".zip", ".rar", ".7z"
    );

    public static PreviewDecision decide(String ext, String fileKind) {
        String normalized = normalizeExt(ext);
        if (BROWSER_NATIVE_EXTS.contains(normalized)) {
            return new PreviewDecision(
                "AVAILABLE",
                "BROWSER_NATIVE",
                true,
                "NOT_REQUIRED",
                false,
                "该格式可接入浏览器原生预览。",
                List.of("OPEN_PREVIEW_STATUS", "DOWNLOAD_VIA_PLATFORM"),
                false,
                "可在线预览",
                "可通过平台受控预览入口打开。",
                "SUCCESS"
            );
        }
        if (OFFICE_EXTS.contains(normalized)) {
            return new PreviewDecision(
                "NEEDS_CONVERSION",
                "OFFICE_CONVERSION",
                false,
                "NOT_STARTED",
                true,
                "Office 文件需要后续接入离线转换服务后才能在线预览。",
                List.of("REQUEST_CONVERSION", "VIEW_METADATA"),
                false,
                "需 Office 转换",
                "需要接入 Office 转换服务后才能在线预览。原始文件仍可按权限交付或下载。",
                "WARNING"
            );
        }
        if (CAD_EXTS.contains(normalized)) {
            return new PreviewDecision(
                "NEEDS_CONVERSION",
                "CAD_CONVERSION",
                false,
                "NOT_STARTED",
                true,
                "CAD 图纸需要后续接入图纸转换或查看引擎后才能在线预览。",
                List.of("REQUEST_CONVERSION", "VIEW_METADATA"),
                false,
                "需 CAD 转换",
                "需要接入 CAD 图纸转换或查看引擎后才能在线预览。原始文件仍可按权限交付或下载。",
                "WARNING"
            );
        }
        if (BIM_EXTS.contains(normalized) || "MODEL".equalsIgnoreCase(fileKind) || "MODEL_VIEWER".equalsIgnoreCase(fileKind)) {
            return new PreviewDecision(
                "NEEDS_CONVERSION",
                "BIM_LIGHTWEIGHT",
                false,
                "NOT_STARTED",
                true,
                "BIM 模型需要后续接入轻量化转换与模型查看器后才能在线预览。",
                List.of("REQUEST_CONVERSION", "VIEW_METADATA"),
                false,
                "需 BIM 轻量化",
                "需要接入 BIM 轻量化转换后才能在线预览。原始模型文件仍可按权限交付或下载。",
                "WARNING"
            );
        }
        if (ARCHIVE_EXTS.contains(normalized)) {
            return new PreviewDecision(
                "UNSUPPORTED",
                "DOWNLOAD_ONLY",
                false,
                "NOT_SUPPORTED",
                false,
                "归档包暂不支持在线预览，只保留元数据治理与受控访问入口。",
                List.of("VIEW_METADATA"),
                true,
                "仅下载原文件",
                "暂不支持在线预览，可按权限下载原文件。",
                "INFO"
            );
        }
        return new PreviewDecision(
            "UNSUPPORTED",
            "NONE",
            false,
            "NOT_SUPPORTED",
            false,
            "当前格式暂未纳入预览能力范围。",
            List.of("VIEW_METADATA"),
            true,
            "暂不支持预览",
            "当前格式暂不支持在线预览。原始文件仍可按权限交付或下载。",
            "INFO"
        );
    }

    /**
     * Returns a short label for the preview capability based on file extension.
     * Used by both data-steward and work-center for consistent display.
     */
    public static String capabilityLabel(String ext) {
        String normalized = normalizeExt(ext);
        if (BROWSER_NATIVE_EXTS.contains(normalized)) return "NATIVE";
        if (OFFICE_EXTS.contains(normalized)) return "OFFICE_CONVERSION_NEEDED";
        if (CAD_EXTS.contains(normalized) || BIM_EXTS.contains(normalized)) return "CONVERSION_NEEDED";
        return "UNSUPPORTED";
    }

    public static boolean isBrowserNative(String ext) {
        return BROWSER_NATIVE_EXTS.contains(normalizeExt(ext));
    }

    public static boolean needsConversion(String ext) {
        String normalized = normalizeExt(ext);
        return OFFICE_EXTS.contains(normalized) || CAD_EXTS.contains(normalized) || BIM_EXTS.contains(normalized);
    }

    public static boolean isUnsupportedPreview(String ext) {
        String normalized = normalizeExt(ext);
        if (BROWSER_NATIVE_EXTS.contains(normalized)) return false;
        if (OFFICE_EXTS.contains(normalized)) return false;
        if (CAD_EXTS.contains(normalized)) return false;
        if (BIM_EXTS.contains(normalized)) return false;
        return true;
    }

    private static String normalizeExt(String ext) {
        if (ext == null || ext.isBlank()) {
            return "";
        }
        String normalized = ext.trim().toLowerCase();
        return normalized.startsWith(".") ? normalized : "." + normalized;
    }
}
