# M5 完整入口与 M6 多记录的最终客户端验证

- Time: `2026-11-12 20:12:00 +08:00`
- Author: `Codex / OpenAI；客户端部署与验证助手`
- Status: `completed`（本条为功能与教程验收；炉槽边框视觉小补丁及最终实例整理另记）
- Scope: 最终 M6 生产包上的完整 M5 耐火砖流程、同一 Minecraft JVM 中的 M6 简短回归、FTB 实际章节与双语界面。没有运行用户 Development 实例、修改其存档或处理 Prism 外网。

## Completed

- **完整 M5 入口未直接授予想法或材料观察。** 新隔离世界、空队伍，从绘图台档案页选择零输入 `the_winter_rescue:refractory_firing` 联络候选，实际计时 **200 tick**，观察到从启动至获取想法 **201 个已加载 tick**。随后项目可见，三项材料观察均未满足，不能开始。
- 对提供的高岭土、铝土矿、黏土球逐一使用真实客户端 N 观察：定向 Windows 按键消息进入 `ObservationClient`，经正常服务端计时出现 `ObservationRecordScreen`；点击真实保留按钮，再正常打开绘图台、选择实际入件记录、点击学习。三项都成为有效档案，开始条件全部满足。保留 preview、inbox、learned 三组图片和真实记录 UUID。
- 正常开始项目，准备阶段手动进入烧制。进入前没有砖参考，显示默认提示；进入后对夹具提供的普通砖进行 N 观察、保留、学习，提示仍冻结为默认，且无试验绑定的普通砖没有满足任务。
- 在真实生存模式的工作台 3×3 菜单，通过正常槽位 C2S 填入九项材料、取出手工耐火黏土球和控温实验炉。炉体正常放置并使用。材料由夹具提供，不宣称完成采矿、解冻、手锤、磨粉的生存链。
- 成果前：实际炉 UI 拒绝批量生产并保留投入；JEI 的真实 recipe lookup 只返回试验，生产配方已注册但隐藏。通过正常 JEI 类别界面截图，不仅查投影布尔值。
- 实际 UI 启动试验，**400 tick / 本炉 1100°C**；运行中加 50 只把下一炉改为 1150，本炉保持 1100。实际产出一块带当前队伍/run 绑定的完好普通砖。用 N 再取得真实试样预览，丢弃记录而不消耗砖，任务就绪后正常手动完成。实际 `ResearchHooks.canUseRecipe(team, recipe)` 和客户端权限均可用，首轮成果事件数为 **1**。
- 成果后：JEI 同时返回试验与生产；正常投料、模式与启动按钮执行 **600 tick / 1150°C** 批量，真实输出 **16** 块普通砖且无试验绑定。没有用直接写输出替代生产。
- 为验证进入前参考分支，**显式遗忘成果作为测试夹具**；沿 UI 启动第二条合法研究，进入烧制时已有此前正常学入的砖参考，显示 **1050–1150°C（含端点）** 精确提示。第二次真实烧制、N 新鲜观察、不消耗样品并手动完成；两条正常完成记录共两次成果事件，每条一次。
- 随后在**同一 JVM 新建另一隔离世界**执行 M6：起始知识、材料和两条历史为明确构造的夹具。客户端显示同项目记录 1/2，真实上一条/下一条按钮可切换；炉有两个目标时要求明确选择，依次显示记录 1 和 2。实际 CUI 的新 `ControlledFurnaceStartPacket` 启动 1100°C / 400 tick 试验，服务端批次与实际产出的砖均绑定所选记录 2。
- M6 简短流程经正常计时的直接 `HELD_ITEM` C2S 采样，仅使记录 2 就绪且不消耗砖；这段**不声称出现按键预览**，与前述 M5 的真实 N 预览路径区分。正常完成记录 2 后，记录 1 保留为 standby，生产权限可用，成果事件只有 1。不是一次真实队伍合并测试；真实两客户端组队/退出/重连证据另见 [M7 日记](2026-11-12_19-04-00_research-m7-two-client-validation.md)。
- 通过 FTB 的实际 `ClientQuestFile` 核对章节 `4A8E1F92C73B605D` 已加载、位于既有 rebuild 组、在正常可见列表中且五卡可见。实际打开章节和全部五张中文卡，最后正常英文资源重载并打开耐火砖卡；中英文截图均已核看、可读。没有点击 checkmark、清除任务进度或用任务书奖励推进研究。

## Decisions

- 功能验证版本为 **FH `f36690072`**，生产 JAR `frostedheart-1.20.1-0.7.10.jar`，**14907337 bytes**。最终数据标记 **TWR `3faf8096c`**；研究内容来自 `aa076eac7`，期间仅补入 `kubejs/config/defaultoptions.txt` 的 TreeChop N 解绑行，无需重跑其余研究数据。
- 最终通过的 helper 为 **119804 bytes**，独立于生产 JAR。只编译/重映射 `m3cValidationJar`，跳过 main、processResources、GameTest 编译；未重编生产以修助手。
- 普通客户端 N 与 TreeChop 0.19.0 设置键的冲突在此前真实运行暴露。Playtest 的现有 options 已明确将 `treechop.key.open_settings_overlay` 设为 unknown，保留 N 观察；主协调已在 TWR 默认配置与旧配置说明中处理。现有 options 不会被默认文件覆盖，最终不恢复这项冲突。
- 使用的是定向窗口输入、真实按钮回调、正常 C2S、实际服务器加载 tick 和渲染截图，不是物理人手输入证明。世界地面、绘图台/工作台、无敌状态、材料、进入后参考砖、第二轮遗忘与 M6 两历史均为测试夹具；本轮没有声称生存通关、崩溃恢复或真实中途队伍合并。

