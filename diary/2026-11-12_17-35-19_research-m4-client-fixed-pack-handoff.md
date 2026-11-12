# M4 固定整合包客户端验证交接

- Time: `2026-11-12 17:35:19 +08:00`
- Author: `Codex / OpenAI；客户端驱动、固定构建与隔离 Prism 验证者`
- Status: `completed`（列明的客户端流程与截图核对完成；生存、物理输入及双客户端另验）
- Scope: 两阶段冶铁项目、风道、临时原型、真实风箱延迟动作、新鲜观察、最终权限与旧承接中文；仅自有辅助源文件、运行产物、已指定隔离实例及本日记。
- Source commits: 主协调告知本轮 M4 保存为 **FH `e8c9a109d` / TWR `2aee5753b`**，包含本轮已编译驱动。本节点没有执行 Git。

## Completed

- 新建独立包 `src/m3c/java/com/teammoeg/frostedresearch/validation/M4ClientValidation.java`；保留稳定 M3 驱动，避免把新文件放进正在编译的 GameTest 源集。
- 自有入口 `M3ValidationMod` 按 `frostedresearch.m4Validation` 注册 M4/M3。沿用现有 m3cValidation sourceSet/JAR，**本轮没有改 build.gradle**。
- 生产与辅助合并构建一次成功，没有为驱动改生产 API 或实现。固定生产 JAR 不含 GameTests、validation 测试类或测试 fixtures；辅助字节码核对中没有在 FH 自定义子类上调用未映射的原版继承方法。
- [build/m4-artifacts/](../build/m4-artifacts/) 保存固定生产 JAR `frostedheart-1.20.1-0.7.10.jar`（14861958 字节）、辅助 JAR `research-client-validation-m3c.jar`（57938 字节）、当时 TWR `kubejs/` 与 `config/fhresearches/` 完整数据副本及 helper javap 输出。
- 仅更新既有实例 `D:/games/minecraft/prismlauncher/instances/The-Winter-Rescue-Research-Playtest-20261112/.minecraft` 的 FH JAR/helper、kubejs 和 config/fhresearches。旧 M3 版本另存在该实例 `m4-backups/`，M3 原始结果、截图、世界均保留。
- 同步核对 iron_bloomery 的 airflow/trial 两阶段、prototype_tuyere/main_bloomery/sandy_tuyere 三稳定配方 ID；旧 config/fhresearches 保留 **81** 份 JSON。固定后立即通知主协调释放 Gradle；此后 M5 开始写源/数据，本节点没有再次同步工作树或调用 Gradle。

## Decisions

- 正式模式读取真实加载的 RecipeManager、知识 JSON 与 KubeJS；不注入替代配方、伪造通关或直接调用 tryBlow / observeResearchTarget。
- 想法、开始前置知识和配方材料由明确 fixture 准备。风道通过实际 JournalButton callback 发旋转 C2S，服务器判通关；阶段推进/完成也使用界面实际 callback。
- 原型通过玩家 inventoryMenu 的槽位 C2S 合成输出/取出断言，再使用世界放置 C2S，使原型正常 setPlacedBy 绑定 team/run/task。
- 风箱通过真实 `gameMode.useItemOn` 发世界交互 C2S；监听真实 RightClickBlock 和之后的正常 ServerTick，不直接调用 use/tick/blow/tryBlow，不手动分发测试 tick。
- 新鲜观察直接发送正常 `ObservationActionPacket.BLOCK` 并等待服务器正常采样计时；先观察无 proof 的普通风口作为对照，再观察本次转化目标。没有伪造观察记录，没有调用任务成功接口，也不要求保留或学习预览记录。
- fixture 暂停开场剧情。驱动另有现成 ImageScreenDialog 连续五次 Esc 关闭路径并记录是否触发；本轮没有出现 M3 那种全屏剧情中断或据此误判超时。

## Validation

### 构建与启动

Java 17，默认 M4 生产配置，**没有** `-Pm4CharcoalPitTests`：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat jar m3cValidationJar --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

[m4-client-build.log](../build/m4-client-build.log)：**BUILD SUCCESSFUL in 2m18s**，生产 jar/reobfJar 和辅助 compile/reobf 均完成。没有重跑主协调已验的服务端套件。

隔离 instance.cfg 临时设 `JvmArgs=-XX:ActiveProcessorCount=4 -Dfrostedresearch.m4Validation=true`；输出根目录使用默认工作目录，避免空格参数解析问题。启动命令：

```powershell
Start-Process -FilePath 'D:/games/minecraft/prismlauncher/prismlauncher.exe' -ArgumentList @('--launch','The-Winter-Rescue-Research-Playtest-20261112','--offline','M4Validation') -WindowStyle Hidden
```

实际环境：Minecraft 1.20.1、Forge 47.4.0、Charcoal Pit 3.12.0、JDK 17.0.2、4096 MB、4 processors。未手动操作共享 Prism 资源索引；未改原 Development 实例或玩家存档。

### 原始证据与结果

**原始证据没有移动，绝对目录：**

`D:/games/minecraft/prismlauncher/instances/The-Winter-Rescue-Research-Playtest-20261112/.minecraft/build/m4-client/`

- 原始 result.json：**6746 字节，17:32:08，status=passed、phase=29、mode=formal-loaded-pack**。
- 本仓已另复制 [build/m4-client/](../build/m4-client/)（结果、progress、60 张 PNG）；完整末轮日志为 [m4-client.log](../build/m4-client.log)。正常保存退出的世界为本实例 `saves/m4-client-1794475848414`，不据此声称独立进程恢复。
- 固定生产 JAR 与实际 mods 部署副本逐字节相同。收尾去掉 m4Validation 开关，仅保留 `JvmArgs=-XX:ActiveProcessorCount=4`，避免下次手动启动自动创建测试世界；固定 M4 JAR、数据及未启用的 helper 保留。

