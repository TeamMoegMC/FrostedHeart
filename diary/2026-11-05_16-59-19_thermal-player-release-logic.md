# 玩家热量、隔热与直接伤害统一修复

- Time: `2026-11-05 16:59:19 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed` for the stated logic fixes and focused validation; survival balance qualification remains outstanding
- Scope: 玩家热量/衣物/装备/食物方向性/伤害与状态、诊断命令、相关Forge测试和气候文档

## Completed

- 辐射以W独立进入身体积分，并同时通过衣物隔热与辐射防护；被动边界只含真实空气/接触温度。移除把平衡点写为feel再判直接烫伤的接线，旧极值/增减API和无调用风寒公式退出。
- 实际严重环境暴露保留原温度分档，以传热比例和真实秒数计算作用；高温或低温疾病继续按实际躯干部位温度，并及时升级、降级、切换和结束。
- 修复热食/冷食反向限幅及零能量副作用、加热背心对过热身体仍补足散热、负重首次生效空指针和衣物正向基础倍率被反算为削弱。
- 用户明确不兼容旧人物温度：保留旧热状态清零，新schema独立部位能量保存/恢复通过。
- 生产10文件净减130行，新增生产类型/Mixin为0；新增9个专用Forge病例，保留原有温泉、接入和燃料保存验证。没有改城镇、研究、世界生成、旧粒子或`design/`。
- Documentation impact: 更新[玩家温度](../docs/climate/player-temperature.md)、[旧玩法对照](../docs/climate/thermal-legacy-gameplay-parity.md)与[实施计划](../plans/2026-11-05_16-24-01_thermal-player-release-logic.md)；追加[可核查验证摘要](../docs/climate/validation/thermal-player-release-logic-2026-11-05.json)。

## Decisions

- 不增加衣物节点、第二个人体求解器或等待/缓存框架，复用现有五部位上下文，分清W、J、空气/接触°C和实际体温。
- 不用全局倍率或虚拟温度覆盖错误伤害判定；辐射只通过身体能量影响持续过热。
- 温度系统自有冷热效果跟随身体状态；原版混乱/缓慢等可能来自外部，不擅自清除。
- 独立编译输出与测试世界避免并发覆盖。此处“独立”是当前源码的输出目录，不复制历史工程、不引入路径hash门禁。

## Validation

- Java17 `assemble compileGameTestJava` 通过，最终完整编译日志 `build/thermal-player-release-compile-20261105-final.log`（52s）。临时init仅排除其他会话尚有编译错误的矿床测试，没有编辑其源码。
- 最终生产代码22个独立重点场景有通过证据：玩家9、温泉5、接入6、篝火燃料/保存2。最后玩家组9/9，日志 `build/thermal-player-release-20261105-r4.log`；另13项在r3通过。明确不是一次22/22全绿。
- r1/r2/r3原失败保留：真实登录赠送护甲导致裸体对照不成立，原版60tick出生保护阻止伤害，简单难度把原始4点伤害缩为3点；现有静态辐射夹具忽略每tick64Brick捕获额度。按真实规则修正测试条件，没有削弱生产伤害去满足错误断言。
- 真实篝火接入的12°C空气、实际辐射、穿羊毛衣物与直接伤害路径通过；严重真实暴露仍伤害，食物/背心/负重/体温状态/存档回归通过。热运行时错误0，最终关闭`rootBytes=0`，测试进程退出。
- 产物 `build/thermal-player-release-20261105/libs/frostedheart-1.20.1-0.7.7.jar`，14,913,361 bytes、3728类、GameTest类0。来自共享工作区，包含其他原有未提交工作；没有自动安装或发布。
- 本批`git diff --check`通过。没有JUnit、历史编译或百人性能矩阵。

## Remaining

- 完整旧版生存/失温/死亡/恢复及食物/水/燃料收益校准仍需独立测量；本批不宣称整个热系统无缺陷或百人生产资格。
- 用户实际密闭房间未重放；已验证其12°C加篝火辐射的致错链路不再用计算平衡点直接伤人。
- 发布提交需按共享工作区实际范围选择；本轮没有提交或push。
