# Resource P0 安全验证

本文记录 `synapse-resource-platform/synapse-resource-server` 作为首个下游 Resource Server 的实际安全闭环。当前只包含技术验收能力，不包含资源目录、菜单、角色、组织或客户端管理业务。

## 1. 请求链路

```text
Client
  -> Gateway 独立验证 Bearer JWT
  -> Gateway 清理外部 X-Synapse-Gateway-* Header
  -> Gateway 保留原始 Authorization
  -> Gateway 对最终 method/path/query、timestamp、nonce、Route audience 和 Token fingerprint 签名
  -> Resource GatewayProofVerificationFilter 验证来源证明
  -> Resource JwtDecoder 独立验证签名、exp、issuer、audience 和 claim contract
  -> Framework 建立 CurrentPrincipalContext
  -> Framework 建立 OperationContext，initiator 默认等于 actor
```

GatewayProof 不创建 Principal，也不能替代 JWT。直接绕过 Gateway 访问非公开接口时，即使 JWT 有效，只要缺少有效 GatewayProof 就返回 403。

## 2. Audience 绑定

Framework GatewayProof v1 canonical string 没有独立 audience 字段，但 `gatewayId` 受 HMAC 签名保护。Platform 使用可信 Spring Cloud Gateway Route ID 作为 audience：

```text
基础 Gateway ID: synapse-gateway
Resource Route ID: synapse-resource-server
实际签名 Gateway ID: synapse-gateway:synapse-resource-server
```

Resource 只信任完整值 `synapse-gateway:synapse-resource-server`。客户端不能决定 Route ID；修改 Route ID 必须同步下游信任配置并按安全协议变更处理。

## 3. JWT 配置

```yaml
synapse:
  security:
    resource-server:
      enabled: true
      issuer-uri: ${IAM_ISSUER_URI:http://127.0.0.1:8100}
      jwk-set-uri: ${IAM_JWK_SET_URI:http://127.0.0.1:8100/oauth2/jwks}
      audiences:
        - ${RESOURCE_SERVER_AUDIENCE:synapse-resource-server}
      issuer-validation-enabled: true
      audience-validation-enabled: true
      denylist-enabled: false
```

Resource 使用 Framework Servlet Resource Server 适配器。JWT 签名、`exp`、issuer、audience 和必填 claim 任一失败均返回 401。当前 denylist 尚未形成共享生产闭环，因此显式关闭；这不代表支持实时撤销。

## 4. GatewayProof 与 Redis Replay Store

```yaml
synapse:
  security:
    gateway-proof:
      enabled: true
      required: true
      gateway-id: ${TRUSTED_GATEWAY_ID:synapse-gateway:synapse-resource-server}
      secret: ${GATEWAY_PROOF_SECRET:}
      timestamp-skew: ${GATEWAY_PROOF_TIMESTAMP_SKEW:60s}
      replay-protection-enabled: true
      fail-fast: true
```

`RedisGatewayProofReplayStore` 实现 Framework `GatewayProofReplayStore`：

```text
SET key 1 NX PX ttl
```

Spring Data Redis 使用单条带过期时间的 `setIfAbsent`，不存在 `exists + save` 两步竞争。key 包含经过 SHA-256 处理的 audience-scoped gatewayId 与 nonce；相同 nonce 在不同 audience 下不冲突。TTL 取 Framework 剩余有效窗口与配置 timestamp window 的较小值，不超过证明有效窗口。Redis 异常、空结果或非法 TTL 均返回 false，验签请求 fail closed。

该能力是“nonce 首次使用原子标记”，不宣称端到端 Exactly-once。

## 5. 技术验收端点

| 路径 | 行为 |
| --- | --- |
| `GET /internal/security/protected` | 要求有效 GatewayProof 和独立 JWT |
| `GET /internal/security/permission` | 额外要求 JWT permission `resource:security:read`，不足返回 403 |
| `GET /internal/security/context` | 返回 CurrentPrincipalContext 与 OperationContext 的最小验收投影 |

普通 `X-User-Id`、`X-Tenant-Id`、`X-Initiator-Id`、`X-Roles`、`X-Permissions` Header 不参与身份建立。上下文中的 principal、tenant、actor 和 initiator 均来自已验证 JWT 及 Framework bridge。

## 6. 测试与 CI

```bash
mvn -pl synapse-resource-platform/synapse-resource-server -am clean verify
```

测试覆盖真实 RSA JWT 验签、issuer/audience/exp、GatewayProof 签名/method/path/token fingerprint/timestamp/audience、nonce 首次/重复、Redis fail-closed、普通身份 Header 忽略、CurrentPrincipalContext、OperationContext 和绕过 Gateway 拒绝。

真实 Redis 使用 Testcontainers。开发机无 Docker 时该测试明确跳过；CI 通过 `-Dsynapse.test.redis.required=true` 强制 Docker/Redis 测试不可跳过。

## 7. 剩余边界

- Resource 正式固定端口尚未在当前平台端口表中分配，仍通过 Nacos 注册实际随机端口；本轮没有擅自新增端口口径。
- GatewayProof secret 当前为单密钥，没有轮换与双读窗口。
- Gateway 与 Resource denylist 尚未共享闭环。
- 其他下游服务尚未接入 GatewayProof 与共享 Redis Replay Store。
