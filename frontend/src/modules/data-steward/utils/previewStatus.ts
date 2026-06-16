type TagType = 'success' | 'warning' | 'info' | 'danger';

export interface PreviewStatusLike {
  fileName?: string | null;
  originalName?: string | null;
  fileExt?: string | null;
  fileKind?: string | null;
  previewStatus?: string | null;
  previewMode?: string | null;
  previewAvailable?: boolean | null;
  conversionStatus?: string | null;
  conversionRequired?: boolean | null;
  downloadOnly?: boolean | null;
  statusLabel?: string | null;
  actionHint?: string | null;
  riskLevel?: string | null;
}

const browserNativeExts = new Set(['.pdf', '.png', '.jpg', '.jpeg', '.webp', '.gif', '.bmp', '.svg']);
const officeExts = new Set(['.doc', '.docx', '.wps', '.xls', '.xlsx', '.ppt', '.pptx']);
const cadExts = new Set(['.dwg', '.dxf', '.dgn']);
const bimExts = new Set(['.rvt', '.ifc', '.nwd', '.nwc', '.glb', '.gltf']);
const archiveExts = new Set(['.zip', '.rar', '.7z']);

export function previewFromFileName(fileName: string | null | undefined, fileKind?: string | null): PreviewStatusLike {
  const ext = normalizeExt(fileName?.includes('.') ? fileName.slice(fileName.lastIndexOf('.')) : '');
  return decidePreview(ext, fileKind);
}

export function previewRiskTagType(preview: PreviewStatusLike | null | undefined): TagType {
  const risk = preview?.riskLevel?.toUpperCase();
  if (risk === 'SUCCESS') return 'success';
  if (risk === 'WARNING') return 'warning';
  if (risk === 'DANGER') return 'danger';
  if (preview?.previewStatus === 'AVAILABLE') return 'success';
  if (preview?.previewStatus === 'NEEDS_CONVERSION') return 'warning';
  if (preview?.previewStatus === 'BLOCKED') return 'danger';
  return 'info';
}

export function previewStatusLabel(preview: PreviewStatusLike | null | undefined): string {
  if (preview?.statusLabel) return preview.statusLabel;
  const labels: Record<string, string> = {
    AVAILABLE: '可在线预览',
    NEEDS_CONVERSION: '需要转换',
    UNSUPPORTED: preview?.previewMode === 'DOWNLOAD_ONLY' ? '仅下载原文件' : '暂不支持预览',
    BLOCKED: '文件不可用'
  };
  const status = preview?.previewStatus ?? '';
  return labels[status] ?? (status || '-');
}

export function previewModeLabel(preview: PreviewStatusLike | null | undefined): string {
  const labels: Record<string, string> = {
    BROWSER_NATIVE: '浏览器原生预览',
    OFFICE_CONVERSION: 'Office 转换预览',
    CAD_CONVERSION: 'CAD 转换预览',
    BIM_LIGHTWEIGHT: 'BIM 轻量化预览',
    DOWNLOAD_ONLY: '仅原文件访问',
    NONE: '暂无预览方式'
  };
  const mode = preview?.previewMode ?? '';
  return labels[mode] ?? (mode || '-');
}

export function conversionStatusLabel(preview: PreviewStatusLike | null | undefined): string {
  const labels: Record<string, string> = {
    NOT_REQUIRED: '不需要转换',
    NOT_STARTED: '尚未开始',
    NOT_SUPPORTED: '暂不支持'
  };
  const status = preview?.conversionStatus ?? '';
  return labels[status] ?? (status || '-');
}

export function previewActionHint(preview: PreviewStatusLike | null | undefined): string {
  if (preview?.previewMode === 'BIM_LIGHTWEIGHT') {
    const actionHint = preview.actionHint ?? '';
    if (actionHint && !actionHint.includes('需要接入 BIM 轻量化') && !actionHint.includes('当前仅登记转换占位')) {
      return actionHint;
    }
    return 'BIM 模型需先完成轻量化任务；完成后可通过平台 Viewer 在线预览。原始模型文件仍可按权限交付或下载。';
  }
  if (preview?.actionHint) return preview.actionHint;
  if (preview?.previewMode === 'OFFICE_CONVERSION') {
    return '需要接入 Office 转换服务后才能在线预览。原始文件仍可按权限交付或下载。';
  }
  if (preview?.previewMode === 'CAD_CONVERSION') {
    return '需要接入 CAD 图纸转换或查看引擎后才能在线预览。原始文件仍可按权限交付或下载。';
  }
  if (preview?.previewMode === 'DOWNLOAD_ONLY') {
    return '暂不支持在线预览，可按权限下载原文件。';
  }
  if (preview?.previewStatus === 'UNSUPPORTED') {
    return '当前格式暂不支持在线预览。原始文件仍可按权限交付或下载。';
  }
  if (preview?.previewStatus === 'AVAILABLE') {
    return '可通过平台受控预览入口打开。';
  }
  return '当前仅判断在线预览能力，不读取文件正文。';
}

