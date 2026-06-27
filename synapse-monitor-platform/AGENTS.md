# Synapse Monitor 模块规则

本模块同时遵守仓库根目录的 [`AGENTS.md`](../AGENTS.md)。

## 1. 模块定位

Monitor 承载未来的平台监控运行时能力。V1 不建设和部署独立 Monitor 微服务。

V1 的基础运行可见性只包括：

- Spring Boot Actuator；
- 各服务健康检查；
- Docker Compose healthcheck；
- 管理端基础状态展示。

未经产品范围变更和正式设计，不得主动实现 Monitor 服务、加入 V1 Docker Compose，或把 Prometheus、Grafana、APM 和告警中心描述为 V1 已交付能力。

## 2. 子模块边界

- api 保存未来稳定监控契约。
- client 提供未来调用适配，只依赖 api。
- server 承载未来启动入口和实现并允许依赖 api。
- 启动类必须位于 `com.indigo.synapse.monitor` 根包。

## 3. 允许依赖

client/server -> api；未来 server 可使用当前 Framework `synapse-observability` 等正式模块。

## 4. 禁止事项

禁止把平台监控后台写入 Framework；禁止跨层反向依赖、跨服务数据库访问和其他模块 server 依赖；禁止把模块骨架视为 V1 功能。

## 5. 验证命令

```bash
mvn -pl synapse-monitor-platform -am test
```

## 6. 相关文档

- [V1 范围](../docs/v1/00-product/v1-scope.md)
- [总体架构](../docs/v1/01-architecture/overall-architecture.md)
