# Architecture Freeze：架构冻结记录

## Status

Draft

## Purpose

Architecture Freeze 表示 Synapse Platform v1 的核心架构边界已经形成基线。冻结后，项目进入：

```text
Architecture -> ADR -> Design -> Coding -> Testing
```

而不是：

```text
Coding -> 发现问题 -> 推翻设计 -> 重写代码
```

## Freeze Scope

冻结范围包括：

- 项目定位；
- Framework / Platform / Business System 边界；
- Platform 服务边界；
- Gateway 入口与信任边界；
- IAM / OAuth2 / OIDC 安全模型；
- RBAC 权限边界；
- 部署模型；
- 网络模型；
- 集成模型；
- 日志、审计、监控与可观测模型；
- V1 一期范围与明确不做事项。

## Freeze Criteria

满足以下条件后，可将状态从 Draft 改为 Frozen：

1. `00-overview` 文档已确认；
2. 关键 ADR 已完成并确认；
3. 服务边界与模块边界已确认；
4. 部署、网络、安全、集成与扩展性主题已完成至少一轮设计；
5. V1 必须完成事项和暂不做事项已明确；
6. README 已指向 v1 架构文档；
7. 旧文档已删除或归档，避免口径冲突。

## Change Rule After Freeze

冻结后，任何影响核心架构的修改必须新增或更新 ADR。

普通实现细节、类命名、接口参数、数据库字段等不在 Architecture Freeze 层面直接讨论，进入后续 Design 阶段处理。
