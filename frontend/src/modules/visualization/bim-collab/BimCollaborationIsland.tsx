import BimPreviewScene from './BimPreviewScene';
import { useEffect, useMemo, useRef, useState } from 'react';
import type { ChangeEvent, CSSProperties, ReactNode } from 'react';
import type {
  FileOwnershipCoverage,
  FileOwnershipTree,
  FileOwnershipTreeNode
} from '@/modules/data-steward/api/dataSteward';
import type {
  SectionNode,
  StandardStatus
} from '@/modules/master-data/api/masterData';
import type {
  BimChartBar,
  BimCollaborationData,
  BimEmbeddedPreviewModel,
  BimLegendItem,
  BimLightweightSummary,
  BimMetric,
  BimSceneNode,
  BimThemeMode,
  BimWorkItem
} from './types';

import './BimCollaborationIsland.css';

interface BimCollaborationIslandProps {
  data: BimCollaborationData;
  embeddedPreviewModels?: BimEmbeddedPreviewModel[];
  lightweightSummary?: BimLightweightSummary;
  standardStatus?: StandardStatus | null;
  sectionTree?: SectionNode[];
  ownershipCoverage?: FileOwnershipCoverage | null;
  ownershipTree?: FileOwnershipTree | null;
  theme: BimThemeMode;
}

type CssVarStyle = CSSProperties & Record<`--${string}`, string | number>;
type ModuleKey = 'overview' | 'bim' | 'tree' | 'assets' | 'spaces' | 'alarms' | 'energy' | 'systems' | 'workorders';
type ViewerCommand =
  | 'main'
  | 'blow'
  | 'model-visible-toggle'
  | 'feature-visible-toggle'
  | 'pick'
  | 'locate'
  | 'hide'
  | 'show-hidden'
  | 'measure-distance'
  | 'measure-area'
  | 'measure-feature-area'
  | 'measure-feature-volume'
  | 'roam-start'
  | 'roam-pause'
  | 'roam-stop'
  | 'clip'
  | 'screenshot'
  | 'measure-clear'
  | 'clear-selection'
  | 'clear';

type ViewerCommandPayload = {
  mode?: 'SPHERE' | 'LINEAR';
  amount?: number;
  durationMs?: number;
};

type ViewerSelectedFeature = {
  featureId: string;
  revitId?: string;
  batchId?: unknown;
  modelFileId?: number | string | null;
  fileName?: string;
};
type ViewerCapabilities = {
  componentIndexAvailable: boolean;
  reason: string;
};
type BimAction = {
  label: string;
  href?: string;
  onClick?: () => void;
  tone?: 'primary' | 'default';
};

const moduleItems: Array<{ key: ModuleKey; label: string }> = [
  { key: 'overview', label: '综合驾驶舱' },
  { key: 'bim', label: 'BIM 场景' },
  { key: 'tree', label: '工程树' },
  { key: 'assets', label: '设备设施' },
  { key: 'spaces', label: '房屋空间' },
  { key: 'alarms', label: '告警监控' },
  { key: 'energy', label: '轻量化模型列表' },
  { key: 'systems', label: '专业系统' },
  { key: 'workorders', label: '工单巡检保养' }
];

