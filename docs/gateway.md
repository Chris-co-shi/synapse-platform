# Synapse Gateway 设计与安全模型

本文描述当前 `synapse-gateway-platform` 已实现的安全、路由和配置事实。部署操作见 [Gateway Docker 部署](../deploy/docker/gateway/README.md)。

## 1. 职责与非职责

Gateway 是平台统一外部入口，负责：

- Spring Cloud Gateway Reactive 路由。
- JWT Access Token 入口认证。
- 明确公开路径白名单，其他路径默认要求认证。
- 清理外部伪造的 `X-Synapse-Gateway-*` Header。
- 为下游请求签发 GatewayProof。
- 原样转发客户端 Bearer Token。
- 对保留的 `/gateway/admin/**` 技术管理命名空间检查 `PERM_gateway:admin`；不执行业务权限判断。

Gateway 不访问数据库，不签发用户 Token，不查询或判断业务资源权限，不承载 IAM 用户/角色/菜单业务，也不传播可直接信任的身份 Header。除保留管理命名空间的技术权限外，接口、资源、组织、租户和数据权限全部由下游服务处理。

## 2. 请求安全链路

```mermaid
flowchart TD
    A["客户端请求"] --> B["Reactive Resource Server 提取 Bearer Token"]
    B --> C["验证签名、时间、issuer、audience、token type 和 required claims"]
    C -->|"失败"| D["Gateway 返回 401"]
    C -->|"成功或公开路径"| E["清理外部 X-Synapse-Gateway-* Header"]
    E --> F["Route 匹配与 StripPrefix"]
    F --> G["按最终路径和 Bearer Token hash 签发 GatewayProof"]
    G --> H["原样携带 Bearer Token 转发下游"]
    H --> I["下游再次验证 JWT 与 GatewayProof"]
    I --> J["下游执行接口、资源和数据权限"]
    J -->|"权限不足"| K["下游返回 403"]
```

JWT 证明调用主体身份，GatewayProof 证明请求经过可信 Gateway。GatewayProof 不能证明用户具备业务权限，两者均不能替代下游授权。

## 3. JWT 验证

Gateway 复用 Framework `synapse-oauth2-resource-server-webflux`，由其完成 Bearer Token 提取、Reactive decoder、签名/时间/issuer/audience/claim 校验、Authentication 建立和统一 401 响应。Gateway 只有一个 `SecurityWebFilterChain`。公开路径 `permitAll`，保留的 `/gateway/admin/**` 要求 `PERM_gateway:admin`，其余路径默认 `authenticated`。该 authority 只保护 Gateway 技术管理命名空间，不表示 Gateway 承担 IAM 或下游业务授权。

关键配置：

```yaml
synapse.security.resource-server:
  enabled: true
  issuer-uri: ${IAM_ISSUER_URI:http://127.0.0.1:8100}
  jwk-set-uri: ${IAM_JWK_SET_URI:http://127.0.0.1:8100/oauth2/jwks}
  issuer-validation-enabled: true
  audience-validation-enabled: true
  audiences: [${SYNAPSE_GATEWAY_AUDIENCE:synapse-platform}]
  denylist-enabled: false
```

`issuer-uri` 限定 Token 签发方，`jwk-set-uri` 提供验签公钥，`audiences` 防止发给其他服务的 Token 被 Gateway 接受。默认必填 claim 包括 `sub`、`exp`、`iat`、`token_type` 和 `principal_type`。

当前没有真实 `TokenDenylistPort`，因此显式关闭 denylist。当前实现不支持实时撤销已经签发且未过期的 Token，不得将其描述为已具备实时撤销能力。

Token 格式、签名、有效期、`nbf`、issuer、audience、token type 或 required claim 任一不合法时，Gateway 返回 401。合法 Token 即使没有 permissions、roles 或 scope claim，也允许进入普通业务路由转发流程。访问保留的 `/gateway/admin/**` 而缺少 `gateway:admin` 权限时返回 403；业务权限不足仍由下游返回 403。

### 3.1 公开路径

仅以下路径无需用户 Access Token：

