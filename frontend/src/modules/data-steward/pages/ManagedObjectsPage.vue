<template>
  <section class="mvp-page object-page">
    <div class="mvp-page__header">
      <div>
        <h1>管理对象</h1>
        <p>{{ projectLabel }}，维护项目设备、系统和空间对象台账。</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建对象</el-button>
      </div>
    </div>

    <section class="object-summary-grid">
      <article v-for="item in summaryCards" :key="item.label" class="object-summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.helper }}</em>
      </article>
    </section>

    <el-alert
      title="当前管理对象为平台台账记录，不从 BIM 模型自动抽取构件，也不触发构件级解析。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-table v-loading="loading" :data="objects" class="master-table" empty-text="暂无管理对象">
      <el-table-column prop="code" label="对象编码" min-width="130" show-overflow-tooltip />
      <el-table-column prop="name" label="对象名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="objectType" label="类型" width="120" />
      <el-table-column prop="discipline" label="专业" width="120">
        <template #default="{ row }">{{ disciplineLabel(row.discipline) }}</template>
      </el-table-column>
      <el-table-column label="关联模型" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ modelName(row.modelIntegrationId) }}</template>
      </el-table-column>
      <el-table-column label="关联部位" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ sectionName(row.sectionNodeId) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button text :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
          <el-button text type="danger" :icon="Delete" @click="handleDelete(row)">停用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑管理对象' : '新建管理对象'" width="660px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="模型集成">
          <el-select v-model="form.modelIntegrationId" filterable :disabled="Boolean(editingId)">
            <el-option v-for="model in publishedModels" :key="model.id" :label="model.name" :value="model.id" />
          </el-select>
          <div class="field-hint">对象必须关联已发布模型集成，便于后续进入构件级解析时平滑承接。</div>
        </el-form-item>
        <el-form-item label="工程部位">
          <el-select v-model="form.sectionNodeId" clearable filterable>
            <el-option v-for="node in sectionOptions" :key="node.id" :label="node.label" :value="node.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" maxlength="64" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="128" />
        </el-form-item>
        <el-form-item label="对象类型">
          <el-select v-model="form.objectType" filterable allow-create>
            <el-option label="设备" value="EQUIPMENT" />
            <el-option label="系统" value="SYSTEM" />
            <el-option label="空间" value="SPACE" />
            <el-option label="构件占位" value="COMPONENT_PLACEHOLDER" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.discipline" maxlength="64" placeholder="如 INTELLIGENT / HVAC" />
        </el-form-item>
        <el-form-item label="属性 JSON">
          <el-input v-model="form.propertiesJson" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue';

import {
  createManagedObject,
  deleteManagedObject,
  fetchManagedObjects,
  fetchModelIntegrations,
  updateManagedObject,
  type ManagedObject,
  type ModelIntegration
} from '@/modules/data-steward/api/dataSteward';
import { fetchSectionTree, type SectionNode } from '@/modules/master-data/api/masterData';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const authStore = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const objects = ref<ManagedObject[]>([]);
const models = ref<ModelIntegration[]>([]);
const sections = ref<SectionNode[]>([]);

const form = reactive({
  modelIntegrationId: null as number | null,
  sectionNodeId: null as number | null,
  code: '',
  name: '',
  objectType: 'EQUIPMENT',
  discipline: 'MEP',
  propertiesJson: '{"source":"manual"}'
});

const projectId = computed(() => {
  const routeId = Number(route.params.projectId);
  return Number.isFinite(routeId) && routeId > 0 ? routeId : authStore.currentProjectId;
});
const projectLabel = computed(() => {
  const current = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return current ? `${current.code} ${current.name}` : '等待项目上下文';
});
const publishedModels = computed(() => models.value.filter((model) => model.status === 'PUBLISHED'));
const sectionOptions = computed(() => flattenSections(sections.value));
const activeObjects = computed(() => objects.value.filter((item) => item.status === 'ACTIVE').length);
const summaryCards = computed(() => [
  { label: '对象总数', value: objects.value.length.toLocaleString('zh-CN'), helper: '平台对象台账' },
  { label: '启用对象', value: activeObjects.value.toLocaleString('zh-CN'), helper: '可参与交付关联' },
  { label: '已发布模型', value: publishedModels.value.length.toLocaleString('zh-CN'), helper: '对象可关联模型' },
  { label: '工程部位', value: sectionOptions.value.length.toLocaleString('zh-CN'), helper: '可绑定部位' }
]);

