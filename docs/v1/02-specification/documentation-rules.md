# Synapse Platform 文档协作与编写规范

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 产品、架构、开发、测试、运维、实施、交付人员和 AI 编码助手 |
| Purpose | 约束 Synapse Platform 文档的编写、评审、GitHub 展示和长期维护方式 |
| Scope | 根 `README.md`、`docs/v1/**` 以及后续所有平台级文档 |
| Status | Accepted |

## 1. Truth First

任何没有确认的信息，都不得写成事实。

禁止：

- 编造产品能力；
- 猜测未来规划；
- 默认采用某种架构、部署方式或商业模式；
- 把建议写成已决策；
- 把未实现能力写成已完成；
- 把待验证方案写成生产可用事实。

正确做法：

- 标记为 `待确认`；
- 标记为 `建议`；
- 标记为 `设计选项`；
- 标记为 `暂不进入 V1`；
- 等待确认后再写入正式结论。

## 2. Confirm Before Writing

每次编写正式文档前，必须先明确：

1. 文档读者；
2. 文档目的；
3. 已确认事实；
4. 不确定点；
5. 需要项目负责人确认的信息；
6. 只能标记为待定的内容。

如果未知信息会影响产品范围、架构边界、安全模型、部署模型、验收标准或交付承诺，应先讨论确认，再写入正式结论。

## 3. Documentation Before Large-scale Coding

Synapse Platform 采用文档驱动的产品研发方式：

```text
Discussion
  -> Confirm Facts and Unknowns
  -> Product / Architecture / Specification
  -> Review
  -> GitHub Commit
  -> Module Design
  -> Development
  -> Testing
  -> Deployment
  -> Delivery
```

文档驱动不等于禁止验证性编码。技术验证可以先进行，但验证结果不能自动成为正式产品承诺或架构决策。

## 4. Every Document Has an Audience

| Document Type | Primary Audience |
| --- | --- |
| Product | 产品负责人、企业客户、售前、项目负责人 |
| Architecture | 架构师、核心研发、测试和运维负责人 |
| Specification | 开发人员、评审人员、AI 编码助手 |
| Design | 模块负责人、开发和测试人员 |
| Testing | 测试、开发和交付验收人员 |
| Deployment | 运维、实施和平台管理员 |
| Delivery | 客户实施、交付和运维团队 |
| Reference | 所有维护者 |
| ADR | 架构师、核心研发和长期维护者 |

重要文档应在开头包含：Audience、Purpose、Scope 和 Status。

## 5. README Is the GitHub Landing Page

根 `README.md` 是 GitHub 仓库门面，不是详细说明书。

它必须：

- 现代化；
- 简洁明了；
- 直观；
- 让访问者在 30 秒内知道项目是什么、解决什么问题、当前状态和阅读入口；
- 与 `docs/v1/**` 保持一致。

README 不应该：

- 堆砌实现细节；
- 复制完整产品或架构文档；
- 承载所有模块设计；
- 把规划能力描述为已完成能力；
- 形成另一个事实来源。

推荐结构：

```text
Project Name
  -> Slogan / One-line Positioning
  -> Product Value
  -> Current Status
  -> Architecture Snapshot
  -> Documentation Navigation
  -> Repository Rules
  -> Security Notice
```

## 6. Enterprise Delivery Standard

所有文档默认按照企业软件产品交付标准编写，而不是 Demo、教程或临时说明。

文档应尽可能做到：

- 开发可执行；
- 测试可验证；
- 部署可落地；
- 运维可理解；
- 客户交付可使用；
- 后续维护可追溯。

## 7. One Source of Truth

同一类事实只能有一个权威来源。

| Information | Source of Truth |
| --- | --- |
| 产品定位、价值、用户和产品边界 | `00-product/product.md` |
| 架构原则 | `01-architecture/architecture-principles.md` |
| 系统上下文和边界 | `01-architecture/system-context-and-boundary.md` |
| 模块边界 | `01-architecture/module-boundary.md` |
| 服务边界 | `01-architecture/service-boundary.md` |
| 开发规范 | `02-specification/**` |
| 模块详细设计 | `03-design/**` |
| 测试和验收 | `04-testing/**` |
| 部署支持矩阵和部署方式 | `05-deployment/**` |
| 交付与运维说明 | `06-delivery/**` |
| 术语、端口和环境变量 | `07-reference/**` |
| 架构决策 | `99-adr/**` |

尚未创建的路径表示计划中的权威位置，不代表内容已经确认。

## 8. Status Must Be Explicit

正式文档使用以下状态：

- `Draft`：草案，可调整；
- `Review`：等待评审和确认；
- `Accepted`：已确认，可进入执行；
- `Frozen`：已冻结，重大修改需要 ADR 或变更记录；
- `Deprecated`：已废弃，仅保留历史追溯。

禁止把 Draft 或规划内容描述为正式交付能力。

## 9. No Hidden Assumptions

建议、决策和待确认项必须明确区分：

- `建议：V1 暂不引入 Service Mesh。`
- `决策：Gateway 是统一入口，但不承担业务授权。`
- `待确认：Kubernetes 是否属于 V1 必交付部署方式。`

行业惯例不能自动视为 Synapse Platform 的已确认需求。

## 10. Review and Commit Rules

以下低风险修改可以在不改变事实的前提下直接提交：

- 目录整理；
- README 导航；
- 链接修复；
- 格式统一；
- 已确认信息的迁移和去重。

以下内容写入前必须确认关键不确定点：

- 产品范围；
- 架构和服务边界；
- 安全与权限模型；
- 部署支持矩阵；
- 测试和验收标准；
- 商业版本与交付承诺。

删除文档前应先判断其中是否包含仍然有效的事实或决策。有效内容应迁移到新的权威位置后再删除旧文件。

## 11. Documentation Structure Baseline

```text
docs/v1
├── 00-product
├── 01-architecture
├── 02-specification
├── 03-design
├── 04-testing
├── 05-deployment
├── 06-delivery
├── 07-reference
└── 99-adr
```

该结构用于支撑产品定义、开发、测试、部署、交付与长期维护。