```text
/actuator/health
/actuator/health/**
/actuator/info
/error
/iam/.well-known/**
/iam/oauth2/token
/iam/oauth2/jwks
```

未公开整个 `/iam/**` 或 `/oauth2/**`。白名单匹配 Gateway 对外路径；`StripPrefix=1` 发生在路由转发阶段，不改变安全链看到的外部路径。

当前 IAM Server 尚未实现 OAuth2 Token、JWK 和 discovery 生产端点，上述 IAM 路径是为后续 IAM 能力预留的明确白名单，不代表端点已经可用。当前测试只验证 Gateway 不会错误返回 401；下游不存在时仍可能返回 503。

## 4. GatewayProof 协议

GatewayProof v1 使用 Framework `synapse-security` 的 canonicalization、HMAC-SHA256、token hash、nonce 和 Header 常量，Platform 不自行实现密码算法。

五个 Header：

```text
X-Synapse-Gateway-Proof-Version
X-Synapse-Gateway-Id
X-Synapse-Gateway-Timestamp
X-Synapse-Gateway-Nonce
X-Synapse-Gateway-Signature
```

v1 canonical request 固定为八行：

```text
version
gatewayId
timestamp
nonce
HTTP_METHOD
normalized_path
normalized_query
bearer_token_sha256
```

- 时间戳是 UTC epoch milliseconds。
- nonce 由 Framework 密码学安全随机生成器产生。
- Bearer Token 只以 SHA-256 小写十六进制指纹参与签名；原始 Token 不进入 canonical request。
- Framework v1 没有独立 audience 行。Platform 使用可信 Route ID 作为 audience，并将签名中的 `gatewayId` 编码为 `<baseGatewayId>:<routeId>`。例如 Resource 路由签发值为 `synapse-gateway:synapse-resource-server`。
- query 由 Framework 解析、按 name/value 排序并使用 RFC 3986 UTF-8 percent encoding。
- 不记录 Token、secret、canonical string 或 token 指纹。

### 4.1 Header 清理

任何外部 `X-Synapse-Gateway-*` Header 都不可信。过滤器通过 Framework 的大小写不敏感判断删除全部同前缀 Header，包括未来尚未定义的字段。即使 `GATEWAY_PROOF_ENABLED=false` 也继续清理，防止开发配置让客户端伪造下游信任边界。

清理与签名由两个独立 GlobalFilter 承担：

- `GatewayProofHeaderSanitizationGlobalFilter` 固定 order `Ordered.HIGHEST_PRECEDENCE`，始终存在。
- `GatewayProofSigningGlobalFilter` 固定 order `10001`，关闭证明时直接继续链路。

### 4.2 最终路径绑定

过滤器顺序位于路由级 `StripPrefix` 和 `RouteToRequestUrlFilter` 之后、Netty 网络转发之前。因此签名绑定最终下游路径，而不是外部路径：

```text
外部请求：GET /iam/test?b=2&a=1
StripPrefix 后：GET /test?b=2&a=1
签名 path：/test
规范化 query：a=1&b=2
```

如果错误地使用 `/iam/test` 验签，Framework verifier 会返回签名无效。顺序由集成测试保护。

当前 Spring Cloud Gateway 4.3.4 中，`StripPrefix` 默认 order 为 `0`，`RouteToRequestUrlFilter` 为
`10000`，`NettyRoutingFilter` 为 `Ordered.LOWEST_PRECEDENCE`。签名 order `10001` 因此位于路径改写和
请求 URL 建立之后、网络转发之前。该顺序属于安全协议，升级 Gateway 版本时必须复核并运行最终路径集成测试。

### 4.3 环境策略

- `dev`、`beta`、`prd` 均默认开启证明。
- 所有环境启用时都必须安全注入 secret；不存在开发环境静默无证明转发。
- 开启时 gateway-id 不能为空，secret 必须至少 32 UTF-8 字节；否则启动失败。
- Gateway 是证明签发方，不启用 `synapse.security.gateway-proof.enabled` 入站验证。

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `synapse.gateway.proof.enabled` | `true` | 是否签发出站 GatewayProof；不影响 Header 清理 |
| `synapse.gateway.proof.gateway-id` | `synapse-gateway` | 基础 Gateway ID；实际签名值追加可信 Route ID audience |
| `synapse.gateway.proof.secret` | 空 | HMAC secret；启用时必须满足 Framework 至少 32 UTF-8 字节规则 |

