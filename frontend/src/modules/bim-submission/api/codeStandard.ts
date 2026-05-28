export type StandardSourceStatus = 'ACTIVE' | 'FALLBACK_PENDING' | 'REFERENCE';
export type ProfessionStatusCode = 'RECORDED' | 'PENDING';
export type ValidationStatus = 'PASSED' | 'WARNING' | 'FAILED';
export type StandardScope = 'GLOBAL' | 'PROFESSION';
export type BimStandardPackageStatus = 'DRAFT' | 'PENDING_CONFIRMATION' | 'ACTIVE' | 'PENDING';
export type CodeRuleLifecycleStatus = 'DRAFT' | 'PENDING_CONFIRMATION' | 'ACTIVE';

export interface StandardSource {
  id: string;
  name: string;
  fileName: string;
  sourceType: string;
  status: StandardSourceStatus;
  precedence: string;
  note: string;
}

export interface CodeSegment {
  name: string;
  chars: string;
  rule: string;
  required: boolean;
}

export interface CodeRule {
  id: string;
  title: string;
  prefix: string;
  example: string;
  source: string;
  status: string;
  lifecycleStatus?: CodeRuleLifecycleStatus;
  description: string;
  scope?: StandardScope;
  applicableProfessions?: string[];
  generationStrategy?: string;
  uniquenessScope?: string;
  segments: CodeSegment[];
}

export interface ProfessionStatus {
  code: string;
  name: string;
  status: ProfessionStatusCode;
  sourceSheet: string;
  note: string;
}

export interface SystemGroup {
  code: string;
  name: string;
  scope: string;
  source: string;
  status: string;
}

export interface ComponentStandard {
  id: string;
  sourceSheet: string;
  sourceRow: number;
  sourceObjectPath: string;
  shenzhenComponentIdentifier: string;
  intelligentMappingName: string;
  fieldCount: number;
  sampleFields: string[];
}

export interface PropertyRule {
  fieldName: string;
  valueType: 'string' | 'double' | 'enum' | 'bool';
  unit: string;
  valueRequirement: string;
  remark: string;
  sourceSheet: string;
  mappingScope: string;
  requiredPolicy: string;
}

export interface ValidationExample {
  label: string;
  value: string;
  status: ValidationStatus;
  reason: string;
}

export interface IntelligentPluginFieldStandardRow {
  profession: string;
  sourceSheet: string;
  sourceRow: number | string;
  sourceObjectPath: string;
  shenzhenComponentIdentifier: string;
  intelligentMappingName: string;
  fieldName: string;
  requiredPolicy: string;
  valueType: PropertyRule['valueType'] | 'string';
  unit: string;
  valueRequirement: string;
  suggestedRevitParameterName: string;
  standardSource: string;
  remark: string;
}

export interface StandardLayer {
  scope: StandardScope;
  title: string;
  versionNo: string;
  status: string;
  description: string;
  appliesTo: string;
  source: string;
}

export interface ProfessionStandardPackage {
  professionCode: string;
  professionName: string;
  versionNo: string;
  status: ProfessionStatusCode;
  sourceFiles: string[];
  componentTypeCount: number;
  systemGroupCount: number;
  fieldMatrixCount: number;
  packageStatus: BimStandardPackageStatus;
  encodingAuthority: string;
  note: string;
}

export interface PlatformEncodingPolicy {
  title: string;
  owner: string;
  pluginRole: string;
  uniquenessScope: string;
  conflictPolicy: string;
  uploadRequiredEvidence: string[];
}

export interface BimCodeStandardOverview {
  standardSources: StandardSource[];
  standardLayers: StandardLayer[];
  professionStandardPackages: ProfessionStandardPackage[];
  platformEncodingPolicy: PlatformEncodingPolicy;
  codeRules: CodeRule[];
  professions: ProfessionStatus[];
  systemGroups: SystemGroup[];
  componentStandards: ComponentStandard[];
  propertyRules: PropertyRule[];
  validationExamples: ValidationExample[];
}

const standardSources: StandardSource[] = [
  {
    id: 'owner-digital-delivery-standard',
    name: '智能化单位数字化交付标准',
    fileName: '附件十一：数字化交付标准(2).doc',
    sourceType: '甲方附件',
    status: 'ACTIVE',
    precedence: '附件标准优先',
    note: '提供 Ctower 位号、物料、设备、文档编码规则和智能化系统功能组；当前用于智能化约束草案，不冻结最终字段值。'
  },
  {
    id: 'owner-component-property-table',
    name: '构件标识系统标识及属性要求表',
    fileName: '附件三 构件标识系统标识及属性要求表(1.3.1.xlsx',
    sourceType: '甲方附件',
    status: 'ACTIVE',
    precedence: '附件标准优先',
    note: '当前按智能化专业标准包管理，提供构件/系统标识、属性名称、类型、单位、取值要求和智能化映射关系；字段值需等真实上传样本后再确认。'
  },
  {
    id: 'company-standard-fallback',
    name: '公司内部兜底标准',
    fileName: '待接入',
    sourceType: '公司内部',
    status: 'FALLBACK_PENDING',
    precedence: '附件未约束时兜底',
    note: '当前只展示兜底位置，不生成最终规则，不替代甲方附件。'
  }
];

