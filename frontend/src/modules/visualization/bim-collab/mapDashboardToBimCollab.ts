import type { DigitalTwinDashboard, DistributionItem } from '@/modules/visualization/api/visualization';

import type {
  BimChartBar,
  BimCollaborationData,
  BimLegendItem,
  BimMetric,
  BimSceneNode,
  BimTimelineEvent,
  BimTone
} from './types';

const chartColors = [
  'var(--sc-accent)',
  'var(--sc-warning)',
  'var(--sc-ok)',
  'oklch(76% 0.1 250)',
  'oklch(70% 0.14 188)'
];

export function mapDashboardToBimCollab(dashboard: DigitalTwinDashboard): BimCollaborationData {
  const project = dashboard.project;
  const asset = dashboard.assetSummary;
  const delivery = dashboard.deliverySummary;
  const quality = dashboard.qualitySummary;
  const model = dashboard.modelSummary;
  const progressPercent = toPercent(delivery.completionRate);
  const updatedAt = dashboard.activity.latestEventAt
    ?? dashboard.activity.latestAssetUpdatedAt
    ?? latestScanUpdate(dashboard)
    ?? '';

  return {
    projectName: project.name,
    projectCode: project.code,
    projectStage: project.projectStage || '未维护',
    managerName: project.projectManagerName || '待维护',
    engineMode: model.engineMode,
    statusLabel: model.statusLabel,
    viewerAvailable: model.viewerAvailable,
    viewerMessage: dashboard.safetyBoundary.viewerMessage,
    updatedAt: formatDateTime(updatedAt),
    overviewMetrics: [
      { label: '模型数量', value: formatCount(model.modelIntegrationCount), hint: `${formatCount(model.publishedModelCount)} 已发布`, tone: model.viewerAvailable ? 'normal' : 'focus' },
      { label: '管理对象', value: formatCount(model.managedObjectCount), hint: model.actionHint || model.engineMode, tone: 'focus' },
      { label: '项目文件', value: formatCount(asset.fileCount), hint: formatBytes(asset.totalSizeBytes), tone: 'normal' }
    ],
    progressPercent,
    progressMetrics: [
      { label: '已完成', value: `${progressPercent}%`, hint: `${formatCount(delivery.completedCount)} 项`, tone: 'normal' },
      { label: '待审核', value: formatCount(delivery.pendingReviewCount), hint: '交付审核', tone: delivery.pendingReviewCount > 0 ? 'warning' : 'muted' },
      { label: '缺失项', value: formatCount(delivery.missingCount), hint: '应交未交', tone: delivery.missingCount > 0 ? 'danger' : 'muted' }
    ],
    issueMetrics: [
      { label: '风险信号', value: formatCount(quality.riskSignalCount), hint: '质量治理', tone: quality.riskSignalCount > 0 ? 'warning' : 'normal' },
      { label: '失败扫描', value: formatCount(quality.failedScanCount), hint: '扫描任务', tone: quality.failedScanCount > 0 ? 'danger' : 'muted' },
      { label: '缺元数据', value: formatCount(quality.missingChecksumCount + quality.missingDisciplineCount + quality.missingVersionCount), hint: 'checksum/专业/版本', tone: 'focus' }
    ],
    modelBars: modelQuantityBars(dashboard),
    objectShares: objectShareLegend(dashboard),
    issueBars: issueTrendBars(dashboard),
    sceneNodes: sceneNodes(dashboard),
    todoMetrics: [
      { label: '整改待闭环', value: formatCount(delivery.openRectificationCount), hint: '交付闭环', tone: delivery.openRectificationCount > 0 ? 'warning' : 'normal' },
      { label: '审核待处理', value: formatCount(delivery.pendingReviewCount), hint: '工作中心', tone: delivery.pendingReviewCount > 0 ? 'warning' : 'muted' },
      { label: '扫描运行中', value: formatCount(quality.runningScanCount), hint: '资产治理', tone: quality.runningScanCount > 0 ? 'focus' : 'muted' },
      { label: '标准就绪', value: delivery.standardReady ? '是' : '否', hint: delivery.readinessIssues[0] ?? '交付标准', tone: delivery.standardReady ? 'normal' : 'warning' }
    ],
    events: recentEvents(dashboard),
    versionInfo: [
      { label: '模型发布', value: formatCount(model.publishedModelCount), hint: `${formatCount(model.modelIntegrationCount)} 个集成`, tone: 'normal' },
      { label: '轻量化', value: model.lightweightStatus || 'MOCK', hint: model.engineMode, tone: model.viewerAvailable ? 'normal' : 'focus' },
      { label: '更新于', value: formatDateTime(updatedAt) || '暂无', hint: '平台事件', tone: 'muted' }
    ],
    safetyNotes: dashboard.safetyBoundary.guarantees,
    nextActionText: delivery.nextActionText
  };
}

