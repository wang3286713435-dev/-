<template>
  <section class="master-data-page">
    <div class="master-data-page__header">
      <div>
        <span class="master-data-page__eyebrow">工程主数据</span>
        <h1>部位树</h1>
        <p>{{ projectLabel }} · 把真实项目拆成可归属、可交付的工程节点。</p>
      </div>
      <div class="master-data-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog(null)">新增根节点</el-button>
      </div>
    </div>

    <MasterDataStepNav active="sections" />

    <StandardStatusPanel :status="standardStatus" />

    <section class="master-workspace-callout">
      <div>
        <span>当前任务</span>
        <strong>确认项目真实结构，再进入节点类型</strong>
        <p>部位树来自资产目录和人工维护，它决定文件归属、缺失项和交付状态按什么项目结构展开。</p>
      </div>
      <div class="master-workspace-callout__actions">
        <el-button type="primary" @click="goNodeTypes">去节点类型</el-button>
        <el-button @click="goDeliverableStandard">查看交付物标准</el-button>
      </div>
    </section>

    <section class="master-workspace-panel">
      <div class="master-workspace-panel__header">
        <div>
          <h2>项目结构</h2>
          <p>按楼栋、楼层、区域或系统维护。不要在这里表达文件夹路径，文件归属会单独记录。</p>
        </div>
      </div>
      <el-table
        v-loading="loading"
        :data="sectionTree"
        row-key="id"
        default-expand-all
        class="master-table"
        empty-text="暂无部位节点"
      >
        <el-table-column prop="name" label="部位名称" min-width="220" />
        <el-table-column prop="code" label="编码" width="180" />
        <el-table-column prop="level" label="层级" width="90" />
        <el-table-column prop="path" label="路径" min-width="180" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openCreateDialog(row)">添加下级</el-button>
            <el-button text @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="danger" @click="handleDelete(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingNode ? '编辑部位节点' : '新增部位节点'" width="520px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="父节点">
          <el-input :model-value="parentNode?.name ?? '根节点'" disabled />
          <div class="field-hint">根节点适合放楼栋、区域或系统；添加下级用于继续拆分楼层、房间或具体专业部位。</div>
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" maxlength="64" />
          <div class="field-hint">建议使用项目内部可识别的编号，后续导入和交付视图会用它追溯部位。</div>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="128" />
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
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh } from '@element-plus/icons-vue';

import MasterDataStepNav from '@/modules/master-data/components/MasterDataStepNav.vue';
import StandardStatusPanel from '@/modules/master-data/components/StandardStatusPanel.vue';
import {
  createSectionNode,
  deleteSectionNode,
  fetchSectionTree,
  fetchStandardStatus,
  updateSectionNode,
  type SectionNode,
  type StandardStatus
} from '@/modules/master-data/api/masterData';
import { useAuthStore } from '@/stores/auth';
import { useProjectWorkspaceContext } from '@/modules/core/composables/useProjectWorkspaceContext';

const authStore = useAuthStore();
const router = useRouter();
const { workspaceProjectId } = useProjectWorkspaceContext();
const loading = ref(false);
const saving = ref(false);
const sectionTree = ref<SectionNode[]>([]);
const standardStatus = ref<StandardStatus | null>(null);
const dialogVisible = ref(false);
const editingNode = ref<SectionNode | null>(null);
const parentNode = ref<SectionNode | null>(null);

const form = reactive({
  code: '',
  name: '',
  sortOrder: 0,
  status: 'ACTIVE'
});

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' }
];

const currentProjectId = computed(() => workspaceProjectId.value ?? authStore.currentProjectId);
const projectLabel = computed(() => {
  const project = authStore.currentUser?.projects.find((item) => item.id === currentProjectId.value)
    ?? authStore.currentUser?.currentProject;
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
    const [status, tree] = await Promise.all([
      fetchStandardStatus(currentProjectId.value),
      fetchSectionTree(currentProjectId.value)
    ]);
    standardStatus.value = status;
    sectionTree.value = tree;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '部位树加载失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog(parent: SectionNode | null) {
  editingNode.value = null;
  parentNode.value = parent;
  Object.assign(form, {
    code: '',
    name: '',
    sortOrder: 0,
    status: 'ACTIVE'
  });
  dialogVisible.value = true;
}

function openEditDialog(node: SectionNode) {
  editingNode.value = node;
  parentNode.value = findNode(sectionTree.value, node.parentId);
  Object.assign(form, {
    code: node.code,
    name: node.name,
    sortOrder: node.sortOrder,
    status: node.status
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
      parentId: parentNode.value?.id ?? null,
      code: form.code,
      name: form.name,
      sortOrder: form.sortOrder,
      status: form.status
    };
    if (editingNode.value) {
      await updateSectionNode(currentProjectId.value, editingNode.value.id, payload);
      ElMessage.success('部位节点已更新');
    } else {
      await createSectionNode(currentProjectId.value, payload);
      ElMessage.success('部位节点已创建');
    }
    dialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handleDelete(node: SectionNode) {
  if (!currentProjectId.value) {
    return;
  }
  try {
    await ElMessageBox.confirm(`确认停用“${node.name}”及其下级节点？`, '停用部位节点', {
      type: 'warning',
      confirmButtonText: '停用',
      cancelButtonText: '取消'
    });
    await deleteSectionNode(currentProjectId.value, node.id);
    ElMessage.success('部位节点已停用');
    await loadPage();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  }
}

function findNode(nodes: SectionNode[], nodeId: number | null): SectionNode | null {
  if (!nodeId) {
    return null;
  }
  for (const node of nodes) {
    if (node.id === nodeId) {
      return node;
    }
    const child = findNode(node.children ?? [], nodeId);
    if (child) {
      return child;
    }
  }
  return null;
}

function goNodeTypes() {
  if (!currentProjectId.value) return;
  router.push({ name: 'project-master-data-node-types', params: { projectId: currentProjectId.value } });
}

function goDeliverableStandard() {
  if (!currentProjectId.value) return;
  router.push({ name: 'project-master-data-deliverable-standard', params: { projectId: currentProjectId.value } });
}
</script>
