import BimPreviewScene from './BimPreviewScene';
import { useEffect, useMemo, useState } from 'react';
import type { ChangeEvent, CSSProperties, ReactNode } from 'react';
import type { BimChartBar, BimCollaborationData, BimLegendItem, BimMetric, BimThemeMode } from './types';

import './BimCollaborationIsland.css';

interface BimCollaborationIslandProps {
  data: BimCollaborationData;
  theme: BimThemeMode;
}

type CssVarStyle = CSSProperties & Record<`--${string}`, string | number>;

export default function BimCollaborationIsland({ data, theme }: BimCollaborationIslandProps) {
  const [activeModelIndex, setActiveModelIndex] = useState(0);
  const modelCount = data.sceneNodes.length;
  const activeModel = data.sceneNodes[activeModelIndex] ?? null;
  const activeSceneNodes = useMemo(() => (activeModel ? [activeModel] : []), [activeModel]);
  const activeViewerAvailable = Boolean(activeModel?.viewerAvailable && data.viewerAvailable);
  const activeViewerMessage = activeModel?.actionHint || data.viewerMessage;

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

function shareDonutValue(items: BimLegendItem[]) {
  const first = Number.parseInt(items[0]?.value ?? '0', 10);
  return Number.isFinite(first) ? first : 0;
}

function formatShort(value: number) {
  if (value >= 10000) return `${Math.round(value / 10000)}万`;
  if (value >= 1000) return `${Math.round(value / 1000)}千`;
  return String(value);
}