function modelQuantityBars(dashboard: DigitalTwinDashboard): BimChartBar[] {
  const source = dashboard.assetSummary.byDiscipline.length > 0
    ? dashboard.assetSummary.byDiscipline
    : fallbackDistribution(dashboard);
  return source.slice(0, 5).map((item, index) => ({
    label: item.label,
    value: item.count,
    displayValue: formatCount(item.count),
    color: chartColors[index % chartColors.length]
  }));
}

function fallbackDistribution(dashboard: DigitalTwinDashboard): DistributionItem[] {
  const asset = dashboard.assetSummary;
  const modelCount = dashboard.modelSummary.modelIntegrationCount || asset.modelFileCount;
  return [
    { code: 'MODEL', label: '模型', count: modelCount, totalSizeBytes: 0 },
    { code: 'DRAWING', label: '图纸', count: asset.drawingFileCount, totalSizeBytes: 0 },
    { code: 'OBJECT', label: '对象', count: dashboard.modelSummary.managedObjectCount, totalSizeBytes: 0 },
    { code: 'FILE', label: '文件', count: Math.max(0, asset.fileCount - asset.modelFileCount - asset.drawingFileCount), totalSizeBytes: 0 }
  ];
}

function objectShareLegend(dashboard: DigitalTwinDashboard): BimLegendItem[] {
  const objectTypeCounts = new Map<string, number>();
  dashboard.modelSummary.objects.forEach((item) => {
    const key = item.objectType || '未分类';
    objectTypeCounts.set(key, (objectTypeCounts.get(key) ?? 0) + 1);
  });

  const rows = objectTypeCounts.size > 0
    ? Array.from(objectTypeCounts.entries()).map(([label, count]) => ({ label, count }))
    : dashboard.assetSummary.byFileKind.slice(0, 5).map((item) => ({ label: item.label, count: item.count }));

  const total = rows.reduce((sum, item) => sum + item.count, 0) || 1;
  return rows.slice(0, 5).map((item, index) => ({
    label: item.label,
    value: `${Math.round((item.count / total) * 100)}%`,
    color: chartColors[index % chartColors.length]
  }));
}

function issueTrendBars(dashboard: DigitalTwinDashboard): BimChartBar[] {
  const qualityBars = dashboard.qualitySummary.metrics
    .filter((item) => item.count > 0)
    .slice(0, 5)
    .map((item, index) => ({
      label: item.label,
      value: item.count,
      displayValue: formatCount(item.count),
      color: severityColor(item.severity, index)
    }));

  if (qualityBars.length > 0) return qualityBars;

  return [
    { label: '待审核', value: dashboard.qualitySummary.pendingReviewCount, displayValue: formatCount(dashboard.qualitySummary.pendingReviewCount), color: 'var(--sc-warning)' },
    { label: '扫描失败', value: dashboard.qualitySummary.failedScanCount, displayValue: formatCount(dashboard.qualitySummary.failedScanCount), color: 'var(--sc-danger)' },
    { label: '缺专业', value: dashboard.qualitySummary.missingDisciplineCount, displayValue: formatCount(dashboard.qualitySummary.missingDisciplineCount), color: 'var(--sc-accent)' },
    { label: '缺版本', value: dashboard.qualitySummary.missingVersionCount, displayValue: formatCount(dashboard.qualitySummary.missingVersionCount), color: 'var(--sc-ok)' }
  ];
}