const standardLayers: StandardLayer[] = [
  {
    scope: 'GLOBAL',
    title: '全专业公共标准',
    versionNo: 'GLOBAL-DRAFT-0.1',
    status: '框架已建立，规则待确认',
    description: '统一管理项目、单体、楼层、空间、通用构件标识、通用字段和编码唯一性策略。',
    appliesTo: '全部专业',
    source: '平台报建契约 + 后续各专业标准公共项沉淀'
  },
  {
    scope: 'PROFESSION',
    title: '智能化专业标准包',
    versionNo: 'INTEL-V1.0-DRAFT',
    status: '约束草案，待真实上传样本校准',
    description: '当前两个真实附件均按智能化专业标准包管理，用于平台校验智能化构件上传编码，不冻结最终字段值。',
    appliesTo: '智能化专业',
    source: '附件十一、附件三'
  },
  {
    scope: 'PROFESSION',
    title: '其他专业标准包',
    versionNo: '待接入',
    status: '待录入',
    description: '暖通、电气、给排水、建筑、结构等专业标准后续作为独立专业包接入。',
    appliesTo: '非智能化专业',
    source: '待提供'
  }
];

const platformEncodingPolicy: PlatformEncodingPolicy = {
  title: '平台规则主导编码',
  owner: '平台维护标准、版本、唯一性范围和冲突确认；管理方确认后发布。',
  pluginRole: '插件在线拉取标准包，在 Revit 中自动生成/写入候选编码并回传证据。',
  uniquenessScope: '同项目、同专业、同模型指纹、同上传批次内构件编码不得重复；跨批次保留历史追踪关系。',
  conflictPolicy: '发现重复、标准版本不一致、候选字段缺失或规则未确认时进入冲突/待确认状态，平台不自动覆盖模型值。',
  uploadRequiredEvidence: ['标准版本', '专业编码', '构件类型', '编码候选值', '模型指纹', 'Revit ElementId', 'Revit UniqueId', '插件版本', '上传批次']
};

const codeRules: CodeRule[] = [
  {
    id: 'tag-code',
    title: '位号编码',
    prefix: 'Ctower',
    example: 'Ctower-A01-MAD01-SRV001_CAB01_MOD01',
    source: '附件十一 1.2',
    status: '约束草案',
    lifecycleStatus: 'DRAFT',
    description: '由单元码、系统码、设备码、装置/组件码、部件码组成，系统码和设备码为必填。',
    scope: 'PROFESSION',
    applicableProfessions: ['智能化'],
    generationStrategy: '插件根据平台发布的智能化标准包自动生成候选位号，平台校验后确认。',
    uniquenessScope: '同项目、同专业、同模型指纹、同上传批次内唯一。',
    segments: [
      { name: '前缀符', chars: 'Ctower', rule: '区分 C塔综合体', required: true },
      { name: '单元码', chars: 'A + NN(N)', rule: '单元对象和功能业态', required: true },
      { name: '系统码', chars: 'AAA + NN', rule: '系统/子系统和具体系统编号', required: true },
      { name: '设备码', chars: 'AAA + NNN', rule: '技术设备类型和设备序号', required: true },
      { name: '装置/组件码', chars: 'AAA + NN', rule: '装置或组件，当前按需填写', required: false },
      { name: '部件码', chars: 'AAA + NN', rule: '对装置/组件的扩展', required: false }
    ]
  },
  {
    id: 'material-code',
    title: '物料编码',
    prefix: 'M',
    example: 'M010203001',
    source: '附件十一 1.3',
    status: '约束草案',
    lifecycleStatus: 'DRAFT',
    description: '由物料大类、中类、小类和流水码组成，前三段为分类，末段为流水。',
    scope: 'PROFESSION',
    applicableProfessions: ['智能化'],
    generationStrategy: '按专业标准包分类生成候选物料编码，未约束分类待公司标准兜底。',
    uniquenessScope: '同专业标准包内分类规则不重复。',
    segments: [
      { name: '物料大类码', chars: 'NN', rule: '对象类属大类', required: true },
      { name: '物料中类码', chars: 'NN', rule: '对象类属中类', required: true },
      { name: '物料小类码', chars: 'NN', rule: '对象类属小类', required: true },
      { name: '流水码', chars: 'NNN', rule: '同类物料顺序', required: true }
    ]
  },
  {
    id: 'equipment-code',
    title: '设备编码',
    prefix: '物料码',
    example: 'M010203001_00001',
    source: '附件十一 1.4',
    status: '约束草案',
    lifecycleStatus: 'DRAFT',
    description: '在物料编码后增加 5 位顺序码，用于形成物理设备编码。',
    scope: 'PROFESSION',
    applicableProfessions: ['智能化'],
    generationStrategy: '设备编码继承物料编码，并追加实例顺序码。',
    uniquenessScope: '同项目、同专业、同设备物料分类内唯一。',
    segments: [
      { name: '物料码', chars: 'NNNNNNNNN', rule: '继承物料编码分类', required: true },
      { name: '连接符', chars: '_', rule: '连接物料码和顺序码', required: true },
      { name: '顺序码', chars: 'NNNNN', rule: '物理设备实例序列', required: true }
    ]
  },
  {
    id: 'document-code',
    title: '文档编码',
    prefix: '文档类型',
    example: 'ETB0001',
    source: '附件十一 1.5 / 附录C',
    status: '公共草案',
    lifecycleStatus: 'DRAFT',
    description: '由 3 位文档类型分类编码和文档计数序号组成。',
    scope: 'GLOBAL',
    applicableProfessions: ['全部专业'],
    generationStrategy: '平台维护文档类型和计数规则，后续可供归档包导出使用。',
    uniquenessScope: '同项目、同文档类型内唯一。',
    segments: [
      { name: '文档类型分类编码', chars: 'AAA', rule: '参考 GB/T 26853.1 与附件附录C', required: true },
      { name: '文档计数序号', chars: 'NNNN', rule: '同类文档递增，位数可扩展', required: true }
    ]
  }
];

