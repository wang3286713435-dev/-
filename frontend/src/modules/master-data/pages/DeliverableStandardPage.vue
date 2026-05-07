<template>
  <section class="master-data-page">
    <div class="master-data-page__header">
      <div>
        <h1>交付物标准</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="master-data-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Plus" :disabled="!canWrite" @click="openCreateDialog('definition')">
          新增交付物定义
        </el-button>
      </div>
    </div>

    <StandardStatusPanel :status="standardStatus" />

    <el-alert v-if="!canWrite" class="node-type-lock" type="warning" :closable="false" show-icon>
      <template #title>请先锁定节点类型</template>
    </el-alert>

    <div class="deliverable-layout">
      <section class="deliverable-panel">
        <div class="deliverable-panel__header">
          <h2>交付物定义</h2>
          <el-button size="small" type="primary" :icon="Plus" :disabled="!canWrite" @click="openCreateDialog('definition')">
            新增
          </el-button>
        </div>
        <el-table
          v-loading="loading"
          :data="definitions"
          class="master-table"
          empty-text="暂无交付物定义"
          highlight-current-row
          @row-click="selectDefinition"
        >
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="code" label="编码" min-width="130" />
          <el-table-column prop="category" label="分类" width="110" />
          <el-table-column label="必交" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.required ? 'success' : 'info'">{{ row.required ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button text :icon="Edit" :disabled="!canWrite" @click.stop="openEditDialog('definition', row)">编辑</el-button>
              <el-button text type="danger" :icon="Delete" :disabled="!canWrite" @click.stop="handleDelete('definition', row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="deliverable-panel">
        <div class="deliverable-panel__header">
          <h2>交付物类型</h2>
          <el-button
            size="small"
            type="primary"
            :icon="Plus"
            :disabled="!canWrite || !selectedDefinition"
            @click="openCreateDialog('type')"
          >
            新增
          </el-button>
        </div>
        <el-table
          v-loading="loadingTypes"
          :data="types"
          class="master-table"
          empty-text="请先选择交付物定义"
          highlight-current-row
          @row-click="selectType"
        >
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="code" label="编码" min-width="130" />
          <el-table-column prop="fileKind" label="文件类型" width="110" />
          <el-table-column prop="bindingStrategy" label="挂接策略" width="120" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button text :icon="Edit" :disabled="!canWrite" @click.stop="openEditDialog('type', row)">编辑</el-button>
              <el-button text type="danger" :icon="Delete" :disabled="!canWrite" @click.stop="handleDelete('type', row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="deliverable-panel">
        <div class="deliverable-panel__header">
          <h2>交付物属性</h2>
          <el-button
            size="small"
            type="primary"
            :icon="Plus"
            :disabled="!canWrite || !selectedType"
            @click="openCreateDialog('attribute')"
          >
            新增
          </el-button>
        </div>
        <el-table v-loading="loadingAttributes" :data="attributes" class="master-table" empty-text="请先选择交付物类型">
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="code" label="编码" min-width="130" />
          <el-table-column prop="valueType" label="值类型" width="100" />
          <el-table-column prop="unit" label="单位" width="90" />
          <el-table-column label="必填" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.required ? 'success' : 'info'">{{ row.required ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button text :icon="Edit" :disabled="!canWrite" @click="openEditDialog('attribute', row)">编辑</el-button>
              <el-button text type="danger" :icon="Delete" :disabled="!canWrite" @click="handleDelete('attribute', row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="deliverable-panel">
        <div class="deliverable-panel__header">
          <h2>目录模板</h2>
          <el-button size="small" type="primary" :icon="Plus" :disabled="!canWrite" @click="openCreateDialog('template')">
            新增
          </el-button>
        </div>
        <el-table v-loading="loading" :data="templates" class="master-table" empty-text="暂无目录模板">
          <el-table-column prop="name" label="名称" min-width="180" />
          <el-table-column prop="templateType" label="目录类型" width="110" />
          <el-table-column prop="sourceType" label="来源" width="100" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button text :icon="Edit" :disabled="!canWrite" @click="openEditDialog('template', row)">编辑</el-button>
              <el-button text type="danger" :icon="Delete" :disabled="!canWrite" @click="handleDelete('template', row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form label-position="top" class="master-form">
        <template v-if="dialogKind === 'definition'">
          <el-form-item label="节点类型">
            <el-select v-model="definitionForm.nodeTypeId" filterable>
              <el-option v-for="nodeType in nodeTypes" :key="nodeType.id" :label="nodeType.name" :value="nodeType.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="编码">
            <el-input v-model="definitionForm.code" maxlength="64" />
          </el-form-item>
          <el-form-item label="名称">
            <el-input v-model="definitionForm.name" maxlength="128" />
          </el-form-item>
          <el-form-item label="分类">
            <el-segmented v-model="definitionForm.category" :options="categoryOptions" />
          </el-form-item>
          <el-form-item label="必交">
            <el-switch v-model="definitionForm.required" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="definitionForm.sortOrder" :min="0" controls-position="right" />
          </el-form-item>
        </template>

        <template v-if="dialogKind === 'type'">
          <el-form-item label="所属定义">
            <el-input :model-value="selectedDefinition?.name ?? ''" disabled />
          </el-form-item>
          <el-form-item label="编码">
            <el-input v-model="typeForm.code" maxlength="64" />
          </el-form-item>
          <el-form-item label="名称">
            <el-input v-model="typeForm.name" maxlength="128" />
          </el-form-item>
          <el-form-item label="文件类型">
            <el-segmented v-model="typeForm.fileKind" :options="fileKindOptions" />
          </el-form-item>
          <el-form-item label="挂接策略">
            <el-segmented v-model="typeForm.bindingStrategy" :options="bindingOptions" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="typeForm.sortOrder" :min="0" controls-position="right" />
          </el-form-item>
        </template>

        <template v-if="dialogKind === 'attribute'">
          <el-form-item label="所属类型">
            <el-input :model-value="selectedType?.name ?? ''" disabled />
          </el-form-item>
          <el-form-item label="编码">
            <el-input v-model="attributeForm.code" maxlength="64" />
          </el-form-item>
          <el-form-item label="名称">
            <el-input v-model="attributeForm.name" maxlength="128" />
          </el-form-item>
          <el-form-item label="值类型">
            <el-segmented v-model="attributeForm.valueType" :options="valueTypeOptions" />
          </el-form-item>
          <el-form-item label="单位">
            <el-input v-model="attributeForm.unit" maxlength="32" />
          </el-form-item>
          <el-form-item label="必填">
            <el-switch v-model="attributeForm.required" />
          </el-form-item>
          <el-form-item label="示例值">
            <el-input v-model="attributeForm.exampleValue" maxlength="256" />
          </el-form-item>
          <el-form-item label="枚举项">
            <el-input v-model="attributeForm.enumOptions" type="textarea" :rows="3" />
          </el-form-item>
        </template>

        <template v-if="dialogKind === 'template'">
          <el-form-item label="目录类型">
            <el-segmented v-model="templateForm.templateType" :options="categoryOptions" />
          </el-form-item>
          <el-form-item label="名称">
            <el-input v-model="templateForm.name" maxlength="128" />
          </el-form-item>
          <el-form-item label="来源">
            <el-segmented v-model="templateForm.sourceType" :options="sourceTypeOptions" />
          </el-form-item>
          <el-form-item label="目录 JSON">
            <el-input v-model="templateForm.rootNodeJson" type="textarea" :rows="4" />
          </el-form-item>
        </template>

        <el-form-item label="状态">
          <el-segmented v-model="activeStatusForm.status" :options="statusOptions" />
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue';

