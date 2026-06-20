# Synapse Report 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Report 承载平台报表能力；当前不预设报表模型、查询 DSL 或导出存储方案。

## 2. 子模块边界

- api 保存稳定报表调用契约。
- client 提供调用适配，只依赖 api。
- server 承载启动入口和报表实现并允许依赖 api。
- Report 启动类必须位于 `com.indigo.synapse.report` 根包，禁止放入 `bootstrap` 子包。

## 3. 允许依赖

client/server -> api；server 可按批准方案使用当前 Framework 和官方组件。

## 4. 禁止事项

禁止引用已删除的 Framework 文件模块；禁止 client 直连报表数据库；禁止跨层反向依赖和其他模块 server 依赖。

## 5. 验证命令

```bash
mvn -pl synapse-report-platform -am test
```

## 6. 相关文档

- [模块边界与服务设计](../docs/02-模块边界与服务设计.md)
