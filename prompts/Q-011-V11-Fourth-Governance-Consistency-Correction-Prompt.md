# Q-011 V11 Fourth Governance Consistency Correction Prompt

这是 Q-011 Evidence Provenance Foundation 的第四轮实施前治理文档一致性修复。

本轮不是实施任务。禁止编写 Java、SQL、Flyway migration、测试或 Q-011 运行时代码。必须先修复正式治理文档中的冲突，完成一致性审查，并等待 Product Owner 明确重新批准／接受后，才能再次授权 Codex 实施。

## 已确认的 Product Owner 行为决定

以下行为已经确定，不得重新解释：

1. Evidence Record 的 Trading Account subject 只需要被 Q-010 识别：
   - `ELIGIBLE_FOR_NEW_ASSOCIATION`：接受；
   - `RECOGNIZED_NOT_ELIGIBLE`：接受，包括 inactive/retired Trading Account；
   - 只有 `NOT_RECOGNIZED` 才拒绝；
   - ResultCode 为 `EVIDENCE_SUBJECT_NOT_RECOGNIZED`。
2. `ActorType.HUMAN` 只对 Record 和 Correct 强制要求：
   - Record：必须是 `HUMAN`；
   - Correct：必须是 `HUMAN`；
   - Provenance read：不要求 `HUMAN`，任何持有 `evidence:read` 且通过 Q-009 授权的 ActorType 均可调用；
   - Full-detail read：不要求 `HUMAN`，任何持有 `evidence:read` 且通过 Q-009 授权的 ActorType 均可调用；
   - Full-detail read 仍必须先成功提交 `evidence_access_log`，才能返回 observation content；
   - Q-008 只能使用窄化的 provenance contract，不能调用 full-detail contract。这是消费者契约限制，不是 ActorType 限制。
3. Correction：
   - 不接受新的 subject 输入，而是复制目标 Evidence 的 subject；
   - 无论新操作还是 replay，都绝不调用 Q-010；
   - replay 必须在目标状态检查之前返回；
   - 新 correction 才要求目标当前为 `ACTIVE`。

这些决定不是本轮的开放问题。本轮任务是让 Requirement、Architecture、ADR、Implementation Design 和状态记录全部准确表达这些决定。

## 文档优先级

严格遵守：

1. `AGENTS.md` 和 development standards；
2. approved Requirement；
3. approved Architecture；
4. accepted ADR；
5. Implementation Design；
6. Prompt。

如果发现上述已确认决定之外的新实质性冲突，不得自行决定，必须停止并报告。

## 开始前必须完整阅读

1. `AGENTS.md`
2. `docs/requirements/Q-011-Evidence-Provenance-Foundation.md`
3. `docs/architecture/q-011-evidence-provenance-foundation-architecture.md`
4. `docs/adr/ADR-013-evidence-provenance-foundation.md`
5. `docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`
6. `docs/skills/development-standards.md`
7. `docs/skills/trusted-actor-authorization.md`
8. `docs/skills/trading-account-reference-authority.md`
9. `docs/skills/brokeros-risk-core-domain.md`
10. 最近所有 Q-011 Lessons Learned
11. `review/q-011/q-011-v3-implementation-blocker-report-20260828-191130.md`
12. 当前 Git status 和已有 `review/q-011/` 内容

不得修改 Q-008、Q-009、Q-010 的代码或文档，不得修改任何现有 Flyway migration。

## 必须修复的问题

### 1. Requirement 的 ActorType 冲突

`docs/requirements/Q-011-Evidence-Provenance-Foundation.md`：

- Goal 5 目前说所有 protected use case 都限制为 `HUMAN`；
- `Q011-FR-005` 则只限制 Record 和 Correct；
- Design §11.4 允许任何获授权 ActorType 执行两种 read。

修复 Goal 5，使其明确表达：

- 所有 protected use case 都必须使用 Q-009 ActorContext 和 capability authorization；
- 只有 Record 和 Correct 附加要求 `ActorType.HUMAN`；
- Provenance read 和 Full-detail read 不附加 ActorType 限制。

完整搜索 Requirement 内所有 `HUMAN`、`SERVICE`、read、protected use case 相关表述，确保没有其他矛盾。

由于 Requirement 正文发生正式修正，应形成新的 Requirement 版本候选，预期为 V3。不得在没有 Product Owner 新确认的情况下自行标记为 APPROVED。

### 2. ADR-013 的 subject-bar 冲突

当前 accepted ADR-013 仍明确写着：

- 只接受 `ELIGIBLE_FOR_NEW_ASSOCIATION`；
- inactive/retired subject 不在当前范围；
- inactive subject 需要未来 Requirement。

