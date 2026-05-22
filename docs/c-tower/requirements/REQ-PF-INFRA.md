# REQ-PF-INFRA: 基础设施配置需求

## 源文件
- SRC-02: 平台清单.xlsx — Sheet "5基础设施配置要求"

## 需求矩阵

| requirement_id | source_file | source_section | requirement_text | platform_domain | module | priority | delivery_phase | acceptance_method | current_platform_fit | gap | risk | owner_role | needs_owner_confirmation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-PF-INFRA-001 | SRC-02 | 5.1-国产化集群 | CPU架构：核心服务器采用国产信创芯片，单节点≥32Core/2.6GHz | 基础设施 | 信创 | P0 | P1 | 硬件验收 | 需新增 | 当前可能非国产硬件 | 国产CPU生态兼容性 | 基础设施负责人 | 否 |
| REQ-PF-INFRA-002 | SRC-02 | 5.1-高可用 | 关键业务（IoT、数据库）Active-Standby或Cluster，故障秒级切换 | 基础设施 | 高可用 | P0 | P1-P4 | 故障演练 | 需新增 | 无高可用架构 | 切换时间和数据一致性 | 架构负责人 | 否 |
| REQ-PF-INFRA-003 | SRC-02 | 5.1-存储 | 全闪存NVMe SSD热数据+大容量HDD温冷，初始≥40TB，预留扩容 | 基础设施 | 存储 | P0 | P1 | 硬件验收 | 需新增 | 见 REQ-PF-HW-008 | 同 REQ-PF-HW-008 | 基础设施负责人 | 否 |
| REQ-PF-INFRA-004 | SRC-02 | 5.2-核心应用服务器 | CPU≥64核/2.6GHz，≥512GB DDR4 ECC，2x480G SSD+4x1.92T NVMe，4x10GE光口 | 基础设施 | 服务器 | P0 | P1 | 硬件验收 | 需新增 | 见 REQ-PF-HW-001 | 同 REQ-PF-HW-001 | 基础设施负责人 | 否 |
| REQ-PF-INFRA-005 | SRC-02 | 5.3-核心数据库服务器 | CPU≥64核/2.6GHz，≥1TB DDR4 ECC，双口16Gb FC HBA，8x3.84T NVMe RAID10 | 基础设施 | 服务器 | P0 | P1 | 硬件验收 | 需新增 | 见 REQ-PF-HW-002 | 同 REQ-PF-HW-002 | 基础设施负责人 | 否 |
| REQ-PF-INFRA-006 | SRC-02 | 5.4-存储节点 | CPU≥16核/2.1GHz，≥128GB DDR4，≥10块4TB SATA+SSD缓存，≥4x10GE | 基础设施 | 存储 | P0 | P1 | 硬件验收 | 需新增 | 见 REQ-PF-HW-003 | 同 REQ-PF-HW-003 | 基础设施负责人 | 否 |
| REQ-PF-INFRA-007 | SRC-02 | 5.5-交换机 | 存储/业务交换机4台+管理交换机2台，10GE接入 | 基础设施 | 网络 | P0 | P1 | 硬件验收 | 需新增 | 见 REQ-PF-HW-004/005 | 同 REQ-PF-HW-004 | 基础设施负责人 | 否 |
| REQ-PF-INFRA-008 | SRC-02 | 5.6-边缘网关 | ≥4核CPU/1.6GHz，≥16GB，≥128G SSD，原生多协议，断网自治 | 基础设施 | 边缘 | P1 | P1 | 功能测试 | 需新增 | 见 REQ-PF-HW-006 | 同 REQ-PF-HW-006 | IoT负责人 | 否 |
| REQ-PF-INFRA-009 | SRC-02 | 5.6-部署模式 | 支持业主指定环境部署，兼容私有化/混合云/业主云 | 基础设施 | 部署 | P0 | P1 | 部署验证 | 需新增 | 当前仅开发环境 | 业主环境不确定 | 基础设施负责人 | 是 |
| REQ-PF-INFRA-010 | SRC-02 | 5.6-环境规划 | 至少规划：开发、集成测试、性能压测、演示、生产、灾备环境 | 基础设施 | 部署 | P0 | P1 | 环境审查 | 需新增 | 仅开发环境 | 多环境维护成本 | 基础设施负责人 | 否 |
