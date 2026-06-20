# Synapse MDM 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

MDM 承载主数据平台能力；当前不虚构主数据实体、编码或治理流程。

## 2. 子模块边界

- api 保存稳定主数据调用契约，不暴露 Entity。
- client 提供调用适配，只依赖 api。
- server 承载启动入口和 MDM 实现并允许依赖 api。

## 3. 允许依赖

client/server -> api；server 可按需使用当前 Framework 正式模块。

## 4. 禁止事项

禁止跨服务共享 Entity/Mapper/Repository，禁止 client 访问数据库，禁止 server 依赖自己的 client 或其他 server。

## 5. 验证命令

```bash
mvn -pl synapse-mdm-platform -am test
```

## 6. 相关文档

- [模块边界与服务设计](../docs/02-模块边界与服务设计.md)