secret 只能通过环境变量或 Secret 管理系统注入，不得写入仓库、镜像、日志或异常。初始化、nonce、
canonical request 或签名任一失败时，Gateway 以 500 失败关闭，不允许无证明转发；日志只记录错误类型、
routeId、method 和 path，不记录 Token、token hash、secret、canonical string、signature 或完整 nonce。

### 4.4 当前限制

- GatewayProof v1 使用单个共享 HMAC secret，当前没有密钥轮换或多版本签发能力。
- 首个 Resource Server 已实现 Redis replay store；其他下游服务仍需各自接入同一验证与共享 Redis 语义。
- Route ID 是 Platform v1 的 audience 标识，修改 Route ID 会改变下游信任配置，必须作为安全协议变更处理。
- GatewayProof 不解决服务间任意调用签名，也不替代 mTLS、JWT 或业务授权。

## 5. 静态路由

| 外部前缀 | 下游服务 | 转发处理 |
| --- | --- | --- |
| `/iam/**` | `synapse-iam-server` | `StripPrefix=1` |
| `/resource/**` | `synapse-resource-server` | `StripPrefix=1` |
| `/config/**` | `synapse-config-server` | `StripPrefix=1` |
| `/audit/**` | `synapse-audit-server` | `StripPrefix=1` |
| `/file/**` | `synapse-file-server` | `StripPrefix=1` |
| `/message/**` | `synapse-message-server` | `StripPrefix=1` |
| `/task/**` | `synapse-task-server` | `StripPrefix=1` |
| `/workflow/**` | `synapse-workflow-server` | `StripPrefix=1` |
| `/integration/**` | `synapse-integration-server` | `StripPrefix=1` |
| `/mdm/**` | `synapse-mdm-server` | `StripPrefix=1` |
| `/report/**` | `synapse-report-server` | `StripPrefix=1` |
| `/monitor/**` | `synapse-monitor-server` | `StripPrefix=1` |

服务实例由 Nacos Discovery 和 Spring Cloud LoadBalancer 解析。

## 6. 下游认证与授权

Gateway 保留原始 `Authorization: Bearer ...` Header，不写入或信任 `X-User-Id`、`X-Username`、`X-Roles`、`X-Permissions` 等身份权限 Header。

首个 `synapse-resource-server` 已完成以下链路；其他下游服务必须按同一规则接入：

1. 独立验证 JWT 签名、时间、issuer、audience 和 claim contract。
2. 验证 GatewayProof，确认请求经过可信 Gateway 且签名材料未被篡改。
3. 在 Controller、方法或业务层执行接口和资源权限。
4. 执行组织、租户、数据范围及资源归属判断。
5. 在业务权限不足时由下游返回 403。

Gateway 已认证不能成为下游跳过 JWT 验证或业务授权的理由。

## 7. 环境变量

