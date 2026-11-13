# 雪橇地面运动参数平滑过渡

- 作者：Codex
- 日期：2026-11-13
- 状态：完成

用 `Sled.sledGroundTransitionTicks`（默认 10 tick，0 立即切换）替代雪地判定保持时间。新增 SledGroundTransition，在地面目标改变时从当前摩擦和前进加速度开始线性过渡；中途换目标重新计时。实际雪地判定恢复即时，刹车、转向、碰撞和重力不插值；入水、离地重置，重新接地立即使用实际参数。COMMON 配置需客户端一致。旧键不迁移，新键使用默认值。

文档：更新 docs/sled.md，说明公式、默认值、目标持续变化、配置替换及生命周期。

验证：直接用 javac 编译生产 SledGroundTransition，并运行 /tmp 中的独立断言程序，覆盖初始化、10 tick 线性插值、中途换目标、0/1 tick、离地重置与落地，全部通过；git diff --check 通过；./gradlew compileJava --rerun-tasks 成功，4 个任务全部执行，21 条已有编译警告。未运行游戏客户端或游戏内驾驶测试，手感需试驾确认。