### 已通过的正式流程

1. **实际配方**：每种目标产物只有对应稳定配方，prototype/sandy_tuyere 为3材料 shapeless；main_bloomery 为4砂浆+4青铜板+1砂砖方块的 shaped；各产出1。三个目标初始都受锁定管理，没有同产物原生别名绕行。
2. **风道与阶段**：真实界面旋转 **10** 次后服务器通关；空纸笔库存可玩。就绪仍停留 airflow，原型立即临时开放，两永久配方仍锁。手动推进后进入 trial，未观察时完成按钮禁用。
3. **原型与实际风箱**：玩家槽位合成输出/取出断言通过，实际物品放置后绑定本队/本 run/observe_tuyere。真实风箱交互后**第10个服务端 tick**转为真实 charcoal_pit:sandy_tuyere，产生正确 proof；正常目标无 BE、无 FACING。试吹本身没有完成观察任务。驱动未建造或成型冶铁炉；未另做整片世界的炉体清点。
4. **新鲜观察**：无关联普通风口的正常采样没有完成任务、没有消耗有效 proof。本次目标的正常采样在服务器标记后 **61 tick**完成（含请求排队间隔，不宣称精确请求起点），proof 被消耗，trial 就绪。此后仍等待手动完成。
5. **最终权限**：完成后主冶铁炉与普通砂质鼓风口两配方同时允许，原型临时授权撤回。实际制作验证只涉及原型；两项最终产品没有在本轮分别手动合成。
6. **旧承接**：本新测试队设置 raw iron finished 后走正式 reconcile 与同步；旧列表状态、正文及旧入口导航均正常，中文为“旧研究承接完成”和既有解释，无 M3 的 rawkey。

### 实际查看的截图与视觉边界

- [03-airflow-board.png](../build/m4-client/03-airflow-board.png)、04：真实棋盘、旋转前后、成功提示、按钮禁用状态。
- [02-airflow-stage-scroll-7.png](../build/m4-client/02-airflow-stage-scroll-7.png)、[05-airflow-conclusion-and-advance-scroll-9.png](../build/m4-client/05-airflow-conclusion-and-advance-scroll-9.png)：阶段标题/目标/方法、通关结论、临时配方及进入下一阶段按钮。
- [06-trial-stage-scroll-9.png](../build/m4-client/06-trial-stage-scroll-9.png)、[11-trial-conclusions-and-finish-scroll-9.png](../build/m4-client/11-trial-conclusions-and-finish-scroll-9.png)：试吹方法、观察要求、两阶段结论、完成前后按钮状态。长文可滚动阅读。
- [08-bound-prototype-before-use.png](../build/m4-client/08-bound-prototype-before-use.png) 与 [10-fresh-bound-observation-completed.png](../build/m4-client/10-fresh-bound-observation-completed.png)：原型与正常砂质风口外观确实不同。09 拍摄时区块网格尚未刷新，虽顶端目标名称已变，不把09当作转化后外观证明。
- [13-chinese-legacy-completion.png](../build/m4-client/13-chinese-legacy-completion.png)、14、15：已逐张查看，旧承接列表与正文中文、旧 iron 档案及回到新项目导航正常，M3 rawkey 缺陷已在固定 M4 中复核修复。
- **07-real-prototype-crafting.png 自动切成创造物品栏**，没有可见2×2网格，不能称可见合成界面或物理拖拽验收。库存包/输出取出断言和后续真实放置均过，但此截图限制保留。
- 整包悬浮 HUD 仍覆盖左上部分书页标题；本轮所需正文、结论、棋盘和按钮可读。日志仍有整包原有的缺资源/JER村民注册等告警，流程通过不等同整包无告警。

## Exact own file list

- `src/m3c/java/com/teammoeg/frostedresearch/validation/M4ClientValidation.java`（已由主协调随 FH e8c9a109d 保存）
- `src/m3c/java/com/teammoeg/frostedresearch/validation/M3ValidationMod.java`（同上）
- `diary/2026-11-12_17-35-19_research-m4-client-fixed-pack-handoff.md`（本轮新增交接）

没有本节点对生产实现、M4服务端测试、build.gradle、TWR源文件、docs 或 design 的编辑。文档影响：本轮增加验证工具与证据，不改变系统行为，living docs 由 M4 实现者维护；本日记补充实际客户端范围。

## Remaining

- 真实物理键鼠、长按观察与预览界面、可见2×2合成界面、完整生存获取与两最终产品手工制作均未验。fixture 创建的创造世界不称生存通关。
- 同队双客户端尚未创建/启动。后续最小方案：两个离线身份 M4Host/M4Peer，连接独立新世界的本机离线测试服（便于放置者退出后服务器仍运行），经 FTB Teams 正常入同队；A通关/放置后退出，B吹气/新鲜观察，A重连核对阶段和权限；另发双方重复推进确认不连跳。不使用原玩家存档，不扩展全套自动化。
- 独立服务器重启、机械风箱、区块卸载等服务端证据由 [M4 实现交接](2026-11-12_17-27-52_research-m4-full-stage-tuyere-handoff.md) 单独记录，本客户端结果不冒充重复验证。
- M5 源与数据正在并行实施；本实例继续保留固定 M4，下一轮同步/构建须由主协调组织。
