<template>
  <section class="mvp-page">
    <div class="mvp-page__header">
      <div>
        <h1>管理对象</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建对象</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="objects" class="master-table" empty-text="暂无管理对象">
      <el-table-column prop="name" label="对象名称" min-width="180" />
      <el-table-column prop="code" label="编码" min-width="130" />
      <el-table-column prop="objectType" label="类型" width="120" />
      <el-table-column prop="discipline" label="专业" width="120" />
      <el-table-column prop="sectionNodeId" label="部位 ID" width="100" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button text :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑管理对象' : '新建管理对象'" width="620px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="模型集成">
          <el-select v-model="form.modelIntegrationId" filterable :disabled="Boolean(editingId)">
            <el-option v-for="model in publishedModels" :key="model.id" :label="model.name" :value="model.id" />
          </el-select>
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
          <el-input v-model="form.objectType" maxlength="64" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.discipline" maxlength="64" />
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
import { ElMessage } from 'element-plus';
import { Edit, Plus, Refresh } from '@element-plus/icons-vue';

import {
  createManagedObject,
  fetchManagedObjects,
  fetchModelIntegrations,
  updateManagedObject,
  type ManagedObject,
  type ModelIntegration
} from '@/modules/data-steward/api/dataSteward';
import { fetchSectionTree, type SectionNode } from '@/modules/master-data/api/masterData';
import { useAuthStore } from '@/stores/auth';

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

const projectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => authStore.currentUser?.currentProject.name ?? '等待项目上下文');
const publishedModels = computed(() => models.value.filter((model) => model.status === 'PUBLISHED'));
const sectionOptions = computed(() => flattenSections(sections.value));

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
      code: form.code,
      name: form.name,
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

function flattenSections(nodes: SectionNode[], prefix = ''): Array<{ id: number; label: string }> {
  return nodes.flatMap((node) => {
    const label = `${prefix}${node.name}`;
    return [{ id: node.id, label }, ...flattenSections(node.children ?? [], `${prefix} / `)];
  });
}
</script>
