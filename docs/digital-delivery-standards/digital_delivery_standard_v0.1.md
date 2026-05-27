# 工程数字化交付标准 V0.1-beta 草案

本标准为 V0.1-beta 可评审草案，不等于当前平台验收标准，也不表示平台或 Hermes 已经具备所有能力。

本版本已吸收数据库 / 平台 Agent 基于真实 NAS 元数据现状的阶段性评审意见，以及 Hermes（Jarvis 为旧称/非正式称呼）对 catalog-only、Tool、Memory、Missing Evidence 边界的阶段性评审意见。

本版本进一步将深圳市建筑工务署数字化交付系列标准作为公司数字化交付标准底座的主干依据。已开发标准不另起体系、不直接废弃，应通过标准对齐矩阵进行回拉、映射、修订和固化。

当前主线保持为：

```text
catalog-only / read-only / permission-aware / Missing Evidence / draft-only
```

V0.1-beta 重点服务“模型 + 图纸 + PDF 出图”资产治理。当前已入库资产规模约 68,904 条，其中主力类型为 RVT/BIM 模型约 29,752 条、PDF 图纸约 21,888 条、DWG 图纸约 16,659 条；IFC / NWC / NWD / DXF / Office / Excel 数量较少。Office / Excel 清单保留为交付包和 Manifest 的后续能力，不作为 V0.1 主线第一优先级。

标准适用策略：V0.1-beta 对历史存量文件以识别、标注、提示整改为主；对新建项目和新交付文件逐步执行推荐命名、目录和 Manifest 标准。不得将本标准理解为要求立即整改全部历史存量文件。

工务署对齐策略：公司标准优先对齐《深圳市建筑工务署建筑工程数字化交付工作导则》（2024 年修订版）、《深圳市建筑工务署建筑工程建筑信息模型数据交付标准》（试行版）、《深圳市建筑工务署建筑工程建设阶段建筑信息模型分类和编码标准》（试行版）和《深圳市建筑工务署建筑工程运维阶段建筑信息模型分类和编码标准》（试行版）。工务署已定义的分类编码、阶段编码、建设阶段/运维阶段映射关系应保持不变；公司仅在其基础上扩展项目实例编码、企业资产编码、成本编码、采购编码、族库编码等企业级编码。

专业字段治理策略：由于当前卓羽智能数据中台中的专业字段尚未形成稳定业务使用场景，V0.1-beta 应趁早把专业编码、中文专业名、中文别名和平台字段映射定义为公司基础标准草案。现有平台 `discipline` 字段只作为现状输入和映射对象，不应反向决定公司专业标准；后续新项目录入、历史文件识别、人工补录和平台字段治理应逐步向本标准收敛。

DWG 图纸内容理解、RVT/BIM 构件级解析、生产级 NAS 向量库、工程语义搜索、全文内容问答、Agent 自动治理执行均属于 future phase。涉及图层、图框、标题栏、外部参照、族、构件、参数、模型内部语义、文件正文的判断，当前必须返回 Missing Evidence。

## 1. 总则

本标准定义工程数字化交付资产的分类、编码、命名、目录、Manifest、最小元数据、权限、生命周期、证据等级、检查项与 Hermes 回答边界。

V0.1-beta 的目标不是完成全量 BIM/CAD 专业标准，而是建立一个能贴合公司真实 NAS 资料沉淀方式、可由平台和 Hermes 逐步落地的目录级治理标准。

规则阶段定义：

- current：当前可基于稳定资产目录、稳定 View/API 元数据、权限策略或已收口 API 状态进行只读判断。
- current-lineage：当前可从历史目录、文件名或脱敏路径关键词中识别的线索，可用于低置信度分类和提示，但不等同于平台稳定字段，也不作为强制合规判断依据。
- backlog：方向明确，但需要补字段、补 View/API、补任务、补流程或人工确认后执行。
- future：需要 DWG/RVT/全文/构件级解析、语义索引或专业审查后才可判断。

## 2. 适用范围

V0.1-beta 当前适用于公司内部 BIM / 设计 / 项目团队的资料治理，尤其是施工图、模型、图纸收发、归档和交付协调场景。

分阶段对象：

| 阶段 | 服务对象 | 标准定位 |
|---|---|---|
| 一期 | 公司内部 BIM、机电交付、设计与项目资料治理团队 | catalog-only 资产目录治理、权限控制、命名/路径/版本线索识别。 |
| 二期 | 业主、施工单位、总包、设计院/顾问、楼宇/物业交付 | 在 V0.1 稳定后扩展交付包、Manifest、权限边界和项目级标准绑定。 |

当前适用：

- RVT/BIM、PDF、DWG、IFC/NWC/NWD/DXF 等工程资产是否进入目录。
- 基于文件名、目录关键词、脱敏路径、元数据的项目/专业/阶段/版本/类型线索识别；其中文件名和路径关键词属于 `current-lineage`。
- 权限标签、保密等级、生命周期、process_status 的目录级解释。
- Hermes 基于只读 Catalog Tool / Gateway / View Contract 的目录查询回答。
- Missing Evidence 的触发、解释与记录。

当前不适用：

- DWG 图层、图框、标题栏、块属性、外部参照、标注、坐标内容的合规判断。
- RVT sheets/views/levels/families、Family/Type、构件参数、LOD/LOI 的合规判断。
- PDF / Office / 图纸正文内容问答。
- Hermes 直接查底表、生成 SQL、读取 NAS 文件正文、写 DB、写 NAS、写 Hermes memory。
- 物业/运维交付的正式验收标准。

## 3. 术语与定义

