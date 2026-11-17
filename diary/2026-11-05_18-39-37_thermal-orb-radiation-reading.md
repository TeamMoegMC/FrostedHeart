# 温度球直接显示空气与辐射的综合供暖参考

- Time: `2026-11-05 18:39:37 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed` for implementation and focused validation; client visual inspection not performed
- Scope: `FrostedHud`、玩家观测/同步、`FHBodyDataSyncPacket`、现有辐射折算说明、相关Forge用例和气候文档

## Completed

- 按用户澄清，温度球的原数字直接显示“采样空气＋辐射折算增温”，球色取同一Celsius值；复用现有 `RadiantEquivalentTemperature` 的 `q * 0.8 / 6.0`，没有新增图标、面板、模型或Mixin。
- `getDisplayTemperatureC()` 只做已有样本的固定运算；不改空气getter、身体能量或直接伤害输入，不额外查询世界/发射射线。衣物不进入显示换算，避免穿衣放大显示温度。
- 现有身体包增加float辐射，版本2、载荷9字节。变化判断覆盖空气、核心和0.1°C合成读数；辐射单独变化和归零均可同步。死亡/重置/读盘清掉旧辐射。
- 修复原数字整数位提前四舍五入的问题，防止24.7被画成25.7；三位显示饱和至所选单位999，防止高值只画末三位。
- Documentation impact: 更新[玩家温度](../docs/climate/player-temperature.md)、[网络与生命周期](../docs/climate/data-lifecycle-and-integration.md)、[旧玩法对照](../docs/climate/thermal-legacy-gameplay-parity.md)和[数值测量](../docs/climate/thermal-gameplay-balance.md)的当前/历史显示口径。没有编辑城镇、研究、世界生成、整合包配置或`design/`。

## Decisions

- 显示是固定供暖参考，不把显示的合成°C再当真实空气/皮肤温度用于伤害，也不声称它等于实际裸体身体平衡点。
- 直接复用已有float辐射与packet，避免另存一套折算缓存或再设计一套压缩格式。默认20tick周期、20TPS下，100人每次都发送的新增包内容上限为400bytes/s，未计网络封装；这是理论载荷计算，不是百人实测。
- 客户端与服务端须一同更新此包；人物存档schema不变。只编译当前源码到独立输出，避免其他会话的构建覆盖，不复制历史工程或设置路径hash门禁。

## Validation

- Java17 `assemble compileGameTestJava -I build/thermal-orb-radiation.init.gradle` 通过，58s；日志 `build/thermal-orb-radiation-compile-20261105.log`。临时init沿用既有配置排除其他会话未完成的矿床测试；未编辑该子系统。
- 一次统一Forge运行 `frostedheart_player_release` **12/12通过**，日志 `build/thermal-orb-radiation-20261105-r1.log`，摘要 `build/thermal-orb-radiation-20261105-r1.json`。原9项身体/衣物/伤害/装备/存档回归，加3项显示、协议与清零用例；真实篝火病例亦断言合成读数高于空气。
- 验证12°C空气、95.5W/m²辐射在客户端数据得到24.733333°C；75/150W/m²分别得到22/32°C，空气、核心和实际暴露不被显示换算改写。辐射单独变化/归零、重复抑制、强制同步、packet编码解码、客户端归零、死亡/重置/重载均通过。
- 热运行时错误0、退出码0、最终关闭 `rootBytes=0`。未运行JUnit、长时间数值矩阵或百人压力测试。HUD绘制源码已编译/复查，没有启动客户端截图核验。
- 生产JAR已生成于 `build/thermal-orb-radiation-20261105/libs/`；属于共享工作区当前产物，包含其他原有改动，没有自动安装、提交或发布。

## Remaining

- 客户端实际画面尚未核验；本次编译与服务端协议/玩法检查不能代替该证据。
- [旧玩法生存/资源数值差距](../docs/climate/thermal-gameplay-balance.md)仍存在；显示修正不构成该数值目标通过。
