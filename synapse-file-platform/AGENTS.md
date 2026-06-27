# Synapse File 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

File 承载平台文件服务。当前 Framework 已删除原 `synapse-file`，文件业务与实现归 Platform。

## 2. 子模块边界

- api 保存稳定文件服务契约，不暴露存储实现。
- client 提供调用适配，只依赖 api。
- server 承载启动入口与文件平台实现并允许依赖 api。
- File 启动类必须位于 `com.indigo.synapse.file` 根包，禁止放入 `bootstrap` 子包。

## 3. 允许依赖

client/server -> api；server 可直接选择经批准的官方存储组件。

## 4. 禁止事项

禁止重新引用或模拟已删除的 Framework `synapse-file`；禁止 client 直接访问服务存储或数据库；禁止跨层反向依赖。

## 5. 验证命令

```bash
mvn -pl synapse-file-platform -am test
```

## 6. 相关文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [服务边界](../docs/v1/01-architecture/service-boundary.md)
- [安全架构](../docs/v1/01-architecture/security-architecture.md)
- [通信架构](../docs/v1/01-architecture/communication-architecture.md)