function sceneNodes(dashboard: DigitalTwinDashboard): BimSceneNode[] {
  return dashboard.modelSummary.models.slice(0, 12).map((item, index) => ({
    id: `model-${item.id}`,
    modelFileId: item.modelFileId,
    label: item.name,
    meta: `${item.versionNo || '未维护版本'} · ${item.modelFormat || 'UNKNOWN'}`,
    modelFormat: item.modelFormat || 'UNKNOWN',
    versionNo: item.versionNo || '未维护版本',
    integrationStatus: item.status || '状态未知',
    status: modelStatusTone(item.status, index),
    weight: item.componentCount || dashboard.modelSummary.managedObjectCount || 1,
    previewStatus: item.previewStatus || 'UNSUPPORTED',
    previewMode: item.previewMode || 'NONE',
    conversionStatus: item.conversionStatus || 'NOT_SUPPORTED',
    viewerAvailable: Boolean(item.viewerAvailable),
    statusLabel: item.statusLabel || '暂不支持预览',
    actionHint: item.actionHint || dashboard.safetyBoundary.viewerMessage
  }));
}

function recentEvents(dashboard: DigitalTwinDashboard): BimTimelineEvent[] {
  const events = dashboard.activity.recentEvents.slice(0, 4).map((item) => ({
    id: `event-${item.id}`,
    title: item.summary || item.actionCode,
    time: formatDateTime(item.createdAt),
    tone: eventTone(item.actionCode)
  }));
  if (events.length > 0) return events;

  const scanEvents = dashboard.activity.scanTasks.slice(0, 4).map((item) => ({
    id: `scan-${item.id}`,
    title: `扫描任务 ${item.status}，已扫描 ${formatCount(item.totalScanned)} 项`,
    time: formatDateTime(item.updatedAt),
    tone: item.failedCount > 0 ? 'warning' : 'focus'
  }));
  if (scanEvents.length > 0) return scanEvents;

  return [{
    id: 'next-action',
    title: dashboard.deliverySummary.nextActionText,
    time: '当前建议',
    tone: dashboard.deliverySummary.missingCount > 0 ? 'warning' : 'normal'
  }];
}

function latestScanUpdate(dashboard: DigitalTwinDashboard) {
  const updates = dashboard.activity.scanTasks
    .map((item) => item.updatedAt)
    .filter(Boolean)
    .sort();
  return updates.length > 0 ? updates[updates.length - 1] : null;
}

function modelStatusTone(status: string, index: number): Exclude<BimTone, 'muted'> {
  const normalized = status.toUpperCase();
  if (normalized.includes('PUBLISH') || normalized.includes('APPROVED')) return 'normal';
  if (normalized.includes('FAIL') || normalized.includes('REJECT')) return 'danger';
  if (normalized.includes('PENDING') || normalized.includes('DRAFT')) return 'warning';
  return index === 0 ? 'focus' : 'normal';
}

function eventTone(actionCode: string): BimTone {
  const normalized = actionCode.toUpperCase();
  if (normalized.includes('FAIL') || normalized.includes('REJECT')) return 'danger';
  if (normalized.includes('REVIEW') || normalized.includes('RECTIFICATION')) return 'warning';
  if (normalized.includes('PUBLISH') || normalized.includes('APPROVE')) return 'normal';
  return 'focus';
}

function severityColor(severity: string, index: number) {
  const normalized = severity.toUpperCase();
  if (normalized.includes('ERROR') || normalized.includes('HIGH') || normalized.includes('DANGER')) return 'var(--sc-danger)';
  if (normalized.includes('WARN') || normalized.includes('MEDIUM')) return 'var(--sc-warning)';
  return chartColors[index % chartColors.length];
}

function toPercent(rate: number | null | undefined) {
  return Math.max(0, Math.min(100, Math.round((rate ?? 0) * 100)));
}

function formatCount(value: number | null | undefined) {
  return new Intl.NumberFormat('zh-CN').format(value ?? 0);
}

function formatBytes(value: number | null | undefined) {
  const size = value ?? 0;
  if (size >= 1024 ** 4) return `${(size / 1024 ** 4).toFixed(1)} TB`;
  if (size >= 1024 ** 3) return `${(size / 1024 ** 3).toFixed(1)} GB`;
  if (size >= 1024 ** 2) return `${(size / 1024 ** 2).toFixed(1)} MB`;
  if (size >= 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${size} B`;
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}
