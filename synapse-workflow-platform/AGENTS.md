# Synapse Workflow 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Workflow 承载平台工作流能力；当前不虚构流程模型、引擎表结构或业务审批语义。

## 2. 子模块边界

- api 保存稳定工作流调用契约。
- client 提供调用适配，只依赖 api。
- server 承载启动入口和工作流实现并允许依赖 api。

## 3. 允许依赖

client/server -> api；server 可按批准方案接入工作流及当前 Framework 技术模块。

## 4. 禁止事项

禁止 api/client 包含引擎持久化实现；禁止引用已删除/更名 artifact；禁止 server 依赖自己的 client 或其他 server。

## 5. 验证命令

```bash
mvn -pl synapse-workflow-platform -am test
```

## 6. 相关文档

- [模块边界与服务设计](../docs/02-模块边界与服务设计.md)
