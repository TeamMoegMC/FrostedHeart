# SurroundingTemperatureSimulator 深度优化（循环交换/缓存复位/硬件取整/快速随机）

- Time: `2026-08-03 03:35:00 UTC+8`
- Author: `Kimi-K3; coding agent`
- Status: `completed`
- Scope: `src/main/java/com/teammoeg/frostedheart/content/climate/player/SurroundingTemperatureSimulator.java`

## Completed

在 claude-opus 一轮优化基础上完成两轮深化（第一轮实施于 2026-08-02，本条为补记+续作）：

第一轮（2026-08-02，已编译验证）：

1. **粒子外层循环交换**：粒子轨迹相互独立 → 粒子外层/轮次内层，位置与速度驻留寄存器；
   删除 WorkBuffer 的 qx/qy/qz/pvx/pvy/pvz（默认 rdiff=10 → n≈4169，省约 200KB/线程），
   消除每次调用 6n 初始化写入与热循环每步 9 次数组访存。
   代价：rnd 消费顺序变化 → 蒙特卡洛路径不同但统计等价（可接受）。
2. **无符号单分支越界检查**：`((px|py|pz) >>> 5) != 0` 替代 6 次比较有符号检查；
   topY 的 xz 检查同样单分支化（注意：topY 只需 xz 在界内，**不能**与三轴检查合并，
   否则 y 越界、xz 界内的粒子会从真实地形高度误判为 -32767 强风）。

第二轮（2026-08-03，本次）：

3. **世代标记缓存 → Arrays.fill(null) 复位**：删除 gen[]/currentGen/reset()/getCached/putCached。
   128KB 引用清零由 JIT 内在化为 SIMD memset（数 μs），优于热循环每步额外读 gen[idx]+比较
   （约 8.3 万次/调用）；WorkBuffer 内存 460→132KB/线程（较最初 -71%）。
4. **手写 floor 分支 → (int) Math.floor()**：旧写法"int 截断+条件递减"是数据依赖分支，
   粒子方向各异致约半数误预测；Math.floor 为 JIT 内在函数（roundsd+cvttsd2si），无分支，逐点等价。
5. **SplittableRandom → 自实现 xoroshiro128+（FastRandom）**：原 nextInt(bound) 对非 2 幂上界
   使用取模拒绝采样（idiv 数十周期，反弹密集场景每调用数万次）；新实现用 Lemire 高位乘法定界
   （Java 17 无 unsignedMultiplyHigh，以 multiplyHigh+符号修正等效），无除法无拒绝循环。
   均匀分布语义不变。

## Decisions

- 明确不做**跨调用结果/位置缓存复用**：无法廉价可靠地侦测区域内方块更新（火把放置等），
  会产生玩法可见的陈旧数据 → 破坏语义。
- 不做 stateCache 静态共享：BlockTempData 由数据包驱动，reload 会产生脏数据；收益仅约 1%。
- 最大外部杠杆是配置 `simulationDivision`（n∝rdiff³，10→5 约省 87% 计算），
  但改变数值精度，属用户配置层，代码不动。
- 构造器 8× PalettedContainer.copy()（threadSafe 路径，主线程）为线程安全必需，保留。

## Validation

- `gradlew compileJava` 通过（仅 JEI 既有 deprecation 警告，与本次无关）。
- 环境坑（重要）：本机用户目录 `C:\Users\da's'b` 含撇号，导致 Gradle Worker Daemon
  主类加载失败（ClassNotFoundException: GradleWorkerMain）。
  规避：建立 Junction `C:\Users\dasb` → `C:\Users\da's'b`，编译时
  `GRADLE_USER_HOME="C:\Users\dasb\.gradle" ./gradlew compileJava` 即可，无需重下依赖。

## Remaining

- 未做运行时基准对比（无 JMH 环境）；如需量化，建议在 create 世界地下/地面两场景
  用 `tasksRemain` 或 spark 采样对比。
- `getHitingFace` 为保留的死代码，如需精简可删。
