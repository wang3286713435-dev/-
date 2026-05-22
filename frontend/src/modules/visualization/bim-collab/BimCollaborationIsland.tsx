import BimPreviewScene from './BimPreviewScene';
import { useEffect, useMemo, useState } from 'react';
import type { ChangeEvent, CSSProperties, ReactNode } from 'react';
import type {
  BimChartBar,
  BimCollaborationData,
  BimLegendItem,
  BimMetric,
  BimThemeMode,
  BimWorkItem
} from './types';

import './BimCollaborationIsland.css';

interface BimCollaborationIslandProps {
  data: BimCollaborationData;
  theme: BimThemeMode;
}

type CssVarStyle = CSSProperties & Record<`--${string}`, string | number>;
type ModuleKey = 'overview' | 'bim' | 'assets' | 'spaces' | 'alarms' | 'energy' | 'systems' | 'workorders';

const moduleItems: Array<{ key: ModuleKey; label: string }> = [
  { key: 'overview', label: '综合驾驶舱' },
  { key: 'bim', label: 'BIM 场景' },
  { key: 'assets', label: '设备设施' },
  { key: 'spaces', label: '房屋空间' },
  { key: 'alarms', label: '告警监控' },
  { key: 'energy', label: '建筑能效' },
  { key: 'systems', label: '专业系统' },
  { key: 'workorders', label: '工单巡检保养' }
];