| 变量 | 用途 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev`、`beta` 或 `prd` |
| `SERVER_PORT` | Gateway 监听端口，默认 `8080` |
| `NACOS_SERVER_ADDR` | Nacos 地址 |
| `NACOS_NAMESPACE` / `NACOS_GROUP` | 环境隔离 |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | Nacos 认证 |
| `IAM_ISSUER_URI` | JWT 预期 issuer |
| `IAM_JWK_SET_URI` | IAM JWK Set 地址 |
| `SYNAPSE_GATEWAY_AUDIENCE` | Gateway 接受的 audience |
| `GATEWAY_PROOF_ENABLED` | 是否签发出站证明 |
| `GATEWAY_ID` | 基础 Gateway 标识；不得包含 `:` |
| `GATEWAY_PROOF_SECRET` | HMAC secret，开启证明时必填 |

## 8. 本地构建与验证

构建前应确保当前 Framework 构件可由本地 Maven 仓库或已配置的远程仓库解析。全仓基线验证使用：

```bash
mvn validate
mvn clean test
```

Gateway 与首个 Resource 验证：

```bash
mvn -pl synapse-gateway-platform -am clean verify
mvn -pl synapse-resource-platform/synapse-resource-server -am clean verify
mvn clean verify
mvn -pl synapse-gateway-platform dependency:tree
git diff --check
```

CI 的 Resource Job 设置 `-Dsynapse.test.redis.required=true`，Docker 不可用时测试失败而不是跳过；本地无 Docker 时真实 Redis Testcontainers 测试会明确跳过。

开发启动需要可访问 IAM JWK；如果只运行自动化测试，不访问真实 IAM/Nacos。

```bash
SPRING_PROFILES_ACTIVE=dev \
IAM_ISSUER_URI=http://127.0.0.1:8100 \
IAM_JWK_SET_URI=http://127.0.0.1:8100/oauth2/jwks \
GATEWAY_PROOF_SECRET='<at-least-32-byte-secret>' \
mvn -f synapse-gateway-platform/pom.xml spring-boot:run
```

## 9. curl 示例

```bash
curl -i http://127.0.0.1:8080/actuator/health/readiness
curl -i http://127.0.0.1:8080/resource/example
curl -i -H 'Authorization: Bearer <access-token>' \
  http://127.0.0.1:8080/resource/example
```

第二个请求应返回 401。第三个请求只有在 Token、IAM JWK、Nacos 和下游服务均有效时才能成功转发。不要把真实 Token 写入脚本或提交到仓库。

## 10. 常见故障

- 启动提示 issuer/audience 配置非法：检查 `IAM_ISSUER_URI` 和 `SYNAPSE_GATEWAY_AUDIENCE`。
- JWT 始终返回 401：检查 JWK 可达性、Token 签名、issuer、audience、有效期和必填 claim。
- 任一环境启动失败且提示 GatewayProof 配置非法：提供至少 32 字节 secret，确认 `GATEWAY_ID` 非空且不包含 `:`。
- 下游 GatewayProof 验签失败：确认两端 secret 和 audience-scoped gateway-id 一致、服务器时间同步，并确认下游使用 StripPrefix 后路径。
- 路由返回 503：检查 Nacos 注册、namespace/group 和目标服务名。
- macOS 出现 Netty DNS native 警告：公共依赖已移除 Mac 专用 native artifact；警告不应通过污染 Linux 镜像解决。

## 11. Kubernetes 后续边界

当前没有 Kubernetes YAML 或 Helm。Gateway 已保持无状态、环境变量配置、stdout/stderr 日志、readiness/liveness 端点和 SIGTERM 优雅停机，后续可映射 Deployment、Service、ConfigMap、Secret 及 probes。迁移任务应独立设计，不在本任务伪称已实现。

## 12. Docker 运行与部署边界

Gateway 提供 Java 21 JRE runtime-only 镜像和 Docker Compose v2 单实例部署，详细操作见
[Gateway Docker 部署](../deploy/docker/gateway/README.md)。容器使用固定 UID/GID 的非 root 用户，配置完全由
环境注入，日志写入 stdout/stderr，不保存业务数据、源码或构建凭据。

- liveness：`/actuator/health/liveness`。
- readiness：`/actuator/health/readiness`，同时作为 Compose healthcheck。
- 优雅停机：Spring shutdown phase 30 秒，Compose stop grace period 40 秒。
- GatewayProof secret 只能由服务器受限环境文件或 Secret 系统注入，不进入镜像、label 或日志。
- Compose 当前只有单 Gateway 实例，镜像替换可能短暂中断；真正无中断需要多实例负载均衡或 Kubernetes Deployment。

容器模型已保持与未来 Deployment、Service、ConfigMap、Secret、readinessProbe、livenessProbe 和
terminationGracePeriodSeconds 的自然映射，但当前任务不创建任何 Kubernetes 或 Helm 文件。
