# Q-011 V3 实施阻塞审查报告

- 审查时间：2026-08-28 19:11:30（Asia/Kuala_Lumpur）
- 审查对象：Q-011 Evidence Provenance Foundation 第二份恢复提示及其治理文档
- 实施状态：未开始
- 结论：**BLOCKED — 正式文档仍存在高优先级冲突，必须先修订并重新批准**

## 审查范围

本次在写代码前核对了：

- 根目录 `AGENTS.md`；
- `docs/requirements/Q-011-Evidence-Provenance-Foundation.md`（V2）；
- `docs/architecture/q-011-evidence-provenance-foundation-architecture.md`（V2）；
- `docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`（V3）；
- 第二份 Q-011 Codex 恢复提示；
- V3 中作为权威执行顺序的 §11.1、§11.4，以及数据库约束矩阵 §8.5。

V3 已统一授权、`HUMAN` 检查、幂等重放、Q-010 调用及修正目标检查的主要执行顺序；但下列冲突仍阻止精确实施。

## 阻塞项 1：Q011-FR-002 与 Architecture/Design 的资格门槛冲突

Requirement `Q011-FR-002` 明确规定：

- Evidence 的 Trading Account subject 只需被 Q-010 识别；
- “recognized”是比 `ELIGIBLE_FOR_NEW_ASSOCIATION` 更低的门槛；
- 两者不得混淆。

但 Architecture §9 和 Implementation Design §6.1 要求 subject 必须是
`ELIGIBLE_FOR_NEW_ASSOCIATION`，并拒绝 `RECOGNIZED_NOT_ELIGIBLE`。

根据 Implementation Design §1.1，Requirement 的优先级高于 Architecture 和
Implementation Design。根据 `AGENTS.md` 的 Requirements Discipline，不得静默缩小
已批准 Requirement。因此当前无法自行决定应接受还是拒绝已识别但当前不具备新关联资格的账户。

### 必须作出的决定

由 Product Owner 明确选择并批准以下一种方案：

1. 修改 Requirement，使“当前必须 eligible”成为正式要求；或
2. 修改 Architecture/Design，使记录操作接受 Q-010 返回的
   `RECOGNIZED_NOT_ELIGIBLE`。

## 阻塞项 2：读取用例的 ActorType 规则冲突

Implementation Design §5.2 表示所有受保护的 Q-011 用例均由经过认证的
`HUMAN` 通过正常 HTTP 边界触发。

Implementation Design §11.4 则明确规定：

- Record 和 Correct 必须是 `HUMAN`；
- Provenance read 和 Full-detail read 不要求 `HUMAN`，任何获授权 ActorType
  均可执行。

这会改变 `SERVICE` Actor 能否读取 Evidence provenance 或完整详情，属于安全契约，
不能由实现者自行解释。

### 建议修正

若 §11.4 代表批准决定，应将 §5.2 限定为 Record/Correct authoring 用例；否则必须
同步修改 §11.4、Architecture 和安全测试要求。

## 阻塞项 3：Architecture V2 的批准状态在 Design 内不一致

Architecture V2 自身的 Document Status 和 Architecture Gate 均标记为：

```text
APPROVED — 2026-08-28 — Product Owner
```

但 Implementation Design §21 仍写着：

```text
Architecture: V2 — pending Product Owner approval
```

该状态必须在正式文档中统一，避免后续评审包错误陈述治理门禁。

## 阻塞项 4：§8.5 未完整追踪 §8.1–§8.4 的数据库约束

§8.5 声称逐项覆盖 §8.1–§8.4 的每个数据库不变量、执行机制和证明测试，第二份恢复
提示也要求实现并验证该完整矩阵。但当前矩阵遗漏或错误归类了多项约束，包括：

- `evidence_record.source` 的允许值检查；
- `evidence_record.status` 的允许值检查；
- `evidence_ref`、`recorded_by_actor_ref`、`operation_id` 的 UUIDv4 形状检查；
- `evidence_operation.operation_type` 的允许值检查；
- 部分 FK、索引及对应测试映射；
- “1–4000 / 1–1000 字节”一行只指向 `evidence_record` 的 observation 检查，
  但 correction reason 实际由 `evidence_operation_history` 约束，职责归属混杂。

### 必须修正

扩充 §8.5，使 §8.1–§8.4 的每个 `CHECK`、`UNIQUE`、FK 和要求验证的索引均具有：

- 唯一、准确的不变量描述；
- 对应表与命名约束；
- 精确 DDL 机制；
- 明确的 MySQL 8.4 证明测试。

## 本次未执行的事项

由于恢复提示明确要求遇到任何冲突时停止，本次没有：

- 新增或修改 Java 代码；
- 新增 Flyway V4 迁移；
- 修改 V1–V3 迁移；
- 新增或运行 Q-011 测试；
- 创建 Q-011 v9 实施评审包；
- 修改 Q-009、Q-010 或 Q-008 文件；
- 暂存、提交或推送 Git 变更。

本报告是独立的阻塞记录，不是实施完成评审包，也不表示 Q-011 已完成或获批。
