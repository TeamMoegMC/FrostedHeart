# 核对原材料表面模型的深层导热限制

- Time: `2026-09-13 02:16:30 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `只读核对原架构并纠正材料plan的边界解释；未修改代码`

## Completed

- 用户指出原架构已有阻止深层导热的限制。核对当前/HEAD的BrickTopologyCompiler.face：两个V0直接不建边；封闭V0材料无exposure时不建节点。WorkerPageStore.faceResidualC只取transport边界。
- 只读核对旧提交93f40924f的BrickMaterialKernel，从adjacentAir生成材料表面接触；未复制工程或编译旧版本。
- 在[主plan第5.4节](../plans/2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)补充原架构证据，说明防止深层实体导热的范围原本就存在。未在核对源码中发现统一两层计数器；现有V0-V0阻断比开放第二层更保守。

## Decisions

- 原F10描述的实心块间无导热首先是旧表面模型能力边界，不能自动认定所有接触都应开放。
- 前一版plan追加材料温差递归扩展属于超出原范围，已撤回。代码实施以保留原阻断为前提，只按用户确认的两层资格有限开放接触。
- 材料状态完整性、固定热容和相变连续性修复不等于扩大地下导热范围。
- Documentation impact: 补充计划和本日记；当前living docs对原表面模型边界的描述与源码一致，无需改写。

## Validation

- 对照源码、HEAD、上述旧提交及docs/climate/heat-production-and-network.md；文档链接和空白检查通过。
- 未修改生产/测试代码，未运行构建或游戏测试。

## Remaining

- 实施两层方案时保留原空气驻留边界，并验证第三层既有节点、跨Brick及重叠范围不穿透。