const professions: ProfessionStatus[] = [
  { code: 'site', name: '总图专业', status: 'PENDING', sourceSheet: '总图专业', note: '全专业占位，待后续录入。' },
  { code: 'architecture', name: '建筑专业', status: 'PENDING', sourceSheet: '建筑专业', note: '全专业占位，待后续录入。' },
  { code: 'structure', name: '结构专业', status: 'PENDING', sourceSheet: '结构专业', note: '全专业占位，待后续录入。' },
  { code: 'plumbing', name: '给排水专业', status: 'PENDING', sourceSheet: '给排水专业', note: '当前只识别与智能化关联的传感器片段。' },
  { code: 'hvac', name: '通风与空调专业', status: 'PENDING', sourceSheet: '通风与空调专业', note: '当前只识别与智能化关联的传感器和执行器片段。' },
  { code: 'intelligent', name: '电气/智能化专业', status: 'RECORDED', sourceSheet: '电气专业', note: '已录入智能化核心系统组、48 类映射构件和常用字段。' }
];

const professionStandardPackages: ProfessionStandardPackage[] = [
  {
    professionCode: 'global',
    professionName: '全专业公共标准',
    versionNo: 'GLOBAL-DRAFT-0.1',
    status: 'PENDING',
    sourceFiles: ['平台报建契约'],
    componentTypeCount: 0,
    systemGroupCount: 0,
    fieldMatrixCount: 49,
    packageStatus: 'DRAFT',
    encodingAuthority: '平台统一维护公共字段和唯一性规则。',
    note: '用于承接后续各专业公共字段沉淀，不替代专业包。'
  },
  {
    professionCode: 'intelligent',
    professionName: '智能化专业标准包',
    versionNo: 'INTEL-V1.0-DRAFT',
    status: 'RECORDED',
    sourceFiles: ['附件十一：数字化交付标准(2).doc', '附件三 构件标识系统标识及属性要求表(1.3.1.xlsx'],
    componentTypeCount: 48,
    systemGroupCount: 34,
    fieldMatrixCount: 504,
    packageStatus: 'DRAFT',
    encodingAuthority: '平台维护草案规则并校验上传编码，插件执行候选编码注入和回传。',
    note: '当前两个标准文件均归入智能化专业包；字段值和最终必填清单需等真实上传样本后确认。'
  },
  {
    professionCode: 'hvac',
    professionName: '暖通专业标准包',
    versionNo: '待接入',
    status: 'PENDING',
    sourceFiles: ['待提供'],
    componentTypeCount: 0,
    systemGroupCount: 0,
    fieldMatrixCount: 0,
    packageStatus: 'PENDING',
    encodingAuthority: '待后续专业标准接入后确认。',
    note: '当前仅显示入口，不使用智能化标准代替。'
  },
  {
    professionCode: 'plumbing',
    professionName: '给排水专业标准包',
    versionNo: '待接入',
    status: 'PENDING',
    sourceFiles: ['待提供'],
    componentTypeCount: 0,
    systemGroupCount: 0,
    fieldMatrixCount: 0,
    packageStatus: 'PENDING',
    encodingAuthority: '待后续专业标准接入后确认。',
    note: '当前仅显示入口，不使用智能化标准代替。'
  },
  {
    professionCode: 'electrical',
    professionName: '电气专业标准包',
    versionNo: '待接入',
    status: 'PENDING',
    sourceFiles: ['待提供'],
    componentTypeCount: 0,
    systemGroupCount: 0,
    fieldMatrixCount: 0,
    packageStatus: 'PENDING',
    encodingAuthority: '待后续专业标准接入后确认。',
    note: '当前仅显示入口，不使用智能化标准代替。'
  }
];

