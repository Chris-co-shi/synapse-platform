# Synapse Integration 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Integration 提供未来的平台外部系统集成边界。Integration 不进入 V1，当前只保留工程边界，禁止预设具体厂商协议或业务适配模型。

未经产品范围变更和正式设计，不得主动实现 Integration 业务能力或加入 V1 Docker Compose。

## 2. 子模块边界

- api 保存稳定集成契约。
- client 提供调用适配，只依赖 api。
- server 承载未来启动入口和实现并允许依赖 api。
- 启动类必须位于 `com.indigo.synapse.integration` 根包。

## 3. 禁止事项

禁止在 api 暴露厂商实现细节；禁止引用已删除或更名 artifact；禁止跨层反向依赖及依赖其他模块 server；禁止把规划描述成已实现能力。

## 4. 验证命令

```bash
mvn -pl synapse-integration-platform -am test
```

## 5. 相关文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [总体架构](../docs/v1/01-architecture/overall-architecture.md)
