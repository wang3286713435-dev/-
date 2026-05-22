# REQ-PF-HW: 硬件配置需求

## 源文件
- SRC-02: 平台清单.xlsx — Sheet "4.2硬件清单表"
- SRC-02: 平台清单.xlsx — Sheet "5基础设施配置要求"

## 需求矩阵

| requirement_id | source_file | source_section | requirement_text | platform_domain | module | priority | delivery_phase | acceptance_method | current_platform_fit | gap | risk | owner_role | needs_owner_confirmation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-PF-HW-001 | SRC-02 | 4.2-行1 | 核心应用服务器 x3：国产CPU≥64核/2.6GHz，≥512GB DDR4 ECC，NVMe SSD缓存 | 基础设施 | 服务器 | P0 | P1 | 硬件验收 | 需新增 | 需采购国产架构服务器 | 国产服务器供货周期 | 基础设施负责人 | 否 |
| REQ-PF-HW-002 | SRC-02 | 4.2-行2 | 核心数据库服务器 x1：国产CPU≥64核/2.6GHz，≥1TB DDR4 ECC，NVMe SSD RAID10 | 基础设施 | 服务器 | P0 | P1 | 硬件验收 | 需新增 | 需采购高配国产数据库服务器 | 大内存服务器成本高 | 基础设施负责人 | 否 |
| REQ-PF-HW-003 | SRC-02 | 4.2-行3 | 存储节点 x3：国产CPU≥16核/2.1GHz，≥128GB DDR4，≥10块4TB SATA+SSD缓存 | 基础设施 | 存储 | P0 | P1 | 硬件验收 | 需新增 | 需采购分布式存储 | 存储容量和扩展规划 | 基础设施负责人 | 否 |
| REQ-PF-HW-004 | SRC-02 | 4.2-行4 | 存储/业务交换机 x4：≥2.56Tbps，24x10GE+6x40GE | 基础设施 | 网络 | P0 | P1 | 硬件验收 | 需新增 | 需采购万兆交换机 | 网络拓扑规划 | 基础设施负责人 | 否 |
| REQ-PF-HW-005 | SRC-02 | 4.2-行5 | 管理交换机 x2：≥670Gbps，24xGE+4x10GE SFP+ | 基础设施 | 网络 | P0 | P1 | 硬件验收 | 需新增 | 需采购管理交换机 | 带内/带外管理规划 | 基础设施负责人 | 否 |
| REQ-PF-HW-006 | SRC-02 | 4.2-行6 | 边缘计算网关 x3：断网自治、数据清洗压缩≥40%、原生多协议 | 基础设施 | 边缘计算 | P1 | P1 | 功能测试+性能测试 | 需新增 | 需采购和部署边缘网关 | 网关协议适配和规则管理 | IoT+基础设施 | 否 |
| REQ-PF-HW-007 | SRC-02 | 4.2-行7 | 网络安全设备 x1套：漏洞扫描+日志审计+主机安全+WAF，满足等保二级 | 基础设施 | 安全 | P0 | P1 | 等保测评 | 需新增 | 需采购安全设备套件 | 安全设备选型和部署 | 安全负责人 | 否 |
| REQ-PF-HW-008 | SRC-02 | 5.基础设施 | 分级存储：NVMe SSD热数据(3个月)+HDD温冷，初始可用≥40TB | 基础设施 | 存储 | P0 | P1 | 硬件验收 | 需新增 | 需规划分级存储策略 | 存储容量和生命周期管理 | 基础设施负责人 | 否 |
