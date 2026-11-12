# M7 双真实客户端驱动与 Peer 实例准备

- Time: `2026-11-12 18:10:05 +08:00`
- Author: `Codex / OpenAI；客户端与协作验证者`
- Status: `partial`（源码与实例准备完成；等待主协调构建/启动窗口，尚未编译运行）
- Scope: 自有 src/m3c helper、任务专属 Peer 实例；使用固定 M4，不提前接入 M5 控温炉接口。

## Completed

- 新增 `src/m3c/java/com/teammoeg/frostedresearch/validation/M7ClientValidation.java`。入口 `M3ValidationMod` 优先按 `frostedresearch.m7Role=host|peer` 选择；既有 M3/M4 分支保留。现有 helper sourceSet/打包可收录新类，不改 build.gradle，不影响 M5 compileGameTestJava。
- 准备实例 `D:/games/minecraft/prismlauncher/instances/The-Winter-Rescue-Research-Peer-20261112`。仅复制固定 M4 的 mods/config/kubejs 与 instance.cfg、mmc-pack.json、twricon.png；其中 FH/helper 再从 build/m4-artifacts 覆盖。61 个 JAR、81 份旧研究 JSON；FTB Teams 为2001.3.0。**没有复制 saves，实例没有启动。**
- 原 Playtest 的 M3/M4 世界和证据保留，原 Development、共享资源索引、用户 Prism 18668 未操作。没有执行 Gradle 或 Git，没有启动第二客户端。

## Decisions

- Host 以 M7Host 创建全新集成世界并留服；Peer 以 M7Peer 真正连接、退出、重连。角色间 JSON 文件只协调动作，研究/权限事实取自服务器或各客户端实际同步状态。
- 原版 publishServer 会监听所有网卡并广播。测试 helper 使用原有 `ServerConnectionListener.startTcpServerListener(127.0.0.1,port)` 绑定选出的空闲端口，以离线身份模式运行，并设置集成服原有 publishedPort/publishedGameType 状态；不启动 LAN pinger。不是专用服、不是内存连接，也不声称使用原版 LAN 按钮。两个 Minecraft 字段通过 Forge ObfuscationReflectionHelper 使用 SRG `f_120017_` / `f_174966_`，已对本地映射只读核对，待构建运行验证。
- 使用实际 C2S 命令 `ftbteams party create M7Study`、`ftbteams party invite M7Peer`、`ftbteams party join M7Study`，检查真实 FTB Teams API、Chorda FTB provider 和双方 holder ID；先建同队，再准备知识和开始研究，不用测试 provider 或手动 merge 冒充协作。
- Host 实际旋转风道并推进 trial；Peer 用普通库存槽位 C2S 制作/放置，记录默认 EntityPlaceEvent，随后正常断线。Host 等待真实 PlayerLoggedOutEvent 和玩家列表离线，再吹气/新鲜观察/完成；Peer 重连检查 completed run、双永久权限及临时撤回。Host 最后才结束测试世界。
- 第二 run fixture 明确遗忘第一 run 的 Design，再真实开始、通关。双方发重复 expectedStage=airflow 推进，Peer 同时重发第一 run 的完成请求；检查第二 run 没跳阶段、成果获取次数仍为1。最后通过正常 C2S 放弃第二 run，保留首轮完成历史及全部证据。
- 材料、知识、平台和 Peer 免伤为 fixture；SURVIVAL 模式只为实际库存界面，不称生存通关。操作为 callback/C2S，不称物理键鼠。

## Validation

- 只读核对本地 FTB Teams 2001.3.0 API/命令类字节码、Minecraft ConnectScreen/IntegratedServer 接口与 SRG 映射；核对 Peer 目录只有 config/kubejs/mods，没有 saves。
- 没有编译或运行，因此本记录不宣称双客户端流程、FTB 命令、登录、字段映射或新驱动已经通过。
- 待窗口只构建辅助包，例如 Java17 下 `gradlew.bat m3cValidationJar -x compileJava -x processResources --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`。不得覆盖 build/m4-artifacts 的固定生产 JAR；新 helper 应另存 M7 产物并部署两实例。
- 运行时分别设 m7Role=host/peer，以已验证 Prism `--launch <实例ID> --offline M7Host|M7Peer` 启动。默认协调/证据目录为 Host `.minecraft/build/m7-session/session-<timestamp>/`，各角色独立 result、progress 和截图；M3/M4 证据目录不复用。

## Exact own file list

- `src/m3c/java/com/teammoeg/frostedresearch/validation/M7ClientValidation.java`
- `src/m3c/java/com/teammoeg/frostedresearch/validation/M3ValidationMod.java`
- `diary/2026-11-12_18-10-05_research-m7-peer-driver-preparation.md`

生产代码、正式数据、服务端测试、build.gradle、docs、design 均未编辑。文档影响：只有测试工具准备，行为文档无需更新；实际结论在后续验证日记补充，不改写本准备记录。

## Remaining

- 主协调提供构建与双客户端窗口；首次编译后检查重映射 owner，并实测上述流程。不得提前运行 Gradle或客户端。
- M5 UI 待接口稳定：从无想法的新队伍通过明确零输入联络候选及基准10秒等待取得耐火砖想法；项目出现但材料条件未满足；正常观察 fixture 高岭土/铝土矿/黏土物品、保留并在绘图台学习，再开始炉实验。此入门链不用直接 learn/produce 替代，不提前猜控温炉 API。
