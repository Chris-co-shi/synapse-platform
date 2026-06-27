# Synapse Task 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Task 提供平台任务调度与任务运行时边界；当前不预设未落地的任务业务模型。

## 2. 子模块边界

- api 保存稳定任务调用契约。
- client 提供调用适配，只依赖 api。
- server 承载启动入口和任务实现并允许依赖 api。
- Task 启动类必须位于 `com.indigo.synapse.task` 根包，禁止放入 `bootstrap` 子包。

## 3. 允许依赖

client/server -> api；server 可按需使用当前 Framework 正式模块。

## 4. 禁止事项

禁止引用已删除的 `synapse-cloud` 或旧 `synapse-mq`；禁止跨层反向依赖、跨服务数据库访问和共享持久化类型。

## 5. 验证命令

```bash
mvn -pl synapse-task-platform -am test
```

## 6. 相关文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [服务边界](../docs/v1/01-architecture/service-boundary.md)
- [安全架构](../docs/v1/01-architecture/security-architecture.md)
- [通信架构](../docs/v1/01-architecture/communication-architecture.md)