import StandardStatusPanel from '@/modules/master-data/components/StandardStatusPanel.vue';
import {
  createDeliverableAttribute,
  createDeliverableDefinition,
  createDeliverableType,
  createDirectoryTemplate,
  deleteDeliverableAttribute,
  deleteDeliverableDefinition,
  deleteDeliverableType,
  deleteDirectoryTemplate,
  fetchDeliverableAttributes,
  fetchDeliverableDefinitions,
  fetchDeliverableTypes,
  fetchDirectoryTemplates,
  fetchNodeTypes,
  fetchStandardStatus,
  updateDeliverableAttribute,
  updateDeliverableDefinition,
  updateDeliverableType,
  updateDirectoryTemplate,
  type DeliverableAttribute,
  type DeliverableDefinition,
  type DeliverableType,
  type DirectoryTemplate,
  type NodeType,
  type StandardStatus
} from '@/modules/master-data/api/masterData';
import { useAuthStore } from '@/stores/auth';

type DialogKind = 'definition' | 'type' | 'attribute' | 'template';

const authStore = useAuthStore();
const loading = ref(false);
const loadingTypes = ref(false);
const loadingAttributes = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const dialogKind = ref<DialogKind>('definition');
const editingId = ref<number | null>(null);

const standardStatus = ref<StandardStatus | null>(null);
const nodeTypes = ref<NodeType[]>([]);
const definitions = ref<DeliverableDefinition[]>([]);
const types = ref<DeliverableType[]>([]);
const attributes = ref<DeliverableAttribute[]>([]);
const templates = ref<DirectoryTemplate[]>([]);
const selectedDefinition = ref<DeliverableDefinition | null>(null);
const selectedType = ref<DeliverableType | null>(null);

