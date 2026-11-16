# 风道空气按空腔补生与粒子碰撞

- Time: `2026-11-16 23:39:00 +08:00`
- Author: `Codex / OpenAI；主代理实现与验证，6 Luna文档核对`
- Status: `completed`
- Scope: FH AirflowParticles、桌面粒子渲染、GameTest/M4助手及文档；Research Playtest JAR。

## Completed

- 粒子圆点保持1 GUI像素，碰撞半径0.24逻辑格，直径1.92 GUI像素；砂砖检测也使用实体半径。等质量粒子碰撞交换法向速度并分离重叠，固定2子步/每子步2轮修正，速度限0.45格/tick。渲染在tick间插值。
- 目标数ceil(所有OPEN/BED空腔面积×0.5粒/格)，包括独立开凿区与预挖炉腔。每tick最多补16粒；初始分布全空腔，新开凿后优先在入口或新区域补生，受限时选其它合法空腔。出生检查避免重叠和撞入砖；不再固定500tick消失，出界/失效位置补回，重置清空。
- 1格空间桶查邻近粒子，复用索引数组，不做全体两两扫描。新增targetCount/openCount/collisions/lastPairChecks用于测试和调参观察。粒子仍仅客户端视觉，不参与服务端通关与失败判定，不修改保存格式/研究数据。
- 更新阶段/风道说明和GameTest入口；无TWR源码、语言、配方、build.gradle或design/修改。

## Validation

- Java17 runGameTestServer，命名空间frostedresearch_m4，明确 `All 11 required tests passed`（3m47s），日志 `build/research-particles-20261116-tests.log`。覆盖半径对砖的提前碰撞、粒间弹性响应/重叠分离、独立空腔补生、数量比例、长时有限状态/邻近筛选，以及现有风道与阶段用例。
- 全开空腔（保护区保留）387粒，预热240次后连续1000次更新：平均901微秒/tick、末次3381对邻近检查。记录为本机计算耗时，不含渲染、不是整包FPS保证；不设依赖机器速度的测试断言。
- jar/reobfJar/m3cValidationJar通过（1m8s），日志 `build/research-particles-20261116-build.log`。生产JAR排除验证助手/GameTests。
- 正式Playtest新夹具世界result.json=passed/phase9：初始61粒目标，观察阶段累计462次接触修正；示例风道完成后90粒目标且实际补齐。冷粒子活动、两种冷风泄漏/重置、两笔正常通关、43格笔画退出补发/重开、临时原型权限与返回原任务页仍通过。
- 人工查看03初始和04通关画面，确认粒子分布扩展到预挖腔与新通道，密度适当、无旧入口堆积。证据：`C:/Users/chang/.codex/visualizations/2026/11/08/01a1ba78-b37a-7762-9426-964f95a6aee3/research-airflow-particles-20261116/build/m4-client/`。
- Playtest仅同步FH JAR并逐字节核对，备份 `build/prism-backups/2026-11-16-airflow-particles/`；游戏正常保存退出，临时助手/启用文件移除，JvmArgs原值保留。未操作用户已有世界或Development/runClient。
- 仅提交本轮FH文件，不push，保留FH .gitignore与TWR原暂存config/.gitignore等无关改动。

## Remaining

- 本轮为计算/客户端事件及夹具验收；完整生存、物理鼠标、新双客户端或不同硬件帧率未实测。
