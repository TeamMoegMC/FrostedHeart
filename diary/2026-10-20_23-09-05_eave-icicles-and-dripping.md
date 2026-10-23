# 房檐冰溜子、融化滴水与冰屑外圈

- Time: `2026-10-20 23:09:05 +08:00`
- Author: `OpenAI Codex；实现与验证代理`
- Status: `completed`
- Scope: `frostedheart:eave_icicles`、屋顶生长抽样、坠落/振动、玩家滴水潮湿、冰锥预警粒子

## Completed

- 新增 `EaveIcicleBlock`、方块/物品注册、四个外缘方向 multipart、每侧五根细锥的模型、语言、精准采集 loot 与 2°C/126666.6666666667 J 通用相变数据。复用既有 `icicle_tip` 纹理，不新增位图。
- 自然生长不限群系/区块年龄，需屋顶露天、完整底面、外缘空气、当前降水或积雪及屋顶材料 ≤0°C；共享地表列抽样按频率归一为根部 0.5%/随机刻及低温倍率，允许沿檐连续排列。对普通地面/水列先筛选，不额外消耗既有天气随机序列。手动放置只检查支撑。
- 冰溜子热裂直接坠落；爆炸/敲钟沿用 32/12 格球形扫描及 5% 完整段响应，不使用预警分支。支撑丢失次 tick 坠落。复用原版坠落实体与已有碰水/落地碎裂、灭火、保存和客户端渲染；扫过目标时造成一次固定 0.5 点伤害后碎掉，不额外击退，不随高度放大。
- 新增 `IceDripping`，材料达到 2°C 即可滴水；长冰锥只从末端排出，允许上段暖/末端冷。每 20 ticks 玩家附近已加载 section 低频查找，palette 预筛、出口去重与 50% 抽样，平均 2 秒一次；服务端不创建逐滴实体或方块计时记录。
- 滴水向下射线最多 16 格，实际碰撞形状/液体遮挡，只对路径上首位玩家给予/刷新最低等级潮湿 100 ticks，不缩短更长效果。头部任意头盔或实际佩戴具有正防水值的帽子免疫此次滴水，其他部位无效。
- 冰锥预警 `FALLING_DUST` 从本段 x/z=0.06/0.94 的外圈落下，绕开下方锥体，保留开裂位置与正常粒子地形碰撞。
- 文档影响：新增[冰溜子与滴水](../docs/climate/eave-icicles.md)，更新[气候入口](../docs/climate/README.md)、[冰锥](../docs/climate/icicle.md)，追加[讨论](../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md)，完成[计划](../plans/2026-10-20_17-23-37_eave-icicles-and-melt-drips.md)。

## Decisions

- 按用户最后要求，滴水不读取潜热进度，伤害用于反馈、不要求额外击退；本轮温度 `thermal/` 路径没有代码变更，仅调用既有 `MinecraftPhaseController.temperature` 并补上共用地表抽样的一处玩法调用，没有扩展求解、编译、ACK、休眠或存档结构。
- 更正早期半砖/楼梯建议并得到用户确认：按实际完整 `Direction.DOWN` 支撑面，支持下半砖与普通楼梯，不偏移适配上半砖/倒置楼梯。
- 低成本近似：滴水服务端命中在抽样时即时结算，客户端发原版粒子，不逐滴同步下落时间；玩家附近水平 ±8 格的出口按需产生滴水。材料未知时遵守既有温度读取的 NaN/回退规则。
- 地表列抽样可处理旧区块；无需为所有屋顶注册随机刻或保存建筑索引，屋顶改变会在后续抽样中自然生效。
- 本轮只修改 FH；TWR 仓库已搜索相关标识，未更改整合包数据。没有暂存、提交或打包 JAR。

## Validation

- Java 17 的 `runGameTestServer -PgameTestNamespaces=frostedheart,frostedheart_thin_ice,frostedheart_icicle --offline --no-daemon --max-workers=2 --console=plain -Dnet.minecraftforge.gradle.check.certs=false`：2026-10-20 18:01:37 **130/130 required tests 通过**，Gradle `BUILD SUCCESSFUL in 9m 1s`，日志 `build/eave-icicles-regression-final.log`。生产与 GameTest 源码编译通过。
- 新增六项 GameTest 验证房檐实际形状/覆盖、积雪供水、材料温度驱动的非洞穴自然生长、立即坠落/支撑丢失/固定 0.5 点单次伤害、普通相变删除/精准采集、暖上段冷末端滴水、5 秒潮湿/更长效果保留、头盔与微量防水帽免疫、任意头槽物品不能冒充头盔、16 格射线及平台遮挡。原有温度/薄冰/冰锥 124 项同时通过。
- 初轮测试失败原因已处理：测试模板只清空 20 格高，屋顶上方仍有地形，夹具现清空其列至高度图表面；FakePlayer 本来不附加 FH 衣物能力，改用正常 mock survival player；生长抽样先排除水/普通地面再消费随机数，旧地表结冰回归恢复通过。没有修改温度测试断言或阈值来回避失败。
- 资源 JSON 解析、四方向 multipart、十个模型面片坐标、翻译与两种冰的相变预算一致性检查通过。`git diff --check` 通过。
- 原有 16×5 冰锥爆发场景本轮观测振动 6.4571 ms、60 步手动物理推进 13.6894 ms；此项不是新增滴水/整合包 TPS 性能结论。
- 测试使用命令级 `JAVA_TOOL_OPTIONS=-Xmx1536m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8` 与 Gradle JVM 1 GiB，未修改构建配置。旧 JAR 仍为 2026-10-18 21:27:10 的产物，未包含本次实现。

## Remaining

- 客户端模型、外圈冰屑与滴水粒子观感，以及多人/大型房屋真实性能和生长速度需内测；本轮无客户端视觉验证，不将自动测试当作试玩结果。
