package com.zhuoyu.delivery.shared.preview;

import java.util.List;

public record PreviewDecision(
    String previewStatus,
    String previewMode,
    Boolean previewAvailable,
    String conversionStatus,
    Boolean conversionRequired,
    String message,
    List<String> supportedActions,
    Boolean downloadOnly,
    String statusLabel,
    String actionHint,
    String riskLevel
) {
}
