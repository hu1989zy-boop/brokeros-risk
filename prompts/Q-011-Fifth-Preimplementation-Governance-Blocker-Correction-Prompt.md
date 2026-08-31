# Q-011 Fifth Pre-implementation Governance Blocker Correction Prompt

这是 Q-011 Evidence Provenance Foundation 的第五次实施前治理一致性修复任务。

Codex 已读取 Product Owner 明确批准的 Requirement V3、Architecture V4、ADR-013 amendment、Implementation Design V5，以及第四份 resume Prompt。Codex 在开始写代码前再次按照 Prompt 的 `stop-on-contradiction` 规则执行检查，并发现 Requirement 与 ADR 中仍存在三个正式状态／追踪冲突，因此正确停止，未创建任何 Java、SQL、Flyway migration、测试或 v12 implementation review package。

本任务只修复治理文档和审批记录，不得开始 Q-011 实施。

## 开始前必须读取

1. `AGENTS.md`
2. `docs/skills/development-standards.md`
3. `docs/requirements/Q-011-Evidence-Provenance-Foundation.md`
4. `docs/architecture/q-011-evidence-provenance-foundation-architecture.md`
5. `docs/adr/ADR-013-evidence-provenance-foundation.md`
6. `docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`
7. `docs/lessons/2026-08-28-q-011-fourth-governance-consistency-correction.md`
8. `review/q-011/` 下已有的所有 Q-011 governance review packages 和 blocker reports
9. 当前 Git status

必须保留并服从以下已经批准的业务行为，不得重新设计：

- Subject 接受 `ELIGIBLE_FOR_NEW_ASSOCIATION` 和 `RECOGNIZED_NOT_ELIGIBLE`；只有 `NOT_RECOGNIZED` 拒绝。
- `HUMAN` 仅对 Record 和 Correct 强制要求；两个 read use case 不附加 ActorType 限制。
- Q-008 只能使用 narrow provenance contract，不能使用 full-detail contract。
- Full-detail read 必须在返回 content 前提交 access-log row，且事务不能标记为 database read-only。
- Correction 复制目标 subject、要求 reason、永远不调用 Q-010，并在 replay 时跳过目标 `ACTIVE` 检查。
- Design §11.1／§11.4 的执行顺序和 §8.5 的 constraint-to-test 列表均不因本轮修复而改变。

## Codex 发现的阻塞项

### 1. Requirement V3 的当前批准状态自相矛盾

文件：

`docs/requirements/Q-011-Evidence-Provenance-Foundation.md`

顶部 Status 和 §17 Current Gate 明确写明：

- Requirement V3 已由 Product Owner APPROVED；
- Architecture V4 已批准；
- ADR-013 amendment 已 RE-ACCEPTED；
- Design V5 已批准；
- Implementation 已获得新的明确授权；
- Implementation Allowed = YES。

但文件最后 §19 当前仍写：

```text
Status: this is Requirement V3, a draft candidate. It is not
self-approved. See §17/Status for the current gate.
```

这不是安全的历史记录表述：

- 使用 `this is` 当前时态；
- 把当前 V3 称为 `draft candidate`；
- 同时要求读者查看 §17 的 current gate，而 §17 又明确说 V3 已 APPROVED。

修复要求：

- 保留“Claude Code 没有自行批准”的真实治理事实；
- 明确说明 §19 记录的是修订稿形成时的历史状态；
- 明确说明该修订稿随后于 2026-08-28 获 Product Owner 批准；
- 当前状态只能指向 §17，并必须与顶部 Status 一致；
- 不得继续以当前时态称 V3 为未批准的 draft candidate。

建议表达结构（不要机械复制，如有仓库既定措辞则使用既定措辞）：

```text
Historical drafting status: V3 was produced as a candidate and was not
self-approved by its author. It was subsequently APPROVED by the Product
Owner on 2026-08-28. Section 17 is the authoritative current gate.
```

### 2. Requirement V3 多次引用不存在的 §20

同一文件多处要求读者 `see §20`，至少包括当前第 12、19、25、30、590、607 行附近。

但 Requirement 文件当前只有：

- §1–§19；
- 最后一个 heading 是 `## 19. Requirement Correction Record...`；
- 文件在 §19 后结束；
- 不存在 Requirement §20。

这使以下内容失去有效追踪目标：

- 第四轮修订说明；
- Requirement V3 版本依据；
- Implementation Allowed 的历史；
- Current Gate 的 finding-by-finding record。

修复要求：

1. 逐一检查每个 `§20` 引用的语义目标；
2. 不得全局盲目替换；
3. 如果引用目标是 Requirement 自身的 V2→V3 修订记录，应指向 Requirement §19；
4. 如果引用目标是四份文档的完整第四轮 finding-by-finding audit，应明确指向 Implementation Design §20.10；
5. 每处链接／章节引用必须指向真实存在且内容匹配的章节；
6. 修复后执行 heading/reference audit，证明不存在对缺失章节的当前引用。

### 3. ADR-013 的 amendment re-acceptance 状态自相矛盾

文件：

`docs/adr/ADR-013-evidence-provenance-foundation.md`

顶部 Status 和 Approval Boundary 明确写明：

- 原 ADR 已 Accepted；
- amendment 已由 Product Owner RE-ACCEPTED；
- 当前状态为 Accepted as amended。

但前言当前仍写：

```text
See "Amendment" below for the complete correction and its still-pending
re-acceptance status.
```

这与同文件顶部及 §Approval Boundary 直接冲突。

修复要求：

