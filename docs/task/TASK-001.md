# TASK-001 Synapse Platform 初始工程结构

## 状态

已被 TASK-002 扩展和迁移。

TASK-001 曾用于建立初始 Platform Maven 骨架。当前仓库实际结构以 [TASK-002](TASK-002.md) 为准。

## 后续迁移

TASK-002 已完成以下调整：

- 保留 `synapse-gateway-platform` 作为 Gateway 可启动服务。
- 将 IAM / Message / File / Task 子模块迁移为 `synapse-xxx-api`、`synapse-xxx-client`、`synapse-xxx-server`。
- 新增 Resource / Config / Audit / Workflow / Integration / MDM / Report / Monitor 平台模块。
- 根工程继续直接 import `com.indigo.synapse:synapse-bom`。
- 不创建 `synapse-platform-bom`。
- 不创建 `synapse-platform-common`。
- 不创建统一 `synapse-platform-api`。

## 当前判断

查看当前模块结构、依赖边界、配置规则和验证结果时，请以 `docs/task/TASK-002.md`、根 `README.md` 和根 `AGENTS.md` 为准。
