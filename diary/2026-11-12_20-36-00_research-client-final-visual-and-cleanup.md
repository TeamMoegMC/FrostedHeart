# 炉槽边框最终视觉复核与隔离实例整理

- Time: `2026-11-12 20:36:00 +08:00`
- Author: `Codex / OpenAI；客户端部署与验证助手`
- Status: `completed`
- Scope: 仅绘制修正后的普通客户端视觉复核，以及两个本任务隔离 Prism 实例的最终可用状态；不重跑已完成的 M5/M6 功能流程。

## Completed

- 按主协调交付换入最新 `frostedheart-1.20.1-0.7.10.jar`，**14907400 bytes**；相对 [完整功能验证](2026-11-12_20-12-00_research-m5-m6-client-validation.md) 的 FH `f36690072` / 14907337 bytes，仅 `ControlledFurnaceScreen.drawSlot` 绘制变化，增加 63 bytes。没有把新图冒称为新一轮完整功能测试。
- **移走验证 helper 后，以普通客户端运行。** JVM `15780` 实际 Java `17.0.2`、`java.home` 为所配置的 JDK17，筛选确认没有 `frostedresearch.*` 参数。正常主菜单选择保留的 `m6-brief-1794485184508` 世界；通过正常聊天传送命令调整夹具视角、真实右键炉体打开界面，以游戏 F2 保存原始 1280×800 PNG。
- 已实际核看：三工艺槽有清楚的内色和边框，全部 36 个背包格可辨认；不再是旧图的整块棕色。已有坯料、木炭、砖、1100°C 与 **400/400** 完成状态正常显示。没有新投料、改研究进度或为了截图伪造输出。
- [原生截图](../build/m5-m6-client/visual-final/01-furnace-slot-borders.png)、[视觉结果](../build/m5-m6-client/visual-final/result.json)、部署元数据与退出后的 latest.log 已归档到 `build/m5-m6-client/visual-final/`。原截图仍在 Playtest `.minecraft/screenshots/2026-11-12_20.31.29.png`，未修图。
- 普通测试客户端正常关闭，退出日志确认保存玩家、研究与世界，`m6-brief-1794485184508` 及所有维度 **All chunks / All dimensions are saved**；随后确认该 JVM 已退出。未操作用户原 Prism `18668`、原 Development 或共享资源缓存。

## Final instance state

仅以下两个本任务隔离实例：

| 实例目录 | 最终显示名 | JVM 附加参数 |
|---|---|---|
| `The-Winter-Rescue-Research-Playtest-20261112` | The Winter Rescue - Research Playtest (2026-11-12) | `-XX:ActiveProcessorCount=4` |
| `The-Winter-Rescue-Research-Peer-20261112` | The Winter Rescue - Research Peer (2026-11-12) | `-XX:ActiveProcessorCount=2` |

- 两边 FH 均 **14907400 bytes**，正式数据为 TWR **`3faf8096c`**；其中研究内容仍为 `aa076eac7`，新增 TreeChop 默认键行已同步到两边 `kubejs/config/defaultoptions.txt` 和最终数据快照。没有再启动 Peer 做双客户端测试。
- 两边的 `research-client-validation-m3c.jar` 均已从 `mods/` 移到各自 `.minecraft/build/validation-tools-archive/2026-11-12-final/`，旁边保留清理前 instance.cfg。原验证世界和全部原始证据仍在。
- 清除全部 M3/M4/M5/M6/M7 自动验证参数；不恢复之前曾被重置为 `Unnamed Instance` / 空参数覆盖的配置。保留明确显示名、`OverrideJavaLocation=true`、可用的 `C:/Program Files/Java/jdk-17.0.2/bin/javaw.exe`、关闭自动 Java 选择、覆盖内存为 **1GB 初始 / 4GB 最大**、1280×800 窗口。
- 两边既有 `options.txt` 均为 `key_treechop.key.open_settings_overlay:key.keyboard.unknown`，观察仍为 `key_key.frostedresearch.observe:key.keyboard.n`。不恢复 N 冲突，也没有通过覆盖玩家整个 options 来应用默认文件。
- 所有本节点测试 JVM 停止保存后再次复读：helper 不在 mods、自动参数不存在、名称/Java17/内存仍有效、两边 N 配置正确。用户以后打开这两个实例不会自动跑验证。

## Artifacts and validation boundary

- 最终可部署生产快照：[14907400-byte JAR](../build/m5-m6-client-artifacts/visual-final/frostedheart-1.20.1-0.7.10.jar)。入口元数据：[final-delivery.json](../build/m5-m6-client-artifacts/final-delivery.json)。
- **逻辑版本和视觉版本分别保留**：前者 `build/m5-m6-client-artifacts/frostedheart-1.20.1-0.7.10.jar` 是 14907337-byte 完整验证基线；后者在 `visual-final/`。不用一个同名覆盖文件模糊两份证据的版本。
- 完整 M5/M6 与任务书结果为 `session-1794484844402` 两份 passed，51 图；本条补一张普通客户端边框图。M7 两真实客户端证据仍见 [M7 日记](2026-11-12_19-04-00_research-m7-two-client-validation.md)。
- 本节点只整理运行实例、保存证据和新增本条日记；炉绘制源码修正由实现者交付。没有 Git 暂存、提交、推送，没有为小绘制变化重跑完整服务端或客户端逻辑流程。截图与源码相符，living docs 不因纯边框修正另增系统行为说明。

## Remaining

- 本客户端部署与验证任务无剩余项。主协调继续负责 helper 源码/日记的选择性提交与总体交付；完整生存材料链、物理人手输入体验和真实进行中队伍合并不在本条新增结论中。