- 保留 amendment 在起草时确实 pending 的历史事实，但必须明确标记为历史；
- 当前状态必须写为 amendment 已于 2026-08-28 RE-ACCEPTED；
- 不得继续用 `still-pending` 描述当前状态；
- 全文检查 `pending`、`re-acceptance required`、`not accepted` 等表述；
- 历史段落可以说明“当时 pending”，但必须同时说明后来已重新接受，并不得与当前 Gate 混淆。

## 必须执行的完整扫描

修复前后都执行并人工检查以下命令的结果：

```bash
rg -n '^## |§[0-9]+|see §|See §' \
  docs/requirements/Q-011-Evidence-Provenance-Foundation.md

rg -n 'draft candidate|not self-approved|not approved|pending|still-pending|re-acceptance|required|APPROVED|AUTHORIZED|Implementation Allowed|RE-ACCEPTED' \
  docs/requirements/Q-011-Evidence-Provenance-Foundation.md \
  docs/architecture/q-011-evidence-provenance-foundation-architecture.md \
  docs/adr/ADR-013-evidence-provenance-foundation.md \
  docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md

rg -n 'see §20|See §20|§20' \
  docs/requirements/Q-011-Evidence-Provenance-Foundation.md
```

扫描目标不是让历史关键词全部消失，而是确保：

- 每个保留的 candidate/pending 表述都明确属于历史；
- 每个 current status 都与顶部 Status／Gate 一致；
- 每个章节引用都指向真实存在、语义正确的章节；
- 不再存在把已批准 V3 称为当前 draft candidate 的文字；
- 不再存在把已 re-accepted amendment 称为当前 still-pending 的文字。

另外对四份治理文档重新执行第四轮要求的关键词扫描，至少覆盖：

- `ELIGIBLE_FOR_NEW_ASSOCIATION`
- `RECOGNIZED_NOT_ELIGIBLE`
- `NOT_RECOGNIZED`
- `inactive`
- `retired`
- `HUMAN`
- `SERVICE`
- `automated consumer`
- `read-only`
- `Implementation Allowed`
- `AUTHORIZED`
- `APPROVED`
- `pending`
- `Next gate`

确认本轮状态修复没有重新引入业务行为冲突。

## 版本和审批纪律

本轮三个问题均属于正式治理状态／追踪纠正，不得由 Claude Code 自行决定其修复自动继承旧批准。

完成修订后：

1. 清楚列出修改了哪些当前状态或引用；
2. 明确说明没有改变任何 Q-011 业务行为、执行顺序或数据库约束；
3. 不得自行声称 Product Owner 已批准本轮新增修订；
4. 请求 Product Owner 明确确认：
   - Requirement V3 的状态／章节引用修复获批准；
   - ADR-013 的状态文字修复获确认；
   - 原 Requirement V3、Architecture V4、ADR-013 amendment、Design V5 的批准／重新接受继续有效；
   - 原 fresh implementation authorization 在本轮修复后继续有效，或由 Product Owner 重新授予一份 fresh authorization。

如仓库治理要求此次修复产生新的 Requirement/Architecture/Design 版本，必须说明依据并使用下一版本；不得擅自假定一定需要或一定不需要升版。

## 禁止事项

- 不得创建或修改 Q-011 Java 代码；
- 不得创建 Flyway V4 migration；
- 不得创建 Q-011 测试；
- 不得修改 Q-008、Q-009、Q-010；
- 不得修改任何现有 Flyway migration；
- 不得开始 Maven/MySQL implementation verification；
- 不得创建 implementation review package；
- 不得 stage、commit 或 push；
- 不得覆盖已有 review package；
- 不得修改 `review/review-history/`；
- 不得把自己编写的修复标记为 Product Owner 已批准；
- 如果发现新的实质性冲突，立即停止并精确报告。

## Lessons Learned 和 Review Evidence

本轮至少新增一份诚实 Lessons Learned，记录：

- 将修订发现写成历史记录时，必须明确区分“起草时状态”和“当前状态”；
- `see §N` 属于可机械验证的契约，文档增删章节后必须检查所有反向引用；
- 顶部 Gate 已更新并不能证明文件末尾的 correction record 也已更新；
- `pending` 状态转为 accepted 后，必须搜索全文，而不只是修改 Approval Boundary。

在 `review/q-011/` 下创建一个新的、非覆盖、timestamped governance correction review package。先检查当前最大版本号，再选择下一个未占用版本。由于 v12 原计划用于 implementation，在 implementation 尚未开始的情况下，不得复用或覆盖任何已经存在的包；请根据实际目录状态诚实选择并记录版本号。

至少包含：

- `Summary.md`
- `ConsistencyAudit.md`
- `DocumentReferenceAudit.md`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `Verification.md`
- `OutstandingItems.md`

`Verification.md` 必须记录实际执行的 `rg`、whitespace、Git 和文档检查命令及结果，不得把未执行项写成 PASS。

## 最终输出

最终回复必须：

1. 明确说明本轮只修复治理文档，没有实施 Q-011；
2. 列出所有修改文件；
3. 给出新 review package 的准确路径；
4. 报告扫描命令和结果；
5. 列出需要 Product Owner 明确确认的审批／授权事项；
6. 在 Product Owner 确认前保持：
   `Q-011 IMPLEMENTATION BLOCKED / NOT AUTHORIZED TO RESUME`；
7. 不得将 Q-011 implementation 标记为 complete、approved 或 ready for commit。

如果所有文档冲突都已消除，必须按照 `AGENTS.md` 的 Prompt Delivery Policy，在最终回复末尾提供一份完整的下一轮 Codex resume Prompt，但必须清楚标注：只有 Product Owner 明确确认上述修复和 implementation authorization 后才能执行。

结束标题必须为：

```text
====================================
Codex Prompt
====================================
```
