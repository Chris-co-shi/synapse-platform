# Nacos 配置基线

本目录保存可审计、无密钥的 Nacos Data ID 基线。应用仍以各模块 `application.yml` 为本地默认配置；发布到 Nacos 的内容必须与本目录保持一致，不得维护另一套正式端口。

## Data ID

| Data ID | 正式端口 |
| --- | ---: |
| `synapse-gateway-platform.yml` | 8080 |
| `synapse-iam-server.yml` | 8100 |
| `synapse-message-server.yml` | 8200 |
| `synapse-file-server.yml` | 8300 |
| `synapse-task-server.yml` | 8400 |

应用通过以下规则导入配置：

```yaml
spring:
  config:
    import: optional:nacos:${spring.application.name}.${spring.cloud.nacos.config.file-extension}
```

因此发布时 Data ID 必须与上表完全一致，文件类型为 YAML。namespace、group 按环境选择，例如开发环境使用 `dev` / `SYNAPSE_PLATFORM_DEV`；beta、prod 使用各自隔离的 namespace/group，但 Data ID 与端口口径不变。

## 发布约束

1. 将 `deploy/nacos/config/` 下对应文件发布到目标 namespace/group。
2. 不得在 Nacos 中保留 `20000`、`20001` 等旧端口覆盖项。
3. Gateway 路由继续使用 `lb://<service-name>`，由 Nacos Discovery 解析实例；不要在 Nacos 中改回固定主机端口 URI。
4. `SERVER_PORT`、`IAM_ISSUER_URI`、`IAM_JWK_SET_URI` 可由部署环境覆盖，但默认值必须保持本目录口径。
5. 密钥、密码和 GatewayProof secret 不进入本目录，必须通过环境变量或 Secret 管理系统注入。
6. 远端 Nacos 实例状态不属于 Git 仓库；发布后应导出或通过配置平台审计确认 Data ID 内容与本目录一致。