| 术语 | 定义 | 当前阶段 |
|---|---|---|
| Asset | 可进入工程交付目录管理的文件、模型、清单、说明或辅助资料。 | current |
| Catalog-only | 仅基于资产目录、文件名、脱敏路径线索、元数据进行治理，不读取或理解文件正文和模型内部内容。 | current |
| Hermes | Catalog Query Agent / 资产目录查询助手，不是 Engineering Semantic Agent。 | current |
| Missing Evidence | 当前证据不足以回答或判断时必须返回的状态。 | current |
| Display Path / Path Hint | 面向客户侧和 Agent 的脱敏路径或路径提示；稳定 View 字段仍为 backlog。 | current/backlog |
| Raw Storage Path | 真实存储路径，属于敏感字段，默认不对外暴露。 | current-sensitive |
| Standard Override | 项目、业主、地方、顾问标准对公司默认标准的覆盖记录。 | backlog |
| GWS Standard | 深圳市建筑工务署数字化交付系列标准。公司数字化交付标准以其为主干，不复制另起一套分类编码。 | current |
| Standard Alignment Matrix | 将公司现有规则、平台字段、Agent 边界映射到工务署导则、BIM 数据交付标准、建设阶段编码标准和运维阶段编码标准的对照矩阵。 | current |
| Company Extension Code | 在工务署分类编码不变的前提下，公司扩展的项目实例、企业资产、成本、采购、族库等编码。 | backlog |
| Discipline Code | 公司标准专业编码，如 `ARCH`、`STR`、`PLMB`。V0.1-beta 将其作为业务标准草案，不等同当前平台字段已稳定落地。 | 标准 current / 平台 backlog |
| Discipline Alias | 专业中文别名、目录关键词和文件名前缀，如“建施”“结施”“水施”“电施”。用于历史资料识别和新文件命名提示。 | current-lineage |

## 4. 标准分级与版本管理

### 4.1 三层标准体系

| 层级 | 名称 | 说明 | 当前阶段 |
|---|---|---|---|
| Level 1 | 公司基础标准 | 默认命名、编码、目录、证据和 Agent 回答边界。 | current |
| Level 2 | 项目级标准 | 项目特定目录、阶段、专业、交付包、顾问编码约定。 | backlog |
| Level 3 | 工务署 / 甲方 / 业主 / 地方 / 顾问标准 | 外部强制标准或顾问编码体系；当前公司底座优先以工务署系列标准为主干。 | current/backlog |

冲突处理：

- 项目有明确甲方、地方或顾问标准时，应优先记录并绑定项目级适用标准。
- 深圳市建筑工务署项目或参考工务署体系的项目，应优先记录工务署标准来源和适用版本。
- 公司基础标准作为默认规则，不强制覆盖已有历史资料。
- 冲突项应标记为 `project_override` 或 `owner_override`。
- Hermes 不应自行裁决标准冲突，应返回标准来源、证据等级和 Missing Evidence / Needs Review。

### 4.2 工务署主干与公司扩展边界

工务署标准作为本公司数字化交付标准底座的主干时，应遵守以下边界：

- 工务署建设阶段分类和编码标准中的表代码与分类编码保持不变，例如建筑 `10`、空间 `12`、系统 `16`、构件 `30`、材质 `40`、属性 `41`、项目阶段 `43`、分部分项工程 `44`。
- 工务署运维阶段分类和编码标准中的建筑、空间、构件设备、关系分类编码保持不变；建设阶段编码与运维阶段编码的映射关系不得由 Hermes 或平台自行推断。
- 公司扩展编码应建立独立命名空间，例如 `company_project_instance_code`、`enterprise_asset_code`、`cost_code`、`procurement_code`、`family_library_code`。
- 平台字段可以先记录工务署分类编码、公司扩展编码和字段来源；在无稳定字段前，只能作为 backlog 或 current-lineage 管理。
- Hermes 只能解释已由平台或人工提供的编码字段，不能根据文件名、目录或资产类型自行判定某对象满足工务署分类编码。

### 4.3 版本阶段

- V0.1-beta：贴合真实 NAS 现状的 catalog-only 标准草案。
- V0.1：完成平台 / Hermes 评审闭环后的 catalog-only 基线。
- V0.2：结构化 Manifest、交付包状态、字段来源置信度、项目标准绑定。
- Future：DWG/RVT 解析、构件证据、全文证据、工程语义搜索。

### 4.4 评审责任

| 评审方 | 主要评审内容 | 输出 |
|---|---|---|
| 数据库 / 平台 Agent | 字段支撑、View/API 可落地性、权限与脱敏、状态字段边界。 | current / backlog / future 字段支撑意见。 |
| Hermes Agent | Catalog Tool、回答边界、Memory 边界、Missing Evidence 与 overclaim 风险。 | 可回答 / Missing Evidence / Needs Review 规则意见。 |
| BIM/CAD 负责人 | 专业编码、DWG/RVT 命名、历史编码兼容、future 图层/构件/建模标准。 | 专业标准确认或待定项。 |
| 项目交付负责人 | 交付包、Manifest、项目级标准、甲方/顾问标准覆盖。 | 项目适用标准和交付流程意见。 |
| 标准管理员 | 工务署标准对齐矩阵、公司扩展编码命名空间、标准包同步与版本固化。 | 统一标准包和同步记录。 |

在 V0.1-beta 发给高层评审前，应先完成数据库 / 平台 Agent 与 Hermes Agent 的阶段性评审闭环；BIM/CAD 专业评审可在标准接近 V0.1 或 V0.2 时再启动。

## 5. 工程资产分类标准

V0.1-beta 资产优先级按真实 NAS 主力资产调整为：

