# Gateway-IAM Opaque Token 本地认证闭环测试记录

## 1. 文档元数据

| Item | Value |
| --- | --- |
| 文档状态 | Executed / Failed |
| 执行日期 | 2026-06-28 |
| Platform 分支 | `codex/gateway-iam-auth-loop` |
| Platform HEAD | `a28e6d4904860b513a960bf173a9b7abdcf4193d` |
| Framework HEAD | `79a9a0bb9d8f4275d40b8e79fefcb90c263c11da` |
| 测试模式 | Host-local middleware：宿主机 PostgreSQL、Redis、Nacos；仅启动本次 IAM/Gateway Java 进程 |
| 测试脚本 | `scripts/test-gateway-iam-auth-loop.sh --mode local` |
| 测试临时目录 | `/tmp/synapse-auth-test-local` |
| 结论 | `Failed`：Refresh Token reuse detection 未撤销 successor refresh family |

## 2. 测试目标

验证 Gateway -> IAM Opaque Token 真实链路：

```text
Client
  -> Gateway
  -> Gateway Reactive Redis 授权快照验证
  -> Bearer Token 原样透传
  -> GatewayProof 签发
  -> IAM GatewayProof 验证
  -> IAM Redis 授权快照验证
  -> IAM Controller
```

本轮不使用 Docker Compose 启动 PostgreSQL、Redis、Nacos，不修改 Nacos 配置，不停止宿主机 PostgreSQL、Redis、Nacos，不修改 Framework，不修改历史 migration，不输出密码、Token、Secret 或 token digest。

## 3. 实际环境

| 项目 | 实际值 |
| --- | --- |
| 工作目录 | `/Users/sxc/company/IndigoByte Studios/Synapse/synapse-platform` |
| Maven | `/Users/sxc/Documents/tool/apache-maven-3.9.0/bin/mvn` |
| PostgreSQL | 宿主机 `127.0.0.1:5432`，PostgreSQL 17.9 |
| PostgreSQL database/schema/user | `synapse_iam` / `public` / `postgres` |
| Redis | 宿主机 `127.0.0.1:6379`，`PING -> PONG` |
| Nacos | 宿主机 `127.0.0.1:8848`，namespace 为空，group `SYNAPSE_PLATFORM_DEV` |
| IAM 端口 | `18100` |
| Gateway 端口 | `18080` |
| Redis 故障模拟 | 不停止宿主 Redis；重启本次 Gateway 并指向未监听端口 `16380` |
| 测试账号 | 使用独立 bootstrap 用户 `codex_auth_loop_20260628`，未重置既有 `admin` |

Nacos 日志显示 `dataId=synapse-iam-server.yml, group=SYNAPSE_PLATFORM_DEV` 为空，本轮没有发现远端配置覆盖数据库连接。由于本地数据库实际以 `postgres` 作为唯一用户执行 IAM/Flyway，脚本本地模式按 `postgres` 用户连接。

## 4. 执行结果

| Case | 场景 | 结果 |
| --- | --- | --- |
| 01 | 本地 IAM/Gateway 健康检查 | Passed |
| 02 | PostgreSQL Flyway V2 表存在 | Passed |
| 03 | 未认证访问 `/iam/auth/me` | 401，Passed |
| 04 | 错误账号登录 | 401，错误码统一，Passed |
| 05 | 登录成功，Access Token 非 JWT | Passed |
| 06 | Redis 授权快照存在、TTL 正常、不含原始 Access Token | Passed |
| 07 | Gateway -> IAM `/auth/me` | 200，Passed |
| 08 | 直连 IAM 缺 GatewayProof | 非 200，Passed |
| 09 | 客户端伪造 GatewayProof Header 经 Gateway 访问 | Gateway 清理并重新签发，Passed |
| 10 | Refresh rotation 与 PostgreSQL 状态 | Passed |
| 11 | 旧 Refresh Token reuse detection | HTTP 旧 token 返回 401，但 successor refresh 仍返回 200，Failed |
| 12 | 并发 refresh | 仅一次成功，DB 不存在两个 ACTIVE successor，Passed |
| 13 | Logout 后 Access/Refresh Token 与 DB 状态 | Passed |
| 14 | 删除 Redis 授权快照后访问 | 401，Passed |
| 15 | Redis 不可用时访问 `/iam/auth/me` | 503，Passed |
| 16 | Redis 恢复 | Gateway health + `PING -> PONG`，Passed |
| 17 | IAM 拒绝错误 GatewayProof | Passed |
| 18 | 日志敏感材料扫描 | 未发现密码、Token、Secret 或 digest，Passed |

## 5. 失败点

已确认事实：

- 旧 Refresh Token 复用请求返回 401，说明复用被识别。
- 复用发生后，successor Refresh Token 仍可继续刷新，脚本观测到 HTTP 200。
- PostgreSQL `iam_refresh_session` 中对应 family 没有全部进入 `REUSE_DETECTED`，仍存在 `ACTIVE` session。
- 现有单元测试期望 reuse 后 `fixture.sessions.findByFamilyId(...).allMatch(status == REUSE_DETECTED)`。

影响：

- Refresh Token reuse detection 的真实 PostgreSQL + Redis + HTTP 链路行为与单元测试/安全预期不一致。
- 本轮测试结论为 `Failed`，不是环境阻塞。

## 6. 本轮发现并修正的测试/配置缺口

- `synapse-iam-server/src/main/resources/application-dev.yml` 补齐 `spring.datasource.dynamic.datasource.master.password: ${DB_PASSWORD:}`，否则本地 IAM 无法通过环境变量注入数据库密码。
- `scripts/test-gateway-iam-auth-loop.sh --mode local` 支持 host-local middleware，只启动本次 IAM/Gateway 进程。
- 本地 Redis 不可用测试不停止宿主 Redis，而是将本次 Gateway 指向坏 Redis 端口。
- Case 14 本地模式改为携带有效 Authorization header，避免把未认证 401 误判为 Redis 故障路径。

## 7. 执行命令

```bash
bash -n scripts/test-gateway-iam-auth-loop.sh
git diff --check -- scripts/test-gateway-iam-auth-loop.sh synapse-iam-platform/synapse-iam-server/src/main/resources/application-dev.yml
/Users/sxc/Documents/tool/apache-maven-3.9.0/bin/mvn -pl :synapse-gateway-platform,:synapse-iam-api,:synapse-iam-client,:synapse-iam-server -am clean test
IAM_BOOTSTRAP_USERNAME=codex_auth_loop_20260628 scripts/test-gateway-iam-auth-loop.sh --mode local
```

## 8. 清理与残留

- 脚本退出后未发现本轮 IAM、Gateway 或测试脚本进程残留。
- 宿主 Redis 中测试后仍可见授权快照 Key；本轮未清空宿主 Redis，避免删除非本轮数据。
- 本地 PostgreSQL 已执行 Flyway V2，`iam_refresh_session` 表存在，并保留本轮真实测试数据。

## 9. 尚未完成

- 未修复 Refresh Token reuse detection 的 successor family 撤销行为。
- 未执行 beta/prd 环境验证。
- 未执行跨服务 Resource/Audit 联动验证。
