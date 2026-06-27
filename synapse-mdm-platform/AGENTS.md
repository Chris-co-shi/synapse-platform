# Synapse MDM 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

MDM 承载未来的主数据平台能力。MDM 不进入 V1，当前只保留工程边界，禁止虚构主数据实体、编码体系或治理流程。

未经产品范围变更和正式设计，不得主动实现 MDM 业务能力或加入 V1 Docker Compose。

## 2. 子模块边界

- api 保存稳定主数据契约，不暴露 Entity。
- client 提供调用适配，只依赖 api。
- server 承载未来启动入口和实现并允许依赖 api。
- 启动类必须位于 `com.indigo.synapse.mdm` 根包。

## 3. 禁止事项

禁止跨服务共享 Entity、Mapper、Repository；禁止 client 访问数据库；禁止 server 依赖自己的 client 或其他 server；禁止把规划描述成已实现能力。

## 4. 验证命令

```bash
mvn -pl synapse-mdm-platform -am test
```

## 5. 相关文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [总体架构](../docs/v1/01-architecture/overall-architecture.md)
