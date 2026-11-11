# 放大镜与灵活物品观察

- Time: `2026-11-09 23:06:02 +08:00`
- Author: `Codex; OpenAI GPT-6; 主实现与验证。GPT-6 Luna; 物品注册、语言和像素图标。`
- Status: `completed`
- Scope: `frostedresearch` 观察输入、服务端会话、容器覆盖层、物品与资源、观察编码、GameTest 与客户端验收。

## Completed

- 按项目所有者授权落实[独立讨论稿](../discussion/2026-11-08_23-34-23_research-design-revision-draft.md)中的观察入口与放大镜方案，工作分支为 `research-261108`。
- 新增 `frostedresearch:magnifying_glass`：单件堆叠、无耐久/消耗、指定 tooltip、中英文名称、16×16 图标与模型。玻璃板、铜锭、木棍各一件无序合成。
- `ObservationClient` 支持掉落物射线选取，无世界目标时检视主手/空主手时的副手，以及容器悬停检视。放大镜长按使用键检视世界或另一只手；光标拿放大镜在容器物品上长按右键检视，兼容 Shift。
- `ObservationSessions` 从实际手持、菜单槽或 `ItemEntity` 取样并锁定目标。所有物品入口沿用 `ItemRetrieval.observe`；普通观察 60 tick，放大镜物品观察 30 tick，方块和普通实体仍为 60 tick。目标、数量/NBT、快捷栏选择、工具、菜单变化会取消相应会话。
- `InventoryObservationOverlay` 保留原容器，以覆盖层显示进度和记录；隔离该次放大镜右键的放置/拖动/释放。创造背包映射到 `inventoryMenu` 的真实槽，排除创造目录条目。
- 修复观察的首次初始化依赖：先创建 `Observation.Source` 会通过外部 UUID 编码器触发 `Observation.CODEC`，反过来读取尚未初始化的 `Source.CODEC`。改用独立 `UuidCodecHolder` 共享 UUID 字符串编码，保持记录格式兼容。新进程中第一次直接观察物品已由服务端测试覆盖。
- Documentation impact：更新[观察与笔记](../docs/knowledge/observations-and-notes.md)、[知识入口](../docs/knowledge/README.md)、[操作路线](../docs/knowledge/playthrough.md)、[界面与交互](../docs/knowledge/workbench.md)、[测试入口](../src/gametest/README.md)，并在原讨论稿末尾追加实施记录。未改动 `design/`。

## Decisions

- 世界目标优先；无世界目标时，放大镜检视另一只手。半时长仅用于物品，掉落物按物品记录处理。
- 绘图台检视继续保留；学习用途、差异和理解规则沿用原逻辑。容器主动观察先预览，再由玩家保留或舍弃。
- 原版创造光标只在客户端维护，因此单独标识创造光标入口；服务端仍从玩家实际背包槽取目标。
- 队伍研究建筑与正式实验玩法留待后续设计。

## Validation

- 生产 `compileJava` 通过；资源 JSON 可解析，`git diff --check` 通过。
- Forge GameTest `frostedresearch_observation`：**2 项必需测试全部通过**。覆盖手持/槽位/掉落物共享采样、30/60 tick 完成时机、物品不消耗、换槽/改变堆叠/移走工具/关闭菜单/旧菜单 ID/笔记载体等中断或排除行为。
- `runObservationUiClient`：**11 条客户端模拟操作全部通过**，实际启动客户端和集成服务器、渲染并交换网络包。路线为主手 N、副手回退 N、主手放大镜检视副手、掉落物 N、箱子方块放大镜、Shift＋放大镜，以及普通/创造背包的 N 和光标放大镜（含普通背包 Shift＋右键）。断言生成预览、光标工具保留、舍弃后原容器仍打开。
- 已查看手持、普通背包和创造背包的记录截图。结果与截图保存在 `build/observation-ui-validation/`；服务端通过记录在 `build/observation-validation.log`，客户端最终日志在 `build/observation-client-validation.log`。
- 客户端验收使用正常 `KeyboardHandler` / `MouseHandler` 回调，并仅在该专用 run 中加载 `frostedresearch.observation-validation.mixins.json` 提供按住状态。当前桌面环境的 AWT Robot 和 Windows 注入均未把按键送到 GLFW；故这些结果**不代表物理 OS 按键验收**。普通客户端不加载该测试 Mixin，生产 JAR 不含 GameTest 源码。
- 全量 `compileGameTestJava` 仍受本次改动前的矿脉测试错误阻塞：`DepositClientValidation` 缺少 `stateCounts(int[])`，`DepositGameTests` 的 biome `ParameterList` 泛型不匹配，共四处。未改这些文件；本轮通过 `build/observation-gametest.init.gradle` 暂时排除这两份测试源码后运行目标检查。
- 本地离线 Gradle 调用临时使用 `-Dnet.minecraftforge.gradle.check.certs=false` 跳过卡住的 ForgeGradle 启动连接探测；未修改持久 Gradle 配置或资源下载行为。最终命令为：

```powershell
.\gradlew.bat runGameTestServer runObservationUiClient -I build/observation-gametest.init.gradle -x cleanResearchGameTestWorld -PgameTestNamespaces=frostedresearch_observation -PgameTestDirectory=run-observation-gametest -PthermalTestHeap=3G -PthermalTestProcessors=4 -Dnet.minecraftforge.gradle.check.certs=false --offline --console=plain
# 后续客户端驱动调整仅重跑 runObservationUiClient，最终通过。
```

- 临时测试世界已清理；保留检查日志与截图。客户端原 GUI 缩放、帧率、失焦暂停、观察键和使用键配置已恢复。
- 清理采用可恢复的归档：三个测试世界、服务端运行目录和早期输入驱动失败截图移入 `build/observation-validation-artifacts/`，已从普通游玩存档列表移出。
- 伴生 TWR 仓库仅只读查找注册/配方冲突，未修改；既有暂存 `config/.gitignore` 与 `.workbuddy/` 保留。未提交或推送。

## Remaining

- 用户实际键鼠操作，以及多人独立客户端的人工体验验收；具体手动路线已写入操作文档。
- 矿脉测试的既有编译错误不属于本轮实现范围。
