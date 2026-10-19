# 冰锥末端选中与碰撞范围

- Time: `2026-10-19 13:48:44 +08:00`
- Author: `Codex; implementation agent`
- Status: `completed`
- Scope: `IcicleBlock.SHAPES[TIP]`

## Completed

- 根据游戏截图，将末端选中/碰撞框从整格高缩为上半格，宽深由 4/16 增至 6/16；局部坐标为 `(5,8,5)..(11,16,11)`。
- 更新[冰锥说明](../docs/climate/icicle.md)的形状契约。

## Decisions

- 调整实际 VoxelShape，使其贴合现有末端贴图；不缩放贴图或交叉渲染模型。

## Validation

- 核对末端状态索引、形状范围和方块注册，默认碰撞沿用 `getShape`。
- `git diff --check` 通过。
- 首次打包因本机 native 内存不足失败；降低资源占用重试期间，用户要求后续不必每次打包，已停止本次重试。此次未完成新 JAR，也未执行游戏测试。

## Remaining

- 客户端重新进入游戏后确认末端选中框与碰撞手感。