| 优先级 | 资产编码 | 资产类型 | 典型扩展名 | 当前阶段 | 当前治理重点 | 后续扩展 |
|---|---|---|---|---|---|---|
| 1 | A01 | RVT / BIM 模型 | `.rvt`、`.ifc`、`.nwc`、`.nwd` | current | 模型是否入目录、项目/专业/版本线索、权限边界。 | future：RVT parse / component evidence。 |
| 2 | A02 | PDF 图纸 / 出图 | `.pdf` | current | 图纸文件入目录、图号/专业/阶段/版本线索、与 DWG/RVT 的目录级对应。 | future：全文/版面解析。 |
| 3 | A03 | DWG / CAD 图纸 | `.dwg`、`.dxf` | current | 图纸文件入目录、专业/阶段/图号/版本线索。 | future：DWG parse evidence。 |
| 4 | A04 | 模型交换文件 | `.ifc`、`.nwc`、`.nwd` | current/backlog | 文件类型、模型来源、版本线索。 | future：交换模型解析。 |
| 5 | A05 | 图纸目录 / 说明 / 大样 / 设计总说明 | `.pdf`、`.dwg`、`.docx` | current/backlog | 目录级识别，不做正文判断；正文问答需 `full_text_evidence`。 | future：full_text evidence / manual evidence。 |
| 6 | A06 | 交付包 Manifest / Excel / Office 清单 | `.xlsx`、`.csv`、`.docx` | backlog | 文件存在性和候选清单识别。 | V0.2：结构化 Manifest。 |
| 7 | A07 | 审核记录 / 问题清单 / 变更记录 | `.xlsx`、`.csv`、`.pdf`、`.docx` | backlog | 目录级归类。 | future：结构化问题和正文解析。 |
| 8 | A08 | 其他辅助文件 | 其他 | current/backlog | 入目录、权限、保密等级。 | 按项目扩展。 |

表中 `current` 均指目录级治理能力，包括入目录、格式、项目、权限、文件名/路径线索等；不代表 Hermes 或平台已具备 DWG/RVT/PDF/Office 内容理解能力。

V0.1 不把 Office / Excel 作为主力资产，但保留其作为交付包和 Manifest 的后续能力。

## 6. 项目 / 专业 / 阶段 / 文件类型编码标准

### 6.1 项目编码

项目编码优先使用平台项目关系字段 `project_id` / `project_code`。若历史资料目录中包含“项目编号-项目名”结构，可作为 `path_evidence`。文件名中的项目编号仅作为线索。

### 6.2 阶段编码与工务署交付节点映射

阶段编码以工务署建设阶段分类和编码标准中的项目阶段分类 `43` 为主干。公司当前 `SD` / `DD` / `CD` / `AS` / `OM` 等短码保留为平台展示和历史资料识别别名，不作为替代工务署分类编码的新体系。

历史资料不要求立即改名。阶段识别采用“工务署阶段/节点 + 公司别名 + alias / keyword 映射”。

| 工务署阶段 / 节点 | 工务署分类编码参考 | 公司别名 | alias / 目录关键词示例 | 当前阶段 |
|---|---|---|---|---|
| 策划阶段 | `43-10.00.00` | PLAN | 项目规划、项目建议书、可行性研究、审批立项 | backlog/current-lineage |
| 前期规划阶段 / 勘察 | `43-11.00.00` / `43-11.11.00` | SURVEY | 勘察、初勘、详勘、定测、补充定测 | backlog/current-lineage |
| 设计阶段 / 方案设计 | `43-12.00.00` / `43-12.11.00` | SD | 方案、概念、概设、方案设计 | current-lineage |
| 设计阶段 / 初步设计 | `43-12.12.00` | DD | 初设、初步设计、扩初、初步设计概算 | current-lineage |
| 设计阶段 / 施工图设计 | `43-12.13.00` | CD | 施工图、施工图设计、施设、施工版 | current-lineage |
| 施工阶段 / 工程招标 | `43-13.10.00` | TENDER | 招标、投标、Tender、招采、设备及材料招标 | current-lineage |
| 施工阶段 / 虚拟建造 | `43-13.11.00` | BIM-CONSTRUCTION | BIM 策划、施工 BIM、施工模型、竣工模型、模型应用 | backlog/current-lineage |
| 施工阶段 / 产品预制采购 | `43-13.12.00` | PROCUREMENT | 物料统计、制造委托、工厂加工、材料运输、仓储发货 | backlog |
| 施工阶段 / 工程施工 | `43-13.15.00` | CONSTRUCTION | 地基与基础施工、主体结构施工、机电安装施工、幕墙施工、建筑装饰装修施工 | current-lineage |
| 施工阶段 / 竣工 | `43-13.16.00` | AS | 竣工、竣工图、竣工验收、建筑实体交付、竣工模型交付 | current-lineage |
| 运营维护阶段 | `43-15.00.00`，并衔接工务署运维阶段编码标准 | OM | 运维、运营、设施维护、物业移交 | backlog |

公司历史 NAS 目录中还存在一些不是工务署主阶段本身、但可作为阶段或状态线索的目录词：

| 公司目录线索 | 建议归属 | alias / 目录关键词示例 | 当前阶段 |
|---|---|---|---|
| REVIEW | 设计 / 施工相关审查线索 | 审图、审图通过版、报审、送审、审查 | current-lineage |
| BLUEPRINT | 发布/归档状态线索 | 蓝图、盖章版、归档、发布图、正式图 | current-lineage |
| PROCESS | 文件流转或过程状态线索 | 过程文件、变更、提资、收文件、收图纸、发图、收/发、IN | current-lineage |
| MODEL | BIM 成果类别线索 | BIM模型、模型、RVT、Revit、NWC、NWD、IFC | current-lineage |
| OTHER | 无法识别 | 无法识别、混合目录 | current |

`current-lineage` 表示当前可作为目录级线索，不代表已有稳定文件级阶段字段。

### 6.3 专业编码、中文别名与平台字段映射

本节是专业字段的业务标准草案，不是当前平台字段现状。由于当前平台专业字段尚未形成强业务约束，V0.1-beta 建议现在统一专业编码、中文专业名和中文别名，避免后续交付、查询、权限、统计和 Manifest 再反复返工。

