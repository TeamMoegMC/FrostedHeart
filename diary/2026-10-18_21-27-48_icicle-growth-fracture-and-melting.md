# 冰锥生长、碎裂、相变与坠落

- Time: `2026-10-18 21:27:48 +08:00`
- Author: `OpenAI Codex；主实现与验证代理`
- Status: `completed`
- Scope: `FH 冰锥方块/资源/相变与 TWR cave/icicle 生成；不包含冰溜子`

## Completed

- 注册 `frostedheart:icicle` 方块/物品和同名 feature，接入既有五套模型/贴图、连接形态、中文名与精准采集限定掉落。
- 实现冰洞随机长根、按材料温度延长及低温倍率，自然最多八格，手动长链按真实长度断裂/缩短。预警链暂停延长。
- 实现环境热裂的两层概率、4–32 秒均匀预警、冰屑和薄冰最后一级裂纹声；vanilla scheduled ticks 保存倒计时，缺失计时器时后备 4–16 秒。
- 接入爆炸 32 格和敲钟 12 格的球形振动，完整段 5% 预警、已预警段 90% 立即断裂；palette 筛选已加载 section，同事件只按原状态抽一次。
- 5°C 材料相变每次按完整薄冰实际加热预算 /3 删除连续链末端。存活源段翻转 `melt_cycle`，复用现有 `GAMEPLAY_TRANSITION` 变更日志、迁移和 `APPLIED` ACK 结算，覆盖确认前保存窗口；真正缺失材料记录时先建潜热起点，不免费删除末端。
- 复用 vanilla falling-block 实体同步、渲染和 NBT；只有末端实体造成原版悬挂滴水石原始伤害的 1/3（上限 40/3），落地/入水碎掉，接触普通/灵魂篝火或火焰时熄灭，保留篝火和内容。
- TWR 仅修改 `kubejs/data/the_winter_rescue/worldgen/placed_feature/cave/icicle.json`：保留放置器并改用 FH feature。新地形生成冰锥，旧冰洞靠运行时生长补充，旧普通冰柱不追溯替换。该仓库及共同父目录未发现 AGENTS.md，改动前已检索相关注册与引用。
- 文档影响：新增[冰锥说明](../docs/climate/icicle.md)，更新气候 README、材料/相变和生命周期说明，完成[实施计划](../plans/2026-10-18_20-40-00_icicle-implementation.md)，向[讨论](../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md)追加结果。其他对话维护的世界温度/城镇文档与工作区已有改动均保留。

## Decisions

- 实施初值为热裂门槛 9°C、首层/次层基准各 1/8；根部基准 0.5%/随机刻，共用低温倍率；根部检查水平 ±2、竖直 ±1 格。连续温差，延长读尖端材料、长根读基底材料。
- 生长只检查群系、空间与材料温度，不增加供水或湿度模拟；没有每段常驻方块实体、独立计时 SavedData 或世界扫描。
- 使用一个不可见相变收据位复用现有保存/迁移机制，未扩展 thermal ACK 枚举或 solver 相态框架。新记录共享材料定义和同刻时间戳，保留各位置的精确 H 与实际年龄。
- 保留 30 个与冰锥无关的生成输出原样：27 个薄冰模型仅发生 JSON 字段排序变化，三份 hunter_caged 配方在命令级编码设置下出现中文重编码；均已撤回本轮生成差异，没有修改其生成逻辑。

## Validation

- Java 17 的 `compileGameTestJava runData --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false` 通过，日志 `build/icicle-data-verified.log`。首次 datagen 遇到本机 native 内存不足后，使用命令级 JVM 堆/线程上限重跑；后续保留 UTF-8 参数，未修改项目的持久 JVM 配置。
- 初次冰锥专项 13/13 通过。首次完整 124 项中，现有温度/薄冰 111 项全通过，两项冰锥生长夹具在不同摆放位置失败；核对 `BiomeManager.getBiome` 后，夹具覆盖邻近 quart 采样并增加真实群系与材料温度断言。
- 修正后完整回归：`runGameTestServer jar -PgameTestNamespaces=frostedheart,frostedheart_thin_ice,frostedheart_icicle --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`，124/124 required 通过，日志 `build/icicle-regression-verified.log`。
- 最后材料记录共享定义/时间戳调整后：`runGameTestServer jar -PgameTestNamespaces=frostedheart_icicle ...`，14/14 required 通过并重新生成发行包，日志 `build/icicle-final.log`。调整只涉及冰锥新增材料插入路径，未再次执行不使用该路径的 111 项旧用例。
- 最后两命令使用 `JAVA_HOME=C:/Program Files/Java/jdk-17.0.2`，`JAVA_TOOL_OPTIONS=-Xmx1536m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8`，Gradle 参数 `-Dorg.gradle.jvmargs=-Xmx1024m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8`。
- 覆盖断中段/顶部支撑、12 格手动长链、预警计时保存及替换、防同次振动双抽、球形范围、材料温度/群系生长、自然上限和根距、逐预算末端缩短/存档回读、真实 worker 迁移/重复 ACK、真实落地/入水/篝火接触、四类火源、真实伤害/NBT、精准采集、生成器与 129 位置记录的无损插入和压缩。
- 16×5 场景每次恰好生成 64 个下段实体，全部碎掉。三次单事件振动 6.3461/11.3001/14.1572 ms，60 步手动物理推进合计 10.476/21.5599/23.3841 ms；这是无客户端的局部场景，不等同整合包实际 TPS。
- 发行包 `build/libs/frostedheart-1.20.1-0.7.7.jar`（13698501 bytes，重混淆完成）；检查方块/feature 类、模型、掉落表、5°C 配方、中文名、Minecraft Mixin 映射和 source-set 内容通过，测试类及测试用冰洞群系未进入发行包。检查脚本 `build/verify-icicle-artifact.py`。
- FH 与 TWR 的 `git diff --check` 均通过；TWR 本轮只有上述 cave/icicle 文件变化，两个仓库的其他已有/并行改动保留。

## Remaining

- 客户端外观/声音/粒子、真实卸载重载体验和完整 TWR 新冰洞生成分布尚未人工试玩。
- 需要内测反馈生长速度、根部密度与伤害；大量同时坠落、多人与客户端网络/渲染性能仍需整合包实测。