export function previewOnlineStateText(preview: PreviewStatusLike | null | undefined): string {
  if (preview?.previewAvailable) return '可通过在线预览入口打开';
  if (preview?.conversionRequired) return '在线预览需要后续转换能力';
  if (preview?.downloadOnly) return '仅保留原文件访问与交付';
  return '暂不提供在线预览';
}

export function previewActionLabel(value: string): string {
  const labels: Record<string, string> = {
    OPEN_PREVIEW_STATUS: '查看预览状态',
    DOWNLOAD_VIA_PLATFORM: '平台受控下载',
    REQUEST_CONVERSION: '后续创建转换任务',
    VIEW_METADATA: '查看元数据',
    VIEW_AUDIT: '查看审计',
    FIX_METADATA: '补齐元数据'
  };
  return labels[value] ?? value;
}

function decidePreview(ext: string, fileKind?: string | null): PreviewStatusLike {
  const normalized = normalizeExt(ext);
  const normalizedKind = fileKind?.toUpperCase();
  if (browserNativeExts.has(normalized)) {
    return {
      fileExt: normalized,
      fileKind: normalizedKind,
      previewStatus: 'AVAILABLE',
      previewMode: 'BROWSER_NATIVE',
      previewAvailable: true,
      conversionStatus: 'NOT_REQUIRED',
      conversionRequired: false,
      downloadOnly: false,
      statusLabel: '可在线预览',
      actionHint: '可通过平台受控预览入口打开。',
      riskLevel: 'SUCCESS'
    };
  }
  if (officeExts.has(normalized)) {
    return conversionPreview(normalized, normalizedKind, 'OFFICE_CONVERSION', '需 Office 转换', '需要接入 Office 转换服务后才能在线预览。原始文件仍可按权限交付或下载。');
  }
  if (cadExts.has(normalized)) {
    return conversionPreview(normalized, normalizedKind, 'CAD_CONVERSION', '需 CAD 转换', '需要接入 CAD 图纸转换或查看引擎后才能在线预览。原始文件仍可按权限交付或下载。');
  }
  if (bimExts.has(normalized) || normalizedKind === 'MODEL' || normalizedKind === 'MODEL_VIEWER') {
    return conversionPreview(normalized, normalizedKind, 'BIM_LIGHTWEIGHT', '待 BIM 轻量化', 'BIM 模型需先完成轻量化任务；完成后可通过平台 Viewer 在线预览。原始模型文件仍可按权限交付或下载。');
  }
  if (archiveExts.has(normalized)) {
    return {
      fileExt: normalized,
      fileKind: normalizedKind,
      previewStatus: 'UNSUPPORTED',
      previewMode: 'DOWNLOAD_ONLY',
      previewAvailable: false,
      conversionStatus: 'NOT_SUPPORTED',
      conversionRequired: false,
      downloadOnly: true,
      statusLabel: '仅下载原文件',
      actionHint: '暂不支持在线预览，可按权限下载原文件。',
      riskLevel: 'INFO'
    };
  }
  return {
    fileExt: normalized,
    fileKind: normalizedKind,
    previewStatus: 'UNSUPPORTED',
    previewMode: 'NONE',
    previewAvailable: false,
    conversionStatus: 'NOT_SUPPORTED',
    conversionRequired: false,
    downloadOnly: true,
    statusLabel: '暂不支持预览',
    actionHint: '当前格式暂不支持在线预览。原始文件仍可按权限交付或下载。',
    riskLevel: 'INFO'
  };
}

function conversionPreview(
  fileExt: string,
  fileKind: string | null | undefined,
  previewMode: string,
  statusLabel: string,
  actionHint: string
): PreviewStatusLike {
  return {
    fileExt,
    fileKind,
    previewStatus: 'NEEDS_CONVERSION',
    previewMode,
    previewAvailable: false,
    conversionStatus: 'NOT_STARTED',
    conversionRequired: true,
    downloadOnly: false,
    statusLabel,
    actionHint,
    riskLevel: 'WARNING'
  };
}

function normalizeExt(value: string | null | undefined): string {
  if (!value) return '';
  const trimmed = value.trim().toLowerCase();
  return trimmed.startsWith('.') ? trimmed : `.${trimmed}`;
}