专业字段标准化目标：

- `discipline_code`：公司标准专业编码，作为后续新项目、新交付文件、Manifest 和平台下拉框的推荐值。
- `discipline_name_cn`：标准中文专业名，用于界面展示、报表、交付清单和人工确认。
- `discipline_alias_cn`：中文别名 / 文件名关键词，用于历史 NAS 文件名和目录识别。
- `platform_discipline_value`：当前或后续平台字段枚举映射；它是实现映射，不是业务标准本身。
- `discipline_source`：专业字段来源，建议取值 `manual` / `metadata` / `filename` / `path_hint` / `manifest`。
- `discipline_confidence`：识别置信度，建议分为 `confirmed` / `high` / `medium` / `low` / `missing`。

专业识别不得完全依赖现有平台字段。`GENERAL` 和空专业在历史数据中较多，应结合 filename / path / metadata 联合识别。历史文件中的中文别名只能作为 `current-lineage` 线索；经人工确认或 Manifest 确认后，才可固化为稳定专业字段。

工务署对齐口径：

- 工务署 BIM 数据交付标准在模型数据层面主要按建筑类、结构类、机电类组织模型单元数据。公司专业编码应能向这三类汇总，不得割裂。
- 工务署数字化交付工作导则的命名说明中，电气专业可包含电气和智能化，给水排水专业可包含给水排水和消防。公司保留 `ELEC` / `INT`、`PLMB` / `FIRE` 的细分，是企业治理扩展，不改变工务署主干。
- 工务署运维阶段标准以“专业-系统-设备”的层级组织机电设备对象。公司平台的 `discipline_code` 只是文件/模型资产专业字段，不等同运维构件设备分类编码。

| 公司专业组 | 工务署模型数据专业类 | 公司细分专业编码 | 说明 |
|---|---|---|---|
| 建筑类 | 建筑专业模型单元 | `ARCH`、`DEC`、`LAND`、`CURT`、`CIVIL` | 装饰、景观、幕墙、总图等作为公司细分扩展。 |
| 结构类 | 结构专业模型单元 | `STR`、`SEIS` | 抗震支架是否独立成专业需 BIM/CAD 负责人确认。 |
| 机电类 | 机电专业模型单元 | `PLMB`、`HVAC`、`ELEC`、`FIRE`、`INT`、`GAS` | 给排水/消防、电气/智能化的边界需结合公司业务确认。 |

| 标准编码 | 中文专业 | 中文别名 / 目录关键词 | 平台字段映射建议 | 当前阶段 |
|---|---|---|---|---|
| ARCH | 建筑 | 建筑、建施、建筑专业、A- | ARCHITECTURE | current |
| STR | 结构 | 结构、结施、S- | STRUCTURE | current |
| PLMB | 给排水 | 给排水、水施、消水、P- | PLUMBING | current |
| HVAC | 暖通 | 暖通、空调、通风、暖通施、H- | HVAC | current |
| ELEC | 电气 | 电气、电施、强电、弱电、DS- | ELECTRICAL | current |
| FIRE | 消防 | 消防、消水、喷淋、消防电 | FIRE_PROTECTION | current |
| INT | 智能化 | 智能化、智施、弱电智能化 | INTELLIGENT | current |
| DEC | 装饰 | 装饰、内装、精装、装修 | GENERAL / TBD | current-lineage |
| LAND | 景观 | 景观、园林、绿化 | GENERAL / TBD | current-lineage |
| CURT | 幕墙 | 幕墙、外立面 | GENERAL / TBD | current-lineage |
| GAS | 燃气 | 燃气、煤气 | GAS | current |
| SEIS | 抗震支架 | 抗震、支架、抗震支架 | GENERAL / TBD | backlog |
| CIVIL | 总图 / 室外 | 总图、室外、道路、管综 | GENERAL | current-lineage |
| OTHER | 其他 | GENERAL、空专业、无法识别 | GENERAL / blank | current |

专业别名冲突处理：

- `消水` 可能指给排水或消防水系统，应由 BIM/CAD 负责人确认公司习惯；确认前只作为 `PLMB` / `FIRE` 的低置信度线索。
- `弱电` 可能归入电气或智能化，应由公司业务口径确认；确认前不得自动写成稳定 `ELEC` 或 `INT`。
- `GENERAL` 和空专业不得作为专业已确认，只能表示未知、泛专业或当前平台暂未细分。
- `DEC`、`LAND`、`CURT`、`SEIS`、`CIVIL` 等若当前平台没有稳定枚举，可先进入标准码表和中文别名，平台实现可在 V0.2 通过映射或扩展枚举落地。

后续平台建议：

- 新项目和新交付文件优先使用 `discipline_code` 和 `discipline_name_cn`。
- 历史文件保留原始字段，同时新增标准化建议值和来源置信度。
- 平台 UI 可以先提供标准专业下拉框，再允许项目级 / 业主级 override。
- Hermes 只能说明“目录 / 文件名 / 路径线索显示可能属于某专业”，不得把 `current-lineage` 专业线索说成内容确认。

### 6.4 文件类型编码

| 编码 | 文件类型 | 典型扩展名 | 当前阶段 |
|---|---|---|---|
| RVT | Revit 模型 | `.rvt` | current |
| PDF | PDF 图纸 / 文档 | `.pdf` | current |
| DWG | CAD 图纸 | `.dwg` | current |
| IFC | IFC 交换模型 | `.ifc` | current/backlog |
| NWC | Navisworks 缓存模型 | `.nwc` | current/backlog |
| NWD | Navisworks 发布模型 | `.nwd` | current/backlog |
| DXF | CAD 交换文件 | `.dxf` | current/backlog |
| GIS | GIS 模型 / 地理数据 | `.shp`、`.gdb`、`.mdb`、`.kml`、`.gml`、`.osgb`、`.osg` | backlog |
| POINT_CLOUD | 点云模型 | `.pcd`、`.ply`、`.las`、`.pts` | backlog |
| VIDEO | 视频成果 | `.mpeg2`、`.mpeg4`、`.avi`、`.avs` | backlog |
| OFFICE | Office / Excel / 清单 | `.xlsx`、`.csv`、`.docx`、`.pptx` | backlog |
| OTHER | 其他 | 其他 | current |

