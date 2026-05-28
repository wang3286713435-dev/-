import { http } from '@/app/http';
import type { ApiResponse } from '@/modules/core/api/types';

const bimSubmissionBackendEnabled = false;

export interface BimSubmissionContract {
  id: number;
  projectId: number;
  contractCode: string;
  versionNo: string;
  status: string;
  sourceStandardId: string;
  sourcePath: string | null;
  precedencePolicy: string;
  fieldRulesJson: string;
  valuePackageJson: string | null;
  qualityRulesJson: string | null;
  publishedAt: string | null;
  publishedBy: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface BimSubmissionBatch {
  id: number;
  projectId: number;
  contractId: number;
  contractVersionNo: string;
  batchNo: string;
  batchName: string;
  status: string;
  valuePackageJson: string | null;
  qualitySummaryJson: string | null;
  componentCount: number;
  warningCount: number;
  workOrderCount: number;
  issuedAt: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BimComponentSummary {
  id: number;
  projectId: number;
  batchId: number;
  snapshotId: number;
  batchNo: string;
  externalElementId: string | null;
  elementUniqueId: string;
  componentCode: string | null;
  componentName: string | null;
  categoryName: string | null;
  familyName: string | null;
  typeName: string | null;
  levelName: string | null;
  disciplineCode: string | null;
  systemCode: string | null;
  writeStatus: string;
  qualityStatus: string;
  qualityFlagsJson: string | null;
  updatedAt: string;
}

export interface BimComponentFieldValue {
  id: number;
  componentId: number;
  fieldCode: string;
  fieldName: string | null;
  fieldValue: string | null;
  unitName: string | null;
  valueSource: string | null;
  sourceParameterName: string | null;
  validationStatus: string;
  validationMessage: string | null;
  rawValue: string | null;
  updatedAt: string;
}

export interface BimComponentDetail {
  component: BimComponentSummary;
  rawParametersJson: string | null;
  fields: BimComponentFieldValue[];
}

export interface BimSubmissionWorkOrder {
  id: number;
  projectId: number;
  batchId: number;
  componentId: number | null;
  fieldCode: string | null;
  title: string;
  description: string | null;
  status: string;
  priority: string;
  assignedToType: string;
  requestedValue: string | null;
  currentValue: string | null;
  pluginResponseJson: string | null;
  resolvedAt: string | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BimSubmissionArchive {
  id: number;
  projectId: number;
  batchId: number;
  contractId: number;
  archiveNo: string;
  status: string;
  summaryJson: string | null;
  createdAt: string;
  createdBy: number | null;
}

export interface BimSubmissionOverview {
  currentContract: BimSubmissionContract;
  batches: BimSubmissionBatch[];
  recentComponents: BimComponentSummary[];
  workOrders: BimSubmissionWorkOrder[];
  archives: BimSubmissionArchive[];
}

export type BimDataCenterProfessionStatus = 'RECORDED' | 'PENDING';
export type BimDataCenterQualityStatus = 'PASSED' | 'WARNING' | 'MISSING';
export type BimCodeCandidateStatus = 'PASSED' | 'WARNING' | 'CONFLICT' | 'PENDING_CONFIRMATION';
export type BimPreviewStatus = 'METADATA_ONLY' | 'READY' | 'PENDING_MODEL_VIEWER';
export type BimStandardPackageStatus = 'DRAFT' | 'PENDING_CONFIRMATION' | 'ACTIVE' | 'PENDING';
export type BimUploadedCodeStatus = 'PASSED' | 'WARNING' | 'CONFLICT' | 'PENDING_CONFIRMATION';
export type BimUploadedCodeIssueType =
  | 'FORMAT'
  | 'PROFESSION'
  | 'COMPONENT_TYPE'
  | 'MISSING_FIELD'
  | 'DUPLICATE'
  | 'UNKNOWN_SEGMENT'
  | 'PENDING_RULE';

export interface BimCodeValidationIssue {
  type: BimUploadedCodeIssueType;
  field: string;
  message: string;
}

export interface BimCodeConstraintRule {
  id: string;
  professionCode: string;
  codeType: string;
  title: string;
  status: BimStandardPackageStatus;
  patternDescription: string;
  uniquenessScope: string;
}

export interface BimUploadedCodeRecord {
  id: number;
  projectId: number;
  professionCode: string;
  professionName: string;
  componentType: string;
  componentName: string;
  codeType: string;
  rawCodeValue: string;
  normalizedCodeValue: string;
  sourceParameterName: string;
  standardPackageStatus: BimStandardPackageStatus;
  modelFingerprint: string;
  revitElementId: string;
  revitUniqueId: string;
  batchNo: string;
  validationStatus: BimUploadedCodeStatus;
  validationIssues: BimCodeValidationIssue[];
  sample: boolean;
  uploadedAt: string;
  updatedAt: string;
}

export interface BimUploadedCodeRecordPayload {
  professionCode: string;
  professionName: string;
  componentType: string;
  componentName: string;
  codeType: string;
  rawCodeValue: string;
  sourceParameterName: string;
  modelFingerprint: string;
  revitElementId: string;
  revitUniqueId: string;
  batchNo: string;
  standardPackageStatus?: BimStandardPackageStatus;
}

export interface BimUploadedCodeRecordQuery {
  professionCode?: string;
  keyword?: string;
  validationStatus?: BimUploadedCodeStatus | '';
  includeSamples?: boolean;
}

export interface BimDataCenterProfessionSummary {
  code: string;
  name: string;
  status: BimDataCenterProfessionStatus;
  source: string;
  note: string;
  componentCount: number;
  warningCount: number;
  missingFieldCount: number;
  lastUploadedAt: string | null;
}

export interface BimDataCenterSummary {
  professionCode: string;
  professionName: string;
  componentCount: number;
  passedCount: number;
  warningCount: number;
  missingFieldCount: number;
  modelCount: number;
  batchCount: number;
  lastUploadedAt: string | null;
  status: BimDataCenterProfessionStatus;
  note: string;
}

export interface BimDataCenterComponent {
  id: number;
  projectId: number;
  batchId: number;
  batchNo: string;
  batchName: string;
  professionCode: string;
  professionName: string;
  componentCode: string;
  shenzhenComponentIdentifier: string;
  componentName: string;
  systemCode: string;
  systemName: string;
  modelName: string;
  modelFingerprint: string;
  levelName: string;
  familyName: string;
  typeName: string;
  fieldCompletionRate: number;
  qualityStatus: BimDataCenterQualityStatus;
  warningCount: number;
  missingFieldCount: number;
  pluginVersion: string;
  revitVersion: string;
  standardVersion?: string;
  revitElementId?: string;
  revitUniqueId?: string;
  codeCandidateStatus?: BimCodeCandidateStatus;
  codeConflictReason?: string | null;
  previewStatus?: BimPreviewStatus;
  uploadEvidence: string;
  uploadedAt: string;
}

export interface BimDataCenterFieldValue {
  id: number;
  componentId: number;
  fieldName: string;
  fieldValue: string;
  unitName: string | null;
  valueType: string;
  sourceParameterName: string;
  validationStatus: BimDataCenterQualityStatus;
  validationMessage: string | null;
  standardSource: string;
  requiredPolicy: string;
}

export interface BimDataCenterUploadEvidence {
  snapshotNo: string;
  uploadMode: string;
  uploadedBy: string;
  contractVersion: string;
  modelFingerprint: string;
  pluginVersion: string;
  revitVersion: string;
}

export interface ComponentCodeCandidate {
  componentId: number;
  candidateCode: string;
  standardVersion: string;
  generatedBy: string;
  validationStatus: BimCodeCandidateStatus;
  uniquenessScope: string;
  conflictReason: string | null;
  checkedAt: string;
}

export interface BimComponentLink {
  componentCode: string;
  professionCode: string;
  modelFingerprint: string;
  revitElementId: string;
  revitUniqueId: string;
  batchNo: string;
  previewStatus: BimPreviewStatus;
  previewNote: string;
}

export interface BimExpectedFieldCoverage {
  expectedFieldCount: number;
  filledFieldCount: number;
  missingRequiredFields: string[];
}

export interface BimDataCenterComponentDetail {
  component: BimDataCenterComponent;
  fields: BimDataCenterFieldValue[];
  rawParameters: Record<string, string | number | boolean | null>;
  uploadEvidence: BimDataCenterUploadEvidence;
  codeCandidate: ComponentCodeCandidate;
  componentLink: BimComponentLink;
  fieldCoverage: BimExpectedFieldCoverage;
}

export interface BimDataCenterOverview {
  professionSummaries: BimDataCenterProfessionSummary[];
  summary: BimDataCenterSummary;
  components: BimDataCenterComponent[];
}

export interface BimComponentQuery {
  batchId?: number;
  keyword?: string;
  qualityStatus?: string;
  limit?: number;
}

export interface BimDataCenterQuery {
  professionCode?: string;
  batchId?: number;
  keyword?: string;
  qualityStatus?: BimDataCenterQualityStatus | '';
  limit?: number;
}

export interface BimWorkOrderPayload {
  componentId: number;
  fieldCode: string;
  title: string;
  description?: string;
  priority?: string;
  requestedValue?: string;
}

const mockStandardPath =
  '/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject';

const mockFieldRules = [
  {
    fieldCode: 'component_code',
    fieldName: '构件编码',
    disciplineScope: ['ARCH', 'STR', 'MEP', 'HVAC', 'ELEC', 'PLUMBING'],
    required: true,
    source: 'OWNER_STANDARD',
    pluginParameterName: 'DD_ComponentCode',
    validation: '按项目-专业-系统-流水号生成，插件只写入平台下发值'
  },
  {
    fieldCode: 'tag_code',
    fieldName: '构件标识码',
    disciplineScope: ['ARCH', 'STR', 'MEP', 'HVAC', 'ELEC', 'PLUMBING'],
    required: true,
    source: 'OWNER_STANDARD',
    pluginParameterName: 'DD_TagCode',
    validation: '同批次同模型内唯一'
  },
  {
    fieldCode: 'material_code',
    fieldName: '材料/设备编码',
    disciplineScope: ['ARCH', 'STR', 'MEP', 'HVAC', 'ELEC', 'PLUMBING'],
    required: true,
    source: 'COMPANY_STANDARD',
    pluginParameterName: 'DD_MaterialCode',
    validation: '甲方未约束时按公司内部材料编码兜底'
  },
  {
    fieldCode: 'discipline_code',
    fieldName: '专业编码',
    disciplineScope: ['ARCH', 'STR', 'MEP', 'HVAC', 'ELEC', 'PLUMBING'],
    required: true,
    source: 'OWNER_STANDARD',
    pluginParameterName: 'DD_Discipline',
    validation: '必须落在契约定义的全专业枚举内'
  },
  {
    fieldCode: 'system_code',
    fieldName: '系统编码',
    disciplineScope: ['MEP', 'HVAC', 'ELEC', 'PLUMBING'],
    required: false,
    source: 'MANUAL_OVERRIDE',
    pluginParameterName: 'DD_System',
    validation: '管理方可按报建批次覆盖'
  },
  {
    fieldCode: 'standard_conflict_note',
    fieldName: '冲突确认说明',
    disciplineScope: ['ARCH', 'STR', 'MEP', 'HVAC', 'ELEC', 'PLUMBING'],
    required: false,
    source: 'CONFLICT_REVIEW',
    pluginParameterName: 'DD_ConflictNote',
    validation: '甲方与公司规则冲突时由管理方确认'
  }
];

const mockQualityRules = [
  { ruleCode: 'REQUIRED_FIELD', severity: 'ERROR', description: '候选字段缺失进入质量提示' },
  { ruleCode: 'CONTRACT_VERSION', severity: 'WARNING', description: '插件上传标准版本与平台草案版本不一致时提示' },
  { ruleCode: 'UNIQUE_TAG_CODE', severity: 'ERROR', description: '同批次构件标识码不得重复' }
];

const mockValuePackage = {
  packageNo: 'VP-CT-BIM-2026-001',
  projectCode: 'CTOWER-BIM-001',
  contractVersionNo: 'V1.0',
  generatedBy: 'platform',
  pluginMode: 'online-contract-driven',
  standardPrecedence: 'OWNER_FIRST_COMPANY_FALLBACK'
};

const mockDate = '2026-05-27T09:30:00+08:00';
let mockContractPublishSequence = 0;
let mockBatchSequence = 3;
let mockWorkOrderSequence = 5002;
let mockArchiveSequence = 7002;
let mockUploadedCodeSequence = 9001;

const codeConstraintRules: BimCodeConstraintRule[] = [
  {
    id: 'intel-tag-code-draft',
    professionCode: 'intelligent',
    codeType: '位号编码',
    title: '智能化位号编码约束草案',
    status: 'DRAFT',
    patternDescription: '参考附件十一 Ctower 位号结构，平台先做格式、专业、唯一性和未知码段校验。',
    uniquenessScope: '同项目/同专业/同模型指纹/同上传批次'
  },
  {
    id: 'intel-component-code-draft',
    professionCode: 'intelligent',
    codeType: '构件编码',
    title: '智能化构件编码约束草案',
    status: 'DRAFT',
    patternDescription: '用于平台查询、链接和工单，不代表最终编码字段值已经冻结。',
    uniquenessScope: '同项目/同专业/同模型指纹/同上传批次'
  },
  {
    id: 'intel-system-id-draft',
    professionCode: 'intelligent',
    codeType: '深圳系统标识',
    title: '智能化系统标识约束草案',
    status: 'DRAFT',
    patternDescription: '按附件三系统表归属进行校验，未命中的系统归入待确认。',
    uniquenessScope: '同项目/同专业/同构件类型'
  },
  {
    id: 'intel-component-id-draft',
    professionCode: 'intelligent',
    codeType: '深圳构件标识',
    title: '智能化构件标识约束草案',
    status: 'DRAFT',
    patternDescription: '按附件三对象名称列匹配构件标识，未命中时进入待确认。',
    uniquenessScope: '同项目/同专业/同构件类型'
  },
  {
    id: 'intel-material-code-draft',
    professionCode: 'intelligent',
    codeType: '物料编码',
    title: '智能化物料编码约束草案',
    status: 'DRAFT',
    patternDescription: '参考附件十一 M 前缀物料编码结构，平台先检查格式和重复。',
    uniquenessScope: '同项目/同专业/同物料分类'
  },
  {
    id: 'intel-equipment-code-draft',
    professionCode: 'intelligent',
    codeType: '设备编码',
    title: '智能化设备编码约束草案',
    status: 'DRAFT',
    patternDescription: '参考附件十一设备编码结构，继承物料编码并追加顺序码。',
    uniquenessScope: '同项目/同专业/同设备物料分类'
  }
];

function normalizeCode(value: string) {
  return value.trim();
}

function hasFullWidthCharacter(value: string) {
  return /[^\x00-\x7F]/.test(value);
}

function createUploadedCodeRecord(
  id: number,
  payload: BimUploadedCodeRecordPayload & { projectId: number },
  sample = false
): BimUploadedCodeRecord {
  const timestamp = sample ? '2026-05-27T15:12:00+08:00' : now();
  const record: BimUploadedCodeRecord = {
    id,
    projectId: payload.projectId,
    professionCode: payload.professionCode,
    professionName: payload.professionName,
    componentType: payload.componentType,
    componentName: payload.componentName,
    codeType: payload.codeType,
    rawCodeValue: payload.rawCodeValue,
    normalizedCodeValue: normalizeCode(payload.rawCodeValue),
    sourceParameterName: payload.sourceParameterName,
    standardPackageStatus: payload.standardPackageStatus ?? 'DRAFT',
    modelFingerprint: payload.modelFingerprint,
    revitElementId: payload.revitElementId,
    revitUniqueId: payload.revitUniqueId,
    batchNo: payload.batchNo,
    validationStatus: 'PENDING_CONFIRMATION',
    validationIssues: [],
    sample,
    uploadedAt: timestamp,
    updatedAt: timestamp
  };
  return revalidateUploadedCodeRecord(record, []);
}

function createValidationIssue(
  type: BimUploadedCodeIssueType,
  field: string,
  message: string
): BimCodeValidationIssue {
  return { type, field, message };
}

function revalidateUploadedCodeRecord(
  record: BimUploadedCodeRecord,
  peers: BimUploadedCodeRecord[]
): BimUploadedCodeRecord {
  const normalizedCodeValue = normalizeCode(record.rawCodeValue);
  const issues: BimCodeValidationIssue[] = [];
  const matchedRule = codeConstraintRules.find((rule) => (
    rule.professionCode === record.professionCode && rule.codeType === record.codeType
  ));

  if (!normalizedCodeValue) {
    issues.push(createValidationIssue('MISSING_FIELD', '上传编码值', '插件没有上传编码值，平台只能保留待确认记录。'));
  }
  if (record.professionCode !== 'intelligent') {
    issues.push(createValidationIssue('PROFESSION', '专业', '当前仅智能化专业标准包已录入，其他专业暂不套用智能化规则。'));
  }
  if (!record.componentType.trim()) {
    issues.push(createValidationIssue('COMPONENT_TYPE', '构件类型', '构件类型为空，平台无法匹配专业构件标准包。'));
  }
  if (!matchedRule) {
    issues.push(createValidationIssue('UNKNOWN_SEGMENT', '编码类型', '当前编码类型未接入平台约束规则，需管理方维护后再校验。'));
  }
  if (hasFullWidthCharacter(normalizedCodeValue)) {
    issues.push(createValidationIssue('FORMAT', '上传编码值', '编码值包含中文或全角字符，需使用英文半角符号。'));
  }
  if (record.standardPackageStatus === 'PENDING' || record.standardPackageStatus === 'PENDING_CONFIRMATION') {
    issues.push(createValidationIssue('PENDING_RULE', '标准包状态', '当前专业标准包未录入或待确认，校验结果不能视为最终结论。'));
  }

  const duplicate = peers.some((peer) => (
    peer.id !== record.id
    && peer.projectId === record.projectId
    && peer.professionCode === record.professionCode
    && peer.modelFingerprint === record.modelFingerprint
    && peer.batchNo === record.batchNo
    && normalizeCode(peer.rawCodeValue) === normalizedCodeValue
    && normalizedCodeValue !== ''
  ));
  if (duplicate) {
    issues.push(createValidationIssue('DUPLICATE', '上传编码值', '同项目、同专业、同模型指纹、同批次内编码重复，平台不自动覆盖。'));
  }

  const validationStatus: BimUploadedCodeStatus = issues.some((issue) => issue.type === 'DUPLICATE')
    ? 'CONFLICT'
    : issues.some((issue) => ['FORMAT', 'MISSING_FIELD', 'UNKNOWN_SEGMENT'].includes(issue.type))
      ? 'WARNING'
      : issues.length > 0
        ? 'PENDING_CONFIRMATION'
        : 'PASSED';

  return {
    ...record,
    normalizedCodeValue,
    validationStatus,
    validationIssues: issues
  };
}

function getUploadedCodeRecordRows(projectId: number, includeSamples = false) {
  const sourceRows = includeSamples ? [...uploadedCodeRecords, ...sampleUploadedCodeRecords] : uploadedCodeRecords;
  const projectRows = sourceRows.filter((record) => record.projectId === projectId || projectId > 0);
  return projectRows.map((record) => revalidateUploadedCodeRecord({ ...record, projectId }, projectRows));
}

const sampleUploadedCodeRecords: BimUploadedCodeRecord[] = [
  createUploadedCodeRecord(8801, {
    projectId: 1,
    professionCode: 'intelligent',
    professionName: '智能化',
    componentType: '服务器',
    componentName: '应用服务器 SRV-01',
    codeType: '位号编码',
    rawCodeValue: 'Ctower-A01-MAD01-SRV001',
    sourceParameterName: 'DD_TagCode',
    modelFingerprint: 'sha256:sample-intel-r01-a8392',
    revitElementId: '884201',
    revitUniqueId: 'sample-revit-8801-srv001',
    batchNo: '示例批次',
    standardPackageStatus: 'DRAFT'
  }, true),
  createUploadedCodeRecord(8802, {
    projectId: 1,
    professionCode: 'intelligent',
    professionName: '智能化',
    componentType: '交换机',
    componentName: '核心交换机 SW-Core-01',
    codeType: '位号编码',
    rawCodeValue: 'Ctower-A01-MAD01-SWT001',
    sourceParameterName: 'DD_TagCode',
    modelFingerprint: 'sha256:sample-intel-r01-a8392',
    revitElementId: '884202',
    revitUniqueId: 'sample-revit-8802-swt001',
    batchNo: '示例批次',
    standardPackageStatus: 'DRAFT'
  }, true),
  createUploadedCodeRecord(8803, {
    projectId: 1,
    professionCode: 'intelligent',
    professionName: '智能化',
    componentType: '楼层交换机',
    componentName: '楼层交换机 SW-12F-02',
    codeType: '位号编码',
    rawCodeValue: 'Ctower-A01-MAD01-SWT001',
    sourceParameterName: 'DD_TagCode',
    modelFingerprint: 'sha256:sample-intel-r01-a8392',
    revitElementId: '884203',
    revitUniqueId: 'sample-revit-8803-swt001-duplicate',
    batchNo: '示例批次',
    standardPackageStatus: 'DRAFT'
  }, true),
  createUploadedCodeRecord(8804, {
    projectId: 1,
    professionCode: 'intelligent',
    professionName: '智能化',
    componentType: '门禁控制器',
    componentName: '门禁控制器 ACS-008',
    codeType: '位号编码',
    rawCodeValue: 'Ctower-A01-PSS02-ACS００８',
    sourceParameterName: 'DD_TagCode',
    modelFingerprint: 'sha256:sample-intel-r01-a8392',
    revitElementId: '884204',
    revitUniqueId: 'sample-revit-8804-acs008-fullwidth',
    batchNo: '示例批次',
    standardPackageStatus: 'DRAFT'
  }, true),
  createUploadedCodeRecord(8805, {
    projectId: 1,
    professionCode: 'intelligent',
    professionName: '智能化',
    componentType: '传感器',
    componentName: '送风温湿度传感器 SEN-032',
    codeType: '深圳系统标识',
    rawCodeValue: '',
    sourceParameterName: 'DD_SZ_SystemId',
    modelFingerprint: 'sha256:sample-intel-r00-b5114',
    revitElementId: '884205',
    revitUniqueId: 'sample-revit-8805-sen032-missing',
    batchNo: '示例批次',
    standardPackageStatus: 'DRAFT'
  }, true)
];

const uploadedCodeRecords: BimUploadedCodeRecord[] = [];

let mockContract: BimSubmissionContract = {
  id: 9001,
  projectId: 1,
  contractCode: 'CTOWER-BIM-SUBMISSION',
  versionNo: 'V1.0',
  status: 'PUBLISHED',
  sourceStandardId: 'DigitalDeliveryProject',
  sourcePath: mockStandardPath,
  precedencePolicy: 'OWNER_FIRST_COMPANY_FALLBACK',
  fieldRulesJson: JSON.stringify(mockFieldRules),
  valuePackageJson: JSON.stringify(mockValuePackage),
  qualityRulesJson: JSON.stringify(mockQualityRules),
  publishedAt: mockDate,
  publishedBy: 1,
  createdAt: mockDate,
  updatedAt: mockDate
};

const mockBatches: BimSubmissionBatch[] = [
  {
    id: 101,
    projectId: 1,
    contractId: 9001,
    contractVersionNo: 'V1.0',
    batchNo: 'BIM-CT-20260527-001',
    batchName: 'C塔全专业报建首轮',
    status: 'RECTIFICATION',
    valuePackageJson: JSON.stringify(mockValuePackage),
    qualitySummaryJson: JSON.stringify({ passed: 2, warning: 1, missingFields: 1 }),
    componentCount: 3,
    warningCount: 1,
    workOrderCount: 1,
    issuedAt: '2026-05-27T10:00:00+08:00',
    submittedAt: '2026-05-27T11:20:00+08:00',
    reviewedAt: '2026-05-27T14:10:00+08:00',
    archivedAt: null,
    createdAt: '2026-05-27T10:00:00+08:00',
    updatedAt: '2026-05-27T14:10:00+08:00'
  },
  {
    id: 102,
    projectId: 1,
    contractId: 9001,
    contractVersionNo: 'V1.0',
    batchNo: 'BIM-CT-20260526-002',
    batchName: 'C塔机电样板闭环批次',
    status: 'CLOSED',
    valuePackageJson: JSON.stringify(mockValuePackage),
    qualitySummaryJson: JSON.stringify({ passed: 2, warning: 0, missingFields: 0 }),
    componentCount: 2,
    warningCount: 0,
    workOrderCount: 0,
    issuedAt: '2026-05-26T09:00:00+08:00',
    submittedAt: '2026-05-26T11:30:00+08:00',
    reviewedAt: '2026-05-26T16:10:00+08:00',
    archivedAt: '2026-05-26T17:40:00+08:00',
    createdAt: '2026-05-26T09:00:00+08:00',
    updatedAt: '2026-05-26T17:40:00+08:00'
  }
];

const mockComponents: BimComponentSummary[] = [
  {
    id: 1001,
    projectId: 1,
    batchId: 101,
    snapshotId: 3001,
    batchNo: 'BIM-CT-20260527-001',
    externalElementId: '489223',
    elementUniqueId: '7f0b7d2f-ctower-ahu-0101',
    componentCode: 'CT-MEP-AHU-0101',
    componentName: 'C塔 12F 新风机组 AHU-0101',
    categoryName: '机械设备',
    familyName: '空调机组-平台编码族',
    typeName: 'AHU-18000m3/h',
    levelName: '12F',
    disciplineCode: 'HVAC',
    systemCode: 'MEP-AHU',
    writeStatus: 'WRITTEN',
    qualityStatus: 'PASSED',
    qualityFlagsJson: JSON.stringify([]),
    updatedAt: '2026-05-27T11:24:00+08:00'
  },
  {
    id: 1002,
    projectId: 1,
    batchId: 101,
    snapshotId: 3001,
    batchNo: 'BIM-CT-20260527-001',
    externalElementId: '489557',
    elementUniqueId: '7f0b7d2f-ctower-vav-1208',
    componentCode: 'CT-HVAC-VAV-1208',
    componentName: 'C塔 12F 变风量末端 VAV-1208',
    categoryName: '风管末端',
    familyName: 'VAV BOX-报建族',
    typeName: 'VAV-500L/s',
    levelName: '12F',
    disciplineCode: 'HVAC',
    systemCode: 'MEP-AIR',
    writeStatus: 'PARTIAL',
    qualityStatus: 'WARNING',
    qualityFlagsJson: JSON.stringify([{ fieldCode: 'material_code', message: '材料/设备编码缺失' }]),
    updatedAt: '2026-05-27T11:26:00+08:00'
  },
  {
    id: 1003,
    projectId: 1,
    batchId: 101,
    snapshotId: 3001,
    batchNo: 'BIM-CT-20260527-001',
    externalElementId: '503912',
    elementUniqueId: '7f0b7d2f-ctower-db-1201',
    componentCode: 'CT-ELEC-DB-1201',
    componentName: 'C塔 12F 照明配电箱 DB-1201',
    categoryName: '电气设备',
    familyName: '配电箱-报建族',
    typeName: 'AL-12F',
    levelName: '12F',
    disciplineCode: 'ELEC',
    systemCode: 'EL-LTG',
    writeStatus: 'WRITTEN',
    qualityStatus: 'PASSED',
    qualityFlagsJson: JSON.stringify([]),
    updatedAt: '2026-05-27T11:28:00+08:00'
  },
  {
    id: 1004,
    projectId: 1,
    batchId: 102,
    snapshotId: 3000,
    batchNo: 'BIM-CT-20260526-002',
    externalElementId: '477001',
    elementUniqueId: '7f0b7d2f-ctower-pump-b2-03',
    componentCode: 'CT-PLUMB-PUMP-B2-03',
    componentName: 'C塔 B2 给水泵 PUMP-03',
    categoryName: '给排水设备',
    familyName: '水泵-平台编码族',
    typeName: 'PUMP-45kW',
    levelName: 'B2',
    disciplineCode: 'PLUMBING',
    systemCode: 'WS-PUMP',
    writeStatus: 'WRITTEN',
    qualityStatus: 'PASSED',
    qualityFlagsJson: JSON.stringify([]),
    updatedAt: '2026-05-26T15:48:00+08:00'
  }
];

const dataCenterProfessions: BimDataCenterProfessionSummary[] = [
  {
    code: 'all',
    name: '全专业',
    status: 'RECORDED',
    source: '平台汇总视图',
    note: '当前暂无真实上传数据，可手动打开示例记录验证校验口径。',
    componentCount: 0,
    warningCount: 0,
    missingFieldCount: 0,
    lastUploadedAt: null
  },
  {
    code: 'intelligent',
    name: '智能化',
    status: 'RECORDED',
    source: '附件三智能化映射',
    note: '标准包为草案，暂无真实插件上传数据。',
    componentCount: 0,
    warningCount: 0,
    missingFieldCount: 0,
    lastUploadedAt: null
  },
  {
    code: 'architecture',
    name: '建筑',
    status: 'PENDING',
    source: '待接入',
    note: '标准待录入，数据库数据待接入。',
    componentCount: 0,
    warningCount: 0,
    missingFieldCount: 0,
    lastUploadedAt: null
  },
  {
    code: 'structure',
    name: '结构',
    status: 'PENDING',
    source: '待接入',
    note: '标准待录入，数据库数据待接入。',
    componentCount: 0,
    warningCount: 0,
    missingFieldCount: 0,
    lastUploadedAt: null
  },
  {
    code: 'plumbing',
    name: '给排水',
    status: 'PENDING',
    source: '待接入',
    note: '标准待录入，数据库数据待接入。',
    componentCount: 0,
    warningCount: 0,
    missingFieldCount: 0,
    lastUploadedAt: null
  },
  {
    code: 'hvac',
    name: '暖通',
    status: 'PENDING',
    source: '待接入',
    note: '标准待录入，数据库数据待接入。',
    componentCount: 0,
    warningCount: 0,
    missingFieldCount: 0,
    lastUploadedAt: null
  },
  {
    code: 'electrical',
    name: '电气',
    status: 'PENDING',
    source: '待接入',
    note: '标准待录入，数据库数据待接入。',
    componentCount: 0,
    warningCount: 0,
    missingFieldCount: 0,
    lastUploadedAt: null
  }
];

const dataCenterComponents: BimDataCenterComponent[] = [
  {
    id: 2001,
    projectId: 1,
    batchId: 101,
    batchNo: 'BIM-CT-20260527-001',
    batchName: 'C塔全专业报建首轮',
    professionCode: 'intelligent',
    professionName: '智能化',
    componentCode: 'Ctower-A01-MAD01-SRV001',
    shenzhenComponentIdentifier: '服务器',
    componentName: '智能化机房应用服务器 SRV-01',
    systemCode: 'MAD01',
    systemName: '信息设施系统',
    modelName: 'C塔智能化报建模型-R01.rvt',
    modelFingerprint: 'sha256:ctower-intel-r01-a8392',
    levelName: '15F',
    familyName: '服务器-报建族',
    typeName: '2U机架式服务器',
    fieldCompletionRate: 100,
    qualityStatus: 'PASSED',
    warningCount: 0,
    missingFieldCount: 0,
    pluginVersion: '0.9.3-alpha',
    revitVersion: 'Revit 2024',
    uploadEvidence: 'SNAP-CT-INT-20260527-001',
    uploadedAt: '2026-05-27T15:12:00+08:00'
  },
  {
    id: 2002,
    projectId: 1,
    batchId: 101,
    batchNo: 'BIM-CT-20260527-001',
    batchName: 'C塔全专业报建首轮',
    professionCode: 'intelligent',
    professionName: '智能化',
    componentCode: 'Ctower-A01-MAD01-SWT001',
    shenzhenComponentIdentifier: '交换机',
    componentName: '核心交换机 SW-Core-01',
    systemCode: 'MAD01',
    systemName: '信息设施系统',
    modelName: 'C塔智能化报建模型-R01.rvt',
    modelFingerprint: 'sha256:ctower-intel-r01-a8392',
    levelName: '15F',
    familyName: '网络交换机-报建族',
    typeName: '48口万兆核心交换机',
    fieldCompletionRate: 96,
    qualityStatus: 'WARNING',
    warningCount: 1,
    missingFieldCount: 0,
    pluginVersion: '0.9.3-alpha',
    revitVersion: 'Revit 2024',
    uploadEvidence: 'SNAP-CT-INT-20260527-001',
    uploadedAt: '2026-05-27T15:09:00+08:00'
  },
  {
    id: 2003,
    projectId: 1,
    batchId: 101,
    batchNo: 'BIM-CT-20260527-001',
    batchName: 'C塔全专业报建首轮',
    professionCode: 'intelligent',
    professionName: '智能化',
    componentCode: 'Ctower-A01-PSS01-CAM021',
    shenzhenComponentIdentifier: '摄像机',
    componentName: '12F 电梯厅半球摄像机 CAM-021',
    systemCode: 'PSS01',
    systemName: '公共安全系统',
    modelName: 'C塔智能化报建模型-R01.rvt',
    modelFingerprint: 'sha256:ctower-intel-r01-a8392',
    levelName: '12F',
    familyName: '摄像机-报建族',
    typeName: '400万像素半球摄像机',
    fieldCompletionRate: 100,
    qualityStatus: 'PASSED',
    warningCount: 0,
    missingFieldCount: 0,
    pluginVersion: '0.9.3-alpha',
    revitVersion: 'Revit 2024',
    uploadEvidence: 'SNAP-CT-INT-20260527-001',
    uploadedAt: '2026-05-27T15:04:00+08:00'
  },
  {
    id: 2004,
    projectId: 1,
    batchId: 101,
    batchNo: 'BIM-CT-20260527-001',
    batchName: 'C塔全专业报建首轮',
    professionCode: 'intelligent',
    professionName: '智能化',
    componentCode: 'Ctower-A01-PSS02-ACS008',
    shenzhenComponentIdentifier: '门禁控制器',
    componentName: '9F 办公区门禁控制器 ACS-008',
    systemCode: 'PSS02',
    systemName: '公共安全系统',
    modelName: 'C塔智能化报建模型-R01.rvt',
    modelFingerprint: 'sha256:ctower-intel-r01-a8392',
    levelName: '9F',
    familyName: '门禁控制器-报建族',
    typeName: '双门控制器',
    fieldCompletionRate: 88,
    qualityStatus: 'MISSING',
    warningCount: 1,
    missingFieldCount: 1,
    pluginVersion: '0.9.3-alpha',
    revitVersion: 'Revit 2024',
    uploadEvidence: 'SNAP-CT-INT-20260527-001',
    uploadedAt: '2026-05-27T14:58:00+08:00'
  },
  {
    id: 2005,
    projectId: 1,
    batchId: 102,
    batchNo: 'BIM-CT-20260526-002',
    batchName: 'C塔机电样板闭环批次',
    professionCode: 'intelligent',
    professionName: '智能化',
    componentCode: 'Ctower-A01-BMS01-DDC014',
    shenzhenComponentIdentifier: '现场控制器',
    componentName: 'BMS 现场控制器箱 DDC-014',
    systemCode: 'BMS01',
    systemName: '建筑设备管理系统',
    modelName: 'C塔智能化样板模型-R00.rvt',
    modelFingerprint: 'sha256:ctower-intel-r00-b5114',
    levelName: 'B1',
    familyName: '现场控制器箱-报建族',
    typeName: '24点位DDC控制箱',
    fieldCompletionRate: 100,
    qualityStatus: 'PASSED',
    warningCount: 0,
    missingFieldCount: 0,
    pluginVersion: '0.9.2-alpha',
    revitVersion: 'Revit 2024',
    uploadEvidence: 'SNAP-CT-INT-20260526-002',
    uploadedAt: '2026-05-26T16:40:00+08:00'
  },
  {
    id: 2006,
    projectId: 1,
    batchId: 102,
    batchNo: 'BIM-CT-20260526-002',
    batchName: 'C塔机电样板闭环批次',
    professionCode: 'intelligent',
    professionName: '智能化',
    componentCode: 'Ctower-A01-BMS01-SEN032',
    shenzhenComponentIdentifier: '传感器',
    componentName: '送风温湿度传感器 SEN-032',
    systemCode: 'BMS01',
    systemName: '建筑设备管理系统',
    modelName: 'C塔智能化样板模型-R00.rvt',
    modelFingerprint: 'sha256:ctower-intel-r00-b5114',
    levelName: '12F',
    familyName: '传感器-报建族',
    typeName: '温湿度一体传感器',
    fieldCompletionRate: 94,
    qualityStatus: 'WARNING',
    warningCount: 1,
    missingFieldCount: 0,
    pluginVersion: '0.9.2-alpha',
    revitVersion: 'Revit 2024',
    uploadEvidence: 'SNAP-CT-INT-20260526-002',
    uploadedAt: '2026-05-26T16:32:00+08:00'
  },
  {
    id: 2007,
    projectId: 1,
    batchId: 102,
    batchNo: 'BIM-CT-20260526-002',
    batchName: 'C塔机电样板闭环批次',
    professionCode: 'intelligent',
    professionName: '智能化',
    componentCode: 'Ctower-A01-MAD02-CAB006',
    shenzhenComponentIdentifier: '综合布线配线箱',
    componentName: '综合布线配线箱 CAB-006',
    systemCode: 'MAD02',
    systemName: '信息设施系统',
    modelName: 'C塔智能化样板模型-R00.rvt',
    modelFingerprint: 'sha256:ctower-intel-r00-b5114',
    levelName: '8F',
    familyName: '弱电箱-报建族',
    typeName: '24口配线箱',
    fieldCompletionRate: 100,
    qualityStatus: 'PASSED',
    warningCount: 0,
    missingFieldCount: 0,
    pluginVersion: '0.9.2-alpha',
    revitVersion: 'Revit 2024',
    uploadEvidence: 'SNAP-CT-INT-20260526-002',
    uploadedAt: '2026-05-26T16:20:00+08:00'
  },
  {
    id: 2008,
    projectId: 1,
    batchId: 102,
    batchNo: 'BIM-CT-20260526-002',
    batchName: 'C塔机电样板闭环批次',
    professionCode: 'intelligent',
    professionName: '智能化',
    componentCode: 'Ctower-A01-PSS01-NVR001',
    shenzhenComponentIdentifier: '视频监控主机',
    componentName: '视频监控存储主机 NVR-001',
    systemCode: 'PSS01',
    systemName: '公共安全系统',
    modelName: 'C塔智能化样板模型-R00.rvt',
    modelFingerprint: 'sha256:ctower-intel-r00-b5114',
    levelName: '15F',
    familyName: '视频监控主机-报建族',
    typeName: '64路NVR',
    fieldCompletionRate: 100,
    qualityStatus: 'PASSED',
    warningCount: 0,
    missingFieldCount: 0,
    pluginVersion: '0.9.2-alpha',
    revitVersion: 'Revit 2024',
    uploadEvidence: 'SNAP-CT-INT-20260526-002',
    uploadedAt: '2026-05-26T16:10:00+08:00'
  }
].map(enrichDataCenterComponent);

function createStandardVersion(component: BimDataCenterComponent) {
  return component.batchId === 101 ? 'INTEL-V1.0-DRAFT' : 'INTEL-V0.9-DRAFT';
}

function createRevitElementId(component: BimDataCenterComponent) {
  return String(880000 + component.id);
}

function createRevitUniqueId(component: BimDataCenterComponent) {
  return `revit-${component.id}-${component.componentCode}`;
}

function createComponentCodeCandidate(component: BimDataCenterComponent): ComponentCodeCandidate {
  const isConflict = component.id === 2007;
  const isWarning = component.id === 2002 || component.id === 2006;
  return {
    componentId: component.id,
    candidateCode: isConflict ? 'Ctower-A01-MAD01-SWT001' : component.componentCode,
    standardVersion: component.standardVersion ?? createStandardVersion(component),
    generatedBy: '插件根据平台智能化标准包自动生成候选编码',
    validationStatus: isConflict ? 'CONFLICT' : isWarning ? 'WARNING' : 'PASSED',
    uniquenessScope: '同项目/同专业/同模型指纹/同上传批次',
    conflictReason: isConflict
      ? '候选编码与核心交换机 SW-Core-01 在当前批次内重复，平台不自动覆盖模型值。'
      : isWarning
        ? '候选编码可写入，但字段完整性或类型仍需管理方复核。'
        : null,
    checkedAt: component.uploadedAt
  };
}

function createComponentLink(component: BimDataCenterComponent): BimComponentLink {
  return {
    componentCode: component.componentCode,
    professionCode: component.professionCode,
    modelFingerprint: component.modelFingerprint,
    revitElementId: component.revitElementId ?? createRevitElementId(component),
    revitUniqueId: component.revitUniqueId ?? createRevitUniqueId(component),
    batchNo: component.batchNo,
    previewStatus: component.previewStatus ?? 'METADATA_ONLY',
    previewNote: '首版提供构件元数据预览，三维轻量化模型定位后续专项接入。'
  };
}

function createExpectedFieldCoverage(fields: BimDataCenterFieldValue[]): BimExpectedFieldCoverage {
  return {
    expectedFieldCount: fields.length,
    filledFieldCount: fields.filter((field) => field.fieldValue !== '').length,
    missingRequiredFields: fields
      .filter((field) => field.requiredPolicy.includes('候选必填') && field.fieldValue === '')
      .map((field) => field.fieldName)
  };
}

function enrichDataCenterComponent(component: BimDataCenterComponent): BimDataCenterComponent {
  const standardVersion = createStandardVersion(component);
  const codeCandidate = createComponentCodeCandidate({ ...component, standardVersion });
  return {
    ...component,
    standardVersion,
    revitElementId: createRevitElementId(component),
    revitUniqueId: createRevitUniqueId(component),
    codeCandidateStatus: codeCandidate.validationStatus,
    codeConflictReason: codeCandidate.conflictReason,
    previewStatus: 'METADATA_ONLY'
  };
}

const mockDetails = new Map<number, BimComponentDetail>([
  [
    1001,
    {
      component: mockComponents[0],
      rawParametersJson: JSON.stringify({
        RevitUniqueId: '7f0b7d2f-ctower-ahu-0101',
        Family: '空调机组-平台编码族',
        Type: 'AHU-18000m3/h',
        DD_ComponentCode: 'CT-MEP-AHU-0101',
        DD_TagCode: 'CT-AHU-12F-0101',
        DD_MaterialCode: 'MAT-AHU-001'
      }),
      fields: [
        createMockField(1001, 'component_code', '构件编码', 'CT-MEP-AHU-0101', 'DD_ComponentCode', 'PASSED'),
        createMockField(1001, 'tag_code', '构件标识码', 'CT-AHU-12F-0101', 'DD_TagCode', 'PASSED'),
        createMockField(1001, 'material_code', '材料/设备编码', 'MAT-AHU-001', 'DD_MaterialCode', 'PASSED'),
        createMockField(1001, 'discipline_code', '专业编码', 'HVAC', 'DD_Discipline', 'PASSED')
      ]
    }
  ],
  [
    1002,
    {
      component: mockComponents[1],
      rawParametersJson: JSON.stringify({
        RevitUniqueId: '7f0b7d2f-ctower-vav-1208',
        Family: 'VAV BOX-报建族',
        Type: 'VAV-500L/s',
        DD_ComponentCode: 'CT-HVAC-VAV-1208',
        DD_TagCode: 'CT-VAV-12F-1208',
        DD_MaterialCode: ''
      }),
      fields: [
        createMockField(1002, 'component_code', '构件编码', 'CT-HVAC-VAV-1208', 'DD_ComponentCode', 'PASSED'),
        createMockField(1002, 'tag_code', '构件标识码', 'CT-VAV-12F-1208', 'DD_TagCode', 'PASSED'),
        createMockField(1002, 'material_code', '材料/设备编码', '', 'DD_MaterialCode', 'WARNING', '材料/设备编码缺失'),
        createMockField(1002, 'discipline_code', '专业编码', 'HVAC', 'DD_Discipline', 'PASSED')
      ]
    }
  ],
  [
    1003,
    {
      component: mockComponents[2],
      rawParametersJson: JSON.stringify({
        RevitUniqueId: '7f0b7d2f-ctower-db-1201',
        Family: '配电箱-报建族',
        Type: 'AL-12F',
        DD_ComponentCode: 'CT-ELEC-DB-1201',
        DD_TagCode: 'CT-DB-12F-1201',
        DD_MaterialCode: 'MAT-EL-DB-012'
      }),
      fields: [
        createMockField(1003, 'component_code', '构件编码', 'CT-ELEC-DB-1201', 'DD_ComponentCode', 'PASSED'),
        createMockField(1003, 'tag_code', '构件标识码', 'CT-DB-12F-1201', 'DD_TagCode', 'PASSED'),
        createMockField(1003, 'material_code', '材料/设备编码', 'MAT-EL-DB-012', 'DD_MaterialCode', 'PASSED'),
        createMockField(1003, 'discipline_code', '专业编码', 'ELEC', 'DD_Discipline', 'PASSED')
      ]
    }
  ],
  [
    1004,
    {
      component: mockComponents[3],
      rawParametersJson: JSON.stringify({
        RevitUniqueId: '7f0b7d2f-ctower-pump-b2-03',
        Family: '水泵-平台编码族',
        Type: 'PUMP-45kW',
        DD_ComponentCode: 'CT-PLUMB-PUMP-B2-03',
        DD_TagCode: 'CT-PUMP-B2-03',
        DD_MaterialCode: 'MAT-PUMP-045'
      }),
      fields: [
        createMockField(1004, 'component_code', '构件编码', 'CT-PLUMB-PUMP-B2-03', 'DD_ComponentCode', 'PASSED'),
        createMockField(1004, 'tag_code', '构件标识码', 'CT-PUMP-B2-03', 'DD_TagCode', 'PASSED'),
        createMockField(1004, 'material_code', '材料/设备编码', 'MAT-PUMP-045', 'DD_MaterialCode', 'PASSED'),
        createMockField(1004, 'discipline_code', '专业编码', 'PLUMBING', 'DD_Discipline', 'PASSED')
      ]
    }
  ]
]);

const dataCenterDetails = new Map<number, BimDataCenterComponentDetail>(
  dataCenterComponents.map((component) => {
    const baseFields: BimDataCenterFieldValue[] = [
      createDataCenterField(component, 1, '深圳构件标识', component.shenzhenComponentIdentifier, null, 'string', 'DD_SZ_ComponentId', 'PASSED', null, '附件三', '候选必填/待确认'),
      createDataCenterField(component, 2, '深圳系统标识', component.systemName, null, 'string', 'DD_SZ_SystemId', 'PASSED', null, '附件三', '候选必填/待确认'),
      createDataCenterField(component, 3, '位号编码', component.componentCode, null, 'string', 'DD_TagCode', 'PASSED', null, '附件十一', '候选必填/待确认'),
      createDataCenterField(component, 4, '三级系统分类', component.systemCode, null, 'enum', 'DD_SystemLevel3', 'PASSED', null, '附件三', '候选必填/待确认'),
      createDataCenterField(component, 5, '型号规格', component.typeName, null, 'string', 'DD_ModelSpec', 'PASSED', null, '附件三', '候选必填/待确认')
    ];
    const optionalFields: BimDataCenterFieldValue[] = [
      createDataCenterField(component, 6, '安装位置', component.levelName, null, 'string', 'DD_InstallLocation', 'PASSED', null, '附件三', '选填项'),
      createDataCenterField(component, 7, '电源参数', component.id === 2004 ? '' : 'AC220V', null, 'string', 'DD_Power', component.id === 2004 ? 'MISSING' : 'PASSED', component.id === 2004 ? '门禁控制器电源参数为空，需建模方补录。' : null, '附件三', '候选必填/待确认'),
      createDataCenterField(component, 8, '额定功率', component.id === 2002 ? '待复核' : '120W', 'W', 'double', 'DD_RatedPower', component.id === 2002 ? 'WARNING' : 'PASSED', component.id === 2002 ? 'double 类型应按数值上传，当前仍是文本。' : null, '附件三', '候选必填/待确认')
    ];
    if (component.id === 2006) {
      optionalFields.push(
        createDataCenterField(component, 9, '输出信号类型', '0-10V', null, 'enum', 'DD_OutputSignal', 'WARNING', '枚举值需与附件三保持英文半角符号。', '附件三', '候选必填/待确认')
      );
    }
    const fields = [...baseFields, ...optionalFields];
    const codeCandidate = createComponentCodeCandidate(component);
    return [
      component.id,
      {
        component,
        fields,
        rawParameters: {
          RevitElementId: component.revitElementId ?? createRevitElementId(component),
          RevitUniqueId: component.revitUniqueId ?? createRevitUniqueId(component),
          Category: '智能化设备',
          Family: component.familyName,
          Type: component.typeName,
          DD_StandardVersion: component.standardVersion ?? createStandardVersion(component),
          DD_Discipline: component.professionCode,
          DD_SZ_ComponentId: component.shenzhenComponentIdentifier,
          DD_SZ_SystemId: component.systemName,
          DD_ComponentCode: component.componentCode,
          DD_TagCode: component.componentCode,
          DD_SystemLevel3: component.systemCode,
          DD_ModelSpec: component.typeName,
          DD_InstallLocation: component.levelName,
          DD_Power: component.id === 2004 ? '' : 'AC220V',
          DD_RatedPower: component.id === 2002 ? '待复核' : 120
        },
        uploadEvidence: {
          snapshotNo: component.uploadEvidence,
          uploadMode: '插件在线上传',
          uploadedBy: '建模方账号',
          contractVersion: component.standardVersion ?? createStandardVersion(component),
          modelFingerprint: component.modelFingerprint,
          pluginVersion: component.pluginVersion,
          revitVersion: component.revitVersion
        },
        codeCandidate,
        componentLink: createComponentLink(component),
        fieldCoverage: createExpectedFieldCoverage(fields)
      }
    ];
  })
);

const mockWorkOrders: BimSubmissionWorkOrder[] = [
  {
    id: 5001,
    projectId: 1,
    batchId: 101,
    componentId: 1002,
    fieldCode: 'material_code',
    title: 'VAV-1208 材料/设备编码补录',
    description: '插件已回传构件，但材料/设备编码为空；请建模方在 Revit 模型中补录后重新上传。',
    status: 'OPEN',
    priority: 'NORMAL',
    assignedToType: 'MODELING_PARTY',
    requestedValue: 'MAT-VAV-500',
    currentValue: '',
    pluginResponseJson: null,
    resolvedAt: null,
    closedAt: null,
    createdAt: '2026-05-27T14:18:00+08:00',
    updatedAt: '2026-05-27T14:18:00+08:00'
  }
];

const mockArchives: BimSubmissionArchive[] = [
  {
    id: 7001,
    projectId: 1,
    batchId: 102,
    contractId: 9001,
    archiveNo: 'ARCH-BIM-CT-20260526-002',
    status: 'SEALED',
    summaryJson: JSON.stringify({ 契约版本: 'V1.0', 构件数: 2, 预警数: 0, 整改闭环: 1 }),
    createdAt: '2026-05-26T17:40:00+08:00',
    createdBy: 1
  }
];

function createMockField(
  componentId: number,
  fieldCode: string,
  fieldName: string,
  fieldValue: string,
  sourceParameterName: string,
  validationStatus: string,
  validationMessage: string | null = null
): BimComponentFieldValue {
  return {
    id: Number(`${componentId}${mockFieldRules.findIndex((rule) => rule.fieldCode === fieldCode) + 1}`),
    componentId,
    fieldCode,
    fieldName,
    fieldValue,
    unitName: null,
    valueSource: 'PLUGIN_UPLOAD',
    sourceParameterName,
    validationStatus,
    validationMessage,
    rawValue: fieldValue,
    updatedAt: '2026-05-27T11:30:00+08:00'
  };
}

function createDataCenterField(
  component: BimDataCenterComponent,
  offset: number,
  fieldName: string,
  fieldValue: string,
  unitName: string | null,
  valueType: string,
  sourceParameterName: string,
  validationStatus: BimDataCenterQualityStatus,
  validationMessage: string | null,
  standardSource: string,
  requiredPolicy: string
): BimDataCenterFieldValue {
  return {
    id: Number(`${component.id}${offset}`),
    componentId: component.id,
    fieldName,
    fieldValue,
    unitName,
    valueType,
    sourceParameterName,
    validationStatus,
    validationMessage,
    standardSource,
    requiredPolicy
  };
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function now() {
  return new Date().toISOString();
}

function findBatch(batchId: number) {
  return mockBatches.find((batch) => batch.id === batchId);
}

function recalculateBatch(batchId: number) {
  const batch = findBatch(batchId);
  if (!batch) return;
  const batchComponents = mockComponents.filter((component) => component.batchId === batchId);
  const openOrders = mockWorkOrders.filter((item) => item.batchId === batchId && item.status !== 'CLOSED');
  batch.componentCount = batchComponents.length;
  batch.warningCount = batchComponents.filter((component) => component.qualityStatus === 'WARNING').length;
  batch.workOrderCount = openOrders.length;
  batch.qualitySummaryJson = JSON.stringify({
    passed: batchComponents.filter((component) => component.qualityStatus === 'PASSED').length,
    warning: batch.warningCount,
    openWorkOrders: openOrders.length
  });
}

function makeOverview(projectId: number): BimSubmissionOverview {
  mockContract = { ...mockContract, projectId };
  mockBatches.forEach((batch) => {
    batch.projectId = projectId;
    recalculateBatch(batch.id);
  });
  mockComponents.forEach((component) => {
    component.projectId = projectId;
  });
  mockWorkOrders.forEach((workOrder) => {
    workOrder.projectId = projectId;
  });
  mockArchives.forEach((archive) => {
    archive.projectId = projectId;
  });
  return clone({
    currentContract: mockContract,
    batches: mockBatches,
    recentComponents: mockComponents.slice(0, 8),
    workOrders: mockWorkOrders,
    archives: mockArchives
  });
}

function getDataCenterRows(projectId: number, professionCode = 'all') {
  if (professionCode !== 'all' && professionCode !== 'intelligent') {
    return [] as BimDataCenterComponent[];
  }
  return dataCenterComponents.map((component) => ({ ...component, projectId }));
}

function makeDataCenterSummary(projectId: number, professionCode = 'all'): BimDataCenterSummary {
  const profession = dataCenterProfessions.find((item) => item.code === professionCode) ?? dataCenterProfessions[0];
  const rows = getDataCenterRows(projectId, profession.code);
  return {
    professionCode: profession.code,
    professionName: profession.name,
    componentCount: rows.length,
    passedCount: rows.filter((component) => (
      component.qualityStatus === 'PASSED' && component.codeCandidateStatus === 'PASSED'
    )).length,
    warningCount: rows.filter((component) => (
      component.qualityStatus === 'WARNING'
      || component.codeCandidateStatus === 'WARNING'
      || component.codeCandidateStatus === 'CONFLICT'
    )).length,
    missingFieldCount: rows.reduce((total, component) => total + component.missingFieldCount, 0),
    modelCount: new Set(rows.map((component) => component.modelFingerprint)).size,
    batchCount: new Set(rows.map((component) => component.batchId)).size,
    lastUploadedAt: rows[0]?.uploadedAt ?? null,
    status: profession.status,
    note: profession.note
  };
}

function makeProfessionSummaries(projectId: number) {
  return dataCenterProfessions.map((profession) => {
    const rows = getDataCenterRows(projectId, profession.code);
    if (profession.status === 'PENDING') {
      return { ...profession };
    }
    return {
      ...profession,
      componentCount: rows.length,
      warningCount: rows.filter((component) => (
        component.qualityStatus === 'WARNING'
        || component.codeCandidateStatus === 'WARNING'
        || component.codeCandidateStatus === 'CONFLICT'
      )).length,
      missingFieldCount: rows.reduce((total, component) => total + component.missingFieldCount, 0),
      lastUploadedAt: rows[0]?.uploadedAt ?? null
    };
  });
}

export async function fetchBimDataCenterOverview(projectId: number, professionCode = 'all') {
  const components = getDataCenterRows(projectId, professionCode);
  return clone({
    professionSummaries: makeProfessionSummaries(projectId),
    summary: makeDataCenterSummary(projectId, professionCode),
    components
  } satisfies BimDataCenterOverview);
}

export async function fetchBimDataCenterComponents(projectId: number, params: BimDataCenterQuery = {}) {
  const keyword = params.keyword?.trim().toLowerCase();
  const limit = params.limit ?? 100;
  let rows = getDataCenterRows(projectId, params.professionCode ?? 'all');
  if (params.batchId) {
    rows = rows.filter((component) => component.batchId === params.batchId);
  }
  if (params.qualityStatus) {
    rows = rows.filter((component) => component.qualityStatus === params.qualityStatus);
  }
  if (keyword) {
    rows = rows.filter((component) => [
      component.componentCode,
      component.shenzhenComponentIdentifier,
      component.componentName,
      component.systemCode,
      component.systemName,
      component.modelName,
      component.batchNo,
      component.familyName,
      component.typeName,
      component.levelName,
      component.revitElementId,
      component.revitUniqueId
    ].some((value) => String(value ?? '').toLowerCase().includes(keyword)));
  }
  return clone(rows.slice(0, limit));
}

export async function fetchBimDataCenterComponentDetail(projectId: number, componentId: number) {
  const detail = dataCenterDetails.get(componentId);
  if (!detail || (detail.component.projectId !== projectId && projectId <= 0)) {
    throw new Error('构件不存在');
  }
  return clone({
    ...detail,
    component: {
      ...detail.component,
      projectId
    }
  });
}

export async function fetchBimUploadedCodeRecords(
  projectId: number,
  params: BimUploadedCodeRecordQuery = {}
) {
  const keyword = params.keyword?.trim().toLowerCase();
  let rows = getUploadedCodeRecordRows(projectId, params.includeSamples);
  if (params.professionCode && params.professionCode !== 'all') {
    rows = rows.filter((record) => record.professionCode === params.professionCode);
  }
  if (params.validationStatus) {
    rows = rows.filter((record) => record.validationStatus === params.validationStatus);
  }
  if (keyword) {
    rows = rows.filter((record) => [
      record.professionName,
      record.componentType,
      record.componentName,
      record.codeType,
      record.rawCodeValue,
      record.sourceParameterName,
      record.modelFingerprint,
      record.revitElementId,
      record.revitUniqueId,
      record.batchNo,
      ...record.validationIssues.map((issue) => issue.message)
    ].some((value) => String(value ?? '').toLowerCase().includes(keyword)));
  }
  return clone(rows);
}

export async function createBimUploadedCodeRecord(
  projectId: number,
  payload: BimUploadedCodeRecordPayload
) {
  const record = createUploadedCodeRecord(mockUploadedCodeSequence++, {
    ...payload,
    projectId
  });
  uploadedCodeRecords.unshift(record);
  return clone(revalidateUploadedCodeRecord(record, uploadedCodeRecords));
}

export async function updateBimUploadedCodeRecord(
  projectId: number,
  recordId: number,
  payload: BimUploadedCodeRecordPayload
) {
  const index = uploadedCodeRecords.findIndex((record) => record.id === recordId && record.projectId === projectId);
  if (index < 0) {
    throw new Error('上传编码记录不存在');
  }
  const updated = revalidateUploadedCodeRecord({
    ...uploadedCodeRecords[index],
    professionCode: payload.professionCode,
    professionName: payload.professionName,
    componentType: payload.componentType,
    componentName: payload.componentName,
    codeType: payload.codeType,
    rawCodeValue: payload.rawCodeValue,
    sourceParameterName: payload.sourceParameterName,
    standardPackageStatus: payload.standardPackageStatus ?? uploadedCodeRecords[index].standardPackageStatus,
    modelFingerprint: payload.modelFingerprint,
    revitElementId: payload.revitElementId,
    revitUniqueId: payload.revitUniqueId,
    batchNo: payload.batchNo,
    updatedAt: now()
  }, uploadedCodeRecords);
  uploadedCodeRecords[index] = updated;
  return clone(updated);
}

export async function deleteBimUploadedCodeRecord(projectId: number, recordId: number) {
  const index = uploadedCodeRecords.findIndex((record) => record.id === recordId && record.projectId === projectId);
  if (index < 0) {
    throw new Error('上传编码记录不存在');
  }
  const [deleted] = uploadedCodeRecords.splice(index, 1);
  return clone(deleted);
}

export async function revalidateBimUploadedCodeRecords(projectId: number) {
  const rows = uploadedCodeRecords.filter((record) => record.projectId === projectId || projectId > 0);
  rows.forEach((record) => {
    const updated = revalidateUploadedCodeRecord(record, rows);
    Object.assign(record, updated);
  });
  return clone(rows);
}

export async function fetchBimSubmissionOverview(projectId: number) {
  if (!bimSubmissionBackendEnabled) {
    return makeOverview(projectId);
  }
  const { data } = await http.get<ApiResponse<BimSubmissionOverview>>(
    `/api/bim-submission/projects/${projectId}/overview`
  );
  return data.data;
}

export async function publishBimSubmissionContract(projectId: number, versionNo?: string) {
  if (!bimSubmissionBackendEnabled) {
    mockContractPublishSequence += 1;
    const publishedAt = now();
    mockContract = {
      ...mockContract,
      projectId,
      versionNo: versionNo?.trim() || `V1.${mockContractPublishSequence}`,
      status: 'PUBLISHED',
      publishedAt,
      updatedAt: publishedAt,
      valuePackageJson: JSON.stringify({
        ...mockValuePackage,
        contractVersionNo: versionNo?.trim() || `V1.${mockContractPublishSequence}`
      })
    };
    return clone(mockContract);
  }
  const { data } = await http.post<ApiResponse<BimSubmissionContract>>(
    `/api/bim-submission/projects/${projectId}/contracts:publish`,
    { versionNo }
  );
  return data.data;
}

export async function createBimSubmissionBatch(projectId: number, batchName: string) {
  if (!bimSubmissionBackendEnabled) {
    mockBatchSequence += 1;
    const timestamp = now();
    const batch: BimSubmissionBatch = {
      id: 100 + mockBatchSequence,
      projectId,
      contractId: mockContract.id,
      contractVersionNo: mockContract.versionNo,
      batchNo: `BIM-CT-20260527-${String(mockBatchSequence).padStart(3, '0')}`,
      batchName,
      status: 'ISSUED',
      valuePackageJson: mockContract.valuePackageJson,
      qualitySummaryJson: JSON.stringify({ passed: 0, warning: 0, openWorkOrders: 0 }),
      componentCount: 0,
      warningCount: 0,
      workOrderCount: 0,
      issuedAt: timestamp,
      submittedAt: null,
      reviewedAt: null,
      archivedAt: null,
      createdAt: timestamp,
      updatedAt: timestamp
    };
    mockBatches.unshift(batch);
    return clone(batch);
  }
  const { data } = await http.post<ApiResponse<BimSubmissionBatch>>(
    `/api/bim-submission/projects/${projectId}/batches`,
    { batchName }
  );
  return data.data;
}

export async function fetchBimComponents(projectId: number, params: BimComponentQuery = {}) {
  if (!bimSubmissionBackendEnabled) {
    const keyword = params.keyword?.trim().toLowerCase();
    const limit = params.limit ?? 50;
    let rows = mockComponents.filter((component) => component.projectId === projectId || projectId > 0);
    if (params.batchId) {
      rows = rows.filter((component) => component.batchId === params.batchId);
    }
    if (params.qualityStatus) {
      rows = rows.filter((component) => component.qualityStatus === params.qualityStatus);
    }
    if (keyword) {
      rows = rows.filter((component) => [
        component.componentCode,
        component.componentName,
        component.familyName,
        component.typeName,
        component.elementUniqueId,
        component.disciplineCode,
        component.systemCode
      ].some((value) => value?.toLowerCase().includes(keyword)));
    }
    return clone(rows.slice(0, limit));
  }
  const { data } = await http.get<ApiResponse<BimComponentSummary[]>>(
    `/api/bim-submission/projects/${projectId}/components`,
    { params }
  );
  return data.data;
}

export async function fetchBimComponentDetail(projectId: number, componentId: number) {
  if (!bimSubmissionBackendEnabled) {
    const detail = mockDetails.get(componentId);
    if (!detail || (detail.component.projectId !== projectId && projectId <= 0)) {
      throw new Error('构件不存在');
    }
    return clone(detail);
  }
  const { data } = await http.get<ApiResponse<BimComponentDetail>>(
    `/api/bim-submission/projects/${projectId}/components/${componentId}`
  );
  return data.data;
}

export async function createBimSubmissionWorkOrder(
  projectId: number,
  batchId: number,
  payload: BimWorkOrderPayload
) {
  if (!bimSubmissionBackendEnabled) {
    const component = mockComponents.find((item) => item.id === payload.componentId);
    const dataCenterComponent = dataCenterComponents.find((item) => item.id === payload.componentId);
    if (!component && !dataCenterComponent) {
      throw new Error('构件不存在，无法创建整改工单');
    }
    const detail = mockDetails.get(payload.componentId);
    const dataCenterDetail = dataCenterDetails.get(payload.componentId);
    const currentValue = detail?.fields.find((field) => field.fieldCode === payload.fieldCode)?.fieldValue
      ?? dataCenterDetail?.fields.find((field) => (
        field.fieldName === payload.fieldCode || field.sourceParameterName === payload.fieldCode
      ))?.fieldValue
      ?? null;
    const timestamp = now();
    const workOrder: BimSubmissionWorkOrder = {
      id: mockWorkOrderSequence++,
      projectId,
      batchId,
      componentId: payload.componentId,
      fieldCode: payload.fieldCode,
      title: payload.title,
      description: payload.description ?? null,
      status: 'OPEN',
      priority: payload.priority ?? 'NORMAL',
      assignedToType: 'MODELING_PARTY',
      requestedValue: payload.requestedValue ?? null,
      currentValue,
      pluginResponseJson: null,
      resolvedAt: null,
      closedAt: null,
      createdAt: timestamp,
      updatedAt: timestamp
    };
    mockWorkOrders.unshift(workOrder);
    const batch = findBatch(batchId);
    if (batch && batch.status !== 'CLOSED') {
      batch.status = 'RECTIFICATION';
      batch.updatedAt = timestamp;
    }
    recalculateBatch(batchId);
    return clone(workOrder);
  }
  const { data } = await http.post<ApiResponse<BimSubmissionWorkOrder>>(
    `/api/bim-submission/projects/${projectId}/batches/${batchId}/work-orders`,
    payload
  );
  return data.data;
}

export async function closeBimSubmissionWorkOrder(projectId: number, workOrderId: number, comment?: string) {
  if (!bimSubmissionBackendEnabled) {
    const workOrder = mockWorkOrders.find((item) => item.id === workOrderId);
    if (!workOrder || (workOrder.projectId !== projectId && projectId <= 0)) {
      throw new Error('整改工单不存在');
    }
    const timestamp = now();
    workOrder.status = 'CLOSED';
    workOrder.closedAt = timestamp;
    workOrder.resolvedAt = workOrder.resolvedAt ?? timestamp;
    workOrder.pluginResponseJson = JSON.stringify({ reviewerComment: comment ?? '管理方复核通过' });
    workOrder.updatedAt = timestamp;
    recalculateBatch(workOrder.batchId);
    return clone(workOrder);
  }
  const { data } = await http.post<ApiResponse<BimSubmissionWorkOrder>>(
    `/api/bim-submission/projects/${projectId}/work-orders/${workOrderId}:close`,
    { comment }
  );
  return data.data;
}

export async function archiveBimSubmissionBatch(projectId: number, batchId: number) {
  if (!bimSubmissionBackendEnabled) {
    const batch = findBatch(batchId);
    if (!batch) {
      throw new Error('批次不存在');
    }
    const openOrders = mockWorkOrders.filter((item) => item.batchId === batchId && item.status !== 'CLOSED');
    if (openOrders.length > 0) {
      throw new Error('仍有未关闭整改工单，不能归档');
    }
    const existed = mockArchives.find((archive) => archive.batchId === batchId);
    if (existed) {
      return clone(existed);
    }
    const timestamp = now();
    batch.status = 'CLOSED';
    batch.archivedAt = timestamp;
    batch.updatedAt = timestamp;
    const archive: BimSubmissionArchive = {
      id: mockArchiveSequence++,
      projectId,
      batchId,
      contractId: batch.contractId,
      archiveNo: `ARCH-${batch.batchNo}`,
      status: 'SEALED',
      summaryJson: JSON.stringify({
        契约版本: batch.contractVersionNo,
        构件数: batch.componentCount,
        预警数: batch.warningCount,
        整改闭环: mockWorkOrders.filter((item) => item.batchId === batchId).length
      }),
      createdAt: timestamp,
      createdBy: 1
    };
    mockArchives.unshift(archive);
    return clone(archive);
  }
  const { data } = await http.post<ApiResponse<BimSubmissionArchive>>(
    `/api/bim-submission/projects/${projectId}/batches/${batchId}:archive`
  );
  return data.data;
}