这直接违反 Requirement `Q011-FR-002` 和已确认的 Product Owner 决定。

完整修订 ADR-013 中所有相关部分，包括但不限于：

- Architecture version reference；
- Subject validation decision；
- Alternatives Considered；
- Consequences / Costs and constraints；
- Operational implications；
- Deferred Decisions；
- Approval Boundary；
- 所有 `eligible`、`ineligible`、`inactive`、`retired`、`recognized` 匹配项。

正确决定应为：

- 继续复用 Q-010 现有 `validateForNewRiskCaseAssociation(ActorContext, TradingAccountRef)`；
- 不修改 Q-010；
- 该 contract 已返回三态，Q-011 接受其中两个 recognized 状态；
- 只有 `NOT_RECOGNIZED` 被拒绝；
- inactive/retired Evidence subject 属于 Q-011 当前范围，不是未来 Requirement。

这是 accepted ADR 正文中的实质性决定修正。不得静默保留 `Accepted` 并假装没有变化。请按照仓库 ADR 治理方式形成需要 Product Owner 重新接受的 ADR-013 amendment；保留原接受历史，并明确当前 amendment 尚待重新接受。若仓库没有既定 amendment 状态格式，清楚记录 `Product Owner re-acceptance required`，不得自创“已经重新接受”的事实。

### 3. ADR-013 的 read ActorType 冲突

ADR-013 当前把 full-detail read 描述为只供 direct human review，并表示 automated consumer 不可使用。

修复为：

- Q-008 不得使用 full-detail contract；
- provenance contract 与 full-detail contract 都需要 `evidence:read`；
- Record/Correct 才要求 `HUMAN`；
- read contract 本身不附加 ActorType 限制；
- 获授权的 `SERVICE` actor 可以调用 read use case；
- full-detail access-log-before-disclosure 规则不变。

必须明确区分：

- “Q-008 不能消费 full-detail contract”；
- “所有 SERVICE actor 都不能读取”。

前者保留，后者不是批准行为。

### 4. Architecture V3 内部冲突

完整修复 Architecture 中所有残留项，至少包括：

- §23 item 5 已说 inactive/retired subject 在当前范围；
- §23 item 15 却仍把 inactive-subject Evidence 列为未来 Requirement；
- §23 item 17 仍说 Implementation authorized = No；
- §24 则说 Implementation 已授权；
- 检查 Document Status、Architecture Gate、Next Gate 和全文中的版本／授权状态。

修复后的 Architecture 应形成下一版本候选，预期为 V4。由于本轮重新打开正式治理 Gate，在 Product Owner 明确批准前，不得标记 V4 为 APPROVED，也不得声称 Implementation Allowed。

### 5. Implementation Design V4 内部冲突

完整修复 Design 中所有残留项，至少包括：

- §1.1 仍把 Architecture 写成 V2；
- §1.1 仍把当前 Design 写成 V3；
- §20.9 仍把 inactive-subject Evidence 列为未来 Requirement；
- §21 同时存在两个互相矛盾的 `Next gate`；
- 一个说 Codex 立即实施；
- 另一个说仍等待 V4/Architecture 批准和 fresh authorization。

修复后的 Design 应形成下一版本候选，预期为 V5。不得在 Product Owner 明确批准前标记为 APPROVED。

Design §11.1 和 §11.4 的执行顺序不应因本轮修复而改变，除非发现新的正式冲突。必须保留：

- Authorization 和 `HUMAN` check 在 replay check 之前；
- Record replay 不再次调用 Q-010；
- Correct replay 不检查 target status；
- Correct 永远不调用 Q-010；
- 两种 read 不要求 `HUMAN`；
- Full-detail read 使用短事务写 access log，不能标记为 database read-only。

### 6. Gate／授权状态统一

本轮修复过程中，正式状态必须诚实表达：

- 旧版本的历史批准记录可以保留；
- 新 Requirement/Architecture/ADR amendment/Design 候选尚待 Product Owner 明确批准或重新接受；
- 在这些批准完成前，Q-011 Implementation Allowed = NO；
- 不得继续保留互相矛盾的“已授权”和“仍待授权”当前状态；
- 不得自行代表 Product Owner 批准自己的修订。

Product Owner 后续明确批准全部修订后，再进行一次仅更新 gate 状态的受控修订，并生成新的、完整的 Codex implementation resume Prompt。

## 必须执行的机械一致性审查

不要依赖记忆。使用 `rg` 对全部四份治理文档执行并人工检查至少以下匹配：

