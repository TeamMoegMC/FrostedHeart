# 冰洞第二版本地提交与 Prism 部署

- Time: `2026-10-22 13:52:44 +08:00`
- Author: `OpenAI Codex；主代理收尾与部署，GPT-6.1 Sol 子代理实现及只读复核`
- Status: `completed`（本地测试版本部署完成，最终玩家视觉与长期玩法验收仍开放）
- Scope: FH / TWR `ice-caves`；Prism `The-Winter-Rescue-Development`

## Completed

- 第二版增加整区块洞底/岩壁/高墙/洞顶覆冰、普通地下湖完整冰盖、壁内团状冰、上下大型冰体、小型实体冰刺、完整干湿薄冰陷阱，以及后置晶体和更密的洞顶冰锥；减少矿量和直接露矿候选。
- 最后一处陷阱修正允许挖除盖层及空腔内的矿格，外侧支撑和坑底矿格仍保留；本轮只完成测试、记录与部署，不继续调景观参数。
- TWR 13 个冰洞文件提交为 `a5c8dc441c39ffbafbaf7e17adae946a5d9efc75`，从本地源库 fetch 后快进同步至 `D:/games/minecraft/prismlauncher/instances/The-Winter-Rescue-Development/.minecraft`。
- 新的 `frostedheart-1.20.1-0.7.7.jar` 已替换实例内旧 JAR，大小 `13,769,042` 字节，复制后逐字节一致；验证包含五个新增 feature 类，未混入 GameTest 客户端。
- 旧 JAR 保存于 `build/prism-backups/2026-10-22_13-52-44/`；部署记录位于 `build/ice-caves-v2-deployment-result.json`。
- 文档影响：更新 [冰洞当前实现](../docs/worldgen/ice-caves.md)、世界生成入口和冰锥生成说明，保存最终条件采样与较早正常世界截图，保留首版总结作为历史快照。新版计划维持进行中以跟踪玩家验收。

## Decisions

- 两库继续使用 `ice-caves`；仅选择本功能源码、数据、验证工具及说明文档提交。TWR 既有 11 个暂存文件的补丁保持不变，实例两个 FrostedHeart 配置文件的本地修改保持不变；没有 push。
- 正常世界截图来自最终陷阱/湖比例修正前的一轮，文档明确其参数和标签环境差异。用户请求立即迁移用于观察，本轮不再追加完整截图轮次，不将较早截图冒充最终参数的视觉验收。
- 发现 Gradle 资源处理会把测试群系恢复为空模板，修正 opt-in 验证脚本为在 JVM 启动前准备整合包资源；第一次部署复测的自然采样废弃，不计入结论。临时资源已 cleanup。

## Validation

- `runGameTestServer` 启用四个定向 namespace：最终 **22/22** 通过；日志 `build/ice-caves-v2-deployment-final-tests.log`，运行 4m41s。
- 三种子固定群系条件样本，冰洞/热液最终总量比：黄铁矿 `0.623`、锌 `1.361`、铅 `1.231`、银 `0.708`；逐种子四矿均低于两倍。样本包括全局矿物及热液晶洞。
- 原可见面覆冰：洞底 `77.70%`、岩壁 `80.46%`、高墙 `80.52%`、洞顶 `79.52%`；101 格普通水面全部封冰，其中 76 格薄冰；自然样本检出一座完整全水陷阱，不能用一座样本验收 20% 自然频率。
- `jar` / `reobfJar` 成功，日志 `build/ice-caves-v2-deployment-jar.log`，48s。TWR 冰洞 JSON 解析、feature 注册及引用核对通过；独立只读复核未发现部署阻断问题。
- 部署后实例整合包提交与源库一致，JAR 与本次构建逐字节一致，实例暂存/未暂存补丁均未变化。

## Remaining

- 用户在 Prism 中启动第二版，使用新世界或未生成区块观察；旧区块不会重新生成。最终正常世界的湖面比例、陷阱频率及干湿比例、大冰体和小冰刺观感、长期冻融及温泉体验仍待验收；温泉参数没有修改。
