# 联络桌面允许加入辅助知识

- Time: `2026-11-17 19:08:40 +08:00`
- Author: `Codex / OpenAI；主代理实现与验证`
- Status: `completed`
- Scope: FH联络匹配、临时连连看会话、回归和客户端验收助手；TWR无源码改动。

## Completed

- 修复粗铜＋沙子等组合因多放一份辅助札记而无法匹配土制炉构想的问题。`KnowledgeService.previewLinks`和`matchLink`共用`LinkMatcher.bindWithOptional`，完整绑定必要槽后，允许未绑定的桌面札记匹配本联络路径的辅助条件。
- `linkOptionalKnowledge`统一解析显式`optional_knowledge`及省略时的想法前置继承；支持标签替代物。无关札记、重复记录、缺少必要输入及跨槽条件不满足仍不能匹配。
- 会话只保存必要槽见证，辅助不会因为摆在桌面而变成硬前置。辅助中途失效不取消本局；当前难度固定，重排按当前档案重算。联络历史仍只记录实际必要输入。
- 更新[定义](../docs/knowledge/definitions.md)、[状态/API](../docs/knowledge/state-and-api.md)、[绘图台](../docs/knowledge/workbench.md)、[玩家入口](../docs/knowledge/player-guide.md)、土制炉/耐火砖说明及[测试入口](../src/gametest/README.md)。无build.gradle、gradle.properties、design/改动。

## Decisions

- 允许本路径声明的辅助札记，不把联络改成任意知识的子集搜索；保留无关记录的失配反馈。`bindExact`的证据提交等原有用途保持严格匹配。
- 必要与辅助条件重叠时通过回溯寻找有效绑定，避免先绑定辅助后留下无关札记导致误判。

## Validation

- Java17 `runGameTestServer jar reobfJar m3cValidationJar validateResearchCatalog -PgameTestNamespaces=frostedresearch_m1,frostedresearch_m2 -PgameTestDirectory=build/research-link-auxiliary-20261117 -PthermalTestHeap=4G -PthermalTestProcessors=4 -PresearchCatalogDir=../TheWinterRescue/config/fhresearches --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过，4m4s；16项required GameTests全部通过，81个正式旧研究定义有效。日志`build/research-link-auxiliary-20261117.log`。
- 定向回归覆盖粗铜＋任一/全部辅助、预览与执行一致、必要输入不可替换、无关/重复记录、辅助继承/覆盖/显式空列表、重叠条件回溯及标签；含遗忘已放桌面的辅助后继续、重排和普通通关结算。
- Research Playtest实际客户端`passed/phase16`：新flat世界准备粗铜与沙子档案，通过Screen点击把两份都放桌面→找到土制炉候选→1数字连连看→Esc取消→相同组合重开→准备黏土档案→原重排按钮降至0数字→普通Screen/C2S配对及中央结束牌→正常获得想法并返回札记。原绘图台持久棋盘完全保留，不消耗纸笔。人工核对候选与1数字棋盘截图。
- 客户端证据：`C:/Users/chang/.codex/visualizations/2026/11/08/01a1ba78-b37a-7762-9426-964f95a6aee3/research-link-auxiliary-20261117/build/link-client/`。完整客户端日志一并保存。
- 正式JAR已同步Research Playtest并逐字节核对；备份`build/prism-backups/2026-11-17_19-04-40-link-auxiliary/`。客户端正常退出，临时helper和marker已移除。
- 选择性提交本轮FH修改；保留原.gitignore及其它未跟踪文件、TWR原暂存config/.gitignore及.workbuddy；不推送。

## Remaining

- None。客户端使用夹具知识和新世界、真实Screen事件与C2S；不代表物理鼠标键盘或完整生存通关。桌面上限5份和辅助难度0–7数字的原有范围不变。
