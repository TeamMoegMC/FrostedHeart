# 冰洞首版提交与本地测试部署

- Time: `2026-10-21 23:24:20 +08:00`
- Author: `OpenAI Codex；统筹、实现与本地提交部署`
- Status: `partial`
- Scope: FrostedHeart / TheWinterRescue `ice-caves`；PrismLauncher 开发实例

## Completed

- 实现冰洞冰面、冰晶、荧石晶体、软锰矿、装饰热液岩与温泉；TWR 提供群系挂接和生成频率。
- 温泉接触边界为 38°C；用户批准专属换热倍率，当前暂定 64，沿用分部位、衣物和身体能量模型。普通水不变。
- 按用户要求选择必要文件本地提交，并同步至开发实例的 `.minecraft` Git 仓库；不推送远端。
- 文档影响：新增 [温泉](../docs/climate/hot-spring.md)、[冰洞方块](../docs/climate/ice-cave-blocks.md)、[世界生成](../docs/worldgen/ice-caves.md)，更新玩家体温说明和入口。

## Decisions

- 保留其余任务、暖石、配方及调查工作。包含本次依赖但先前提交漏跟踪的冰溜子源码、必要资源及冰锥模型父文件，避免提交后的源码缺少已注册类型或模型。
- TWR 同时提交冰洞已引用的 `cave/icicle.json` 功能冰锥配置；其余原有暂存文件保留。
- 部署用于人工观察与调参，不宣称自然温泉频率或客户端外观已验收。

## Validation

- 此前统一运行 13 项 GameTest 全通过；针对体温、边界的 JUnit 已通过。后续静止校准夹具和自然泉池诊断尚待完整运行。
- 三个种子每群系合计 48 个内部区块的条件采样中，调整后冰洞黄铁矿、锌、铅、银均超过对应含晶洞的热液样本。
- 该轮温泉为 0；现已提高候选尝试频率，并增加装饰前盆地诊断，尚无新频率下自然泉池成功的实测证据。
- 本地部署的编译、定向测试和 JAR 结果见后续 Validation outcome。

## Validation outcome

- `compileGameTestJava test --tests '*PlayerThermalModelHotSpringTest' --tests '*IceCaveSpaceTest' jar` 成功（1m 16s）；4 项定向 JUnit 全通过，`reobfJar` 成功。日志：`build/ice-caves-local-deploy.log`。
- 修复最新采样诊断中 `Comparator.comparingInt(BlockPos::getX)` 的重载类型推断，显式指定 `BlockPos`；未改变生成算法。
- 本次所选 FH 49 个 JSON 与 TWR 10 个 JSON 均解析成功；冰洞群系引用的 TWR placed feature 文件齐全。
- 这次只编译 GameTest 源码，没有重跑自然地形采样或实际客户端；上述剩余项不因打包成功而视为完成。

## Remaining

- 新频率自然温泉采样、静止校准完整复测、客户端视觉与实际生存回暖、软管滑轮和水龙头交互验收。
- 完整交接目标尚未完成；[交接计划](../plans/2026-10-21_20-27-26_ice-caves-worldgen-handoff.md) 保持进行中。