## 7. 工程文件命名标准

V0.1-beta 不再假设历史资料已经符合单一命名格式。命名标准分为推荐新格式、历史命名识别、外部编码兼容和泛名文件标记。

### 7.1 推荐新文件命名格式

工务署导则要求文件夹和文件命名简明、便于辨识、查阅与搜索；字段之间宜使用半角下划线 `_` 分隔，文件夹命名由交付节点和成果类别组成。公司推荐格式在此基础上保留项目编号和资产类型字段，便于平台资产目录治理。

```text
项目编号_楼栋或区域_专业_阶段_资产类型_图号或模型编号_版本.扩展名
```

示例：

```text
P2026-A01_B03_ARCH_CD_DWG_A-101_V03.dwg
P2026-A01_B03_MEP_MODEL_RVT_MEP-MODEL_V02.rvt
P2026-A01_B03_ARCH_CD_PDF_A-101_V03.pdf
```

### 7.2 历史 DWG 命名识别规则

真实 DWG 命名可能包含多体系：

| 命名线索 | 示例 | 处理 |
|---|---|---|
| 中文专业前缀 | 建施、结施、智施、电施、消水施、暖通施 | 作为 `filename_evidence`。 |
| 专业缩写 | GS、JS、DS、SS、NS、KS | 作为项目级或历史专业线索，需人工或配置确认。 |
| 图号体系 | A-101、A-T-、S-101 | 作为图号线索，不证明标题栏内容。 |
| 版本号 | V1.0、V3.0、V3.1、V4.0 | 作为版本线索。 |
| 日期 | 2024、2025、YYYYMMDD | 作为批次或版本线索。 |
| 外部顾问编码 | IS22...、TM29... | 保留原样，标记为 external_consultant_code。 |
| 泛名文件 | 底图、图框、通用图、大样图 | 可识别，但不作为完整交付资产合规命名正例。 |

历史命名识别只能说明“文件名/目录线索显示……”，不得等同于图纸内容或专业确认。

### 7.3 RVT 命名标准

当前无法证明已有稳定公司级 RVT 命名标准。大量 `model_数字.rvt` 属于弱命名 / 扫描型命名，需要后续治理，不应作为公司标准样例。

工务署导则中三维模型命名应包含项目名称、单体/子项、专业/专项及楼层、日期、版本、标段等自定义字段。公司推荐 RVT 命名结构应与该要求兼容，并增加平台资产类型字段。

推荐 RVT 命名结构：

```text
项目编号_楼栋或区域_专业_阶段_RVT_模型用途或模型编号_版本.rvt
```

历史 RVT 以识别、归类、标注为主。可从项目名、楼栋/区域、专业、楼层、功能区、版本等线索中辅助判断，但不得认定为完整合规。

### 7.4 外部编码兼容

甲方、顾问、地方或总包已有编码体系时，不强制重命名历史文件。应记录：

- `external_code_system`
- `owner_override`
- `project_override`
- `field_source`
- `confidence`

Hermes 不自行裁决编码冲突。

## 8. 工程目录结构标准

真实 NAS 资料沉淀逻辑更接近：

```text
项目编号-项目名/
  文件收发/
    收/发/IN/
    日期或批次/
    图纸/
      CAD/
      PDF/
      按专业/
    模型/或BIM模型/
  过程文件/
  发布文件/
  提资文件/
```

V0.1-beta 采用两层目录标准：

| 层级 | 定义 | 当前阶段 |
|---|---|---|
| 当前识别层 | 兼容真实 NAS：项目、文件收发、收/发/IN、日期/批次、图纸、CAD、PDF、模型/BIM模型、过程文件、发布文件、提资文件。 | current |
| 推荐治理层 | 逐步收敛到：项目 -> 阶段/批次 -> 专业 -> 资产类型。 | backlog |

不得要求历史资料立即迁移到标准目录。当前只做目录关键词识别和风险提示。

路径边界：

- raw `storage_path` 是内部敏感字段。
- Catalog API 的 `logicalPath` 可作为当前页面展示用的脱敏项目内路径。
- `FileAssetView.logical_path` 本身仍按内部字段处理，不应直接暴露给 Hermes。
- 稳定 View/Gateway contract 字段 `display_path` / `path_hint` 仍为 backlog。
- 如果只有 raw path 而无脱敏路径证据，Hermes 应拒绝展示路径并返回路径证据不可用。

## 9. DWG 图纸资产目录标准

V0.1 不认定公司已有统一 CAD 图层标准、图框标准或标题栏标准已稳定落地。CAD 图层、图框、块属性、外部参照等标准需后续由 BIM/CAD 负责人确认。

当前 DWG 只做目录级治理：

| 字段 | 要求 | 当前阶段 | 证据 |
|---|---|---|---|
| `file_id` | 必须 | current | `catalog_evidence` |
| `project_id` / `project_code` | 必须 | current | `catalog_evidence` |
| `file_name` | 必须 | current | `filename_evidence` |
| `file_ext` / `file_format` | `.dwg` / `.dxf` | current | `metadata_evidence` |
| `discipline` | 字段或线索 | current | `metadata_evidence` / `filename_evidence` / `path_evidence` |
| `stage` | 线索 | backlog/current-lineage | `filename_evidence` / `path_evidence` |
| `drawing_number` | 线索 | backlog/current-lineage | `filename_evidence` |
| `version_no` / `version` | 字段或线索 | current/backlog | `metadata_evidence` / `filename_evidence` |
| `permission_tags` | 必须 | current | `metadata_evidence` |
| `confidentiality_level` | 必须 | current | `metadata_evidence` |

