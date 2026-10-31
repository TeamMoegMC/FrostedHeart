# 正式热模拟专用服务器验证与初版提交

- Time: `2026-10-30 01:18:00 +08:00`
- Author: `Codex; OpenAI GPT-6.1 Sol; dedicated-server verification and commit selection`
- Status: `completed implementation and stated validation; selected commit prepared under the requested title`
- Scope: `正式JAR服务端启动/保存/读盘、篝火点火登记、相关回归和热系统提交范围`

## Completed

- 使用生产JAR、Java17、Forge47.3.0正常forgeserver入口运行独立实例，无开发类路径或GameTest类。新建平坦世界，真实建房、生成燃烧篝火、读取燃料、save-all flush、stop，再用新Java进程重启同一世界并再次保存停服。
- 磁盘v6温场有531个系数、499个非零，全部有限；样本260→540tick，燃料18927→18643，两次正常退出且所有维度保存完成。细节和原失败见[验收记录](../docs/climate/validation/thermal-dedicated-server-2026-10-30.json)。
- 实际保存发现直接生成燃烧篝火的入口缺口：onPlace早于新BlockEntity进入区块发现，旧回调只启动引擎。现复用PhysicalSourceSpatialIndex.resyncBlock直接登记当前BlockState，覆盖新/已有引擎，无新队列或生产顶层类型。
- 在原源测试类增加预加载远区点火回归，不手动补发区块事件，使用真实燃料、局部空气房间和既有同高度近远温差0.05°C判据；原熄灭/重燃/拆除/替换回归保持。最终2项通过、热错误0、关闭root0。
- 提交按用户指定标题“新温度热模拟系统多人服务器初版完成”选取：热运行时、必要玩法接入/配置、热测试及记录归并；共享build.gradle/.gitignore只选相关片段。研究界面/字体、矿脉生成、非热JUnit清理和其他模块改动保留在工作区。
- 打包边界额外选入既有的ResearchGameTests原样迁移：从src/main移到src/gametest，忽略换行符后内容一致，避免正式包再次带入测试类；没有选择研究功能修改。

## Decisions

- 最小依赖首次加载缺LDLib和Caupona，按项目原有运行依赖补齐。误拷完整整合包研究目录后引用了缺失KubeJS等物品，测试副本另存，改用有效的纸张单根目录；没有关闭研究校验或修改研究/城镇程序。
- 旧点火遗漏的“启动成功但没温场”运行不计热模拟通过。补充用例先后纠正了切窗前计数、地下实心场景和复用失败世界的测试问题；原失败完整保留，最终运行使用新世界。
- 用户问及加载成本：新源事件直接登记；已有篝火补发现仍读区块BE位置表，8游标×32位置/tick、总256、每tick1新快照。完整位置集合复制和冷启动遍历不受这个逐位置额度覆盖；当前拓扑构建仍是单独成本。

## Validation

- assemble/compileGameTestJava及后续测试源码编译通过；生产包426thermal类、0GameTest类、旧粒子4类保留。
- 正式服务端创建/新进程重启两轮均退出0、保存完成、热运行错误0；解析真实.mca记录验证v6与继续推进。
- 源回归2项最终通过，直接点火发布sampleTick600、airC12.912°C；原生命周期快照检查跨4次发热变化通过。
- Documentation impact: 更新[运行合同](../docs/climate/thermal-runtime-control.md)的源发现/加载说明、[部署资格](../docs/climate/thermal-regional-server-validation.md)和[待办](../plans/2026-10-29_23-38-00_thermal-next-steps.md)，保留各版本性能结果的原资格。

## Remaining

- 实际整合包目录/世界、真实玩家连接与百人负载仍未验收；此处正式服务端使用9个模组/库JAR和最小有效研究目录，没有真实连接玩家。
- 本次生产JAR来自工作区构建；热模拟提交排除研究界面/字体和矿脉改动，未复制历史源码重编，也未重跑无关性能矩阵或JUnit。