const systemGroups: SystemGroup[] = [
  { code: 'GEC', name: '自动喷水灭火系统', scope: '给排水/智能化', source: '附件十一', status: '跨专业关联' },
  { code: 'HBA', name: '冷热源系统', scope: '暖通/智能化', source: '附件十一', status: '跨专业关联' },
  { code: 'KDA', name: '火灾报警控制系统', scope: '电气/智能化', source: '附件十一', status: '跨专业关联' },
  { code: 'KDG', name: '防火门监控系统', scope: '电气/智能化', source: '附件十一', status: '跨专业关联' },
  { code: 'KDH', name: '送风余压监控系统', scope: '电气/智能化', source: '附件十一', status: '跨专业关联' },
  { code: 'M', name: '智能化系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MA', name: '信息设备系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MAA', name: '通信接入系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MAD', name: '综合布线系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MAE', name: '电话交换系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MAF', name: '计算机网络系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MAK', name: '卫星电视系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MAM', name: '背景音乐系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MAN', name: '信息导引及发布系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MAP', name: '会议AV系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MB', name: '公共安全系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MBA', name: '综合安防管理系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MBB', name: '视频监控系统（含商业客流统计）', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MBE', name: '门禁控制系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MBH', name: '停车场管理系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MBJ', name: '车位引导及反向寻车系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MC', name: '建筑设备管理系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MCA', name: '建筑设备监控系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MCB', name: '建筑能效监管系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MCC', name: '智能照明系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MCE', name: '智慧卫生间系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MCK', name: '机房动力环控系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MD', name: '信息化应用系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MDA', name: '智能卡应用系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'ME', name: '智能化集成系统', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MEA', name: '物联网IOT云管理平台', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'MF', name: '机房工程', scope: '智能化', source: '附件十一', status: '已录入' },
  { code: 'NA', name: '垂直电梯', scope: '电气/智能化', source: '附件十一', status: '跨专业关联' },
  { code: 'NB', name: '自动扶梯', scope: '电气/智能化', source: '附件十一', status: '跨专业关联' }
];

const componentStandards: ComponentStandard[] = [
  component('c01', '给排水专业', 1273, '传感器', '传感器', '传感器', 0, []),
  component('c02', '通风与空调专业', 1473, '传感器', '传感器', '传感器', 0, []),
  component('c03', '通风与空调专业', 1481, '执行器', '电动执行机构', '电动执行机构', 0, []),
  component('c04', '电气专业', 828, '主机设备 / 服务器', '服务器', '服务器', 9, ['编号', '三级系统分类', '型号规格', '信息总点数', '电源参数', '额定功率', '高度', '宽度']),
  component('c05', '电气专业', 854, '电话交换机', '电话交换机', '电话交换机', 8, ['编号', '三级系统分类', '型号规格', '电源参数', '额定功率', '高度', '宽度', '厚度']),
  component('c06', '电气专业', 867, '电话配线设备 / 电话配线箱', '综合布线信息配线箱', '综合布线信息配线箱', 11, ['编号', '三级系统分类', '型号规格', '信息总点数', '安装方式']),
  component('c07', '电气专业', 879, '电话配线设备 / 电话主配线架', '电话主配线架', '电话主配线架', 9, ['编号', '三级系统分类', '型号规格', '信息总点数', '电源参数']),
  component('c08', '电气专业', 894, '核心交换机', '核心交换机', '核心交换机', 8, ['编号', '三级系统分类', '型号规格', '厚度', '长度', '宽度', '电源参数']),
  component('c09', '电气专业', 904, '接入交换机', '楼层交换机', '楼层交换机', 8, ['编号', '三级系统分类', '型号规格', '厚度', '长度', '宽度', '电源参数']),
  component('c10', '电气专业', 915, '路由器', '路由器', '路由器', 13, ['三级系统分类', '型号规格', '吞吐量', '端口数量', 'CPU主频', '内存容量', '有线/无线']),
  component('c11', '电气专业', 929, '集线器', '集线器', '集线器', 7, ['三级系统分类', '型号规格', '电源参数', '额定功率', '高度', '宽度', '厚度']),
  component('c12', '电气专业', 940, '配线架 / 总配线架', '综合布线总配线架', '综合布线总配线架', 9, ['编号', '三级系统分类', '型号规格', '信息总点数', '电源参数']),
  component('c13', '电气专业', 951, '配线架 / 楼层配线架', '综合布线楼层配线架', '综合布线楼层配线架', 19, ['名称', '编号', '编码', '建筑单体名称', '所在楼层', '空间名称', '基点坐标X']),
  component('c14', '电气专业', 971, '综合布线信息配线箱', '综合布线信息配线箱', '综合布线信息配线箱', 21, ['名称', '编号', '编码', '建筑单体名称', '所在楼层', '空间名称', '一级系统分类']),
  component('c15', '电气专业', 1000, '信息插座', '信息插座', '信息插座', 17, ['名称', '编码', '建筑单体名称', '所在楼层', '空间名称', '基点坐标X', '三级系统分类']),
  component('c16', '电气专业', 1041, '卫星电视天线', '卫星电视天线', '卫星电视天线', 19, ['名称', '编码', '建筑单体名称', '所在楼层', '空间名称', '占位长度', '一级系统分类']),
  component('c17', '电气专业', 1061, '电视前端箱', '电视前端箱', '电视前端箱', 22, ['名称', '编号', '编码', '建筑单体名称', '所在楼层', '空间名称', '电源参数']),
  component('c18', '电气专业', 1089, '分配器', '分配器', '分配器', 7, ['三级系统分类', '型号规格', '高度', '宽度', '深度', '安装方式', '底距地高度']),
  component('c19', '电气专业', 1097, '分支器', '分支器', '分支器', 7, ['三级系统分类', '型号规格', '高度', '宽度', '深度', '安装方式', '底距地高度']),
  component('c20', '电气专业', 1110, '会议主机', '会议系统主机柜', '会议系统主机柜', 8, ['编号', '三级系统分类', '型号规格', '电源参数', '额定功率']),
  component('c21', '电气专业', 1119, '广播主机', '广播主机柜', '广播主机柜', 8, ['编号', '三级系统分类', '型号规格', '电源参数', '额定功率']),
  component('c22', '电气专业', 1133, '会议单元', '会议单元', '会议单元', 4, ['三级系统分类', '型号规格', '电源参数', '额定功率']),
  component('c23', '电气专业', 1139, '功率放大器', '功率放大器', '功率放大器', 7, ['三级系统分类', '型号规格', '电源参数', '额定功率', '高度', '宽度', '厚度']),
  component('c24', '电气专业', 1148, '扬声器', '扬声器', '扬声器', 10, ['三级系统分类', '型号规格', '额定电压', '额定功率', '外壳防护等级', '安装方式']),
  component('c25', '电气专业', 1163, '广播分区配线箱', '广播分区配线箱', '广播分区配线箱', 8, ['编号', '三级系统分类', '型号规格', '高度', '宽度', '厚度', '安装方式']),
  component('c26', '电气专业', 1173, '信息发布主机柜', '信息导引系统主机柜', '信息导引系统主机柜', 8, ['编号', '三级系统分类', '型号规格', '电源参数', '额定功率']),
  component('c27', '电气专业', 1182, '触摸屏', '电子触摸台', '电子触摸台', 4, ['三级系统分类', '型号规格', '电源参数', '额定功率']),
  component('c28', '电气专业', 1195, '显示设备 / 壁装显示器', '壁装显示屏', '壁装显示屏', 9, ['三级系统分类', '型号规格', '电源参数', '额定功率', '安装方式']),
  component('c29', '电气专业', 1244, '传感器', '传感器', '传感器', 6, ['三级系统分类', '型号规格', '输出信号类型', '性能参数', '安装方式', '底距地高度']),
  component('c30', '电气专业', 1258, '现场控制器', '现场控制器箱', '现场控制器箱', 14, ['编号', '三级系统分类', '型号规格', '模拟输入点数量', '模拟输出点数量', '数字输入点数量']),
  component('c31', '电气专业', 1273, '模块', '输入输出模块', '输入输出模块', 5, ['三级系统分类', '型号规格', '高度', '宽度', '厚度']),
  component('c32', '电气专业', 1283, '执行器', '电动执行机构', '电动执行机构', 12, ['三级系统分类', '型号规格', '控制方式', '输入信号', '基本误差', '时滞']),
  component('c33', '电气专业', 1298, '建筑设备控制主机', '设备监控主机柜', '设备监控主机柜', 8, ['编号', '三级系统分类', '型号规格', '电源参数', '额定功率']),
  component('c34', '电气专业', 1653, '余压监控系统设备 / 余压现场控制器', '现场控制器箱', '现场控制器箱', 14, ['编号', '三级系统分类', '型号规格', '模拟输入点数量', '数字输入点数量']),
  component('c35', '电气专业', 1674, '视频监控系统 / 视频监控主机', '视频监控主机柜', '视频监控主机柜', 12, ['编号', '三级系统分类', '型号规格', '视频输入回路数', '视频输出回路数']),
  component('c36', '电气专业', 1688, '视频监控系统 / 监控摄像机', '摄像机', '摄像机', 9, ['三级系统分类', '型号规格', 'CCD尺寸', 'CCD像素', '形状样式', '安装方式']),
  component('c37', '电气专业', 1705, '视频监控系统 / 电视墙', '视频电视墙', '视频电视墙', 9, ['三级系统分类', '型号规格', '电源参数', '额定功率', '长度', '宽度', '安装方式']),
  component('c38', '电气专业', 1715, '视频监控系统 / 视频存储设备', '视频存贮设备', '视频存贮设备', 8, ['三级系统分类', '型号规格', '电源参数', '额定功率', '最长存储时间']),
  component('c39', '电气专业', 1725, '出入口控制系统 / 出入口控制器箱', '出入口控制器箱', '出入口控制器箱', 10, ['编号', '三级系统分类', '型号规格', '电源参数', '安装方式']),
  component('c40', '电气专业', 1736, '出入口控制系统 / 出入口控制主机', '出入口系统主机柜', '出入口系统主机柜', 8, ['编号', '三级系统分类', '型号规格', '电源参数', '额定功率']),
  component('c41', '电气专业', 1745, '出入口控制系统 / 身份识别装置', '入门读卡器', '入门读卡器', 4, ['三级系统分类', '型号规格', '安装方式', '底距地高度']),
  component('c42', '电气专业', 1750, '出入口控制系统 / 出门按钮', '出口按钮', '出口按钮', 4, ['三级系统分类', '型号规格', '安装方式', '底距地高度']),
  component('c43', '电气专业', 1791, '汽车库管理系统设备 / 汽车库管理系统主机', '停车管理系统主机柜', '停车管理系统主机柜', 8, ['编号', '三级系统分类', '型号规格', '电源参数', '额定功率']),
  component('c44', '电气专业', 1802, '汽车库管理系统设备 / 停车发卡读卡机 / 停车读卡机', '读卡机柜', '读卡机柜', 7, ['三级系统分类', '型号规格', '电源参数', '额定功率', '高度', '宽度', '厚度']),
  component('c45', '电气专业', 1814, '汽车库管理系统设备 / 车库入口补光灯', '补光灯', '补光灯', 12, ['三级系统分类', '型号规格', '额定电压', '功率因数', '光源类别', '外壳防护等级']),
  component('c46', '电气专业', 1829, '汽车库管理系统设备 / 满位显示屏', '车位显示屏', '车位显示屏', 8, ['三级系统分类', '型号规格', '质量', '电源参数', '额定功率', '像素解析度']),
  component('c47', '电气专业', 1890, '弱电导管', '导管', '导管', 3, ['三级系统分类', '型号规格', '敷设方式']),
  component('c48', '电气专业', 1904, '弱电线槽', '线槽', '线槽', 4, ['三级系统分类', '型号规格', '耐火性能分级', '吊装底距地高度'])
];

const propertyRules: PropertyRule[] = [
  prop('深圳构件标识', 'string', '', '取自对象名称列，按最细分类填写', '构件必须添加，最终必填口径待真实上传样本确认', '附件三说明', '全专业构件', '候选必填/待确认'),
  prop('深圳系统标识', 'string', '', '取自系统表对象名称列，按最细分类填写', '系统必须添加，最终必填口径待真实上传样本确认', '系统表', '给排水/通风与空调/电气系统构件', '候选必填/待确认'),
  prop('名称', 'string', '', '文本', '需与模型对象一致', '电气专业', '综合布线/电视系统构件', '待确认'),
  prop('编号', 'string', '', '文本', '常用于柜、箱、主机等设备', '电气专业', '智能化设备', '候选必填/待确认'),
  prop('编码', 'string', '', '文本', '可承接平台编码或模型编码', '电气专业', '智能化设备', '待确认'),
  prop('建筑单体名称', 'string', '', '文本', '用于空间定位', '电气专业', '智能化设备', '待确认'),
  prop('所在楼层', 'string', '', '文本', '用于楼层定位', '电气专业', '智能化设备', '待确认'),
  prop('空间名称', 'string', '', '文本', '用于房间或空间定位', '电气专业', '智能化设备', '待确认'),
  prop('基点坐标X', 'double', 'm', '数值', '坐标字段必须按数值处理', '电气专业', '空间定位字段', '待确认'),
  prop('基点坐标Y', 'double', 'm', '数值', '坐标字段必须按数值处理', '电气专业', '空间定位字段', '待确认'),
  prop('基点坐标Z', 'double', 'm', '数值', '坐标字段必须按数值处理', '电气专业', '空间定位字段', '待确认'),
  prop('一级系统分类', 'string', '', '文本', '与系统表父级分类保持一致', '电气专业', '系统分类字段', '待确认'),
  prop('二级系统分类', 'string', '', '文本', '与系统表中级分类保持一致', '电气专业', '系统分类字段', '待确认'),
  prop('三级系统分类', 'string', '', '详见系统表对象名称要求', '系统构件需按系统表取值', '电气专业/系统表', '智能化系统构件', '候选必填/待确认'),
  prop('型号规格', 'string', '', '文本', '设备型号或规格', '电气专业', '智能化设备', '候选必填/待确认'),
  prop('信息总点数', 'string', '', '文本', '服务器、配线设备常见字段', '电气专业', '信息设备', '待确认'),
  prop('电源参数', 'string', '', '文本', '应保持英文半角符号', '电气专业', '带电设备', '候选必填/待确认'),
  prop('额定功率', 'double', 'W', '数值', '必须按数值字段处理', '电气专业', '带电设备', '候选必填/待确认'),
  prop('额定电压', 'double', 'V', '数值', '必须按数值字段处理', '电气专业', '带电设备', '待确认'),
  prop('高度', 'double', 'mm', '数值', '尺寸字段按数值处理', '电气专业', '设备尺寸', '待确认'),
  prop('宽度', 'double', 'mm', '数值', '尺寸字段按数值处理', '电气专业', '设备尺寸', '待确认'),
  prop('厚度', 'double', 'mm', '数值', '尺寸字段按数值处理', '电气专业', '设备尺寸', '待确认'),
  prop('长度', 'double', 'mm', '数值', '尺寸字段按数值处理', '电气专业', '设备尺寸', '待确认'),
  prop('深度', 'double', 'mm', '数值', '尺寸字段按数值处理', '电气专业', '设备尺寸', '待确认'),
  prop('安装方式', 'string', '', '文本', '墙装、吊装等安装方式需保持标准值', '电气专业', '安装字段', '待确认'),
  prop('底距地高度', 'double', 'm', '数值', '按米为单位', '电气专业', '安装字段', '待确认'),
  prop('模拟输入点数量', 'double', '', '数值', '现场控制器点位字段', '电气专业', '现场控制器箱', '待确认'),
  prop('模拟输出点数量', 'double', '', '数值', '现场控制器点位字段', '电气专业', '现场控制器箱', '待确认'),
  prop('数字输入点数量', 'double', '', '数值', '现场控制器点位字段', '电气专业', '现场控制器箱', '待确认'),
  prop('数字输出点数量', 'double', '', '数值', '现场控制器点位字段', '电气专业', '现场控制器箱', '待确认'),
  prop('输出信号类型', 'string', '', '文本', '传感器输出信号字段', '电气专业', '传感器', '待确认'),
  prop('性能参数', 'string', '', '文本', '传感器性能字段', '电气专业', '传感器', '待确认'),
  prop('控制方式', 'string', '', '文本', '执行器控制字段', '电气专业', '电动执行机构', '待确认'),
  prop('输入信号', 'string', '', '文本', '执行器输入字段', '电气专业', '电动执行机构', '待确认'),
  prop('时滞', 'double', 's', '数值', '执行器响应字段', '电气专业', '电动执行机构', '待确认'),
  prop('吞吐量', 'double', 'tps', '数值', '路由器性能字段', '电气专业', '路由器', '待确认'),
  prop('端口数量', 'double', '个', '数值', '路由器性能字段', '电气专业', '路由器', '待确认'),
  prop('有线/无线', 'enum', '', '"有线"|"无线"', '必须从枚举中选择', '电气专业', '路由器', '待确认'),
  prop('外壳防护等级', 'string', '', 's"IP"', '属性值以 IP 开始', '电气专业', '设备外壳', '待确认'),
  prop('CCD尺寸', 'double', '吋', '数值', '摄像机字段', '电气专业', '摄像机', '待确认'),
  prop('CCD像素', 'string', '', '文本', '摄像机字段', '电气专业', '摄像机', '待确认'),
  prop('形状样式', 'string', '', '文本', '摄像机字段', '电气专业', '摄像机', '待确认'),
  prop('视频输入回路数', 'double', '', '数值', '视频监控主机字段', '电气专业', '视频监控主机柜', '待确认'),
  prop('视频输出回路数', 'double', '', '数值', '视频监控主机字段', '电气专业', '视频监控主机柜', '待确认'),
  prop('最长存储时间', 'double', '年', '数值', '视频存储设备字段', '电气专业', '视频存贮设备', '待确认'),
  prop('光源光通量', 'double', 'lm', '数值', '补光灯字段', '电气专业', '补光灯', '待确认'),
  prop('像素解析度', 'double', '点/m²', '数值', '车位显示屏字段', '电气专业', '车位显示屏', '待确认'),
  prop('敷设方式', 'string', '', '文本', '弱电导管字段', '电气专业', '导管', '待确认'),
  prop('耐火性能分级', 'string', '', '文本', '弱电线槽字段', '电气专业', '线槽', '待确认')
];

const validationExamples: ValidationExample[] = [
  { label: '位号编码', value: 'Ctower-A01-MAD01-SRV001_CAB01', status: 'PASSED', reason: '包含 Ctower 前缀、单元码、系统码和设备码。' },
  { label: '文档编码', value: 'ETB0001', status: 'PASSED', reason: 'ETB 对应三维模型，后接四位计数序号。' },
  { label: '属性类型', value: '额定功率 = 220W', status: 'FAILED', reason: '额定功率为 double/W，应上传数值 220，单位单独保存。' },
  { label: '枚举取值', value: '有线/无线 = 蓝牙', status: 'FAILED', reason: '取值要求为 "有线"|"无线"，不能使用未列入值。' },
  { label: '系统标识', value: '三级系统分类 = 视频监控系统', status: 'WARNING', reason: '应与系统表最细对象名称对齐，需管理方确认归属层级。' },
  { label: '半角符号', value: '外壳防护等级 = ＩＰ５４', status: 'FAILED', reason: '属性值涉及符号和字母时需使用英文半角。' },
  { label: '父子继承', value: '停车读卡机缺少停车发卡读卡机父级字段', status: 'WARNING', reason: '子类构件应满足父类模型单元属性要求。' }
];

const basePluginFields: Array<Omit<IntelligentPluginFieldStandardRow, 'profession' | 'sourceSheet' | 'sourceRow' | 'sourceObjectPath' | 'shenzhenComponentIdentifier' | 'intelligentMappingName'>> = [
  {
    fieldName: '深圳构件标识',
    requiredPolicy: '候选必填/待确认',
    valueType: 'string',
    unit: '',
    valueRequirement: '取自附件三对象名称列，按最细分类填写',
    suggestedRevitParameterName: 'DD_SZ_ComponentId',
    standardSource: '附件三',
    remark: '建议每个构件上传，平台用于构件标准归类；最终必填口径待真实上传样本确认。'
  },
  {
    fieldName: '深圳系统标识',
    requiredPolicy: '候选必填/待确认',
    valueType: 'string',
    unit: '',
    valueRequirement: '取自系统表对象名称列，按最细分类填写',
    suggestedRevitParameterName: 'DD_SZ_SystemId',
    standardSource: '附件三',
    remark: '建议系统相关构件上传，平台用于系统归属和跨专业查询；最终必填口径待真实上传样本确认。'
  },
  {
    fieldName: '位号编码',
    requiredPolicy: '候选必填/待确认',
    valueType: 'string',
    unit: '',
    valueRequirement: '以 Ctower 为前缀，包含单元码、系统码、设备码等码段',
    suggestedRevitParameterName: 'DD_TagCode',
    standardSource: '附件十一',
    remark: '插件在建模阶段写入候选位号，平台校验后确认。'
  },
  {
    fieldName: '构件编码',
    requiredPolicy: '候选必填/待确认',
    valueType: 'string',
    unit: '',
    valueRequirement: '平台内构件唯一编码，后续用于追踪、工单和归档',
    suggestedRevitParameterName: 'DD_ComponentCode',
    standardSource: '平台报建契约',
    remark: '首版为前端契约字段，后端落库时保持同名语义；不代表最终编码结果已冻结。'
  },
  {
    fieldName: '专业编码',
    requiredPolicy: '候选必填/待确认',
    valueType: 'string',
    unit: '',
    valueRequirement: '智能化专业建议写入 intelligent',
    suggestedRevitParameterName: 'DD_Discipline',
    standardSource: '平台报建契约',
    remark: '用于数据中心按专业隔离，不与其他专业构件混排；枚举值后续由平台标准库确认。'
  }
];

function component(
  id: string,
  sourceSheet: string,
  sourceRow: number,
  sourceObjectPath: string,
  shenzhenComponentIdentifier: string,
  intelligentMappingName: string,
  fieldCount: number,
  sampleFields: string[]
): ComponentStandard {
  return {
    id,
    sourceSheet,
    sourceRow,
    sourceObjectPath,
    shenzhenComponentIdentifier,
    intelligentMappingName,
    fieldCount,
    sampleFields
  };
}

function prop(
  fieldName: string,
  valueType: PropertyRule['valueType'],
  unit: string,
  valueRequirement: string,
  remark: string,
  sourceSheet: string,
  mappingScope: string,
  requiredPolicy: string
): PropertyRule {
  return { fieldName, valueType, unit, valueRequirement, remark, sourceSheet, mappingScope, requiredPolicy };
}

function toPluginRequiredPolicy(policy: string) {
  if (policy === '必填' || policy === '候选必填/待确认') return '候选必填/待确认';
  if (policy === '选填项') return '选填';
  return '选填/待确认';
}

function toRevitParameterName(fieldName: string) {
  const aliases: Record<string, string> = {
    名称: 'DD_Name',
    编号: 'DD_Number',
    编码: 'DD_Code',
    建筑单体名称: 'DD_BuildingName',
    所在楼层: 'DD_LevelName',
    空间名称: 'DD_SpaceName',
    基点坐标X: 'DD_BasePointX',
    基点坐标Y: 'DD_BasePointY',
    基点坐标Z: 'DD_BasePointZ',
    一级系统分类: 'DD_SystemLevel1',
    二级系统分类: 'DD_SystemLevel2',
    三级系统分类: 'DD_SystemLevel3',
    型号规格: 'DD_ModelSpec',
    信息总点数: 'DD_InfoPointCount',
    电源参数: 'DD_Power',
    额定功率: 'DD_RatedPower',
    额定电压: 'DD_RatedVoltage',
    高度: 'DD_Height',
    宽度: 'DD_Width',
    厚度: 'DD_Thickness',
    长度: 'DD_Length',
    深度: 'DD_Depth',
    安装方式: 'DD_InstallMethod',
    底距地高度: 'DD_BottomHeight',
    模拟输入点数量: 'DD_AI_Count',
    模拟输出点数量: 'DD_AO_Count',
    数字输入点数量: 'DD_DI_Count',
    数字输出点数量: 'DD_DO_Count',
    输出信号类型: 'DD_OutputSignal',
    性能参数: 'DD_Performance',
    控制方式: 'DD_ControlMode',
    输入信号: 'DD_InputSignal',
    时滞: 'DD_TimeLag',
    吞吐量: 'DD_Throughput',
    端口数量: 'DD_PortCount',
    '有线/无线': 'DD_WiredWireless',
    外壳防护等级: 'DD_IPRating',
    CCD尺寸: 'DD_CCDSize',
    CCD像素: 'DD_CCDPixel',
    形状样式: 'DD_ShapeStyle',
    视频输入回路数: 'DD_VideoInputCount',
    视频输出回路数: 'DD_VideoOutputCount',
    最长存储时间: 'DD_MaxStorageYears',
    光源光通量: 'DD_LuminousFlux',
    像素解析度: 'DD_PixelResolution',
    敷设方式: 'DD_LayingMethod',
    耐火性能分级: 'DD_FireResistanceClass'
  };
  return aliases[fieldName] ?? `DD_${fieldName}`;
}

function makePluginRow(
  componentStandard: ComponentStandard,
  field: Omit<IntelligentPluginFieldStandardRow, 'profession' | 'sourceSheet' | 'sourceRow' | 'sourceObjectPath' | 'shenzhenComponentIdentifier' | 'intelligentMappingName'>
): IntelligentPluginFieldStandardRow {
  return {
    profession: '智能化',
    sourceSheet: componentStandard.sourceSheet,
    sourceRow: componentStandard.sourceRow,
    sourceObjectPath: componentStandard.sourceObjectPath,
    shenzhenComponentIdentifier: componentStandard.shenzhenComponentIdentifier,
    intelligentMappingName: componentStandard.intelligentMappingName,
    ...field
  };
}

export function fetchIntelligentPluginFieldStandardRows() {
  const propertyRuleMap = new Map(propertyRules.map((rule) => [rule.fieldName, rule]));
  const rows = componentStandards.flatMap((componentStandard) => {
    const baseRows = basePluginFields.map((field) => makePluginRow(componentStandard, field));
    if (componentStandard.sampleFields.length === 0) {
      return [
        ...baseRows,
        makePluginRow(componentStandard, {
          fieldName: '父级继承字段',
          requiredPolicy: '按父级继承/待确认',
          valueType: 'string',
          unit: '',
          valueRequirement: '子类构件继承父类属性要求，待插件团队与管理方确认字段清单',
          suggestedRevitParameterName: 'DD_InheritedFields',
          standardSource: '附件三',
          remark: '附件中未给出首版样例字段，前端只提示继承规则，不伪装为最终标准。'
        })
      ];
    }
    const sampleRows = componentStandard.sampleFields.map((fieldName) => {
      const rule = propertyRuleMap.get(fieldName);
      return makePluginRow(componentStandard, {
        fieldName,
        requiredPolicy: toPluginRequiredPolicy(rule?.requiredPolicy ?? '待确认'),
        valueType: rule?.valueType ?? 'string',
        unit: rule?.unit ?? '',
        valueRequirement: rule?.valueRequirement ?? '按附件三字段名称和值严格填写',
        suggestedRevitParameterName: toRevitParameterName(fieldName),
        standardSource: rule?.sourceSheet ?? componentStandard.sourceSheet,
        remark: rule?.remark ?? '字段来自附件三构件样例字段，字段策略待管理方确认。'
      });
    });
    return [...baseRows, ...sampleRows];
  });
  return clone(rows);
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

export function fetchBimCodeStandardOverview(): BimCodeStandardOverview {
  const packageRows = professionStandardPackages.map((item) => (
    item.professionCode === 'intelligent'
      ? { ...item, fieldMatrixCount: fetchIntelligentPluginFieldStandardRows().length }
      : item
  ));
  return clone({
    standardSources,
    standardLayers,
    professionStandardPackages: packageRows,
    platformEncodingPolicy,
    codeRules,
    professions,
    systemGroups,
    componentStandards,
    propertyRules,
    validationExamples
  });
}
