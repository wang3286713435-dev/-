<template>
  <component
    :is="interactive ? 'button' : 'section'"
    class="workspace-ui-card"
    :class="[
      `workspace-ui-card--${variant}`,
      `workspace-ui-card--${density}`,
      { 'is-interactive': interactive }
    ]"
    :type="interactive ? 'button' : undefined"
  >
    <header v-if="$slots.header || title || eyebrow" class="workspace-ui-card__header">
      <div>
        <span v-if="eyebrow" class="workspace-ui-card__eyebrow">{{ eyebrow }}</span>
        <strong v-if="title">{{ title }}</strong>
      </div>
      <slot name="header" />
    </header>
    <slot />
  </component>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string;
  eyebrow?: string;
  variant?: 'default' | 'soft' | 'accent' | 'success' | 'warning' | 'danger';
  density?: 'comfortable' | 'compact';
  interactive?: boolean;
}>(), {
  variant: 'default',
  density: 'comfortable',
  interactive: false
});
</script>

<style scoped>
.workspace-ui-card {
  appearance: none;
  display: grid;
  gap: var(--ux4-card-gap);
  min-width: 0;
  width: 100%;
  padding: var(--ux4-card-pad);
  border: 1px solid var(--ux4-border);
  border-radius: var(--ux4-radius-card);
  background: var(--ux4-surface);
  box-shadow: var(--ux4-shadow-card);
  color: inherit;
  font-family: inherit;
  text-align: left;
  transition:
    border-color var(--ux4-motion-fast) var(--ux4-ease),
    box-shadow var(--ux4-motion-fast) var(--ux4-ease),
    transform var(--ux4-motion-fast) var(--ux4-ease);
}

.workspace-ui-card--compact {
  --ux4-card-pad: 14px;
  --ux4-card-gap: 10px;
}

.workspace-ui-card--soft {
  background: var(--ux4-surface-soft);
}

.workspace-ui-card--accent {
  border-color: var(--ux4-blue-border);
  background: var(--ux4-blue-soft);
}

.workspace-ui-card--success {
  border-color: var(--ux4-green-border);
  background: var(--ux4-green-soft);
}

.workspace-ui-card--warning {
  border-color: var(--ux4-amber-border);
  background: var(--ux4-amber-soft);
}

.workspace-ui-card--danger {
  border-color: var(--ux4-red-border);
  background: var(--ux4-red-soft);
}

.workspace-ui-card.is-interactive {
  cursor: pointer;
  min-height: 44px;
}

.workspace-ui-card.is-interactive:hover {
  border-color: var(--ux4-blue-border);
  box-shadow: var(--ux4-shadow-raised);
  transform: translateY(-1px);
}

.workspace-ui-card.is-interactive:active {
  transform: translateY(0);
}

.workspace-ui-card.is-interactive:focus-visible {
  outline: 3px solid var(--ux4-focus);
  outline-offset: 2px;
}

.workspace-ui-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.workspace-ui-card__header > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.workspace-ui-card__header strong {
  color: var(--ux4-text-strong);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.workspace-ui-card__eyebrow {
  color: var(--ux4-blue);
  font-size: 11px;
  font-weight: 700;
  line-height: 1.2;
}

@media (prefers-reduced-motion: reduce) {
  .workspace-ui-card {
    transition: none;
  }

  .workspace-ui-card.is-interactive:hover {
    transform: none;
  }
}
</style>
