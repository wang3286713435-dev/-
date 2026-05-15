# Enterprise Agent v1.1 Test Fixtures Report

日期：2026-05-12

范围：仅用于企业 Agent / Hermes v1.1 小型非敏感文件接入测试。

## 1. 文件准备

已准备 4 个非敏感合成文件：

```text
agent-test-readme.txt
agent-test-brief.pdf
agent-test-checklist.docx
agent-test-register.xlsx
```

约束结果：

```yaml
single_file_over_50mb: false
total_over_200mb: false
contains_bim_large_model: false
contains_rvt_dwg_ifc_nwd: false
contains_real_project_name: false
contains_real_file_name: false
contains_real_nas_path: false
```

## 2. DB / REST 准备

专用测试数据标记：

```yaml
project_code: "AGENT_TEST_V11"
asset_source: "AGENT_TEST"
source_type: "AGENT_TEST"
```

已完成：

```yaml
fixture_project_created: true
fixture_file_metadata_created: true
agent_api_key_created: true
agent_api_key_secret_location: "local secure env only"
agent_api_key_secret_printed: false
```

## 3. 治理字段证明

DB 聚合验证通过：

```yaml
fixture_count_matching_all_requirements: 4
lifecycle_status: "active"
confidentiality_level_not_unknown: true
index_eligibility_not_catalog_only: true
storage_locator_present: true
```

REST 聚合验证通过：

```yaml
rest_fixture_count: 4
project_scope_present: true
lifecycle_active: true
confidentiality_not_unknown: true
index_not_catalog_only: true
storage_locator_present: true
```

## 4. 安全边界

```yaml
real_business_rows_output: false
secret_output: false
raw_response_output: false
real_project_name_output: false
real_file_name_output: false
real_nas_path_output: false
nas_scan_triggered: false
bim_large_model_added: false
```

说明：`AGENT_TEST` 语义只用于明确合成的企业 Agent 测试夹具。默认资产仍保持 v1.1 的 fail-closed 策略，未被提升为可索引状态。
