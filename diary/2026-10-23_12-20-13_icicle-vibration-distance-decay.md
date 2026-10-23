# 冰锥振动概率随距离线性衰减

- Time: `2026-10-23 12:20:13 +08:00`
- Author: `OpenAI Codex；实现代理`
- Status: `completed`
- Scope: `IcicleEvents.vibrate`、冰锥与冰溜子的爆炸/敲钟响应

## Completed

- 共用候选扫描后，以事件到方块中心的三维距离 d 和影响半径 R 计算 `max(0,1-d/R)`，同时乘到完整冰锥开裂、预警冰锥立即破碎和冰溜子坠落概率。
- 爆炸半径仍为 32 格、敲钟仍为 12 格；中心基准概率仍为 5%/90%/5%，半径一半减半，边界归零。保持每事件原预警状态快照，不检查墙体遮挡。
- 文档影响：更新 [冰锥](../docs/climate/icicle.md) 和 [冰溜子](../docs/climate/eave-icicles.md) 的公式、单位和核对日期。

## Decisions

- 用户选择线性衰减到 0%，代替此前讨论的指数衰减到 20%。只对候选悬冰计算距离倍率，不增加搜索范围、声学模拟或温度系统修改。

## Validation

- `git diff --check` 通过。
- Java 17 `compileJava --offline --no-daemon --max-workers=2` 通过，`BUILD SUCCESSFUL in 2m 15s`；记录 21 条其他路径的 Mixin/弃用等编译警告。日志见 `build/icicle-distance-attenuation-compile.log`。
- 本轮未运行游戏内概率统计或 GameTest，未打包 JAR、暂存、提交或部署。

## Remaining

- 游戏内体验和平衡待内测。
