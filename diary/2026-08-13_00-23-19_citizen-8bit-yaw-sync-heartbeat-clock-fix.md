# 8-bit yaw 重构回归修复：瞬移 + yaw 僵住（第 8 次）

- **Time**: `2026-08-13 00:23:19 +08:00`
- **Author**: `开发助手`
- **Status**: `DONE`（编译通过 EXIT=0，仅既有 JEI deprecation 20 条；待 runClient 实机验证）
- **Scope**: `citizen/sync/SyncEngine.java`、`citizen/client/ClientCitizen.java`

## 背景

8-bit yaw 重构（commit 99f449d49）后实机两个症状：**居民走路瞬移**、**走路 yaw 不改变**。
排查确认不是编码宽度/偏移不匹配——8-bit 在收发两端完全一致（所有转换点均正确用 256
分度），是两个独立的同步逻辑/时钟单位回归。

## 根因 A：心跳判定恒真 → yaw/state 永远不上线（解释 yaw 不改变）

`SyncEngine.flushDeltas` 收集阶段（原 197-204 行）把居民加入 `due` 的同时**就地覆盖
规范模型**（`sim.syaw[i] = sim.yaw[i]`、`sim.sstate[i] = sim.state[i]`）；组包阶段的
纯心跳判定 `st == sim.sstate[i] && sim.yaw[i] == sim.syaw[i]` 因刚被覆盖为当前值而
**恒为真** → 每条批量条目都打成 `ENTRY_PURE_HEARTBEAT` → 编码省略 yaw 字节 →
客户端永远沿用出生包的旧 yaw/state。服务端 `sim.yaw` 每 tick 都在转（3 步/tick
≈4.2°），但从未发出。设计语义（类 javadoc）：纯心跳 = "自上次发送以来只有位置变了"，
比较必须针对**上一次 flush 已发送**的规范值。

## 根因 B：时钟单位错位 → 插值窗口坍缩为 1 tick（解释瞬移）

`ClientCitizen.now()` 返回 `getGameTime() + getFrameTime()`——单位是 **tick**，注释却写
"秒"，类内全部常量都是秒制（窗口钳 0.05~1.0、外推门限 0.35、钳制 1.5）。实际发包
间隔 4/8/20 tick，用 tick 钟后 interval 恒被钳到 1.0 tick → x0→x1 插值 1 tick 完成 →
渲染位置冻结到下个快照再猛跳：近档每次 ~0.55 格、远档每次 ~3.9 格 = 可见瞬移。
重构把 `nanoTime()/1e9` 换成游戏时间（为暂停感知）但常量未按 tick 换算。

## 修复

1. **SyncEngine**：规范模型回写从收集阶段移到 `flushDeltas` 末尾（组包循环之后、
   经 id 反查容器与索引，防御移除竞态）；心跳判定改为与上一次 flush 的规范值比较。
   同步重写过时注释（原注释仍引用已删除的 `rdir==DIR_NONE` 停止信号）。
2. **ClientCitizen**：`now()` 改为 `(getGameTime() + getFrameTime()) / 20.0` 恢复秒单位
   （保留暂停感知，帧时间小数使包间隔亚 tick 精确）；全部秒制常量与速度换算
   （`SPEED × 20 / FIXED_SCALE` 方块/秒）立即恢复正确，无需改动。

## Decisions

- 回写整体后移而非引入"变更标志位"：O(1) 存储不动（共享规范模型近似不变），
  单处移动最小 diff，语义与类 javadoc"以任一玩家收到为更新时机"一致。
- 时钟改回秒制而非常量 ×20 改 tick 制：单行改动，类内注释/常量全部以秒记载，
  与历史设计（nanoTime 秒制）对齐。
- 到达停止的 DIR_NONE 信号不恢复：到达后 MOVING 心跳持续按档位重锚，客户端窗口内
  平滑收敛到停止点（稳态 extra≈0 无外推漂移），≤1s 内行为系统切回静止态发完整
  条目；远档 ~1s 视觉滞后属快照插值固有，无跳变。记录为已知行为。

## Validation

- `gradlew.bat compileJava --console=plain -q` → **EXIT=0**，仅 20 条既有 JEI
  deprecation，无新增警告。
- 静态验证：`syaw/sstate/stick` 仅 SyncEngine 消费（stick 门限 / 心跳判定 / isDirty
  DR 预测），isDirty 在收集阶段调用、读上一次 flush 规范值，前后一致；`due.isEmpty()`
  提前返回与移除广播路径不受影响；MAX_ENTRIES 截断下的规范值超前属记载的共享近似。
- 边界推演：转弯期 yaw 每 flush 变化 → 完整条目 ✓；直线匀速 yaw 不变 → 纯心跳沿用
  （客户端缓存即真值）✓；到达停步 → 心跳重锚收敛 ✓。

## Remaining

- runClient 实机验证要点（`/fhcitizen spawn 5 20` 分档观察）：
  1. 直线行走近档（<32 格，4 tick）连续平滑滑动、无冻结-瞬移；远档（64-96 格）
     平滑但滞后约 1 秒行程（设计固有）；
  2. 转弯中 yaw 连续平滑旋转（近档假实体软跟随，远档低模同步），不再僵在出生朝向；
  3. 到达停步平滑收敛、走路动画衰减、无冲过头回弹，≤1s 后切 IDLE 姿态；
  4. 跨 32/64 格档位边界滞后距离平滑变化无跳变；暂停菜单冻结/恢复无跳变；
     `/fhcitizen clear` 正常消失、日志无异常。
