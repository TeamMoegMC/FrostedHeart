# 统一材料数据与相变流程实施

- Time: `2026-09-14 21:19:17 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `材料C/O、显式冷热边、统一提交条件及旧相变入口清理`

## Completed

- 实施[计划](../plans/2026-09-14_19-06-50_thermal-data-driven-phase-architecture.md)。`StateTransitionData`使用显式heating/cooling和固定world_conditions，`MinecraftMaterialLawCompiler`只做参数与端点两遍编译。材料增加或改单向目标不需要Java物质分支。
- 74份完整发行声明位于`src/datagen/resources/data/frostedheart/data/material_transitions.json`，同步生成原state_transition配方路径。每状态只有一个C/O参考，迁移规范化16组参考值，目标H与目标law一致。数值示例及单位记录在[材料规则文档](../docs/climate/heat-production-and-network.md#material-transition-data)。离线迁移脚本/导出位于build，不属于运行代码。
- `MinecraftPhaseController`负责活动/休眠/无记录相变提交。水边缘、岩浆Y>−55与原群系排除均从数据读取；同一热law但不同条件的profile不合并。没有新增生产类、Mixin、规则引擎、节点状态或存档字段。
- 删除`PhysicalState`、`LavaFluidMixin`及注册、旧state_transition.xlsx、阶段槽与互逆推导、独立地表冻结和旧概率配置。移除无生产调用者的`applyGameplayTransition`及已过时的直接绕过材料能量测试片段。常规相变转换均使用编译边。
- 保留无记录雪层的光照减层；温度相变统一使用已有整块Snow→Air目标。天气降雪/降水和原生岩浆点火继续执行。
- Documentation impact: 更新热源/相变、数据生命周期、runtime、世界温度文档和plan结果。未编辑design。伴随仓库`D:/TheWinterRescue`检查无相变覆盖或被删配置引用，未修改；其原有36项工作区变更保留。

## Decisions

- 显热H=O+C*T，转换潜热直接由源/目标端点决定。目标无材料law时才使用终止边latent_heat_j；没有runtime图推导或旧格式读取。
- 主线程条件和效果引用按69个共享profile保存，两个数组各70个槽（含空profile），不逐节点复制。世界条件只有即将提交时才检查，缺省条件不读邻居/群系。
- 未跟踪区域仍是环境平衡近似，原随机命中直接检查同一边，不再套额外概率。材料记录仍必须完成潜热。自然岩浆初温保持已明确延期的范围。

## Validation

- Java17主代码与datagen代码编译通过。
- `runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`：**88/88通过**，[最终日志](../build/thermal-data-gametest-final.log)。新增4项数据/条件回归；核验1,405条编译边的方向、目标状态与能量连续性。保留材料两层、潜热/ACK、休眠/COW、液位参考、解析场、红外及嵌套替换回归。
- 74份源JSON与生成配方逐份比较一致，旧相变API/概率配置/枚举无Java或JSON引用；`git diff --check`通过。
- 休眠随机样本：1/8个Section × 1/64条记录，0 bytes/attempt；最终样本p50约0.17–1.41 μs。4096记录Chunk的1024次真实setBlockState：0 bytes/call，p50约1.79 μs。属于局部微基准，不构成整体服务器加速证明。
- 首轮旧解析场测试在randomTickSpeed=0时仍期望无记录立即转换，已改为在提交检查时开启更新；自定义条件测试已修正与场地负Y不符的高度。首轮纯辐射断言失败，补充配置/源端口诊断后最终完整运行通过，未为此修改热源生产逻辑。
- 完整`runData`被已有`frostedheart:warm_stone`缺失掉落表阻塞，[日志](../build/thermal-data-generation.log)。仅相变资源采用结构化JSON生成，实际Codec/配方加载由GameTests验证；未扩展无关掉落表修复。
- 普通JUnit仍被已有26个旧几何类型引用编译错误阻塞，[日志](../build/thermal-data-unit.log)；本轮更新了StateTransitionDataTest，没有恢复旧几何兼容类。Codec round-trip同时由成功的GameTest验证。

## Remaining

- 多人集中/分散、大面积水域、红外开启/关闭的实际MSPT、带宽和转换延迟尚未实测。水获得随机资格后可能增加更新Section数量。
- 自然岩浆热初态继续延期。全量datagen掉落表及普通JUnit旧测试源集是既有独立问题。