## Validation

- 原始通过目录：`D:/games/minecraft/prismlauncher/instances/The-Winter-Rescue-Research-Playtest-20261112/.minecraft/build/m5-m6-client/session-1794484844402/`。
- 已归档 [完整 result.json](../build/m5-m6-client/session-1794484844402/result.json)、[M6 result.json](../build/m5-m6-client/session-1794484844402/m6-brief/result.json)、中途功能 checkpoint、最终 latest.log、部署元数据及 **51 张原始 1280×800 截图**到 `build/m5-m6-client/session-1794484844402/`。两份结果分别为 `passed / phase 44` 和 `passed / phase 20`。日志对应 `20:07:23 M6_CLIENT_PASSED`、`20:08:07 M5_CLIENT_PASSED`。
- M5 世界的两条合法研究：`ab6fd0cc-3ca4-497a-9083-3f24fd9b1a53`、`18f39438-08a6-4f13-975d-6425a6cb43b8`。M6 构造历史：`65b5ee7f-6211-4c18-ba54-85bbb68aaa5c`、`f539a33a-16f9-4dbd-a422-c5027b88c4b5`。M6 保存世界文件夹为 `m6-brief-1794485184508`。
- 图号：05/06/07 各自 preview/inbox/learned；10 默认提示、12 进入后冻结、14/15 实际工作台合成、16/16a 无成果炉与 JEI、17 当前/下一炉温度、18/19/20 实际试样与 N 预览、21a 解锁后 JEI、23 实际 16 砖、24 精确参考、26 章节、27 五张中文卡、28 英文卡；M6 子目录 01/02/03 序号切换，04/05/06 目标选择，07/08 启动和出样，09/10 仅所选就绪及备用。
- 输出图片抓取在真实槽位输出刚到达的帧，文本同步可能仍显示上一更新的计数；不把这帧文本冒充精确完成 tick 的独立证据。实际输出、启动配方时长和服务端绑定分别由运行断言检查。
- [最终 helper 构建日志](../build/m5-m6-helper-build-2.log)：**BUILD SUCCESSFUL in 54s**。重映射后 `getSlot` 接收者为原版 `AbstractContainerMenu.m_38853_`，不残留 `ControlledFurnaceMenu.getSlot`；[字节码记录](../build/m5-m6-client-artifacts/helper-javap.txt)。
- 某次配置阶段停在 ForgeGradle HTTPS 元数据读取；只读线程与属性核查发现含空格的 JVM 参数没有实际传入 daemon。取消自己的该次构建后，使用本次 PowerShell 的 `JAVA_TOOL_OPTIONS` 传入 UTF-8 与 connect/read 各 10000ms，并用 jcmd 只筛两项超时确认实际生效，再完成上面的构建。不修改全局 JVM 配置、Prism 网络或共享缓存。

## Earlier attempts retained

- `build/m5-client/session-1794481810833`：固定 M5 老包的初轮，仅到资源/世界加载；静态发现 helper 的继承 `getSlot` 未重映射后正常退出。没有入口玩法通过结论。
- `build/m5-m6-client/session-1794483271091`：真实 201 tick 联络与三项未满足条件通过，但 N 打开 TreeChop 设置，phase 602 超时；不算材料观察通过。
- `build/m5-m6-client/session-1794483614602`：只是预期的输出路径。该 JVM 实际没有 `frostedresearch.*` 属性，实例参数覆盖为 false；提示页通过后仍是普通主菜单，未启动 helper、未取得玩法结论。归档 operator-partial、日志与 helper 资源信息警告图，没有伪造 runtime result。
- `build/m5-m6-client/session-1794484089425`：JVM 参数已实际验证，N 高岭土预览及保留成功；helper 604 对屏幕与 inbox 的等待顺序可形成循环等待，正常退出。修为先正常打开绘图台，再等快照；M6 phase 15 同类顺序一并修正。没有修改生产来迎合测试。
- 失败、partial 和通过目录分别保留，不用后轮结果改写前轮结果。最终重新写入已验证的带引号参数与覆盖项，并在 JVM 创建后立即只筛 `frostedresearch.*` 核对，避免无参数空跑。

## Exact helper source ownership

- `src/m3c/java/com/teammoeg/frostedresearch/validation/M3ValidationMod.java`：M3/M4/M5/M6/M7 选择入口。
- `src/m3c/java/com/teammoeg/frostedresearch/validation/M4ClientValidation.java`：先前 M4 客户端路线，本轮未为功能新改。
- `src/m3c/java/com/teammoeg/frostedresearch/validation/M5ClientValidation.java`：完整入口、合成、炉、参考、JEI、FTB 和同 JVM 衔接。
- `src/m3c/java/com/teammoeg/frostedresearch/validation/M6ClientValidation.java`：多记录与新 CUI 启动的简短回归。
- `src/m3c/java/com/teammoeg/frostedresearch/validation/M7ClientValidation.java`：两真实客户端路线，join 已改为邀请者 `M7Host`；先前运行中的聊天纠正另有原始证据。

本节点未修改生产源码、正式研究数据、build.gradle 或 design；没有 Git 暂存、提交、推送。功能行为未因助手修正而变化，living docs 的按键说明由主协调维护，本条只记录客户端验收证据。

## Remaining

- 主协调的炉槽边框纯绘制小补丁：新 JAR 上正常打开保留世界炉界面做一次视觉复核，无需重跑本条功能流程。
- 所有测试结束后，将两个隔离实例的验证 helper 移入证据/备份目录，清理自动验证参数并复读；保留可用 Java17、内存、明确实例名称、修正后的 N 与所有验证世界。另建日记记录最终产物与整理结果，不覆盖本条逻辑版本证据。
