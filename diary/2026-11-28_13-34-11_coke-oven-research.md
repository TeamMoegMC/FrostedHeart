# T1 焦炉研究

- Time: `2026-11-28 13:34:11 +08:00`
- Author: Codex（OpenAI，实现与验证）
- Status: completed
- Scope: 焦炉研究、控温试样、旧研究承接和 T1 任务书。

## Completed

- 接入烧成炭堆的挖掘触发、两项必需观察与三项辅助观察、六种控温试样和两次独立检视。完成研究同时开放焦炉砖配方与成形权限。
- 控温炉支持 400°C 和标签输入。旧研究效果映射到两个独立成果，并在 T1 可选支线指引玩家完成研究后凑齐 27 块焦炉砖。
- 更新[焦炉研究](../docs/knowledge/coke-oven.md)、[控温炉](../docs/knowledge/controlled-furnace.md)和[旧研究处置](../docs/research/legacy-content-disposition.md)。

## Decisions

- 玩家根据停炉产物探索时长。烧透试样以专用物品 ID 和新鲜观察判定。27 块焦炉砖的任务和实际搭炉属于研究后的支线。

## Validation

- 13 项控温炉 Forge GameTest 通过。核对正式包六种试样、两种语言、开题条件和焦炉砖配方。
- Research Playtest 临时世界通过测试。实际玩家挖掘炭堆触发构想，验证 1% 与 11% 的概率规则、试烧分档、两次样品检视和两项权限。炉温和倒计时通过客户端界面输入，样品研究检查使用服务端测试调用。记录位于 `build/coke-oven-client/`。

## Remaining

- 旧档替换映射已在试玩客户端激活。现成生存存档迁移和完整焦炉实地成形未纳入本轮试玩。
