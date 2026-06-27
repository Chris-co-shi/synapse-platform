# Synapse Report 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Report 承载未来的平台报表能力。Report 不进入 V1，当前只保留工程边界，禁止预设报表模型、查询 DSL 或导出存储方案。

未经产品范围变更和正式设计，不得主动实现 Report 业务能力或加入 V1 Docker Compose。

## 2. 子模块边界

- api 保存稳定报表调用契约。
- client 提供调用适配，只依赖 api。
- server 承载未来启动入口和实现并允许依赖 api。
- 启动类必须位于 `com.indigo.synapse.report` 根包。

## 3. 禁止事项

禁止引用已删除的 Framework 文件模块；禁止 client 直连报表数据库；禁止跨层反向依赖和其他模块 server 依赖；禁止把规划描述成已实现能力。

## 4. 验证命令

```bash
mvn -pl synapse-report-platform -am test
```

## 5. 相关文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [总体架构](../docs/v1/01-architecture/overall-architecture.md)