const definitionForm = reactive({
  nodeTypeId: null as number | null,
  code: '',
  name: '',
  category: 'DOCUMENT',
  required: true,
  sortOrder: 0,
  status: 'ACTIVE'
});

const typeForm = reactive({
  code: '',
  name: '',
  fileKind: 'DOCUMENT',
  bindingStrategy: 'SECTION_NODE',
  sortOrder: 0,
  status: 'ACTIVE'
});

const attributeForm = reactive({
  code: '',
  name: '',
  valueType: 'TEXT',
  unit: '',
  required: false,
  exampleValue: '',
  enumOptions: '',
  sortOrder: 0,
  status: 'ACTIVE'
});

const templateForm = reactive({
  templateType: 'DOCUMENT',
  name: '',
  rootNodeJson: '{"children":[]}',
  sourceType: 'MANUAL',
  sortOrder: 0,
  status: 'ACTIVE'
});

const categoryOptions = [
  { label: '文档', value: 'DOCUMENT' },
  { label: '图纸', value: 'DRAWING' },
  { label: '模型', value: 'MODEL' }
];
const fileKindOptions = categoryOptions;
const bindingOptions = [
  { label: '部位', value: 'SECTION_NODE' },
  { label: '对象', value: 'MANAGED_OBJECT' },
  { label: '项目', value: 'PROJECT' }
];
const valueTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '数字', value: 'NUMBER' },
  { label: '日期', value: 'DATE' },
  { label: '枚举', value: 'ENUM' },
  { label: '布尔', value: 'BOOLEAN' }
];
const sourceTypeOptions = [
  { label: '手工', value: 'MANUAL' },
  { label: '标准', value: 'STANDARD' }
];
const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' }
];

const currentProjectId = computed(() => authStore.currentProjectId);
const canWrite = computed(() => Boolean(standardStatus.value?.nodeTypesLocked));
const projectLabel = computed(() => {
  const project = authStore.currentUser?.currentProject;
  return project ? `${project.code} | ${project.name}` : '等待项目上下文';
});
const dialogTitle = computed(() => {
  const prefix = editingId.value ? '编辑' : '新增';
  const labels: Record<DialogKind, string> = {
    definition: '交付物定义',
    type: '交付物类型',
    attribute: '交付物属性',
    template: '目录模板'
  };
  return `${prefix}${labels[dialogKind.value]}`;
});
const activeStatusForm = computed(() => {
  if (dialogKind.value === 'definition') return definitionForm;
  if (dialogKind.value === 'type') return typeForm;
  if (dialogKind.value === 'attribute') return attributeForm;
  return templateForm;
});

watch(
  currentProjectId,
  () => {
    loadPage();
  },
  { immediate: true }
);

