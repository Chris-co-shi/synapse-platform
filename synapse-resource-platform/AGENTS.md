# Synapse Resource 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Resource 提供平台资源目录及资源相关能力；当前不得虚构未落地的业务模型。

## 2. 子模块边界

- api 只保存稳定契约。
- client 只提供调用适配并允许依赖对应 api。
- server 承载启动入口和后续模块实现并允许依赖对应 api。

## 3. 允许依赖

client -> api，server -> api，模块 -> 当前 Framework 正式技术模块。

## 4. 禁止事项

禁止 api -> client/server、client -> server、server -> 自己的 client/其他 server；禁止跨服务共享持久化类型或访问数据库。

## 5. 验证命令

```bash
mvn -pl synapse-resource-platform -am test
```

## 6. 相关文档

- [模块边界与服务设计](../docs/02-模块边界与服务设计.md)