export default function BimCollaborationIsland({ data, theme }: BimCollaborationIslandProps) {
  const [activeModule, setActiveModule] = useState<ModuleKey>('overview');
  const [activeModelIndex, setActiveModelIndex] = useState(0);
  const modelCount = data.sceneNodes.length;
  const activeModel = data.sceneNodes[activeModelIndex] ?? null;
  const activeSceneNodes = useMemo(() => (activeModel ? [activeModel] : []), [activeModel]);
  const activeViewerAvailable = Boolean(activeModel?.viewerAvailable && data.viewerAvailable);
  const activeViewerMessage = activeModel?.actionHint || data.viewerMessage;
  const riskWorkItems: BimWorkItem[] = data.operations.workItems;

  useEffect(() => {
    setActiveModelIndex(0);
  }, [data.projectCode, data.sceneNodes.map((item) => item.id).join('|')]);

  useEffect(() => {
    if (modelCount <= 1) return undefined;
    const timer = window.setInterval(() => {
      setActiveModelIndex((current) => (current + 1) % modelCount);
    }, 5200);
    return () => window.clearInterval(timer);
  }, [modelCount]);

  function showPreviousModel() {
    if (modelCount <= 1) return;
    setActiveModelIndex((current) => (current - 1 + modelCount) % modelCount);
  }

  function showNextModel() {
    if (modelCount <= 1) return;
    setActiveModelIndex((current) => (current + 1) % modelCount);
  }

  function selectModel(event: ChangeEvent<HTMLSelectElement>) {
    const nextIndex = data.sceneNodes.findIndex((item) => item.id === event.currentTarget.value);
    if (nextIndex >= 0) {
      setActiveModelIndex(nextIndex);
    }
  }

  function renderSceneModule() {
    return (
      <div className="sc-bim-layout">
        <aside className="sc-bim-stack" aria-label="协同数据概览">
          <Panel title="项目概览" meta={data.managerName}>
            <MetricList metrics={data.overviewMetrics} />
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

        <main className="sc-bim-stage" aria-label="项目 BIM 协同视图">
          <header className="sc-bim-stage__top">
            <div>
              <h2>{data.projectName}</h2>
              <span>{activeModel ? `${activeModel.label} / ${activeModel.meta}` : '当前项目暂时无模型预览'}</span>
            </div>
            <div className="sc-bim-carousel" aria-label="项目模型轮播">
              <button disabled={modelCount <= 1} type="button" onClick={showPreviousModel}>上一项</button>
              <span>{modelCount > 0 ? `模型 ${activeModelIndex + 1}/${modelCount}` : '暂无模型'}</span>
              <button disabled={modelCount <= 1} type="button" onClick={showNextModel}>下一项</button>
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
              ) : data.sceneNodes.map((item, index) => (
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
            <BimPreviewScene
              nodes={activeSceneNodes}
              viewerAvailable={activeViewerAvailable}
              viewerMessage={activeViewerMessage}
            />
          </div>

          <div className="sc-bim-toolbar" aria-label="模型工具栏">
            {['漫游', '剖切', '测量', '标注', '筛选', '设置'].map((item) => (
              <button
                key={item}
                disabled={!activeViewerAvailable}
                title={activeViewerAvailable ? item : '真实 BIM Viewer 待后续引擎接入'}
                type="button"
              >
                {item}
              </button>
            ))}
          </div>
        </main>

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
      </div>
    );
  }

  function renderAssetsModule() {
    return (
      <ModulePageShell
        title="设备设施"
        description="展示当前项目管理对象台账中的设备对象、专业分布和模型绑定状态。"
        metrics={[
          { label: '设备对象', value: formatShort(data.operations.equipmentCount), hint: 'EQUIPMENT 管理对象', tone: 'normal' },
          { label: '已绑定部位', value: formatShort(data.operations.linkedObjectCount), hint: '关联工程部位', tone: 'focus' },
          { label: '未绑定对象', value: formatShort(data.operations.unlinkedObjectCount), hint: '待补齐位置关系', tone: data.operations.unlinkedObjectCount > 0 ? 'warning' : 'muted' }
        ]}
      >
        <Panel title="对象类型分布">
          {data.operations.byObjectType.length > 0 ? <BarChart bars={data.operations.byObjectType} /> : <EmptyState title="暂无设备设施数据" description="当前项目还没有登记设备、系统或空间管理对象。" />}
        </Panel>
        <Panel title="专业分布">
          {data.operations.byDiscipline.length > 0 ? <BarChart bars={data.operations.byDiscipline} /> : <EmptyState title="暂无专业分布" description="管理对象未维护专业字段。" />}
        </Panel>
        <Panel title="数据边界">
          <NoteList notes={['仅展示平台管理对象台账', '不显示设备在线率、传感器点位或实时运行值', '如需设备实时态势，需要接入物联数据源']} />
        </Panel>
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
          {spaces.length > 0 ? <Rows rows={spaces.map((item) => ({ title: item.name, meta: `${item.code} · ${item.level} 级`, value: `${item.objectCount} 对象` }))} /> : <EmptyState title="暂无房屋空间数据" description="当前项目未维护工程部位树或空间对象。" />}
        </Panel>
        <Panel title="空间关联对象">
          {spaces.some((item) => item.objectCount > 0) ? <Rows rows={spaces.filter((item) => item.objectCount > 0).map((item) => ({ title: item.name, meta: `${item.equipmentCount} 设备 / ${item.systemCount} 系统`, value: `${item.objectCount} 个` }))} /> : <EmptyState title="暂无空间绑定" description="对象台账还没有绑定到具体工程部位。" />}
        </Panel>
        <Panel title="数据边界">
          <NoteList notes={['空间来自平台工程部位和 SPACE 对象', '不展示演示院区、楼层面积或房间实时状态', '后续可补接房产/空间台账字段']} />
        </Panel>
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
          {riskWorkItems.length > 0 ? <WorkItemList items={riskWorkItems} /> : <EmptyState title="暂无待处理风险" description="当前项目没有未关闭整改或失败扫描任务。" />}
        </Panel>
        <Panel title="未接入内容">
          <NoteList notes={['不展示虚构设备告警', '不展示环境监测报警', '需要物联平台后才能启用实时告警']} />
        </Panel>
      </ModulePageShell>
    );
  }

  function renderEnergyModule() {
    return (
      <ModulePageShell
        title="建筑能效"
        description="平台当前没有真实水、电、气、冷热能耗采集，本模块只展示接入状态。"
        metrics={[
          { label: '能耗数据源', value: '未接入', hint: '不展示演示能耗', tone: 'warning' },
          { label: '模型文件', value: data.overviewMetrics[0]?.value ?? '0', hint: '可为后续空间能耗绑定做准备', tone: 'focus' },
          { label: '空间基础', value: formatShort(data.operations.sectionNodeCount), hint: '工程部位', tone: 'muted' }
        ]}
      >
        <Panel title="接入状态">
          <EmptyState title="暂无真实能耗数据" description="未接入能耗表计、采集网关或第三方能管系统时，不展示演示曲线。" />
        </Panel>
        <Panel title="后续可绑定基础">
          <Rows rows={[
            { title: '空间维度', meta: '工程部位树', value: formatShort(data.operations.sectionNodeCount) },
            { title: '对象维度', meta: '设备/系统对象', value: formatShort(data.operations.equipmentCount + data.operations.systemObjectCount) },
            { title: '模型维度', meta: '模型集成', value: data.overviewMetrics[0]?.value ?? '0' }
          ]} />
        </Panel>
        <Panel title="数据边界">
          <NoteList notes={data.operations.unavailableModules.filter((item) => item.includes('能耗')).length > 0 ? data.operations.unavailableModules.filter((item) => item.includes('能耗')) : ['未接入真实能耗采集']} />
        </Panel>
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
          {systems.length > 0 ? <Rows rows={systems.map((item) => ({ title: item.name, meta: `${item.code} · ${item.discipline}`, value: `${item.linkedEquipmentCount} 设备` }))} /> : <EmptyState title="暂无专业系统对象" description="当前项目尚未登记 SYSTEM 类型管理对象。" />}
        </Panel>
        <Panel title="专业分布">
          {data.operations.byDiscipline.length > 0 ? <BarChart bars={data.operations.byDiscipline} /> : <EmptyState title="暂无专业数据" description="对象台账未维护专业字段。" />}
        </Panel>
        <Panel title="数据边界">
          <NoteList notes={['系统来自平台 SYSTEM 管理对象', '不展示演示的变配电、制冷站等虚构系统', '设备运行状态需后续接物联数据源']} />
        </Panel>
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
          {riskWorkItems.length > 0 ? <WorkItemList items={riskWorkItems} /> : <EmptyState title="暂无平台任务" description="当前项目没有未关闭整改或失败扫描任务。" />}
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
        <Panel title="数据边界">
          <NoteList notes={['不展示虚构巡检、保养或维修工单', '当前任务来自平台整改、审核和扫描治理', '接入 CMMS/运维系统后可替换为真实工单']} />
        </Panel>
      </ModulePageShell>
    );
  }

  function renderActiveModule() {
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

function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="sc-bim-empty-state">
      <strong>{title}</strong>
      <span>{description}</span>
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
