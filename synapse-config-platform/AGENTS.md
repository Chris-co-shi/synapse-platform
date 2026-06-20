# Synapse Config 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Config 是平台配置服务边界；Framework `synapse-config` 仅为技术抽象，两者不得混淆。

## 2. 子模块边界

- api 保存稳定配置服务契约。
- client 提供配置服务调用适配，只依赖 api。
- server 承载可启动配置平台能力并允许依赖 api。

## 3. 允许依赖

client/server -> api；server 按需使用当前 Framework 技术模块和官方组件。

## 4. 禁止事项

禁止 api/client 包含数据库实现，禁止 client/server 反向依赖，禁止把配置中心业务放入 Framework。

## 5. 验证命令

```bash
mvn -pl synapse-config-platform -am test
```

## 6. 相关文档

- [模块边界与服务设计](../docs/02-模块边界与服务设计.md)