watch(projectId, () => loadPage(), { immediate: true });

async function loadPage() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [nextObjects, nextModels, nextSections] = await Promise.all([
      fetchManagedObjects(projectId.value),
      fetchModelIntegrations(projectId.value),
      fetchSectionTree(projectId.value)
    ]);
    objects.value = nextObjects;
    models.value = nextModels;
    sections.value = nextSections;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '管理对象加载失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  editingId.value = null;
  Object.assign(form, {
    modelIntegrationId: publishedModels.value[0]?.id ?? null,
    sectionNodeId: sectionOptions.value[0]?.id ?? null,
    code: '',
    name: '',
    objectType: 'EQUIPMENT',
    discipline: 'MEP',
    propertiesJson: '{"source":"manual"}'
  });
  dialogVisible.value = true;
}

function openEditDialog(row: ManagedObject) {
  editingId.value = row.id;
  Object.assign(form, {
    modelIntegrationId: row.modelIntegrationId,
    sectionNodeId: row.sectionNodeId,
    code: row.code,
    name: row.name,
    objectType: row.objectType,
    discipline: row.discipline ?? '',
    propertiesJson: row.propertiesJson ?? ''
  });
  dialogVisible.value = true;
}

async function handleSave() {
  if (!projectId.value || !form.modelIntegrationId) return;
  saving.value = true;
  try {
    const payload = {
      modelIntegrationId: form.modelIntegrationId,
      sectionNodeId: form.sectionNodeId,
      code: form.code.trim(),
      name: form.name.trim(),
      objectType: form.objectType,
      discipline: form.discipline,
      propertiesJson: form.propertiesJson,
      status: 'ACTIVE'
    };
    if (editingId.value) {
      await updateManagedObject(projectId.value, editingId.value, payload);
    } else {
      await createManagedObject(projectId.value, payload);
    }
    ElMessage.success('管理对象已保存');
    dialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row: ManagedObject) {
  if (!projectId.value) return;
  try {
    await ElMessageBox.confirm('停用该平台对象记录，不会修改模型或 NAS 文件。', '停用管理对象', {
      confirmButtonText: '停用',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  try {
    await deleteManagedObject(projectId.value, row.id);
    ElMessage.success('管理对象已停用');
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '停用失败');
  }
}

function modelName(modelId: number) {
  return models.value.find((item) => item.id === modelId)?.name ?? `模型集成 ${modelId}`;
}

function sectionName(sectionId: number | null) {
  if (!sectionId) return '-';
  return sectionOptions.value.find((item) => item.id === sectionId)?.label ?? `部位 ${sectionId}`;
}

function statusLabel(value: string) {
  const labels: Record<string, string> = {
    ACTIVE: '启用',
    INACTIVE: '停用'
  };
  return labels[value] ?? value;
}

function disciplineLabel(value: string | null | undefined) {
  const labels: Record<string, string> = {
    MEP: '机电',
    HVAC: '暖通',
    ELECTRICAL: '电气',
    PLUMBING: '给排水',
    INTELLIGENT: '智能化',
    FIRE_PROTECTION: '消防'
  };
  if (!value) return '-';
  return labels[value] ?? value;
}

function flattenSections(nodes: SectionNode[], prefix = ''): Array<{ id: number; label: string }> {
  return nodes.flatMap((node) => {
    const label = `${prefix}${node.name}`;
    return [{ id: node.id, label }, ...flattenSections(node.children ?? [], `${prefix} / `)];
  });
}
</script>

<style scoped>
.object-page {
  min-width: 0;
}

.object-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.object-summary-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-surface);
}

.object-summary-card span,
.object-summary-card em {
  display: block;
  color: var(--zy-muted);
  font-size: 12px;
  font-style: normal;
}

.object-summary-card strong {
  display: block;
  margin: 6px 0 4px;
  color: var(--zy-ink);
  font-size: 22px;
  line-height: 1.15;
}

@media (max-width: 900px) {
  .object-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .object-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
