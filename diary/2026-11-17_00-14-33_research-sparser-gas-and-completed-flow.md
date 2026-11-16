# 风道粒子减量、通风前空炉腔与通关定向气流

- Time: `2026-11-17 00:14:33 +08:00`
- Author: `Codex / OpenAI；主代理实现与验证`
- Status: `completed`
- Scope: FH风道粒子、GameTest/M4客户端助手、知识文档及Research Playtest JAR。

## Completed

- `AirflowParticles.DENSITY` 从0.5改为0.25粒/格，向上取整，理论上限196粒。扩大碰撞半径、未通关时的随机运动/碰撞、入口与新开凿处补生延续。
- 按用户后续要求，通关前不在 `AirflowGame.furnaceCavity` 标识的炭床及上方预挖炉腔生成随机空气，也不允许附近粒子游走进入；这部分不计入未通关可充气面积。入口旁23格对应6粒，玩家新挖的独立区域仍可补生。炭床保持阴燃闪烁。
- 成功后使用既有通关路径，粒子均匀分布并以0.25格/tick沿入口→炭床→出口循环，停止随机转向和粒间碰撞。经过炭床变暖，回入口变冷；回入口时同步插值位置，避免跨图拖影。示例通关45粒，重开已通关盘面直接恢复定向展示。
- 历史已记成功但新规则下无有效路径的盘面保留成功，不伪造穿砖路线。粒子状态不入NBT，通关判定与存档格式不变。
- 更新 [阶段与风道](../docs/knowledge/stages-and-airflow.md) 和 [测试说明](../src/gametest/README.md)。无TWR源码、build.gradle或design/修改。

## Decisions

- 复用 `AirflowGame.path()` 的通关路线，客户端只做展示；通关后无需运行随机碰撞求解器。
- 用户要求预挖炉腔空白后，其面积只在通关展示中计入；因此初始6粒不能直接与上一版包含炉腔的61粒比较，密度本身已减半。

## Validation

- 最终Java17 `runGameTestServer`，命名空间 `frostedresearch_m4`：明确 `All 12 required tests passed`，3m32s，日志 `build/research-fixed-flow-20261117-tests.log`。覆盖密度、未通风炉腔排除、随机碰撞、固定路线与热色、循环无跨图插值、完成盘面重开和重置。
- 大空腔随机模式169粒，1000次更新平均233微秒/tick、末次718对邻近检查。仅为本机计算测量，不含渲染或整包帧率，不设机器速度断言。
- `jar reobfJar m3cValidationJar` 通过，51s，日志 `build/research-fixed-flow-20261117-build.log`。
- 首次客户端检查在phase6超时，尚未向服务端挖出格子；确认CUI悬停缓存取决于先前真实鼠标位置。助手改为在真实Screen点击前调用正常的坐标/悬停刷新，未修改共享CUI或绕过Canvas/网络。首次证据单独保存在 `build/m4-client-first-helper-attempt/`，不算通过。
- 最终正式Playtest新夹具世界 `result.json=passed/phase9`：初始6粒、19次接触修正、预挖炉腔无粒子；通关45粒全部沿有效路线且继续移动；冷热颜色、两种漏气与重置、43格笔画退出补发/重开、临时原型权限、Esc/按钮返回原任务页通过。
- 人工查看初始和成功截图：上方预挖炉腔初始空白，通关后冷/暖粒子沿风道排列。证据根目录：`C:/Users/chang/.codex/visualizations/2026/11/08/01a1ba78-b37a-7762-9426-964f95a6aee3/research-airflow-fixed-flow-20261116/build/m4-client/`。
- Playtest仅替换FH生产JAR并逐字节核对；备份 `build/prism-backups/2026-11-16-fixed-flow/`，manifest记录最终文件。客户端正常退出，临时助手和启用文件已移除，原JvmArgs保留。
- 仅提交本轮FH文件，不push；保留FH .gitignore、TWR原暂存config/.gitignore及其它无关工作。

## Remaining

- 本轮为定向服务端与真实Screen事件/C2S夹具验收；不代表物理鼠标、完整生存、新双客户端或不同硬件帧率验证。
