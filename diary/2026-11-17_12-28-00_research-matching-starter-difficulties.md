# 连连看入门难度与调试研究

- Time: `2026-11-17 12:28:00 +08:00`
- Author: `Codex / OpenAI；主代理实现与验证`
- Status: `completed`
- Scope: FH连连看生成、绘图台纸笔与胜局判定、旧线索/知识项目定义、调试数据包和Research Playtest生产JAR。

## Completed

- 新增0、1、2、3个顺序数字的入门连连看，含中央结束牌分别9、13、23、29张。牌量、顺序任务和布局范围递增；0数字仍需消除图案牌并点结束牌。
- 保留旧level 0/1/2/3分别对应4/5/6/7数字，追加level 4/5/6/7对应0/1/2/3数字。`GenerateInfo.atLeast`统一比较实际数字数；纸笔使用`toolLevel`，入门版和旧初级均需0级纸笔，旧高档要求不变。界面与旧线索编辑器展示数字数。
- `ResearchGame`逆向摆放后回放到中央结束牌；最多64次随机尝试，之后用对称角点剥离构造可解布局。`initWithSolution`供调试读取合法消除序列，保留原`void init`接口，不存储或下发答案。两张牌的位置必须不同。
- 批量测试暴露旧复制构造函数未初始化Card数组的问题：`ResearchGame(ResearchGame)`补上`this()`。原先生成试解会因此抛异常并被旧catch吞掉，无法作为可解性证明。
- 内置`frostedresearch:matching_debug`想法、项目、finding；无自然触发或联络配方，使用`/knowledge grant idea frostedresearch:matching_debug`主动开启。项目八个可选任务覆盖0–7数字，每档16局；可重新排列，放弃重开可清零。finding无生产解锁。
- 更新[连连看与调试路线](../docs/research/gameplay-and-integrations.md#theory-card-game)、[旧定义](../docs/research/definitions-and-codecs.md)、[知识定义](../docs/knowledge/definitions.md)及[测试说明](../src/gametest/README.md)。无TWR源码、build.gradle、gradle.properties或design/改动。

## Decisions

- 难度ID保持兼容，避免修改已有研究JSON、纸笔配方或迁移已保存胜局。内容作者按文档映射填level，不直接比较ID大小。
- 可解保证指初始局面存在合法通关序列；不保证任意误配后仍有解。随机摆放失败由内部备用布局处理。
- 调试研究只使用现有项目、演算和权限入口，不新增专用调试网络协议。

## Validation

- 首轮5项GameTest中3项失败：2项定位到真实的复制构造函数错误；绘图台夹具缺少队伍，改用既有`SinglePlayerTeamAPIProvider`夹具并在finally恢复。原日志`build/research-matching-tests-20261117.log`保留。
- 最终Java17 `runGameTestServer -PgameTestNamespaces=frostedresearch_matching -PgameTestDirectory=build/research-matching-gametest-20261117 -PthermalTestHeap=4G -PthermalTestProcessors=4 --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过，明确`All 5 required tests passed`，3m59s；日志`build/research-matching-tests-20261117-final.log`。
- 每档256种子，共2048局，逐步校验合法普通操作、数字集合、严格递增牌量、NBT恢复及结束牌，665ms完成批量回放，未用forceWin。另测8档强制备用布局、完整难度比较矩阵、旧纸笔等级与新线索codec范围。
- 正式调试JSON经过codec和定义引用校验；检查命令前隐藏、8档各16次胜利、存档后的胜局计数。实际绘图台以石质压力板和炭笔开0–4数字局、消耗纸笔、正常消除及任务计数通过；5–7数字以基础工具拒绝开局且不消耗纸。
- `jar reobfJar validateResearchCatalog -PresearchCatalogDir=../TheWinterRescue/config/fhresearches`通过，81个旧研究定义有效，48s。日志`build/research-matching-build-20261117.log`。
- Playtest未运行时替换唯一FH生产JAR，逐字节核对；生产包包含三个调试定义且不含GameTest/ClientValidation类。备份`build/prism-backups/2026-11-17_12-27-47-matching/`，含manifest；未启动本轮客户端，配置、存档及TWR文件未变。
- 仅提交本轮FH文件，保留FH原`.gitignore`和其它未跟踪内容、TWR原暂存`config/.gitignore`；不合入或推送主分支。

## Remaining

- 本轮通过服务端与真实绘图台方法验证；客户端页面视觉、鼠标操作和主观难度仍待玩家检查。不同种子不保证人类解题耗时严格排序。
