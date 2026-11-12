# 研究改版本轮开发与验收交付

- Time: `2026-11-12 20:34:40 +08:00`
- Author: `Codex / OpenAI GPT-6 Astra；主协调`
- Status: `completed`（本轮开发与自动/客户端验收交付；完整人工生存验收仍待进行）
- Scope: M1–M6机制、三个正式项目、M7教程与协作验证、最终包及两个隔离实例。

## Completed

- 已交付知识前置与披露、行为想法/计时联络、多阶段与固定路径、实物样品、风道和原型、控温炉及配方权限、旧研究承接、合并记录选择和备用进度。土制冶炼炉、冶铁炉、耐火砖已进入正式TWR数据；保留其余旧研究与工业制砖路线。
- 源码和服务端各节点均已有选择性回退点；最后功能基线FH为f36690072，TWR为3faf8096c（研究内容aa076eac7加一行默认N冲突修正）。最终FH与基线的唯一生产源码差异是控温炉槽位背景绘制，单独提交9daee21b9；布局、容器和协议未改。
- 更新living docs的实际验证状态，新增简明玩家入口与81项旧目录处置。为已实施的阶段计划补充Outcome，保留其设计时内容，避免把旧“待实现”文字当成当前待办。原design目录未动。

## Validation

- 主协调直接核对最终服务端通过日志；M6为32项回归、后续最终源码16项定向验证，两轮有重叠，不相加为独立用例数。M4/M5另有独立进程保存/恢复与正式客户端证据，详见[协调记录](../plans/2026-11-12_13-00-00_research-development-coordination.md)。
- [M5/M6完整客户端](2026-11-12_20-12-00_research-m5-m6-client-validation.md)两份原始结果分别passed/phase44和passed/phase20；主协调亲读JSON，查看入档条件、1100/1150温度、16砖输出、记录2选择和英文教程截图。M5入口使用正常联络及N预览/保留/学习，未直接授予入口知识。
- [M7双客户端](2026-11-12_19-04-00_research-m7-two-client-validation.md)在真实FTB队伍和TCP连接上覆盖同队实验、放置者离线、队友继续、重连和重复请求。M6实际客户端的两份历史则是构造夹具；两类证据分开。
- 最终生产JAR为14,907,400字节。槽位补丁compileJava/jar/reobfJar通过；主协调亲看游戏原生截图2026-11-12_20.31.29.png，确认3工艺槽、36背包槽边线可辨，物品及1100°C、400/400完成状态正常。这轮运行没有验证helper或frostedresearch启动参数，归档和最终配置见[视觉/整理记录](2026-11-12_20-36-00_research-client-final-visual-and-cleanup.md)。
- 正常保存退出后，主协调复读Playtest/Peer两实例：同一大小最终FH包、相同默认键文件、mods无helper、无自动验证参数，均Java17.0.2与4GB；N用于观察，TreeChop设置快捷键未绑定。原用户Prism、Development、旧存档和共享缓存保持原样。

## Delivery and remaining

Prism实例名为 **The Winter Rescue - Research Playtest (2026-11-12)** 和 **The Winter Rescue - Research Peer (2026-11-12)**。两个实例都保留测试世界；新建普通世界时可从任务书“研究入门：观察与实物试验”开始。玩家说明见[开始研究](../docs/knowledge/player-guide.md)，内容作者见[编写研究内容](../docs/knowledge/authoring-guide.md)。

测试使用了世界/材料等夹具，未完成从采矿、加工开始的整条生存通关，也未以程序输入冒充物理人手体验；整体计划因此保留人工验收待办。提交只包含本任务文件，TWR原已暂存config/.gitignore和各仓无关工作继续保留。没有推送、合并或发布。
