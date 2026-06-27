# Documentation Rules：Synapse Platform 文档协作与编写规范

## Document Metadata

| Item | Value |
| --- | --- |
| Audience | 架构师、开发人员、测试人员、运维人员、交付人员、项目维护者 |
| Purpose | 约束 Synapse Platform 文档的编写、评审、GitHub 展示和后续维护方式 |
| Scope | `README.md`、`docs/v1/**` 以及后续所有平台级产品、架构、规范、设计、测试、部署与交付文档 |
| Status | Draft |

## 1. Truth First

任何没有确认的信息，都不得写成事实。

禁止：

- 编造产品能力；
- 猜测未来规划；
- 默认采用某种架构；
- 默认支持某种部署方式；
- 把建议写成已决策；
- 把未实现能力写成已完成；
- 把待验证的技术方案写成生产可用事实。

正确做法：

- 标记为 `待确认`；
- 标记为 `建议`；
- 标记为 `设计选项`；
- 标记为 `暂不进入 V1`；
- 等待确认后再写入正式结论。

## 2. Confirm Before Writing

每次编写正式文档前，必须先确认：

1. 文档读者是谁；
2. 文档目的是什么；
3. 当前已知事实是什么；
4. 哪些信息不确定；
5. 哪些内容需要项目负责人确认；
6. 哪些内容只能写成 `待定`，不能写成事实。

如果存在影响产品范围、架构边界、安全模型、部署模型或交付口径的未知信息，应先讨论确认，再写入文档。

## 3. Documentation Before Coding

Synapse Platform 采用文档驱动开发方式。

推荐流程：

```text
Discussion
  -> Requirement Confirmation
  -> Product / Architecture / Specification Document
  -> Review
  -> GitHub Commit
  -> Design
  -> Coding
  -> Testing
  -> Delivery
```

不推荐：

```text
Coding
  -> 发现问题
  -> 推翻设计
  -> 补文档
```

## 4. Every Document Has an Audience

每份正式文档都必须明确读者。

| Document Type | Primary Audience |
| --- | --- |
| Product | 产品负责人、客户、售前、项目负责人 |
| Architecture | 架构师、核心研发 |
| Specification | 开发人员、代码评审人员、AI 编码助手 |
| Design | 模块负责人、开发人员、测试人员 |
| Testing | 测试工程师、开发人员、交付验收人员 |
| Deployment | 运维工程师、实施人员、平台管理员 |
| Delivery | 客户实施、交付团队、运维人员 |
| Reference | 所有维护者 |
| ADR | 架构师、核心研发、长期维护者 |

## 5. README Is the GitHub Landing Page

根 `README.md` 是 GitHub 仓库门面，不是详细说明书。

它必须满足：

- 现代化；
- 简洁明了；
- 直观；
- 适合作为 GitHub 首页；
- 让访问者在 30 秒内知道项目定位、价值、状态和阅读入口。

README 应该回答：

1. Synapse Platform 是什么；
2. 它解决什么问题；
3. 它不是什么；
4. 当前状态是什么；
5. 如何继续阅读文档；
6. 如何理解项目边界。

README 不应该：

- 堆砌过多实现细节；
- 重复完整架构文档；
- 放大量代码级说明；
- 承载所有模块设计；
- 与 `docs/v1/**` 形成多个事实来源。

## 6. Modern README Layout

README 推荐结构：

```text
Project Name
  -> One-line Positioning
  -> Highlights
  -> Current Status
  -> Architecture Snapshot
  -> Documentation Navigation
  -> Repository Structure
  -> Security Notice
```

README 可以使用：

- 简洁短段落；
- 表格；
- ASCII 架构图；
- 清晰导航链接；
- 状态标签；
- 少量重点列表。

README 不追求长，而追求第一眼清晰。

## 7. Enterprise Delivery Standard

所有文档默认按照企业软件产品交付标准编写，而不是 Demo、教程或临时说明。

编写时需要考虑：

- 开发可执行；
- 测试可验证；
- 部署可落地；
- 运维可理解；
- 客户交付可使用；
- 后续维护可追溯。

## 8. One Source of Truth

同一类事实只能有一个权威来源。

示例：

| Information | Source of Truth |
| --- | --- |
| 产品定位 | `00-product/product-positioning.md` |
| 模块边界 | `01-architecture/module-boundary.md` |
| 服务边界 | `01-architecture/service-boundary.md` |
| 开发规范 | `02-specification/**` |
| 测试策略 | `04-testing/testing-strategy.md` |
| 部署模型 | `05-deployment/**` |
| 交付说明 | `06-delivery/**` |
| 端口规划 | `07-reference/ports.md` |
| 架构决策 | `99-adr/**` |

README 只做摘要和导航，不复制完整内容。

## 9. Status Must Be Explicit

所有重要文档应明确状态：

- `Draft`：草案，可大幅调整；
- `Review`：等待确认；
- `Accepted`：已确认，进入执行；
- `Frozen`：冻结，修改需要 ADR 或变更记录；
- `Deprecated`：废弃，保留仅用于历史追溯。

## 10. No Hidden Assumptions

如果文档中出现设计建议，必须明确它是建议还是决策。

示例：

- `建议：V1 暂不引入 Service Mesh。`
- `决策：Gateway 是统一入口，但不承担业务授权。`
- `待确认：Kubernetes 是否作为 V1 必交付部署方式。`

## 11. Review Before Commit

重要文档写入 GitHub 前，应先确认关键不确定点。

如果只是目录结构、README 导航、已确认规则整理等低风险修改，可以直接提交；如果涉及产品范围、架构边界、安全模型、部署模型、交付承诺，必须先确认。

## 12. Documentation Structure Baseline

`docs/v1` 作为 Synapse Platform v1 产品文档空间，后续按以下生命周期组织：

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

该结构用于支撑开发、测试、部署、交付与长期维护。