当前不可判断：

- 图层是否合规。
- 图框 / 标题栏是否完整。
- 外部参照是否丢失。
- 块属性是否合规。
- 标注、坐标、图纸内容是否满足审查要求。

这些均需要 `dwg_parse_evidence` 或 `manual_evidence`，当前必须 Missing Evidence。

## 10. RVT / BIM 模型资产目录标准

V0.1 不认定公司已有统一 Revit 建模标准、族命名标准、构件参数标准已稳定落地。RVT 建模、族/构件/参数标准需后续由 BIM/CAD 负责人确认。

当前 RVT/BIM 只做目录级治理：

| 字段 | 要求 | 当前阶段 | 证据 |
|---|---|---|---|
| `model_id` / `file_id` | 必须至少一个 | current | `catalog_evidence` |
| `project_id` / `project_code` | 必须 | current | `catalog_evidence` |
| `model_name` / `file_name` | 必须 | current | `filename_evidence` / `metadata_evidence` |
| `model_format` / `file_ext` | `.rvt`、`.ifc`、`.nwc`、`.nwd` | current | `metadata_evidence` |
| `discipline` | 字段或线索 | current | `metadata_evidence` / `filename_evidence` / `path_evidence` |
| `stage` | 线索 | backlog/current-lineage | `filename_evidence` / `path_evidence` |
| `version_no` / `version` | 字段或线索 | current/backlog | `metadata_evidence` / `filename_evidence` |
| `preview_available` | 占位 / 不稳定 | placeholder/backlog | `preview_evidence` |
| `preview_status` / `preview_mode` / `conversion_status` | API 状态 | current API / backlog View | `preview_evidence` |
| `component_index_status` | 占位 / future | future | `component_evidence` |

当前不可判断：

- Level 是否规范。
- Grid 是否完整。
- Sheet / View 是否符合规则。
- Family / Type 命名是否规范。
- 构件参数是否完整。
- LOD / LOI 是否满足要求。
- 模型是否包含某类构件。

这些需要 `rvt_parse_evidence` / `component_evidence`，当前必须 Missing Evidence。`component_index_status=NOT_REQUIRED` 不得解释为构件索引完成。

## 11. 数字化交付包标准

V0.1-beta 交付包优先服务模型、图纸、PDF 出图：

1. RVT / BIM 模型。
2. PDF 图纸。
3. DWG 图纸。
4. IFC / NWC / NWD 模型交换文件。
5. 图纸目录、说明、大样、设计总说明等 PDF/DWG。
6. Excel / Office 清单作为后续能力保留。

工务署导则将交付内容划分为三维模型、文档和数据，并要求交付系统承载和管理数字化交付信息。公司 V0.1-beta 仍只做目录级治理；工务署附录中的三维模型交付资料清单、文档交付资料清单和工程分解结构，应作为后续 Manifest、交付包模板和项目级标准绑定的主干来源。

当前可做：

- 判断交付清单文件是否存在。
- 判断相关资产是否进入目录。
- 基于文件名/目录线索解释交付批次、收发、发布、过程文件。

当前不可做：

- 结构化 Manifest 完整性判断。
- 正式交付包状态判断。
- 必交/选交判断。
- 文件内容、模型内部、图纸内容是否符合交付要求。

## 12. 交付清单 Manifest 标准

Manifest 当前只做文件名、目录关键词和资产目录中的候选清单识别，阶段为 `current-lineage`。结构化 Manifest 完整性、必交/选交、交付状态和检查状态仍为 backlog。

建议字段：

| 字段 | 当前阶段 | 说明 |
|---|---|---|
| `manifest_id` | backlog | 稳定 Manifest ID。 |
| `delivery_package_id` | backlog | 稳定交付包 ID。 |
| `asset_id` / `file_id` / `model_id` | current/backlog | 可由资产目录关联。 |
| `asset_name` | current/backlog | 文件名或清单字段。 |
| `asset_type` | current/backlog | 基于 file_ext / file_kind / model_format 映射。 |
| `discipline` | current/backlog | 字段或线索。 |
| `stage` | backlog | 当前多依赖目录/文件名。 |
| `version` | current/backlog | `version_no` 或文件名版本。 |
| `required_optional` | backlog | 必交/选交。 |
| `delivery_status` | backlog | 交付状态。 |
| `check_status` | backlog | 检查状态。 |

Manifest 不应包含 raw `storage_path`，不应被解释为文件正文、DWG/RVT 解析或构件证据。

## 13. 工程资产最小元数据标准

