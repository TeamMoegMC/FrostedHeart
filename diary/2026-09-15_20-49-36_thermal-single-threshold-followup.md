# 统一0.5°C单阈值动态补测

- Time: `2026-09-15 20:49:36 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `单阈值/单hot位图临时原型与动态测试；测试后恢复生产源码和编译产物`

## Completed

- 临时将ThermalDimensionEngine、WorkerPageStore、QueryPublication改成单参数0.5、单份hot位图，移除旧阈值选择/finish交换；同时应用已验证的材料邻居保留修复。没有增加双参数兼容重载。
- 正负0.49/0.50/0.51°C升降序列40次检查通过，并验证驻留收集后仍可读取当次hot位图。
- 六组动态场景（露天、房间、四组分散，各两种摆放）完成600秒加热+3000秒停火，共21600步。继续用真实方向、近源四格4倍直接Air混合和原休眠API。
- 平均峰值Brick为262/84/1048，历史1/0.5为175/70.5/700；平均重新进入68.5/0/274次，历史均0。所有重新进入计数在1200秒后到3600秒未继续增加。
- 不将单阈值称为零切换或最低运行成本。保持用户0.5选择，在计划中写明额外驻留/重建及正式实现待核对项。

## Decisions

- 原材料需求丢失已由临时修复排除；本次新增重新进入不能套用旧缺陷的结论，也没有逐事件证据把全部次数归因于某一个分支。单阈值与hot位图时序简化作为整体候选测试。
- 不自动恢复滞回、不加隐藏阈值或定时器。阈值附近外部温度反复跨越0.5时，热标记随之切换是当前明确定义。
- Documentation impact: 主计划及局部混合计划补充本次实测，历史1/0.5数据保持原值；living docs未改为候选已实施。

## Validation

- `gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false --init-script build/single-frontier-study/init.gradle`，namespace singlefrontier，BUILD SUCCESSFUL。
- 源Air输入单组15360000 J、分散四组61440000 J；未接收/降级均0。所有solver步健康，保存捕获一致性通过。
- 同步动态反馈与确定几何供应沿用上轮方法；未测试真实主线程捕获预算/异步握手/在线玩家负载。局部G仍由测试在fragment上构造，生产源码尚无该功能。
- 1/0.5对照使用前次同场景历史数据；跨次JVM耗时只作参考，不能作严格CPU比率。完整数据和内存统计范围见[计划](../plans/2026-09-15_18-49-35_thermal-local-air-mixing-tradeoff.md)。
- 临时生产差异已精确逆向还原，`git diff -- src/main/java`为空；恢复后compileJava BUILD SUCCESSFUL。正常90项回归未重复运行，本轮没有交付生产修改。
- 删除本次临时Java/编译类、Gradle脚本、差异补丁、CSV、日志及生成测试世界；用户实际run/saves和run/logs未操作。

## Remaining

- 正式实现及真实预算/异步/多人验证；核对阶段性重新进入的温度与入向请求轨迹，评估成本，不以新增隐藏滞回回避用户选择。