export default function BimCollaborationIsland({
  data,
  embeddedPreviewModels = [],
  lightweightSummary,
  standardStatus,
  sectionTree = [],
  ownershipCoverage,
  ownershipTree,
  theme
}: BimCollaborationIslandProps) {
  const [activeModule, setActiveModule] = useState<ModuleKey>('overview');
  const [activeModelIndex, setActiveModelIndex] = useState(0);
  const [selectedFeature, setSelectedFeature] = useState<ViewerSelectedFeature | null>(null);
  const [measurementText, setMeasurementText] = useState('');
  const [showExplosionPanel, setShowExplosionPanel] = useState(false);
  const [explosionMode, setExplosionMode] = useState<'SPHERE' | 'LINEAR'>('SPHERE');
  const [explosionAmount, setExplosionAmount] = useState(0.45);
  const [viewerCapabilities, setViewerCapabilities] = useState<ViewerCapabilities>({
    componentIndexAvailable: false,
    reason: '当前轻量化产物未确认包含构件索引。'
  });
  const viewerFrameRef = useRef<HTMLIFrameElement | null>(null);
  const hasEmbeddedPreview = embeddedPreviewModels.length > 0;
  const shouldFocusViewer = hasEmbeddedPreview && activeModule === 'bim';
  const previewModels = embeddedPreviewModels.length > 0 ? embeddedPreviewModels : data.sceneNodes;
  const modelCount = previewModels.length;
  const activeModel = previewModels[activeModelIndex] ?? null;
  const autoRotatePreview = activeModule === 'overview';
  const activeSceneNodes = useMemo<BimSceneNode[]>(() => (
    activeModel && !('frameUrl' in activeModel) ? [activeModel] : []
  ), [activeModel]);
  const activeViewerAvailable = Boolean(activeModel?.viewerAvailable && (data.viewerAvailable || ('frameUrl' in activeModel)));
  const activeViewerMessage = activeModel?.actionHint || data.viewerMessage;
  const componentInteractionReady = activeViewerAvailable && viewerCapabilities.componentIndexAvailable;
  const componentInteractionReason = viewerCapabilities.reason || '当前轻量化产物未包含构件索引，暂不支持构件拾取、构件隐藏/定位和模型爆炸。';
  const riskWorkItems: BimWorkItem[] = data.operations.workItems;
  const sectionNodes = useMemo(() => flattenSectionNodes(sectionTree), [sectionTree]);
  const ownershipNodes = useMemo(() => flattenOwnershipNodes(ownershipTree?.nodes ?? []), [ownershipTree]);
  const topOwnershipNodes = useMemo(() => (
    ownershipNodes
      .filter((item) => item.fileCount > 0 || item.deliveryRequiredCount > 0)
      .sort((left, right) => (right.fileCount + right.deliveryRequiredCount) - (left.fileCount + left.deliveryRequiredCount))
      .slice(0, 8)
  ), [ownershipNodes]);
  const ownershipRate = Math.round(Number(ownershipCoverage?.assignmentCoverageRate ?? 0));
  const masterOverviewMetrics: BimMetric[] = [
    {
      label: '工程部位',
      value: formatShort(standardStatus?.sectionNodeCount ?? sectionNodes.length),
      hint: standardStatus?.hasSectionTree ? '已接入工程主数据' : '待维护工程树',
      tone: standardStatus?.hasSectionTree ? 'normal' : 'warning'
    },
    {
      label: '交付标准',
      value: standardStatus?.deliverableStandardReady ? '已确认' : '待确认',
      hint: `${formatShort(standardStatus?.deliverableDefinitionCount ?? 0)} 定义 / ${formatShort(standardStatus?.deliverableTypeCount ?? 0)} 类型`,
      tone: standardStatus?.deliverableStandardReady ? 'normal' : 'warning'
    },
    {
      label: '文件归属',
      value: `${ownershipRate}%`,
      hint: `${formatShort(ownershipCoverage?.confirmedFiles ?? 0)} 已确认 / ${formatShort(ownershipCoverage?.unassignedFiles ?? 0)} 未归属`,
      tone: ownershipRate >= 80 ? 'normal' : 'focus'
    }
  ];
  const modelSummary = lightweightSummary ?? {
    totalModelFiles: data.sceneNodes.length,
    readyCount: embeddedPreviewModels.length,
    pendingCount: Math.max(data.sceneNodes.length - embeddedPreviewModels.length, 0),
    failedCount: 0,
    readyModels: embeddedPreviewModels
  };
  useEffect(() => {
    setActiveModelIndex(0);
  }, [data.projectCode, previewModels.map((item) => item.id).join('|')]);

  useEffect(() => {
    setSelectedFeature(null);
    setMeasurementText('');
    setShowExplosionPanel(false);
    setViewerCapabilities({
      componentIndexAvailable: false,
      reason: '正在等待 Viewer 返回构件级能力。'
    });
  }, [activeModel?.id]);

  useEffect(() => {
    function handleViewerEvent(event: MessageEvent) {
      if (event.origin !== window.location.origin) return;
      const message = event.data as { type?: string; event?: string; payload?: Record<string, unknown> } | null;
      if (!message || message.type !== 'glandar-viewer-event') return;
      if (message.event === 'feature-selected') {
        const featureId = String(message.payload?.featureId || '');
        setSelectedFeature(featureId ? {
          featureId,
          revitId: message.payload?.revitId ? String(message.payload.revitId) : '',
          batchId: message.payload?.batchId,
          modelFileId: message.payload?.modelFileId as number | string | null,
          fileName: message.payload?.fileName ? String(message.payload.fileName) : activeModel?.label
        } : null);
      }
      if (message.event === 'feature-hidden') {
        setSelectedFeature(null);
      }
      if (message.event === 'measurement') {
        setMeasurementText(message.payload?.label ? String(message.payload.label) : '');
      }
      if (message.event === 'capabilities') {
        setViewerCapabilities({
          componentIndexAvailable: message.payload?.componentIndexAvailable === true,
          reason: message.payload?.reason ? String(message.payload.reason) : ''
        });
      }
    }
    window.addEventListener('message', handleViewerEvent);
    return () => window.removeEventListener('message', handleViewerEvent);
  }, [activeModel?.label]);

  function selectModel(event: ChangeEvent<HTMLSelectElement>) {
    const nextIndex = previewModels.findIndex((item) => item.id === event.currentTarget.value);
    if (nextIndex >= 0) {
      setActiveModelIndex(nextIndex);
    }
  }

  function openPreviewModel(modelId: string) {
    const nextIndex = previewModels.findIndex((item) => item.id === modelId);
    if (nextIndex >= 0) {
      setActiveModelIndex(nextIndex);
      setActiveModule('bim');
    }
  }

  function sendViewerCommand(action: ViewerCommand, payload?: ViewerCommandPayload) {
    viewerFrameRef.current?.contentWindow?.postMessage({
      type: 'glandar-viewer-command',
      action,
      payload,
      requestId: `${Date.now()}-${action}`
    }, window.location.origin);
  }

  function projectHref(path: string) {
    return `/data-steward/assets/${data.projectId}${path}`;
  }

  function renderActionPanel(title: string, description: string, actions: BimAction[]) {
    return (
      <Panel title={title} meta="真实平台入口">
        <ActionHint description={description} actions={actions} />
      </Panel>
    );
  }

  function renderModelCanvas() {
    if (activeModel && 'frameUrl' in activeModel && activeModel.frameUrl) {
      const frameUrl = withViewerQuery(activeModel.frameUrl, {
        autoRotate: autoRotatePreview ? '1' : '0',
        theme
      });
      return (
        <div className="sc-bim-embedded-viewer">
          <iframe
            ref={viewerFrameRef}
            title={`${activeModel.label} 葛兰岱尔模型预览`}
            src={frameUrl}
            allow="fullscreen"
          />
        </div>
      );
    }

    return (
      <BimPreviewScene
        nodes={activeSceneNodes}
        viewerAvailable={activeViewerAvailable}
        viewerMessage={activeViewerMessage}
      />
    );
  }

  function renderSceneModule() {
    return (
      <div className={shouldFocusViewer ? 'sc-bim-layout is-viewer-focus' : 'sc-bim-layout'}>
        {!shouldFocusViewer ? (
          <aside className="sc-bim-stack" aria-label="协同数据概览">
            <Panel title="项目概览" meta={data.managerName}>
              <MetricList metrics={masterOverviewMetrics} />
            </Panel>

            <Panel title="协同进度">
              <div className="sc-bim-progress-block">
                <Donut value={data.progressPercent} color="var(--sc-accent)" label={`${data.progressPercent}%`} />
                <MetricList metrics={data.progressMetrics} compact />
              </div>
            </Panel>

            <Panel title="问题统计">
              <MetricList metrics={data.issueMetrics} />
            </Panel>
          </aside>
        ) : null}

        <main className="sc-bim-stage" aria-label="项目 BIM 协同视图">
          <header className="sc-bim-stage__top">
            <div>
              <h2>{data.projectName}</h2>
              <span>{activeModel ? `${activeModel.label} / ${activeModel.meta}` : '当前项目暂时无模型预览'}</span>
            </div>
            <div className="sc-bim-carousel" aria-label="项目模型轮播">
              <span>{modelCount > 0 ? '默认全楼层优先 · 手动切换' : '暂无模型'}</span>
            </div>
          </header>

          <div className="sc-bim-model-selector" aria-label="展示模型选择">
            <label htmlFor="sc-bim-active-model">展示模型</label>
            <select
              id="sc-bim-active-model"
              disabled={modelCount === 0}
              value={activeModel?.id ?? ''}
              onChange={selectModel}
            >
              {modelCount === 0 ? (
                <option value="">暂无模型预览</option>
              ) : previewModels.map((item, index) => (
                <option key={item.id} value={item.id}>
                  {index + 1}. {item.label}
                </option>
              ))}
            </select>
            <div className="sc-bim-model-status" aria-label="模型预览状态">
              <span>{activeModel?.modelFormat ?? 'NO MODEL'}</span>
              <span>{activeModel?.statusLabel ?? '暂无模型预览'}</span>
              <span>{activeModel?.conversionStatus ?? 'NOT_STARTED'}</span>
            </div>
            <p>{activeModel?.actionHint ?? '当前项目没有可选择的模型，完成模型集成后将在这里按项目模型展示。'}</p>
          </div>

          <div className="sc-bim-stage__canvas">
            {renderModelCanvas()}
            {selectedFeature ? (
              <aside className="sc-bim-feature-card" aria-label="选中构件信息">
                <header>
                  <span>选中构件</span>
                  <button type="button" onClick={() => sendViewerCommand('clear-selection')}>清除</button>
                </header>
                <strong>{selectedFeature.featureId}</strong>
                <dl>
                  <div>
                    <dt>Revit ID</dt>
                    <dd>{selectedFeature.revitId || '引擎未返回'}</dd>
                  </div>
                  <div>
                    <dt>文件 ID</dt>
                    <dd>{selectedFeature.modelFileId || activeModel?.modelFileId || '-'}</dd>
                  </div>
                  <div>
                    <dt>模型</dt>
                    <dd>{selectedFeature.fileName || activeModel?.label || '-'}</dd>
                  </div>
                  <div>
                    <dt>状态</dt>
                    <dd>已拾取，可定位、隐藏和测量</dd>
                  </div>
                </dl>
                <p>当前显示引擎返回的构件基础信息；完整 BIM 属性等待葛兰岱尔属性接口或后续构件索引接入。</p>
                <div>
                  <button type="button" onClick={() => sendViewerCommand('locate')}>定位构件</button>
                  <button type="button" onClick={() => sendViewerCommand('feature-visible-toggle')}>隐藏构件</button>
                  <button type="button" onClick={() => sendViewerCommand('measure-feature-area')}>表面积</button>
                  <button type="button" onClick={() => sendViewerCommand('measure-feature-volume')}>体积</button>
                </div>
              </aside>
            ) : null}
            {measurementText ? (
              <div className="sc-bim-measurement-result">
                <span>测量结果</span>
                <strong>{measurementText}</strong>
              </div>
            ) : null}
            {activeViewerAvailable && !componentInteractionReady ? (
              <aside className="sc-bim-capability-card" aria-label="构件级能力提示">
                <strong>当前仅支持整体模型预览</strong>
                <p>{componentInteractionReason}</p>
                <span>可继续使用缩放、拖动、主视角、整模显示和距离/面积测量；构件拾取、爆炸、构件隐藏/定位等待葛兰岱尔输出构件索引。</span>
              </aside>
            ) : null}
          </div>

          <div className="sc-bim-toolbar" aria-label="模型工具栏">
            {[
              { label: '主视角', action: 'main' },
              { label: '爆炸', action: 'blow-panel', needsComponentIndex: true },
              { label: '显示', action: 'model-visible-toggle' },
              { label: '隐藏', action: 'feature-visible-toggle', needsFeature: true, needsComponentIndex: true },
              { label: '定位', action: 'locate', needsFeature: true, needsComponentIndex: true },
              { label: '距离', action: 'measure-distance' },
              { label: '面积', action: 'measure-area' },
              { label: '清除', action: 'measure-clear' }
            ].map((item) => (
              <button
                key={item.label}
                disabled={!activeViewerAvailable || Boolean(item.needsComponentIndex && !componentInteractionReady) || Boolean(item.needsFeature && !selectedFeature)}
                title={!activeViewerAvailable ? '真实 BIM Viewer 待后续引擎接入' : item.needsComponentIndex && !componentInteractionReady ? componentInteractionReason : item.label}
                onClick={() => {
                  if (item.action === 'blow-panel') {
                    setShowExplosionPanel((visible) => !visible);
                    return;
                  }
                  sendViewerCommand(item.action as ViewerCommand);
                }}
                type="button"
              >
                {item.label}
              </button>
            ))}
            {showExplosionPanel ? (
              <div className="sc-bim-explosion-panel" aria-label="模型爆炸面板">
                <div>
                  <span>模型爆炸</span>
                  <button type="button" onClick={() => setShowExplosionPanel(false)}>收起</button>
                </div>
                <label>
                  类型
                  <select value={explosionMode} onChange={(event) => setExplosionMode(event.currentTarget.value as 'SPHERE' | 'LINEAR')}>
                    <option value="SPHERE">球面爆炸</option>
                    <option value="LINEAR">线性爆炸</option>
                  </select>
                </label>
                <label>
                  幅度 {explosionAmount.toFixed(2)}
                  <input
                    max="1"
                    min="0"
                    step="0.01"
                    type="range"
                    value={explosionAmount}
                    onChange={(event) => setExplosionAmount(Number(event.currentTarget.value))}
                  />
                </label>
                <div>
                  <button
                    disabled={!componentInteractionReady}
                    type="button"
                    onClick={() => sendViewerCommand('blow', { mode: explosionMode, amount: explosionAmount, durationMs: 720 })}
                  >
                    应用
                  </button>
                  <button
                    disabled={!componentInteractionReady}
                    type="button"
                    onClick={() => {
                      setExplosionAmount(0);
                      sendViewerCommand('blow', { mode: explosionMode, amount: 0, durationMs: 720 });
                    }}
                  >
                    复原
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </main>

        {!shouldFocusViewer ? (
          <>
            <aside className="sc-bim-stack" aria-label="模型与质量统计">
              <Panel title="模型量统计" meta="平台资产">
                <BarChart bars={data.modelBars} />
              </Panel>

              <Panel title="构件分类统计">
                <div className="sc-bim-legend-block">
                  <Donut value={shareDonutValue(data.objectShares)} color="var(--sc-warning)" label={data.overviewMetrics[1]?.value ?? '0'} />
                  <LegendList items={data.objectShares} />
                </div>
              </Panel>

              <Panel title="问题趋势">
                <BarChart bars={data.issueBars} />
              </Panel>
            </aside>

            <footer className="sc-bim-bottom">
              <section>
                <h3>待办事项</h3>
                <MetricList metrics={data.todoMetrics.slice(0, 3)} compact />
                <ActionButtons actions={[
                  { label: '文件管理', href: projectHref('?tab=files'), tone: 'primary' },
                  { label: '交付状态', href: projectHref('/work/document-delivery') },
                  { label: '数据质量', href: '/data-steward/quality' }
                ]} />
              </section>
              <section>
                <h3>最近动态</h3>
                <div className="sc-bim-event-list">
                  {data.events.map((item) => (
                    <article key={item.id} className={`is-${item.tone}`}>
                      <span>{item.title}</span>
                      <small>{item.time}</small>
                    </article>
                  ))}
                </div>
              </section>
              <section>
                <h3>版本管理</h3>
                <MetricList metrics={data.versionInfo} compact />
              </section>
            </footer>
          </>
        ) : null}
      </div>
    );
  }

  function renderTreeModule() {
    return (
      <ModulePageShell
        title="工程树视图"
        description="把 BIM 协同接回平台工程主数据：部位树、文件归属、交付标准和轻量化模型都围绕工程节点展开。"
        metrics={[
          {
            label: '工程部位',
            value: formatShort(standardStatus?.sectionNodeCount ?? sectionNodes.length),
            hint: standardStatus?.hasSectionTree ? '部位树已维护' : '需要维护部位树',
            tone: standardStatus?.hasSectionTree ? 'normal' : 'warning'
          },
          {
            label: '文件归属',
            value: `${ownershipRate}%`,
            hint: `${formatShort(ownershipCoverage?.assignedFiles ?? 0)} 已归属`,
            tone: ownershipRate >= 80 ? 'normal' : 'focus'
          },
          {
            label: '交付标准',
            value: standardStatus?.deliverableStandardReady ? '已就绪' : '待确认',
            hint: `${formatShort(standardStatus?.deliverableTypeCount ?? 0)} 个交付类型`,
            tone: standardStatus?.deliverableStandardReady ? 'normal' : 'warning'
          }
        ]}
      >
        <Panel title="工程树节点">
          {topOwnershipNodes.length > 0
            ? <OwnershipNodeRows nodes={topOwnershipNodes} />
            : <EmptyState title="工程树还没有文件归属" description="当前项目已有部位或资产，但文件还没有归属到具体工程节点。" actions={[{ label: '去文件归属治理', href: projectHref('?tab=ownership'), tone: 'primary' }]} />}
        </Panel>
        <Panel title="专业字段">
          {data.modelBars.length > 0
            ? <BarChart bars={data.modelBars} />
            : <EmptyState title="暂无专业统计" description="文件资产或管理对象还没有维护专业字段。" actions={[{ label: '去文件管理补专业', href: projectHref('?tab=files'), tone: 'primary' }]} />}
        </Panel>
        <Panel title="工程主数据状态">
          <Rows rows={[
            {
              title: '部位树',
              meta: standardStatus?.hasSectionTree ? '来自工程主数据模块' : '尚未形成可用工程树',
              value: formatShort(standardStatus?.sectionNodeCount ?? sectionNodes.length)
            },
            {
              title: '节点类型',
              meta: standardStatus?.nodeTypesLocked ? '已锁定，可驱动交付' : '待配置或待锁定',
              value: formatShort(standardStatus?.nodeTypeCount ?? 0)
            },
            {
              title: '交付物标准',
              meta: standardStatus?.deliverableStandardReady ? '文档/图纸交付可用' : '标准未完全就绪',
              value: formatShort(standardStatus?.deliverableDefinitionCount ?? 0)
            },
            {
              title: '未归属文件',
              meta: '应继续治理到工程树节点',
              value: formatShort(ownershipCoverage?.unassignedFiles ?? 0)
            }
          ]} />
        </Panel>
        {renderActionPanel('可执行动作', 'BIM 协同只消费平台已经确认的工程主数据和文件归属，不另建一套架空模型结构。', [
          { label: '工程部位树', href: projectHref('/master-data/sections'), tone: 'primary' },
          { label: '文件归属治理', href: projectHref('?tab=ownership') },
          { label: '交付物标准', href: projectHref('/master-data/deliverable-standard') }
        ])}
      </ModulePageShell>
    );
  }

  function renderAssetsModule() {
    return (
      <ModulePageShell
        title="设备设施"
        description="展示当前项目管理对象台账中的设备对象、专业分布和工程部位绑定状态。"
        metrics={[
          { label: '设备对象', value: formatShort(data.operations.equipmentCount), hint: 'EQUIPMENT 管理对象', tone: 'normal' },
          { label: '已绑定部位', value: formatShort(data.operations.linkedObjectCount), hint: '关联工程部位', tone: 'focus' },
          { label: '未绑定对象', value: formatShort(data.operations.unlinkedObjectCount), hint: '待补齐位置关系', tone: data.operations.unlinkedObjectCount > 0 ? 'warning' : 'muted' }
        ]}
      >
        <Panel title="对象类型分布">
          {data.operations.byObjectType.length > 0
            ? <BarChart bars={data.operations.byObjectType} />
            : <EmptyState title="暂无设备设施数据" description="当前项目还没有登记设备、系统或空间管理对象。" actions={[{ label: '去管理对象登记', href: projectHref('/data-steward/objects'), tone: 'primary' }]} />}
        </Panel>
        <Panel title="专业分布">
          {data.operations.byDiscipline.length > 0
            ? <BarChart bars={data.operations.byDiscipline} />
            : <EmptyState title="暂无专业分布" description="管理对象未维护专业字段，可先使用文件资产专业字段治理。" actions={[{ label: '补充对象专业', href: projectHref('/data-steward/objects'), tone: 'primary' }]} />}
        </Panel>
        {renderActionPanel('可执行动作', '设备设施来自平台管理对象台账，必须绑定工程部位后才进入 BIM 协同视图。', [
          { label: '管理对象', href: projectHref('/data-steward/objects'), tone: 'primary' },
          { label: '工程部位树', href: projectHref('/master-data/sections') },
          { label: '文件归属治理', href: projectHref('?tab=ownership') }
        ])}
      </ModulePageShell>
    );
  }

  function renderSpacesModule() {
    const spaces = data.operations.spaces.slice(0, 8);
    return (
      <ModulePageShell
        title="房屋空间"
        description="展示当前项目工程部位树和空间类对象，不虚构院区、楼栋或房间。"
        metrics={[
          { label: '工程部位', value: formatShort(data.operations.sectionNodeCount), hint: '平台主数据', tone: 'normal' },
          { label: '空间对象', value: formatShort(data.operations.spaceObjectCount), hint: 'SPACE 管理对象', tone: 'focus' },
          { label: '部位绑定', value: formatShort(data.operations.linkedObjectCount), hint: '对象关联位置', tone: 'muted' }
        ]}
      >
        <Panel title="工程部位列表">
          {spaces.length > 0
            ? <Rows rows={spaces.map((item) => ({ title: item.name, meta: `${item.code} · ${item.level} 级`, value: `${item.objectCount} 对象` }))} />
            : <EmptyState title="暂无房屋空间数据" description="当前项目未维护工程部位树或空间对象。" actions={[{ label: '维护工程部位树', href: projectHref('/master-data/sections'), tone: 'primary' }]} />}
        </Panel>
        <Panel title="空间关联对象">
          {spaces.some((item) => item.objectCount > 0)
            ? <Rows rows={spaces.filter((item) => item.objectCount > 0).map((item) => ({ title: item.name, meta: `${item.equipmentCount} 设备 / ${item.systemCount} 系统`, value: `${item.objectCount} 个` }))} />
            : <EmptyState title="暂无空间绑定" description="对象台账还没有绑定到具体工程部位。" actions={[{ label: '去管理对象绑定', href: projectHref('/data-steward/objects'), tone: 'primary' }]} />}
        </Panel>
        {renderActionPanel('可执行动作', '房屋空间先由工程部位树和 SPACE 管理对象支撑，不使用演示楼层或虚构房间。', [
          { label: '维护部位树', href: projectHref('/master-data/sections'), tone: 'primary' },
          { label: '管理空间对象', href: projectHref('/data-steward/objects') },
          { label: '查看文件归属', href: projectHref('?tab=ownership') }
        ])}
      </ModulePageShell>
    );
  }

  function renderAlarmsModule() {
    return (
      <ModulePageShell
        title="告警监控"
        description="当前平台未接入真实 IoT 告警，先展示交付质量风险、扫描失败和整改闭环。"
        metrics={[
          { label: '质量风险', value: data.issueMetrics[0]?.value ?? '0', hint: '平台治理信号', tone: 'warning' },
          { label: '扫描失败', value: data.issueMetrics[1]?.value ?? '0', hint: '资产治理', tone: 'danger' },
          { label: '整改待闭环', value: data.todoMetrics[0]?.value ?? '0', hint: '交付闭环', tone: 'focus' }
        ]}
      >
        <Panel title="风险趋势">
          <BarChart bars={data.issueBars} />
        </Panel>
        <Panel title="待处理事项">
          {riskWorkItems.length > 0
            ? <WorkItemList items={riskWorkItems} />
            : <EmptyState title="暂无待处理风险" description="当前项目没有未关闭整改或失败扫描任务。" actions={[{ label: '查看数据质量', href: '/data-steward/quality', tone: 'primary' }]} />}
        </Panel>
        {renderActionPanel('可执行动作', '当前告警监控映射为平台真实风险：质量问题、扫描失败、待审核和整改闭环。', [
          { label: '数据质量总览', href: '/data-steward/quality', tone: 'primary' },
          { label: '扫描任务', href: '/data-steward/scans' },
          { label: '整改闭环', href: projectHref('/work/rectifications') }
        ])}
      </ModulePageShell>
    );
  }

  function renderEnergyModule() {
    return (
      <ModulePageShell
        title="轻量化模型列表"
        description="集中展示当前项目已完成葛兰岱尔轻量化的模型，可直接打开预览或回到文件管理定位资产。"
        metrics={[
          { label: '模型文件', value: formatShort(modelSummary.totalModelFiles), hint: '当前项目模型资产', tone: 'normal' },
          { label: '已轻量化', value: formatShort(modelSummary.readyCount), hint: '可直接预览', tone: 'focus' },
          { label: '待轻量化', value: formatShort(modelSummary.pendingCount), hint: modelSummary.failedCount > 0 ? `${formatShort(modelSummary.failedCount)} 个失败` : '等待转换', tone: modelSummary.pendingCount > 0 ? 'warning' : 'muted' }
        ]}
      >
        <Panel title="已轻量化模型">
          {modelSummary.readyModels.length > 0
            ? <LightweightModelList items={modelSummary.readyModels} onPreview={openPreviewModel} />
            : <EmptyState title="暂无可预览模型" description="当前项目还没有完成轻量化的模型。" actions={[{ label: '查看模型文件', href: projectHref('?tab=files&fileKind=MODEL'), tone: 'primary' }]} />}
        </Panel>
        <Panel title="轻量化状态">
          <Rows rows={[
            { title: '模型总数', meta: '平台资产目录', value: formatShort(modelSummary.totalModelFiles) },
            { title: '已轻量化', meta: '葛兰岱尔 READY', value: formatShort(modelSummary.readyCount) },
            { title: '待轻量化', meta: '未生成 Viewer 入口', value: formatShort(modelSummary.pendingCount) },
            { title: '转换失败', meta: '需排查引擎或文件', value: formatShort(modelSummary.failedCount) }
          ]} />
        </Panel>
        {renderActionPanel('可执行动作', '轻量化列表只展示平台已登记并可预览的模型，可打开预览或回到文件管理定位资产。', [
          { label: '进入 BIM 场景', onClick: () => setActiveModule('bim'), tone: 'primary' },
          { label: '模型文件管理', href: projectHref('?tab=files&fileKind=MODEL') },
          { label: '模型集成治理', href: projectHref('/data-steward/models') }
        ])}
      </ModulePageShell>
    );
  }

  function renderSystemsModule() {
    const systems = data.operations.systems.slice(0, 10);
    return (
      <ModulePageShell
        title="专业系统"
        description="展示当前项目登记的 SYSTEM 管理对象和关联设备数量。"
        metrics={[
          { label: '系统对象', value: formatShort(data.operations.systemObjectCount), hint: 'SYSTEM 管理对象', tone: 'normal' },
          { label: '设备对象', value: formatShort(data.operations.equipmentCount), hint: '可按专业关联', tone: 'focus' },
          { label: '专业数量', value: formatShort(data.operations.byDiscipline.length), hint: '对象专业字段', tone: 'muted' }
        ]}
      >
        <Panel title="系统清单">
          {systems.length > 0
            ? <Rows rows={systems.map((item) => ({ title: item.name, meta: `${item.code} · ${item.discipline}`, value: `${item.linkedEquipmentCount} 设备` }))} />
            : <EmptyState title="暂无专业系统对象" description="当前项目尚未登记 SYSTEM 类型管理对象。" actions={[{ label: '登记系统对象', href: projectHref('/data-steward/objects'), tone: 'primary' }]} />}
        </Panel>
        <Panel title="专业分布">
          {data.operations.byDiscipline.length > 0
            ? <BarChart bars={data.operations.byDiscipline} />
            : <EmptyState title="暂无专业数据" description="对象台账未维护专业字段。" actions={[{ label: '补充对象专业', href: projectHref('/data-steward/objects'), tone: 'primary' }]} />}
        </Panel>
        {renderActionPanel('可执行动作', '专业系统来自 SYSTEM 管理对象，不展示演示变配电或制冷站数据。', [
          { label: '管理系统对象', href: projectHref('/data-steward/objects'), tone: 'primary' },
          { label: '查看模型集成', href: projectHref('/data-steward/models') },
          { label: '查看工程部位', href: projectHref('/master-data/sections') }
        ])}
      </ModulePageShell>
    );
  }

  function renderWorkordersModule() {
    return (
      <ModulePageShell
        title="工单巡检保养"
        description="平台当前未接入独立运维工单系统，先展示整改、审核和扫描治理任务。"
        metrics={[
          { label: '整改待闭环', value: data.todoMetrics[0]?.value ?? '0', hint: '交付闭环', tone: 'warning' },
          { label: '审核待处理', value: data.todoMetrics[1]?.value ?? '0', hint: '工作中心', tone: 'focus' },
          { label: '扫描运行中', value: data.todoMetrics[2]?.value ?? '0', hint: '资产治理', tone: 'muted' }
        ]}
      >
        <Panel title="平台任务">
          {riskWorkItems.length > 0
            ? <WorkItemList items={riskWorkItems} />
            : <EmptyState title="暂无平台任务" description="当前项目没有未关闭整改或失败扫描任务。" actions={[{ label: '进入整改闭环', href: projectHref('/work/rectifications'), tone: 'primary' }]} />}
        </Panel>
        <Panel title="最近动态">
          <div className="sc-bim-event-list">
            {data.events.map((item) => (
              <article key={item.id} className={`is-${item.tone}`}>
                <span>{item.title}</span>
                <small>{item.time}</small>
              </article>
            ))}
          </div>
        </Panel>
        {renderActionPanel('可执行动作', '工单巡检保养先映射为平台整改、审核和扫描治理任务，不虚构 CMMS 工单。', [
          { label: '整改闭环', href: projectHref('/work/rectifications'), tone: 'primary' },
          { label: '文档交付', href: projectHref('/work/document-delivery') },
          { label: '扫描任务', href: '/data-steward/scans' }
        ])}
      </ModulePageShell>
    );
  }

  function renderActiveModule() {
    if (activeModule === 'tree') return renderTreeModule();
    if (activeModule === 'assets') return renderAssetsModule();
    if (activeModule === 'spaces') return renderSpacesModule();
    if (activeModule === 'alarms') return renderAlarmsModule();
    if (activeModule === 'energy') return renderEnergyModule();
    if (activeModule === 'systems') return renderSystemsModule();
    if (activeModule === 'workorders') return renderWorkordersModule();
    return renderSceneModule();
  }

  return (
    <section className={`sc-bim-window sc-bim-window--${theme}`} aria-label="BIM 协同管理平台窗口">
      <div className="sc-bim-window__header">
        <div>
          <span>当前项目：{data.projectName}</span>
          <strong>{data.projectCode} · {data.projectStage}</strong>
        </div>
        <div className="sc-bim-window__badges" aria-label="模型适配状态">
          <span>{data.engineMode}</span>
          <span>{data.statusLabel}</span>
          <span>更新 {data.updatedAt || '暂无'}</span>
        </div>
      </div>

      <nav className="sc-bim-module-nav" aria-label="BIM 协同模块">
        {moduleItems.map((item) => (
          <button
            key={item.key}
            type="button"
            className={activeModule === item.key ? 'is-active' : undefined}
            aria-pressed={activeModule === item.key}
            onClick={() => setActiveModule(item.key)}
          >
            {item.label}
          </button>
        ))}
      </nav>

      {renderActiveModule()}

      <div className="sc-bim-boundary" aria-label="安全边界">
        <span>安全边界</span>
        {data.safetyNotes.slice(0, 3).map((item) => (
          <em key={item}>{item}</em>
        ))}
      </div>
    </section>
  );
}

function Panel({ title, meta, children }: { title: string; meta?: string; children: ReactNode }) {
  return (
    <section className="sc-bim-panel">
      <header>
        <h3>{title}</h3>
        {meta ? <span>{meta}</span> : null}
      </header>
      <div className="sc-bim-panel__body">{children}</div>
    </section>
  );
}

function MetricList({ metrics, compact = false }: { metrics: BimMetric[]; compact?: boolean }) {
  return (
    <div className={compact ? 'sc-bim-metrics is-compact' : 'sc-bim-metrics'}>
      {metrics.map((item) => (
        <article key={`${item.label}-${item.value}`} className={`is-${item.tone ?? 'muted'}`}>
          <span>{item.label}</span>
          <strong>{item.value}</strong>
          {item.hint ? <em>{item.hint}</em> : null}
        </article>
      ))}
    </div>
  );
}

function Donut({ value, color, label }: { value: number; color: string; label: string }) {
  const safeValue = Math.max(0, Math.min(100, value));
  return (
    <div
      className="sc-bim-donut"
      style={{
        '--sc-donut-value': `${safeValue}%`,
        '--sc-donut-color': color
      } as CssVarStyle}
      role="img"
      aria-label={`${label} 占比`}
    >
      <span>{label}</span>
    </div>
  );
}

function BarChart({ bars }: { bars: BimChartBar[] }) {
  const max = Math.max(...bars.map((item) => item.value), 1);
  return (
    <div className="sc-bim-chart" role="img" aria-label="统计柱状图">
      <div className="sc-bim-chart__axis" aria-hidden="true">
        <span>{formatShort(max)}</span>
        <span>{formatShort(Math.round(max / 2))}</span>
        <span>0</span>
      </div>
      <div
        className="sc-bim-chart__plot"
        style={{ '--sc-chart-columns': bars.length } as CssVarStyle}
        aria-hidden="true"
      >
        <i />
        <i />
        <i />
        {bars.map((item) => (
          <b
            key={item.label}
            style={{
              height: `${Math.max(6, Math.round((item.value / max) * 100))}%`,
              background: item.color
            }}
            title={`${item.label} ${item.displayValue}`}
          />
        ))}
      </div>
      <div
        className="sc-bim-chart__labels"
        style={{ '--sc-chart-columns': bars.length } as CssVarStyle}
        aria-hidden="true"
      >
        {bars.map((item) => (
          <span key={item.label}>{item.label}</span>
        ))}
      </div>
    </div>
  );
}

function LegendList({ items }: { items: BimLegendItem[] }) {
  return (
    <div className="sc-bim-legend-list">
      {items.map((item) => (
        <div key={item.label}>
          <span><i style={{ background: item.color }} />{item.label}</span>
          <strong>{item.value}</strong>
        </div>
      ))}
    </div>
  );
}

function ModulePageShell({
  title,
  description,
  metrics,
  children
}: {
  title: string;
  description: string;
  metrics: BimMetric[];
  children: ReactNode;
}) {
  return (
    <section className="sc-bim-module-page" aria-label={title}>
      <header className="sc-bim-module-page__hero">
        <div>
          <h2>{title}</h2>
          <p>{description}</p>
        </div>
        <MetricList metrics={metrics} compact />
      </header>
      <div className="sc-bim-module-page__grid">{children}</div>
    </section>
  );
}

function EmptyState({
  title,
  description,
  actions = []
}: {
  title: string;
  description: string;
  actions?: BimAction[];
}) {
  return (
    <div className="sc-bim-empty-state">
      <strong>{title}</strong>
      <span>{description}</span>
      {actions.length > 0 ? <ActionButtons actions={actions} /> : null}
    </div>
  );
}

function ActionHint({ description, actions }: { description: string; actions: BimAction[] }) {
  return (
    <div className="sc-bim-action-hint">
      <span>{description}</span>
      <ActionButtons actions={actions} />
    </div>
  );
}

function ActionButtons({ actions }: { actions: BimAction[] }) {
  return (
    <div className="sc-bim-action-buttons">
      {actions.map((item) => (
        <button
          key={item.label}
          className={item.tone === 'primary' ? 'is-primary' : undefined}
          type="button"
          onClick={() => {
            if (item.onClick) {
              item.onClick();
              return;
            }
            if (item.href) {
              window.location.href = item.href;
            }
          }}
        >
          {item.label}
        </button>
      ))}
    </div>
  );
}

function Rows({ rows }: { rows: Array<{ title: string; meta: string; value: string }> }) {
  return (
    <div className="sc-bim-row-list">
      {rows.map((item) => (
        <article key={`${item.title}-${item.meta}`}>
          <div>
            <strong>{item.title}</strong>
            <span>{item.meta}</span>
          </div>
          <em>{item.value}</em>
        </article>
      ))}
    </div>
  );
}

function OwnershipNodeRows({ nodes }: { nodes: FileOwnershipTreeNode[] }) {
  return (
    <div className="sc-bim-tree-node-list">
      {nodes.map((item) => (
        <article key={item.nodeKey}>
          <div>
            <strong>{item.nodeLabel}</strong>
            <span>{compactNodePath(item.nodePath)}</span>
          </div>
          <dl>
            <div>
              <dt>文件</dt>
              <dd>{formatShort(item.fileCount)}</dd>
            </div>
            <div>
              <dt>已确认</dt>
              <dd>{formatShort(item.confirmedFileCount)}</dd>
            </div>
            <div>
              <dt>缺失</dt>
              <dd>{formatShort(item.deliveryMissingCount)}</dd>
            </div>
          </dl>
        </article>
      ))}
    </div>
  );
}

function LightweightModelList({
  items,
  onPreview
}: {
  items: BimEmbeddedPreviewModel[];
  onPreview: (modelId: string) => void;
}) {
  return (
    <div className="sc-bim-model-list">
      {items.map((item) => (
        <article key={item.id}>
          <div>
            <strong>{item.label}</strong>
            <span>文件 {item.modelFileId} · {item.sizeLabel || item.meta} · {item.versionNo}</span>
          </div>
          <em>{item.statusLabel}</em>
          <div className="sc-bim-model-list__actions">
            <button type="button" onClick={() => onPreview(item.id)}>打开预览</button>
            {item.fileManagerUrl ? (
              <button type="button" onClick={() => { window.location.href = item.fileManagerUrl || '#'; }}>文件管理定位</button>
            ) : null}
          </div>
        </article>
      ))}
    </div>
  );
}

function WorkItemList({ items }: { items: BimWorkItem[] }) {
  return (
    <div className="sc-bim-row-list">
      {items.map((item) => (
        <article key={item.id}>
          <div>
            <strong>{item.title}</strong>
            <span>{item.category} · {item.source}</span>
          </div>
          <em>{item.status}</em>
        </article>
      ))}
    </div>
  );
}

function NoteList({ notes }: { notes: string[] }) {
  return (
    <ul className="sc-bim-note-list">
      {notes.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  );
}

function shareDonutValue(items: BimLegendItem[]) {
  const first = Number.parseInt(items[0]?.value ?? '0', 10);
  return Number.isFinite(first) ? first : 0;
}

function formatShort(value: number) {
  if (value >= 10000) return `${Math.round(value / 10000)}万`;
  if (value >= 1000) return `${Math.round(value / 1000)}千`;
  return String(value);
}

function withViewerQuery(url: string, params: Record<string, string>) {
  const [path, query = ''] = url.split('?');
  const search = new URLSearchParams(query);
  Object.entries(params).forEach(([key, value]) => search.set(key, value));
  return `${path}?${search.toString()}`;
}

function flattenSectionNodes(nodes: SectionNode[]): SectionNode[] {
  return nodes.flatMap((item) => [item, ...flattenSectionNodes(item.children ?? [])]);
}

function flattenOwnershipNodes(nodes: FileOwnershipTreeNode[]): FileOwnershipTreeNode[] {
  return nodes.flatMap((item) => [item, ...flattenOwnershipNodes(item.children ?? [])]);
}

function compactNodePath(path: string) {
  const parts = path.split('/').filter(Boolean);
  if (parts.length <= 3) return path || '-';
  return parts.slice(-3).join(' / ');
}