| 标准字段 | 当前阶段 | 证据 | 备注 |
|---|---|---|---|
| `file_id` | current | `catalog_evidence` | 文件资产稳定标识。 |
| `model_id` | current | `catalog_evidence` | 模型资产稳定标识。 |
| `project_id` / `project_code` | current | `catalog_evidence` | 项目维度基础字段。 |
| `asset_type` | current/backlog | `metadata_evidence` | 当前由 file_kind / file_ext / model_format 映射。 |
| `file_format` / `file_ext` / `model_format` | current | `metadata_evidence` | 格式白名单基础。 |
| `discipline` | current/current-lineage/backlog | `metadata_evidence` / `filename_evidence` / `path_evidence` | 平台字段为 current；文件名/路径识别为 current-lineage；GENERAL 和空值较多，需治理。 |
| `stage` | backlog/current-lineage | `filename_evidence` / `path_evidence` | 当前无稳定文件级阶段字段，历史目录和文件名只能作为线索。 |
| `version_no` / `version` | current/backlog | `metadata_evidence` / `filename_evidence` | 字段或线索。 |
| `storage_path` | current-sensitive | internal only | 不默认对外暴露，不进入 Hermes memory。 |
| `display_path` / `path_hint` | backlog | `path_evidence` | Catalog API 的脱敏 `logicalPath` 可用于当前展示；稳定 View/Gateway contract 字段待补。 |
| `updated_at` | current | `metadata_evidence` | 目录记录更新时间，不是 NAS mtime。 |
| `last_seen_at` | current | `metadata_evidence` | 扫描/验证线索。 |
| `source_modified_at` / `file_modified_at` | backlog | `metadata_evidence` | 暂无稳定字段。 |
| `checksum` | current nullable | `metadata_evidence` | 空值不代表文件无变化。 |
| `checksum_status` | backlog | `metadata_evidence` | 需稳定校验状态。 |
| `process_status` | current | `metadata_evidence` | 不等于 semantic index status。 |
| `index_eligibility` | current | `metadata_evidence` | 当前为 catalog_only。 |
| `semantic_index_status` | future | `full_text_evidence` | 当前不得承诺。 |
| `component_index_status` | future/placeholder | `component_evidence` | **字段出现不等于构件索引真实可用；`NOT_REQUIRED` 不代表完成。** |
| `preview_available` | placeholder/backlog | `preview_evidence` | `ModelAssetView.preview_available` 可为占位，不代表真实预览能力。 |
| `preview_status` / `preview_mode` / `conversion_status` | current API / backlog View | `preview_evidence` | Preview API / export-precheck 可解释预览状态；稳定 View 字段仍待补，且预览不等于正文证据。 |
| `permission_tags` | current | `metadata_evidence` | 权限感知基础。 |
| `confidentiality_level` | current | `metadata_evidence` | 保密等级基础。 |
| `field_source` / `confidence` | backlog | `metadata_evidence` | 记录字段来源与置信度。 |

## 14. 权限与保密等级标准

原则：

- 真实 `storage_path` 默认不暴露。
- raw `storage_path` 不得进入 Hermes memory、向量库、外部日志或用户可见回答。
- 客户侧和 Agent 默认使用 Gateway/API 脱敏后的 `logicalPath`、`display_path`、`path_hint` 或项目内相对路径；稳定 View 字段仍为 backlog。
- 缺少可验证权限标签、保密等级或项目 scope 时，Hermes 应返回 `permission_denied_or_unproven`，默认 fail-closed。

允许动作：

| 动作 | 当前是否允许 |
|---|---|
| `catalog_query` | 是 |
| `agent_catalog_assist` | 是 |
| `operation_plan_draft` | 是 |
| `file_open_request` | 否，需后续权限流程 |
| 写 DB / 写 NAS / 写 Hermes memory 中的 NAS 内容 | 否 |

## 15. 生命周期与处理状态标准

标准生命周期建议：

| 标准状态 | 含义 | 当前阶段 |
|---|---|---|
| `discovered` | 已发现 | current |
| `cataloged` | 已入目录 | current |
| `verified` | 已校验 | backlog |
| `submitted` | 已提交 | backlog |
| `accepted` | 已接收 | backlog |
| `archived` | 已归档 | backlog |
| `deprecated` | 已废弃 | backlog |
| `deleted` | 已删除或不可见 | current/backlog |

历史枚举映射建议：

| 历史状态 | 标准状态建议 | Agent 说明 |
|---|---|---|
| `active` | `cataloged` | 当前目录可见资产。 |
| `archived` | `archived` | 已归档线索。 |
| `unknown` | Missing Evidence | 不得推断真实生命周期。 |
| `deleted_candidate` | `deleted` / 待复核 | 删除候选或不可见线索。 |
| `stale_unverified` | `discovered` / 待复核 | 长时间未验证线索。 |

`process_status` 是平台治理/处理状态，不得解释为 `semantic_index_status`。

## 16. 标准检查项与证据等级

| 证据等级 | 当前是否可用 | 能判断什么 | 不能判断什么 |
|---|---|---|---|
| `catalog_evidence` | current | 资产是否入目录、ID、类型、项目、基础关联。 | 正文、图纸内容、模型构件。 |
| `filename_evidence` | current-lineage | 文件名中的专业、阶段、版本、图号、顾问编码线索。 | 稳定结构化字段、文件内部事实。 |
| `path_evidence` | current-lineage，必须脱敏 | 项目、收发、批次、专业、CAD/PDF/RVT、过程/发布/模型线索。 | raw path、稳定结构化字段、文件内容。 |
| `metadata_evidence` | current/backlog | 稳定 View/API 字段、权限、状态、checksum 值。 | 未采集字段和内容语义。 |
| `preview_evidence` | current API / backlog View | 预览状态、预览模式、转换状态。 | 正文、图纸内容、模型构件或内容合规判断。 |
| `full_text_evidence` | future | 正文内容。 | BIM 构件级事实。 |
| `dwg_parse_evidence` | future | DWG 图层、图框、标题栏、外参、块属性。 | 未解析时不得判断。 |
| `rvt_parse_evidence` | future | RVT sheets/views/levels/families。 | 构件参数仍需 component evidence。 |
| `component_evidence` | future | BIM 构件、参数、LOD/LOI、构件存在性。 | 未索引时不得回答。 |
| `missing_evidence` | current | 当前证据不足。 | 不是否定事实。 |

## 17. Catalog-only 阶段可执行检查

V0.1-beta current / current-lineage 检查限于：

- 文件格式白名单。
- 资产是否进入目录。
- `file_id` / `model_id` 是否存在。
- 文件名专业/阶段/版本线索识别，仅为 current-lineage。
- 目录层级项目 / 文件收发 / 过程文件 / 发布文件 / 模型线索识别，仅为 current-lineage，且必须使用脱敏路径。
- 权限标签 / 保密等级检查。
- raw `storage_path` 默认不暴露。
- catalog metadata 不作为正文 evidence。

