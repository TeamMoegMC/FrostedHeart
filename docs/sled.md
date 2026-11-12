# 雪橇雪地判定

状态：已按源码核对。最后核对日期：2026-11-12。
范围：`SledEntity.isOnSnow()`、`FHConfig.Common.sledSnowGraceTicks`。

通用配置 `Sled.sledSnowGraceTicks` 控制雪地判定保持时间，单位 tick（20 tick = 1 秒），默认 10，范围 0 至 2147483647。0 禁用保持。

雪橇每 tick 检测实际地面；检测到雪时刷新倒计时。此后连续未检测到雪的前 N tick 仍由 `isOnSnow()` 返回 true，影响雪地加速、粒子和冲刺粒子判定。再次检测成功会刷新时间。实际 `GroundStatus`、雪层高度和摩擦计算不变；保持时间也会在空中或水中正常流逝。计时只存在于实体运行期间，不保存到存档。
