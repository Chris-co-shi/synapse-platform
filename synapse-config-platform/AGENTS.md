# Synapse Config 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Config 是 V1 P1 的平台级业务配置服务。Framework `synapse-config` 仅提供技术抽象，Nacos 提供技术配置和服务发现，三者不得混淆。

V1 最小闭环只确认：

- 国际化资源；
- 字典类型；
- 字典项。

## 2. 子模块边界

- `synapse-config-api`：保存国际化和字典稳定契约。
- `synapse-config-client`：提供查询和调用适配，只依赖 api。
- `synapse-config-server`：承载业务、持久化、缓存失效和启动入口。
- Config 启动类必须位于 `com.indigo.synapse.config` 根包，禁止放入 `bootstrap` 子包。

## 3. 强制边界

Config 可以管理平台级国际化和字典，但不得管理：

- 数据源、Redis、Nacos 或 RocketMQ 连接参数；
- GatewayProof Secret；
- RSA 私钥；
- 容器和部署环境变量；
- MES、WMS 等业务系统的私有领域配置；
- 未经产品范围确认的通用动态配置平台能力。

## 4. 安全与缓存

- Config 作为受保护服务，独立验证 Opaque Access Token 的 Redis 授权快照。
- 修改国际化和字典需要明确 permission。
- 查询可以使用 Redis 或本地缓存，但数据库仍是 Config 数据事实来源。
- 变更后必须执行缓存失效，禁止长期返回旧配置。
- Redis 不可用时允许读取仍在有效期内的缓存；写操作失败关闭。
- 禁止信任 Gateway 注入的身份和权限 Header。

## 5. 可靠审计

国际化资源、字典类型和字典项的新增、修改、启用、禁用与删除必须写入本地 Outbox，通过 RocketMQ 可靠发送给 Audit。

## 6. 允许依赖

client/server -> api；server 可按需使用当前 Framework 正式技术模块和官方组件。

## 7. 禁止事项

- 禁止 api/client 包含数据库实现。
- 禁止 client 依赖 server。
- 禁止 server 依赖自己的 client 或其他平台 server。
- 禁止跨服务数据库访问。
- 禁止把 Config 业务反向写入 Framework。
- 禁止把 Nacos 技术配置复制为 Config 业务数据。
- 禁止自行扩张 V1 产品范围。

## 8. 验证命令

```bash
mvn -pl synapse-config-platform -am test
```

测试至少覆盖：

- 国际化资源查询与修改；
- 字典类型和字典项生命周期；
- 权限校验；
- 缓存失效；
- Redis 故障读写边界；
- Outbox 原子性和审计事件。

## 9. 相关文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [总体架构](../docs/v1/01-architecture/overall-architecture.md)
- [服务边界](../docs/v1/01-architecture/service-boundary.md)
- [安全架构](../docs/v1/01-architecture/security-architecture.md)
- [通信架构](../docs/v1/01-architecture/communication-architecture.md)
