# 粗档摘要按需生成与同帧等价验证

- Time: `2026-11-07 21:45:38 +08:00`
- Author: `OpenAI Codex / GPT-6.1 Sol; primary implementation and validation agent`
- Status: `completed`（选定实现与定向验证）
- Scope: 热系统帧背景、细档发布、已有降档任务、材料控制和对应GameTest；配套仓库与design未改

## Completed

- 按[计划](../plans/2026-11-07_16-00-11_thermal-minimum-summary-on-demand.md)完成普通细档跳过最低摘要；`RegionalQuerySnapshot.airBackground`持有原几何背景，余温/顶点扰动及几何盘点不再依赖有无最低温场摘要。
- 原`MINIMUM_LAYOUT`任务按需生成摘要和传热图，私有结果复用已有publication；维持Accepted采样时刻和输入序号，不领取新J。静态布局/背景映射仍随几何准备。
- 材料控制第一次用到摘要时，从修正前的同一帧准备，再执行旧增量池/相变修正，保持零容量等待历史。该少见边界同步完成，scratch计入completion预算；没有回执日志、队列或新调度器。
- 普通细档工作空间不再预留每轮最低投影；首次降档补齐原投影空间和0-pair初始工作空间语义。保存格式、20tick子步、物性、源功率和默认配置不变。
- 新增GameTest内的同帧参考投影和控制断言，复用原真实转换/材料场景；双算不进入生产。生产8文件净增33行，新增生产类型0，本批减少常态工作与状态对象，不宣称净删行。

## Decisions

- 从最终受控H重投影未必等于原增量控制，因此保留原数学路径，按实际需求补齐其输入；采用单次协调边界准备以避免再建控制准备状态机。
- 编译当前源码，沿用每标签独立输出。R1在无关TownViewClientValidation的stage变量冲突处编译失败，热验证局部排除城镇测试；R2在未进入热测试前卡在原版出生区距离更新，原记录保留。R3使用固定平坦世界和显式房间，不修改生产世界生成。
- 同一Accepted作参考，区别本次计算变化与既有启动时序差异；保留较早和较晚两份参考，未丢弃不利结果。
- Documentation impact: 更新[运行合同](../docs/climate/thermal-runtime-control.md)与[基线入口说明](../docs/climate/thermal-validation-baseline.md)，新增[验证摘要](../docs/climate/validation/thermal-minimum-summary-2026-11-07.json)；核对源码后修正旧文档将CUT_INTERVAL_TICKS写成40的错误，实际源码仍20。未改design及历史日记。

## Validation

- Java17生产编译通过；排除不相关城镇/存量deposit辅助后GameTest编译通过。R3同一编译完成grid-switch1、materials8、curves1、background1、reload1，共12/12，热保存/worker错误0、关闭root0。
- 两次真实同帧降档与14次材料控制，新旧参考温度差0°C、热量差0J；独立背景的顶点扰动和0.5/1°C活动判断一致。
- 3120条空气、1560条材料记录对最近R6参考：最大空气差1.7212897773788427e-11°C、材料温差1.2434497875801753e-13°C，采样tick/可用性匹配，自然/风/辐射列相同。局部几何和输入显式固定，世界预设不同的范围限制见摘要。
- 对较早R5仍有已知第280tick3.3829712802443836°C差异；第300tick后最大0.001088215020217831°C。该结果保留为未通过，不以选择后段窗口覆盖。
- 实际保存/卸载/冷却/新Chunk重载/再点火通过，含卸载等待的冷却窗3222tick，最终live pages0。原始日志、CSV和配置保留于ignored `build/thermal-baselines/minimum-demand-r3/`。
- 最后核对8个生产文件在该次编译后未变化；脚本语法、JSON计数与结果路径、文档相对链接和diff空白检查通过。

## Remaining

- 极端相变区域首次同步摘要准备的协调延迟未新增测量；没有整体提速百分比、大基地、完整整合包或新跨JVM资格。
- 原启动帧重复性、原封闭房间退出断言及业务条件阻止相变的平台问题仍按原记录处理。
