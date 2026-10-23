# 下调冰块与石头破碎振动的开裂概率

- Time: `2026-10-23 19:36:08 +08:00`
- Author: `OpenAI Codex；实现代理`
- Status: `completed`
- Scope: `IcicleEvents.ICE_BREAK`、`STONE_BREAK` 与相关文档／测试夹具

## Completed

- 按用户要求直接在当前 `master` 分支修改：冰块破碎中心开裂概率 1% → 0.4%，普通石质方块／矿石破碎中心开裂概率 0.5% → 0.2%。冰溜子共用对应概率直接坠落。
- 同步既有 `IcicleVibrationGameTests.actualBreakEffectsClassifyIceStoneOreAndIgnoreOtherBlocks` 的固定随机区间，保留对冰／石头概率差异的检验。
- 文档影响：更新[冰锥参数表](../docs/climate/icicle.md)及[冰溜子振动说明](../docs/climate/eave-icicles.md)，标明晚间调参未重跑早间 25 项测试。

## Decisions

- 范围、线性距离衰减、已开裂段立即破碎概率及其他事件参数沿用现有实现。本次仅提交相关文件，保留原有未提交内容。

## Validation

- 源码、文档与既有夹具阈值核对通过：距离 2 格时，冰块实际开裂概率为 0.0035、石头为 0.0015；0.002..0.003 的抽样可区分两者，0.0005..0.001 可触发石头。
- `git diff --check` 通过；未重新编译、运行 GameTest 或打包 JAR。

## Remaining

- 新概率的游戏内平衡体验待试玩。