async function loadPage() {
  if (!currentProjectId.value) return;
  loading.value = true;
  try {
    const [status, nodeTypeList, definitionList, templateList] = await Promise.all([
      fetchStandardStatus(currentProjectId.value),
      fetchNodeTypes(currentProjectId.value),
      fetchDeliverableDefinitions(currentProjectId.value),
      fetchDirectoryTemplates(currentProjectId.value)
    ]);
    standardStatus.value = status;
    nodeTypes.value = nodeTypeList;
    definitions.value = definitionList;
    templates.value = templateList;
    selectedDefinition.value = definitionList[0] ?? null;
    await loadTypes();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '交付物标准加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadStatus() {
  if (!currentProjectId.value) return;
  standardStatus.value = await fetchStandardStatus(currentProjectId.value);
}

async function loadTypes() {
  if (!currentProjectId.value || !selectedDefinition.value) {
    types.value = [];
    selectedType.value = null;
    attributes.value = [];
    return;
  }
  loadingTypes.value = true;
  try {
    types.value = await fetchDeliverableTypes(currentProjectId.value, selectedDefinition.value.id);
    selectedType.value = types.value[0] ?? null;
    await loadAttributes();
  } finally {
    loadingTypes.value = false;
  }
}

async function loadAttributes() {
  if (!currentProjectId.value || !selectedType.value) {
    attributes.value = [];
    return;
  }
  loadingAttributes.value = true;
  try {
    attributes.value = await fetchDeliverableAttributes(currentProjectId.value, selectedType.value.id);
  } finally {
    loadingAttributes.value = false;
  }
}

async function selectDefinition(row: DeliverableDefinition) {
  selectedDefinition.value = row;
  await loadTypes();
}

async function selectType(row: DeliverableType) {
  selectedType.value = row;
  await loadAttributes();
}

function assertWritable() {
  if (canWrite.value) return true;
  ElMessage.warning('请先锁定节点类型');
  return false;
}

function openCreateDialog(kind: DialogKind) {
  if (!assertWritable()) return;
  if (kind === 'type' && !selectedDefinition.value) {
    ElMessage.warning('请先选择交付物定义');
    return;
  }
  if (kind === 'attribute' && !selectedType.value) {
    ElMessage.warning('请先选择交付物类型');
    return;
  }
  dialogKind.value = kind;
  editingId.value = null;
  resetForm(kind);
  dialogVisible.value = true;
}

function openEditDialog(kind: DialogKind, row: DeliverableDefinition | DeliverableType | DeliverableAttribute | DirectoryTemplate) {
  if (!assertWritable()) return;
  dialogKind.value = kind;
  editingId.value = row.id;
  if (kind === 'definition') {
    const item = row as DeliverableDefinition;
    Object.assign(definitionForm, item);
  }
  if (kind === 'type') {
    const item = row as DeliverableType;
    Object.assign(typeForm, item);
  }
  if (kind === 'attribute') {
    const item = row as DeliverableAttribute;
    Object.assign(attributeForm, {
      ...item,
      unit: item.unit ?? '',
      exampleValue: item.exampleValue ?? '',
      enumOptions: item.enumOptions ?? ''
    });
  }
  if (kind === 'template') {
    const item = row as DirectoryTemplate;
    Object.assign(templateForm, {
      ...item,
      rootNodeJson: item.rootNodeJson ?? ''
    });
  }
  dialogVisible.value = true;
}

async function handleSave() {
  if (!currentProjectId.value || !assertWritable()) return;
  saving.value = true;
  try {
    if (dialogKind.value === 'definition') {
      await saveDefinition();
      definitions.value = await fetchDeliverableDefinitions(currentProjectId.value);
      selectedDefinition.value = definitions.value.find((item) => item.id === editingId.value) ?? definitions.value[0] ?? null;
      await loadTypes();
    }
    if (dialogKind.value === 'type') {
      await saveType();
      await loadTypes();
    }
    if (dialogKind.value === 'attribute') {
      await saveAttribute();
      await loadAttributes();
    }
    if (dialogKind.value === 'template') {
      await saveTemplate();
      templates.value = await fetchDirectoryTemplates(currentProjectId.value);
    }
    await loadStatus();
    dialogVisible.value = false;
    ElMessage.success('已保存');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handleDelete(kind: DialogKind, row: DeliverableDefinition | DeliverableType | DeliverableAttribute | DirectoryTemplate) {
  if (!currentProjectId.value || !assertWritable()) return;
  try {
    await ElMessageBox.confirm(`确认删除“${row.name}”？`, '删除交付物标准', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
    if (kind === 'definition') {
      await deleteDeliverableDefinition(currentProjectId.value, row.id);
      definitions.value = await fetchDeliverableDefinitions(currentProjectId.value);
      selectedDefinition.value = definitions.value[0] ?? null;
      await loadTypes();
    }
    if (kind === 'type') {
      await deleteDeliverableType(currentProjectId.value, row.id);
      await loadTypes();
    }
    if (kind === 'attribute') {
      await deleteDeliverableAttribute(currentProjectId.value, row.id);
      await loadAttributes();
    }
    if (kind === 'template') {
      await deleteDirectoryTemplate(currentProjectId.value, row.id);
      templates.value = await fetchDirectoryTemplates(currentProjectId.value);
    }
    await loadStatus();
    ElMessage.success('已删除');
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  }
}

async function saveDefinition() {
  if (!currentProjectId.value || !definitionForm.nodeTypeId) {
    throw new Error('请选择节点类型');
  }
  const payload = {
    nodeTypeId: definitionForm.nodeTypeId,
    code: definitionForm.code,
    name: definitionForm.name,
    category: definitionForm.category,
    required: definitionForm.required,
    sortOrder: definitionForm.sortOrder,
    status: definitionForm.status
  };
  if (editingId.value) {
    await updateDeliverableDefinition(currentProjectId.value, editingId.value, payload);
  } else {
    const created = await createDeliverableDefinition(currentProjectId.value, payload);
    editingId.value = created.id;
  }
}

async function saveType() {
  if (!currentProjectId.value || !selectedDefinition.value) return;
  const payload = {
    deliverableDefinitionId: selectedDefinition.value.id,
    code: typeForm.code,
    name: typeForm.name,
    fileKind: typeForm.fileKind,
    bindingStrategy: typeForm.bindingStrategy,
    sortOrder: typeForm.sortOrder,
    status: typeForm.status
  };
  if (editingId.value) {
    await updateDeliverableType(currentProjectId.value, editingId.value, payload);
  } else {
    const created = await createDeliverableType(currentProjectId.value, payload);
    editingId.value = created.id;
  }
}

async function saveAttribute() {
  if (!currentProjectId.value || !selectedType.value) return;
  const payload = {
    deliverableTypeId: selectedType.value.id,
    code: attributeForm.code,
    name: attributeForm.name,
    valueType: attributeForm.valueType,
    unit: attributeForm.unit,
    required: attributeForm.required,
    exampleValue: attributeForm.exampleValue,
    enumOptions: attributeForm.enumOptions,
    sortOrder: attributeForm.sortOrder,
    status: attributeForm.status
  };
  if (editingId.value) {
    await updateDeliverableAttribute(currentProjectId.value, editingId.value, payload);
  } else {
    const created = await createDeliverableAttribute(currentProjectId.value, payload);
    editingId.value = created.id;
  }
}

async function saveTemplate() {
  if (!currentProjectId.value) return;
  const payload = {
    templateType: templateForm.templateType,
    name: templateForm.name,
    rootNodeJson: templateForm.rootNodeJson,
    sourceType: templateForm.sourceType,
    sortOrder: templateForm.sortOrder,
    status: templateForm.status
  };
  if (editingId.value) {
    await updateDirectoryTemplate(currentProjectId.value, editingId.value, payload);
  } else {
    const created = await createDirectoryTemplate(currentProjectId.value, payload);
    editingId.value = created.id;
  }
}

function resetForm(kind: DialogKind) {
  if (kind === 'definition') {
    Object.assign(definitionForm, {
      nodeTypeId: nodeTypes.value[0]?.id ?? null,
      code: '',
      name: '',
      category: 'DOCUMENT',
      required: true,
      sortOrder: definitions.value.length + 1,
      status: 'ACTIVE'
    });
  }
  if (kind === 'type') {
    Object.assign(typeForm, {
      code: '',
      name: '',
      fileKind: 'DOCUMENT',
      bindingStrategy: 'SECTION_NODE',
      sortOrder: types.value.length + 1,
      status: 'ACTIVE'
    });
  }
  if (kind === 'attribute') {
    Object.assign(attributeForm, {
      code: '',
      name: '',
      valueType: 'TEXT',
      unit: '',
      required: false,
      exampleValue: '',
      enumOptions: '',
      sortOrder: attributes.value.length + 1,
      status: 'ACTIVE'
    });
  }
  if (kind === 'template') {
    Object.assign(templateForm, {
      templateType: 'DOCUMENT',
      name: '',
      rootNodeJson: '{"children":[]}',
      sourceType: 'MANUAL',
      sortOrder: templates.value.length + 1,
      status: 'ACTIVE'
    });
  }
}
</script>

<style scoped>
.deliverable-layout {
  display: grid;
  gap: 18px;
}

.deliverable-panel {
  display: grid;
  gap: 12px;
}

.deliverable-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.deliverable-panel__header h2 {
  margin: 0;
  font-size: 18px;
}
</style>
