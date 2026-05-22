export type BimThemeMode = 'dark' | 'light';

export type BimTone = 'normal' | 'warning' | 'danger' | 'focus' | 'muted';

export interface BimMetric {
  label: string;
  value: string;
  hint?: string;
  tone?: BimTone;
}

export interface BimChartBar {
  label: string;
  value: number;
  displayValue: string;
  color: string;
}

export interface BimLegendItem {
  label: string;
  value: string;
  color: string;
}

export interface BimSceneNode {
  id: string;
  modelFileId: number;
  label: string;
  meta: string;
  modelFormat: string;
  versionNo: string;
  integrationStatus: string;
  status: Exclude<BimTone, 'muted'>;
  weight: number;
  previewStatus: string;
  previewMode: string;
  conversionStatus: string;
  viewerAvailable: boolean;
  statusLabel: string;
  actionHint: string;
}

export interface BimTimelineEvent {
  id: string;
  title: string;
  time: string;
  tone: BimTone;
}

export interface BimCollaborationData {
  projectName: string;
  projectCode: string;
  projectStage: string;
  managerName: string;
  engineMode: string;
  statusLabel: string;
  viewerAvailable: boolean;
  viewerMessage: string;
  updatedAt: string;
  overviewMetrics: BimMetric[];
  progressPercent: number;
  progressMetrics: BimMetric[];
  issueMetrics: BimMetric[];
  modelBars: BimChartBar[];
  objectShares: BimLegendItem[];
  issueBars: BimChartBar[];
  sceneNodes: BimSceneNode[];
  todoMetrics: BimMetric[];
  events: BimTimelineEvent[];
  versionInfo: BimMetric[];
  safetyNotes: string[];
  nextActionText: string;
}
