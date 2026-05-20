<template>
  <section class="master-data-page">
    <div class="master-data-page__header">
      <div>
        <h1>节点类型</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="master-data-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button :disabled="!nodeTypes.length" @click="handleLockAll">全部锁定</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增节点类型</el-button>
      </div>
    </div>

    <StandardStatusPanel :status="standardStatus" />

    <el-alert
      class="masterdata-review-alert"
      type="info"
      :closable="false"
      show-icon
      title="如果节点类型来自接入草案，它只是标准配置建议。锁定前请确认这些类型确实适用于当前真实项目。"
    />

    <section class="workflow-guide">
      <div class="workflow-guide__main">
        <span class="workflow-guide__step">第 2 步</span>
        <h2>确认哪些部位层级可以承载交付标准</h2>
        <p>
          节点类型用于说明部位树里哪些层级会参与交付，例如楼层、系统或机房。锁定后，平台会把这些类型作为后续交付物标准和缺失项计算的稳定依据。
        </p>
      </div>
      <ol class="workflow-guide__steps">
        <li>先确认名称和适用层级是否符合项目拆分方式。</li>
        <li>确认无误后锁定。锁定后不建议频繁调整结构，避免影响已配置标准。</li>
        <li>全部锁定后，进入交付物标准页面，配置要交什么资料。</li>
      </ol>
    </section>

    <el-alert
      class="node-type-lock"
      :type="lockStatus?.allNodeTypesLocked ? 'success' : 'info'"
      :closable="false"
      show-icon
    >
      <template #title>
        节点类型锁定状态：{{ lockStatus?.allNodeTypesLocked ? '全部锁定' : '仍可维护' }}
      </template>
      <p class="status-helper">
        {{
          lockStatus?.allNodeTypesLocked
            ? '当前节点类型已冻结，可以继续配置交付物标准。'
            : '锁定前仍可调整。锁定后才能作为交付标准配置的前置条件。'
        }}
      </p>
    </el-alert>

    <el-table v-loading="loading" :data="nodeTypes" class="master-table" empty-text="暂无节点类型">
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="code" label="编码" width="180" />
      <el-table-column prop="scopeLevel" label="适用层级" width="110" />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="locked" label="锁定" width="110">
        <template #default="{ row }">
          <el-tag :type="row.locked ? 'success' : 'warning'">{{ row.locked ? '已锁定' : '未锁定' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button text :disabled="row.locked" @click="openEditDialog(row)">编辑</el-button>
          <el-button text type="primary" :disabled="row.locked" @click="handleLock(row)">锁定</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingNodeType ? '编辑节点类型' : '新增节点类型'" width="520px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="编码">
          <el-input v-model="form.code" maxlength="64" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="128" />
        </el-form-item>
        <el-form-item label="适用层级">
          <el-input-number v-model="form.scopeLevel" :min="1" controls-position="right" />
          <div class="field-hint">填写它适用于部位树的第几层，例如 1 代表根节点，2 代表根节点下一级。</div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-segmented v-model="form.status" :options="statusOptions" />
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
import { Plus, Refresh } from '@element-plus/icons-vue';

import StandardStatusPanel from '@/modules/master-data/components/StandardStatusPanel.vue';
import {
  createNodeType,
  fetchNodeTypeLockStatus,
  fetchNodeTypes,
  fetchStandardStatus,
  lockAllNodeTypes,
  lockNodeType,
  updateNodeType,
  type NodeType,
  type NodeTypeLockStatus,
  type StandardStatus
} from '@/modules/master-data/api/masterData';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const nodeTypes = ref<NodeType[]>([]);
const standardStatus = ref<StandardStatus | null>(null);
const lockStatus = ref<NodeTypeLockStatus | null>(null);
const editingNodeType = ref<NodeType | null>(null);

const form = reactive({
  code: '',
  name: '',
  scopeLevel: 1,
  sortOrder: 0,
  status: 'ACTIVE'
});

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' }
];

const currentProjectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => {
  const project = authStore.currentUser?.currentProject;
  return project ? `${project.code} | ${project.name}` : '等待项目上下文';
});

watch(
  currentProjectId,
  () => {
    loadPage();
  },
  { immediate: true }
);

async function loadPage() {
  if (!currentProjectId.value) {
    return;
  }
  loading.value = true;
  try {
    const [status, types, lock] = await Promise.all([
      fetchStandardStatus(currentProjectId.value),
      fetchNodeTypes(currentProjectId.value),
      fetchNodeTypeLockStatus(currentProjectId.value)
    ]);
    standardStatus.value = status;
    nodeTypes.value = types;
    lockStatus.value = lock;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '节点类型加载失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  editingNodeType.value = null;
  Object.assign(form, {
    code: '',
    name: '',
    scopeLevel: 1,
    sortOrder: 0,
    status: 'ACTIVE'
  });
  dialogVisible.value = true;
}

function openEditDialog(nodeType: NodeType) {
  editingNodeType.value = nodeType;
  Object.assign(form, {
    code: nodeType.code,
    name: nodeType.name,
    scopeLevel: nodeType.scopeLevel,
    sortOrder: nodeType.sortOrder,
    status: nodeType.status
  });
  dialogVisible.value = true;
}

async function handleSave() {
  if (!currentProjectId.value) {
    return;
  }
  saving.value = true;
  try {
    const payload = {
      code: form.code,
      name: form.name,
      scopeLevel: form.scopeLevel,
      sortOrder: form.sortOrder,
      status: form.status
    };
    if (editingNodeType.value) {
      await updateNodeType(currentProjectId.value, editingNodeType.value.id, payload);
      ElMessage.success('节点类型已更新');
    } else {
      await createNodeType(currentProjectId.value, payload);
      ElMessage.success('节点类型已创建');
    }
    dialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handleLock(nodeType: NodeType) {
  if (!currentProjectId.value) {
    return;
  }
  try {
    await ElMessageBox.confirm(
      `锁定“${nodeType.name}”后，该类型会作为交付标准配置依据，不建议再频繁修改。确认继续？`,
      '锁定节点类型',
      {
        type: 'warning',
        confirmButtonText: '锁定',
        cancelButtonText: '取消'
      }
    );
    await lockNodeType(currentProjectId.value, nodeType.id);
    ElMessage.success('节点类型已锁定');
    await loadPage();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  }
}

async function handleLockAll() {
  if (!currentProjectId.value) {
    return;
  }
  try {
    await ElMessageBox.confirm(
      '全部锁定后，节点类型将成为交付物标准配置的稳定底座。确认当前结构已核对完成？',
      '全部锁定',
      {
        type: 'warning',
        confirmButtonText: '全部锁定',
        cancelButtonText: '取消'
      }
    );
    await lockAllNodeTypes(currentProjectId.value);
    ElMessage.success('节点类型已全部锁定');
    await loadPage();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  }
}
</script>
