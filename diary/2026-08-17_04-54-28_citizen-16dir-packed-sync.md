# Citizen 同步：连续 yaw → 16 向方向打包单字节，及客户端瞬移修复

- Time: `2026-08-17 04:54 +08:00`
- Author: `Kimi-K3; AI assistant`
- Status: `completed`
- Scope: `src/main/java/com/teammoeg/frostedheart/content/town/citizen/{sim,sync,client}`

## Completed

- **同步语义从 8-bit 连续 yaw 改为 16 向移动方向**。state(3bit)+dir(4bit) 打包为一个字节
  （`CitizenState.packStateDir/unpackState/unpackDir`），bit7 保留。批包条目恒定 6–8 字节
  （原 7–9，且心跳条目省 yaw 的分支编码被消灭）；出生包同样省 1 字节。
- **服务端彻底删除 yaw**：`CitizenSim.yaw/syaw` → `dir/sdir`（0–15，默认 4=南）；
  `MovementSystem` 删除每 tick ±3 步软转向，直写 `sim.dir[i]=moveDir16`；
  位移/可通行检查/DR 外推统一走新表 `DIR_X_16/DIR_Z_16`（与真实移动严格同源）。
- **NBT 迁移**：存档写 `dir/sdir`；读旧存档时 `yaw/syaw` 经 `CitizenState.dirFromYaw`
  四舍五入到 16 向。`TownSimData`/`UnmanagedCitizenData` 拷贝点同步更新。
- **客户端本地软转向**：`ClientCitizen.visYaw`（0–255）按游戏时间闭环追赶
  `DIR_TO_YAW[dir]`（60 步/秒，>45° 加速 4 倍，零头累积不丢帧）；dir 变化时按实测
  包间隔 `prevGap` 预推进（回溯转向，抹平一个档位的网络延迟）。位置外推只用
  精确的 DIR_16，渲染朝向可"慢半拍"——两者解耦。
- **瞬移修复**：删除 `renderPos()` 的 `interval>0.35` 外推门限（近档 0.2s 窗口永不
  外推 → 包稍晚到就冻结→追赶，碎步感即小瞬移）；DR 误差阈值 0.3→0.2 格。
- 改动文件：CitizenState / CitizenSim / TownSimData / UnmanagedCitizenData /
  MovementSystem / SyncEngine / S2CCitizenBatchPacket / S2CCitizenSpawnPacket /
  ClientCitizen / ClientCitizenCache / ClientCitizenRenderer / FakeCitizenManager。

## Decisions

- **服务端不存 yaw 的理由**：服务端不渲染，yaw 唯一用途是同步；同步语义改为
  "移动方向"后，软转向是纯视觉需求，归属客户端。
- **DR 外推改用 DIR_16 而非滞后 yaw**：旧模型外推方向（软转向中间值）与真实位移
  方向（moveDir16）不一致，转弯期制造误差触发冤枉补包；现在模型与真实同源。
- **spawn 直接 snap 到目标朝向**：客户端无历史朝向，任何"过渡"都是编造数据且不可感知。
- **多客户端一致性不追求严格**：差异窗口 ≤ 转向时长，之后必然收敛；旧方案也从未
  帧级一致（各自按收包时刻插值）。
- **历史回归警示保留**：规范模型仍在 flush 末尾统一回写（回归 A：提前回写会让
  脏检测恒真/恒假，dir/state 冻结）。

## Validation

- `./gradlew compileJava` BUILD SUCCESSFUL（仅既有 JEI deprecation 警告）。
- 全库 grep 无残留引用：`ENTRY_PURE_HEARTBEAT`、`sim.yaw/syaw`、`c.yaw` 均已清除
  （仅 CitizenSim.load 保留旧存档键读取用于迁移）。

## Remaining

- 游戏内实测：多人/拥挤（分离力）与贴墙滑行场景观察修正包频率；若 0.2 格阈值
  在人群场景带宽偏高，可回退到 0.25。
- 可选增强：dir 变化时对渲染位置做"曲线回填"（当前为直线 lerp 切角，误差 ≤ 阈值）。
