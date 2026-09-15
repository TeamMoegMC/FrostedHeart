# 动态热场阈值验证与材料邻接请求循环

- Time: `2026-09-15 19:58:13 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `1/0.5°C前沿阈值对照、驻留缺陷诊断、计划更新；临时生产修改已还原`

## Completed

- 使用动态反馈夹具比较0.125/0.0625与1/0.5阈值：露天、封闭房间、四组分散热源，两种摆放；每组600秒加热+3000秒停火。原逻辑和材料需求修正各12组，总24组、86400个求解步。
- 复用生产拓扑构建/提交、源事件/重绑/账本、solver、query热标记、驻留请求和休眠保存/恢复；两个阈值组均使用真实方向及四格近源4倍直接Air混合原型。
- 查明`WorkerPageStore.collectMaterialFrontier`对已驻留邻居直接continue会丢失持续需求：冷目标Page进入后立即被取消，再被原所有者申请。房间一小时平均重新进入次数原阈值1659、新阈值6262。
- 临时修正为读取已驻留邻居的已提交签名，继续通过原材料/接触面条件发出orDesired。修正后所有12组重新进入次数为0，不需要新增缓存/定时器/状态。
- 在修正后的公平对照中，1/0.5阈值使三场景峰值Brick减少约66%～72%；四组分散热源峰值2086→700，3600秒驻留1842→330。
- 有效近/远测点的新旧阈值最大采样差约0.30/0.53°C。新阈值180秒远点未建立状态，保留未知，未用自然温度充当模拟结果。
- 源实际送入Air：单组15360000 J、四组61440000 J，降级/未接收0；所有运行完成，保存捕获一致性断言通过。

## Decisions

- 推荐1°C扩张、0.5°C释放温差保活，但必须一起修正材料邻接的持续请求，不能只改两个常量。
- 当前Page内captured Brick不单独回收；源、热节点、相变或入向需求仍能保留Page。这部分结构成本不通过新增逐Brick驱逐机制处理。
- Documentation impact: 在[局部混合计划](../plans/2026-09-15_18-49-35_thermal-local-air-mixing-tradeoff.md)补充动态结果、修复细节和验收；主计划同步入口。runtime文档补充已验证的当前驻留缺陷，不把临时修正或新阈值写成已实施。

## Validation

- 两次`gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false --init-script build/frontier-study/init.gradle`，临时namespace frontierstudy，均BUILD SUCCESSFUL。
- 测量的worker段是solver/query/驻留收集及轻量反馈记录；拓扑prepare/commit/rebind与试验面重建分别计时。四组分散worker p50为2840.45→631.95 μs，累计生产拓扑时间2584.34→703.89 ms，不是完整多人MSPT。
- 四处分散arena/query原始数组载荷2789.67→1071.12 KiB，不包括拓扑集合、引用、对象头、源与休眠数据，不声称是总heap。
- 同步反馈代理保留主线程的requested=source|worker、captured仅增长、无兴趣整Page保存/退出规则，但未测试真实队列/捕获预算/异步调度、磁盘持久化或玩家在线负载。
- 临时WorkerPageStore修正已精确还原，`git diff -- src/main/java`为空；随后`compileJava` BUILD SUCCESSFUL，恢复原生产编译产物。正常90项回归未重复运行，因为本轮没有交付生产改动。
- 删除临时GameTest源码/编译类、Gradle脚本、CSV、日志和生成的测试世界；保留计划中的数据与复核方法。用户实际run/saves及run/logs未操作。

## Remaining

- 正式实现真实方向、局部4倍、阈值和材料请求连续性，并增加跨Page冷材料保留/退出回归；完成真实capture预算/异步/多人CPU与heap验收。
- 当前生产仍存在上述请求循环；本轮是测试与方案整理，未启用1/0.5阈值。