- `ELIGIBLE_FOR_NEW_ASSOCIATION`
- `RECOGNIZED_NOT_ELIGIBLE`
- `NOT_RECOGNIZED`
- `eligible`
- `ineligible`
- `inactive`
- `retired`
- `future Requirement`
- `HUMAN`
- `SERVICE`
- `automated consumer`
- `read-only`
- `Implementation Allowed`
- `Implementation authorized`
- `AUTHORIZED`
- `APPROVED`
- `pending`
- `Next gate`
- `V2`
- `V3`
- `V4`
- `V5`

检查目标不是做到零匹配，而是确保每个保留匹配都符合最终决定和历史语境。历史缺陷说明可以保留旧行为，但必须明确标注为历史、已修复，不能继续作为当前决定出现。

另需逐项确认：

1. Requirement 的 Goal、FR、SR、Acceptance Criteria、Current Gate 相互一致；
2. Architecture 的 Decision Summary、Subject Validation、Security、Failure Model、Traceability、Future Requirements、Gate 相互一致；
3. ADR 的 Decision、Alternatives、Consequences、Security、Deferred Decisions 相互一致；
4. Design 的 Scope、Authorization、Use Cases、§11.1、§11.4、Tests、Future Scope、Gate 相互一致；
5. 四份文档之间的 subject bar、ActorType read policy、实施授权状态一致。

## 禁止事项

- 不得编写或修改 Java；
- 不得创建 Flyway V4；
- 不得创建 Q-011 测试；
- 不得修改 Q-008、Q-009、Q-010；
- 不得修改 V1–V3 migration；
- 不得开始 Q-011 implementation；
- 不得 stage、commit 或 push；
- 不得覆盖任何已有 review package；
- 不得修改 `review/review-history/`；
- 不得把自己编写的修订标记为 Product Owner 已批准；
- 不得把未执行验证写成 PASS。

## Lessons Learned 和 Review Package

新增一份诚实的 Lessons Learned，例如：

`docs/lessons/2026-08-28-q-011-fourth-governance-consistency-correction.md`

内容应记录至少：

- accepted ADR 也必须在上游 Requirement 决定改变或纠正后进行全文同步；
- “Architecture/Design 已修复”不能替代对 ADR、Requirement Goals、Future Scope 和 Gate 状态的机械扫描；
- ActorType 限制与 consumer-contract 限制必须分开描述；
- 同一文档出现两个 `Next gate` 是状态更新不完整的直接证据。

创建一个新的、不可覆盖的 timestamped review package。先检查 `review/q-011/` 当前最大版本号，再使用下一个未占用版本；按当前状态预期为：

`review/q-011/review-q-011-v11-governance-consistency-correction-<YYYYMMDD-HHMMSS>/`

至少包含：

- `Summary.md`
- `ArchitectureReview.md`
- `ConsistencyAudit.md`
- `DecisionMatrix.md`
- `DocumentVersionMatrix.md`
- `ProjectTree.txt`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `Verification.md`
- `OutstandingItems.md`

`ConsistencyAudit.md` 必须列出上述每个 `rg` 命令及人工审查结论。

`DecisionMatrix.md` 必须逐项确认：

- recognized subject bar；
- `RECOGNIZED_NOT_ELIGIBLE`；
- `NOT_RECOGNIZED`；
- Record ActorType；
- Correct ActorType；
- Provenance-read ActorType；
- Full-detail-read ActorType；
- Q-008 consumer limitation；
- Full-detail access audit；
- correction Q-010 behavior；
- replay order；
- 当前 approval/authorization state。

## 最终输出要求

完成文档修复和验证后：

1. 明确说明本轮只完成治理文档修复，没有实施 Q-011；
2. 列出修改文件；
3. 列出所有实际执行的验证命令和结果；
4. 给出 review package 路径；
5. 明确列出仍需 Product Owner 作出的批准：
   - Requirement 新版本批准；
   - Architecture 新版本批准；
   - ADR-013 amendment 重新接受；
   - Implementation Design 新版本批准；
   - 独立的 implementation authorization；
6. 在未取得上述批准前，结论必须是：
   `Q-011 IMPLEMENTATION BLOCKED / NOT AUTHORIZED`；
7. 不得将 Q-011 标记为 complete、approved implementation 或 ready for commit。

如果修复后仍发现任何新的行为冲突，立即停止，不要生成 Codex 实施指令；报告精确文件、章节和冲突双方。

如果文档已完全一致，则最终回复必须按照仓库 Prompt Delivery Policy，以以下标题结束，并附上一份完整、可直接执行但明确注明“必须等待 Product Owner 完成上述批准后才能使用”的下一轮 Codex 实施 Prompt：

```text
====================================
Codex Prompt
====================================
```
