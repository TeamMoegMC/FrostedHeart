# 红外视野在无温度覆盖区域没有开启反馈

- Time: `2026-11-05 15:55:54 +08:00`
- Author: `OpenAI Codex / GPT-6; primary investigation agent`
- Status: `completed`（原因定位；未修改红外行为）
- Scope: 红外按键、显示覆盖条件、实际整合包加载日志与已安装 JAR

## Completed

- 实际实例的 `key.frostedheart.infrared_view` 仍绑定 `I`，没有发现同键配置；LDLib 正常初始化，`FHKeyHandler` 和 `FHClientEvents` 已订阅，Oculus 日志明确没有启用光影包。
- 用 `javap` 核对已安装 JAR：`toggleInfraredView()` 切换 `open`；`clientTick()` 仅在 `open && displayHasCoverage` 时扩大扫描半径；`updateData()` 通过材料 `knownPresence` 或解析场 `knownFieldPages` 判断覆盖。
- 无覆盖时半径保持为零，`renderInfraredView()` 直接返回。开启动作没有额外状态提示，因此玩家看到的表现是按键没有反应。
- 只读检查最近存档的红外窗口对应 81 个已保存区块，没有发现 `FrostedHeartThermal` 记录，也没有在窗口高度内发现篝火或供暖设备。此检查描述磁盘快照，不代表任意后续实时状态。
- 用户随后确认：没有篝火时无法显示红外视野；该现象与无覆盖条件吻合。篝火使材料模拟产生首批可显示数据，并非按键处理器直接检查篝火。

## Decisions

- 本次请求为原因定位，保留当前生产代码、实例配置、模组 JAR 与存档；未运行构建或操作游戏输入。
- Documentation impact: [世界气候与温度](../docs/climate/world-climate-and-temperature.md) 已明确说明整个窗口无覆盖时保留原场景，以及全窗口自然背景估计已删除。无需修改现行系统文档。
- 配套整合包仓库和实际实例均没有写入变更。

## Validation

- 对照 `options.txt`、`logs/latest.log`、`logs/debug.log` 与已安装 JAR 字节码，核实按键注册、事件订阅、依赖加载和显示门槛。
- 只读解析 `level.dat` 与窗口对应区域文件，检查材料温度持久化标记、供暖相关方块状态及方块实体。
- 用户提供有无篝火时的实际游戏表现作为现场确认；本次没有独立启动客户端复现。

## Remaining

- 本次诊断已完成。无覆盖时是否显示扫描效果或提供开关状态反馈，属于尚未实施的交互调整。