## 18. Missing Evidence 规则

凡涉及以下问题，当前必须返回 Missing Evidence：

- DWG 图层、图框、标题栏、外部参照、块属性、标注、坐标、图纸内容。
- RVT sheets/views/levels/families、Family/Type。
- BIM 构件参数、构件清单、LOD/LOI、构件存在性。
- PDF / Office / 图纸正文内容。
- 工程语义判断。

标准回复原则：

```text
当前只有 catalog metadata / filename evidence / path evidence，缺少对应解析证据，因此不能判断该内容级问题，应标记为 Missing Evidence。
```

## 19. Hermes 查询与回答边界

Hermes 当前是 Catalog Query Agent / 资产目录查询助手，不是 Engineering Semantic Agent、DWG 内容理解 Agent、RVT 构件问答 Agent 或 NAS 全文问答 Agent。

Hermes 只能通过只读 Catalog Tool / Gateway / View Contract 获取资产目录。不得暗示 Hermes 可以直接查底表、生成 SQL、读取真实 NAS 文件正文、写入 DB、写入 NAS。

Hermes memory 允许记录：

- `related_file_ids`
- `query_id`
- `project_id`
- 用户确认 / 反馈标签
- 低敏偏好

`related_file_ids` / `query_id` / `project_id` 只能作为低敏引用和反馈关联，不代表 Hermes 已读取、索引或长期记住 NAS 文件内容。跨项目或跨 tenant 使用前必须重新校验权限和 project scope。用户确认 / 反馈标签必须低敏化，禁止把客户资料、文件正文、raw path、合同条款原文或其他敏感业务材料写入 memory。

Hermes memory 禁止记录：

- raw `storage_path`
- raw catalog row
- DWG/RVT 文件内容
- 文件正文内容
- 客户敏感材料

回答分层：

| 分层 | 规则范围 | 回答方式 |
|---|---|---|
| 可目录级回答 | 资产入目录、格式、ID、稳定项目字段、权限、checksum 值存在性 | 必须说明“基于资产目录/元数据”。 |
| current-lineage 线索回答 | 文件名/脱敏路径中的专业、阶段、版本、图号、收发、过程、发布、Manifest/清单候选线索 | 必须说明“只是目录/文件名/路径线索，不等同稳定字段或合规判断”。 |
| API 状态回答 | `previewStatus` / `previewMode` / `conversionStatus` | 只能解释预览/转换状态，不得解释为正文、图纸内容或模型构件证据。 |
| backlog Missing Evidence | Manifest 完整性、交付包状态、display_path/path_hint 稳定 View 字段、source_modified_at、checksum_status | 说明缺少稳定 View/API 或任务证据。 |
| future Missing Evidence | DWG/RVT/构件/全文/工程语义 | 必须 Missing Evidence。 |

## 20. 当前系统字段映射建议

详见 `03 数据标准/标准到平台字段映射.md`。本章不写死真实底表，只给出稳定 View/API 映射建议。

关键原则：

- `FileAssetView` / `ModelAssetView` 是 Hermes 对接的稳定读模型建议。
- `FileAssetView.project_name` 当前可作为文件资产项目名称来源；`ModelAssetView.project_name` 当前不作为稳定字段，模型侧需通过 `project_id` / `project_code` 回查项目名称。
- Hermes 不直接访问 `data_file_resources` 等业务底表。
- 标准字段到平台字段的映射必须注明 current / backlog / future。
- `GENERAL` / 空专业 / 弱命名文件必须允许 Missing Evidence 或低置信度线索。
- 工务署分类编码和公司扩展编码应进入平台配置或标准字典，而不是散落在 Agent prompt、前端下拉框或临时脚本中。

## 21. 后续扩展路线

V0.1 后续路线：

| 方向 | 阶段 | 说明 |
|---|---|---|
| 字段来源与置信度 | backlog | `field_source` / `confidence`。 |
| display_path / path_hint 稳定 View | backlog | 替代 raw `storage_path` 对外展示。 |
| 结构化 Manifest / 交付包状态 | backlog | DeliveryPackageView / ManifestView。 |
| source_modified_at / checksum_status | backlog | 稳定采集与校验状态。 |
| preview_available / preview_status | current API / backlog View | REST 预览状态可用于解释预览能力；稳定 View 字段仍待补。 |
| CAD 图层 / 图框 / 标题栏标准 | future | 需 BIM/CAD 负责人确认，需 `dwg_parse_evidence`。 |
| Revit 建模 / Family / 构件参数标准 | future | 需 BIM/CAD 负责人确认，需 `rvt_parse_evidence` / `component_evidence`。 |
| NAS semantic collection / 工程语义搜索 | future | 当前禁止作为 V0.1 能力承诺。 |

## 22. 工务署对齐与标准固化路线

详见 `01 数字化交付总则/工务署标准对齐矩阵.md` 和 `integration-contracts/shared_standard_source_contract.md`。

V0.1-beta 后续标准固化顺序：

1. 将公司现有资产分类、阶段、专业、文件类型、命名、Manifest 字段逐项映射到工务署 2024 导则和 2023 分类编码标准。
2. 保持工务署建设阶段和运维阶段分类编码不变，建立公司扩展编码命名空间。
3. 将平台已开发字段回拉到标准字段：能稳定支持的标 current；只能识别线索的标 current-lineage；需要字段和 View/API 的标 backlog；涉及内容解析的标 future。
4. 将标准包同步到平台和 Hermes 文档目录，后续平台 Agent 与 Hermes Agent 均以同一套共享标准为准。
5. 待平台配置、专业字典、Manifest 和项目级标准绑定能力成熟后，再邀请 BIM/CAD 负责人和公司高层评审总文档。

Future 能力上线前，Hermes 不得把目录、文件名、路径或元数据解释为图纸内容、模型内部、全文正文或构件级事实。
