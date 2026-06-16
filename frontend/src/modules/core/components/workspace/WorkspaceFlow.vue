<template>
  <ol class="workspace-flow-ui" aria-label="项目流程">
    <li
      v-for="step in steps"
      :key="step.key"
      class="workspace-flow-ui__step"
      :class="`is-${step.state}`"
    >
      <span class="workspace-flow-ui__index">{{ step.index }}</span>
      <div>
        <strong>{{ step.title }}</strong>
        <em>{{ step.status }}</em>
      </div>
    </li>
  </ol>
</template>

<script setup lang="ts">
export interface WorkspaceFlowStep {
  key: string;
  index: number | string;
  title: string;
  status: string;
  state: 'done' | 'current' | 'pending' | 'locked' | 'blocked';
}

defineProps<{
  steps: WorkspaceFlowStep[];
}>();
</script>

<style scoped>
.workspace-flow-ui {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 18px;
  min-width: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.workspace-flow-ui__step {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 4px 10px;
  min-width: 0;
  min-height: 86px;
  padding: 18px;
  border: 1px solid var(--ux4-border);
  border-radius: var(--ux4-radius-card);
  background: var(--ux4-surface);
  box-shadow: var(--ux4-shadow-card);
}

.workspace-flow-ui__step:not(:last-child)::after {
  content: "";
  position: absolute;
  right: -15px;
  top: 50%;
  width: 12px;
  height: 12px;
  border-top: 1px solid var(--ux4-flow-line);
  border-right: 1px solid var(--ux4-flow-line);
  transform: translateY(-50%) rotate(45deg);
}

.workspace-flow-ui__index {
  display: grid;
  place-items: center;
  grid-row: span 2;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: var(--ux4-blue-soft);
  color: var(--ux4-blue);
  font-family: var(--zy-font-mono);
  font-size: 12px;
  font-weight: 700;
}

.workspace-flow-ui__step strong {
  color: var(--ux4-text-strong);
  font-size: 14px;
  font-weight: 650;
  line-height: 1.35;
}

.workspace-flow-ui__step em {
  color: var(--ux4-text-soft);
  font-size: 12px;
  font-style: normal;
  line-height: 1.45;
}

.workspace-flow-ui__step.is-done {
  border-color: var(--ux4-green-border);
  background: var(--ux4-green-soft);
}

.workspace-flow-ui__step.is-current {
  border-color: var(--ux4-blue);
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.1), var(--ux4-shadow-raised);
}

.workspace-flow-ui__step.is-current .workspace-flow-ui__index {
  animation: ux4-flow-current 1800ms ease-out infinite;
}

.workspace-flow-ui__step.is-blocked {
  border-color: var(--ux4-red-border);
  background: var(--ux4-red-soft);
}

.workspace-flow-ui__step.is-locked {
  opacity: 0.78;
}

@media (max-width: 1280px) {
  .workspace-flow-ui {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .workspace-flow-ui__step::after {
    display: none;
  }
}

@media (max-width: 760px) {
  .workspace-flow-ui {
    grid-template-columns: 1fr;
  }
}

@keyframes ux4-flow-current {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.22);
  }

  50% {
    box-shadow: 0 0 0 6px rgba(37, 99, 235, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .workspace-flow-ui__step.is-current .workspace-flow-ui__index {
    animation: none;
  }
}
</style>
