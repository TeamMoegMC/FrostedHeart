/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.content.climate.data.BlockTempData;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

/**
 * A simulator built on Alphagem618's heat conducting model
 * This simulates heat conduction in a small area around player
 * And would take the area out of minecraft logic to optimize calculations.
 * 
 * Note that the constructor must be called in game thread, while other method could run multithreaded
 *
 * @author khjxiaogu
 * @author Alphagem618
 */

//**********代码优化来自claude-opus
public class SurroundingTemperatureSimulator {


    public record SimulationResult(float blockTemp, float windStrength) {
    }

    /**
     * 缓存的方块信息。不可变，线程安全。
     * 同一 BlockState 实例共享同一 CachedBlockInfo（通过 stateCache 去重）。
     */
    private static final class CachedBlockInfo {
        final VoxelShape shape;
        final List<AABB> aabbList;
        final float temperature;
        final boolean isFull;
        final boolean isEmpty;

        CachedBlockInfo(VoxelShape shape, float temperature) {
            this.shape = shape;
            this.temperature = temperature;
            this.isFull = (shape == FULL);
            this.isEmpty = (shape == EMPTY);
            // 仅对部分形状（楼梯、半砖等）生成 AABB 列表
            this.aabbList = (!isFull && !isEmpty) ? shape.toAabbs() : Collections.emptyList();
        }
    }

    /**
     * 线程局部工作缓冲区。
     * 避免每次模拟调用分配大数组；每次调用通过一次 Arrays.fill(posCache, null)
     * 复位位置缓存（128KB 引用清零被 JIT 内在化为 SIMD memset，仅数 μs）。
     * <p>
     * 曾采用"世代标记"（gen[] + currentGen）惰性失效方案，但热循环每步都要
     * 额外读取 gen[idx] 并比较分支（约 8 万次/调用），累计成本高于一次 memset；
     * 且 gen[] 本身占 128KB/线程。故改为整体清零：缓存内存减半，热循环每步
     * 少一次 load + 比较 + 分支。
     * <p>
     * 粒子状态数组（原 qx/qy/qz/pvx/pvy/pvz，共 6n 个 double，约 200KB/线程）
     * 已随"粒子外层循环交换"删除——每个粒子的轨迹完全独立，其位置与速度在
     * 全部 num_rounds 轮中驻留于局部变量（寄存器），无需数组中转。
     */
    private static final class WorkBuffer {
        // 位置缓存：32^3 = 32768 个槽位，覆盖 [-16,16) 三个轴
        final CachedBlockInfo[] posCache = new CachedBlockInfo[CACHE_SIZE];

        //优化：预计算高度图（32×32 = 1024 个 int）
        final int[] topYCache = new int[CACHE_DIM * CACHE_DIM];
    }

    // ======================== 静态常量 ========================

    private static final VoxelShape EMPTY = Shapes.empty();
    private static final VoxelShape FULL = Shapes.block();

    public static final int range = FHConfig.SERVER.SIMULATION.simulationRange.get();
    private static final int rdiff = FHConfig.SERVER.SIMULATION.simulationDivision.get();
    private static final double v0 = FHConfig.SERVER.SIMULATION.simulationParticleInitialSpeed.get();
    private static final int num_rounds = 20; // 不可配置，影响结果

    private static final int n; // 粒子总数，静态块中计算

    // 速度向量 SoA（消除 Vec3 对象解引用开销）
    private static final double[] speedVX, speedVY, speedVZ;
    private static final int[][] speedVectorByDirection = new int[6][];

    // 越界查询返回的常量空气信息
    private static final CachedBlockInfo AIR_INFO = new CachedBlockInfo(EMPTY, 0f);

    // 位置缓存维度参数
    private static final int CACHE_DIM = 32;      // 每轴 32 格
    private static final int CACHE_OFFSET = 16;    // 偏移量 [-16, 16)
    private static final int CACHE_SIZE = CACHE_DIM * CACHE_DIM * CACHE_DIM; // 32768

    // 线程局部缓冲区池
    private static final ThreadLocal<WorkBuffer> WORK_BUFFER =
            ThreadLocal.withInitial(WorkBuffer::new);


    static {
        Map<Direction, List<Integer>> dirLists = new EnumMap<>(Direction.class);
        for (Direction d : Direction.values()) dirLists.put(d, new ArrayList<>());
        List<double[]> vecs = new ArrayList<>();

        int count = 0;
        for (int i = -rdiff; i <= rdiff; ++i)
            for (int j = -rdiff; j <= rdiff; ++j)
                for (int k = -rdiff; k <= rdiff; ++k) {
                    if (i == 0 && j == 0 && k == 0) continue;
                    float x = i * 1f / rdiff, y = j * 1f / rdiff, z = k * 1f / rdiff;
                    float r = Mth.sqrt(x * x + y * y + z * z);
                    if (r > 1) continue;
                    double vx = x / r * v0, vy = y / r * v0, vz = z / r * v0;
                    vecs.add(new double[]{vx, vy, vz});
                    if (vx > 0) dirLists.get(Direction.EAST).add(count);
                    if (vx < 0) dirLists.get(Direction.WEST).add(count);
                    if (vy > 0) dirLists.get(Direction.UP).add(count);
                    if (vy < 0) dirLists.get(Direction.DOWN).add(count);
                    if (vz > 0) dirLists.get(Direction.SOUTH).add(count);
                    if (vz < 0) dirLists.get(Direction.NORTH).add(count);
                    count++;
                }

        n = count;
        speedVX = new double[n];
        speedVY = new double[n];
        speedVZ = new double[n];
        for (int idx = 0; idx < n; idx++) {
            speedVX[idx] = vecs.get(idx)[0];
            speedVY[idx] = vecs.get(idx)[1];
            speedVZ[idx] = vecs.get(idx)[2];
        }
        for (Direction d : Direction.values())
            speedVectorByDirection[d.ordinal()] =
                    dirLists.get(d).stream().mapToInt(Integer::intValue).toArray();
    }

    // ======================== 实例字段 ========================

    @SuppressWarnings("unchecked")
    public PalettedContainer<BlockState>[] sections = new PalettedContainer[8];
    public Heightmap[] maps = new Heightmap[4];

    private final int ox, oy, oz; // origin 坐标（int 缓存，避免反复 getX/Y/Z）
    private final FastRandom rnd;  // 无锁随机数（xoroshiro128+，见内部类注释）

    // BlockState → CachedBlockInfo 去重缓存（实例级，避免跨世界污染）
    private final IdentityHashMap<BlockState, CachedBlockInfo> stateCache =
            new IdentityHashMap<>(256);

    // ======================== 构造器 ========================

    public SurroundingTemperatureSimulator(ServerLevel world, double sx, double sy, double sz,
                                           boolean threadSafe) {
        int sourceX = Mth.floor(sx), sourceY = Mth.floor(sy), sourceZ = Mth.floor(sz);
        int offsetW = sourceX - range;
        int offsetD = sourceY - range;
        int offsetN = sourceZ - range;
        int chunkOffsetW = offsetW >> 4;
        int chunkOffsetN = offsetN >> 4;
        int chunkOffsetD = offsetD >> 4;

        BlockPos origin = new BlockPos(
                (chunkOffsetW + 1) << 4,
                (chunkOffsetD + 1) << 4,
                (chunkOffsetN + 1) << 4);
        ox = origin.getX();
        oy = origin.getY();
        oz = origin.getZ();

        int i = 0;
        for (int x = chunkOffsetW; x <= chunkOffsetW + 1; x++)
            for (int z = chunkOffsetN; z <= chunkOffsetN + 1; z++) {
                LevelChunk cnk = world.getChunk(x, z);
                int maxIndex = cnk.getSectionsCount();
                int index0 = cnk.getSectionIndexFromSectionY(chunkOffsetD);
                int index1 = index0 + 1;
                // 触发 heightmap 初始化
                cnk.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
                maps[i / 2] = cnk.getOrCreateHeightmapUnprimed(Types.MOTION_BLOCKING_NO_LEAVES);
                if (index0 >= 0 && index0 < maxIndex)
                    sections[i] = cnk.getSection(index0).getStates();
                if (index1 >= 0 && index1 < maxIndex)
                    sections[i + 1] = cnk.getSection(index1).getStates();
                i += 2;
            }

        // 异步路径：复制 PalettedContainer 快照避免线程竞争
        // 注意：Heightmap 未复制，是原代码的已知局限
        if (threadSafe) {
            for (int j = 0; j < sections.length; j++)
                if (sections[j] != null)
                    sections[j] = sections[j].copy();
        }

        rnd = new FastRandom(
                BlockPos.asLong(sourceX, sourceY, sourceZ) ^ (world.getGameTime() >> 6));
    }

    // ======================== 核心模拟 ========================

    public SimulationResult getBlockTemperatureAndWind(double qx0, double qy0, double qz0) {
        // 获取当前线程的工作缓冲区；一次 memset 复位位置缓存（见 WorkBuffer 注释）
        WorkBuffer buf = WORK_BUFFER.get();
        final CachedBlockInfo[] posCache = buf.posCache;
        Arrays.fill(posCache, null);

        //预计算高度图
        fillTopYCache(buf.topYCache);

        float heat = 0f, wind = 0f, minTemp = 0f, maxTemp = 0f;
        // 局部变量缓存，JIT 优化更友好
        final int[] topYCache = buf.topYCache;

        // ★ 粒子外层 / 轮次内层（loop interchange）：
        // 每个粒子的轨迹完全独立（粒子间无交互），交换循环顺序是安全的。
        // 粒子位置与速度在全部 num_rounds 轮中驻留局部变量（寄存器），
        // 省去了原实现每轮 6 次数组读 + 3 次数组写，以及 6n 的初始化填充；
        // 同时删除了 WorkBuffer 中约 200KB/线程 的粒子状态数组。
        // 注意：rnd 的消费顺序与"轮次外层"不同，单次调用的具体随机路径会变，
        // 但蒙特卡洛统计分布不变（结果本就是 n 个粒子的随机平均）。
        for (int i = 0; i < n; ++i) {
            double sx = qx0, sy = qy0, sz = qz0;
            double vx = speedVX[i], vy = speedVY[i], vz = speedVZ[i];

            for (int round = 0; round < num_rounds; ++round) {
                final double dx = sx + vx, dy = sy + vy, dz = sz + vz;

                // Math.floor 是 JIT 内在函数（roundsd 硬件取整 + cvttsd2si），无分支；
                // 旧写法 "int 截断 + 条件递减" 是数据依赖分支，粒子方向各异导致约半数误预测。
                // 两者逐点等价（含 -0.0 与负值边界；世界坐标远小于 int 溢出阈）。
                int bx = (int) Math.floor(dx);
                int by = (int) Math.floor(dy);
                int bz = (int) Math.floor(dz);

                // ---- 缓存查找（直接数组索引 + null 哨兵） ----
                //坐标范围 [-16,16) 映射到 [0,32) 再编码为一维索引。
                final int rx = bx - ox, ry = by - oy, rz = bz - oz;
                // 无符号单分支越界检查：(v+16) 落在 [0,32) 当且仅当其第 5 位及以上全为 0；
                // 任一分量越界（负数或 ≥32）都会在 OR 结果中留下高位 → >>>5 非零。
                final int px = rx + CACHE_OFFSET, py = ry + CACHE_OFFSET, pz = rz + CACHE_OFFSET;
                CachedBlockInfo info;
                if (((px | py | pz) >>> 5) != 0) {
                    info = AIR_INFO;
                } else {
                    int idx = (px << 10) | (py << 5) | pz;
                    info = posCache[idx];
                    if (info == null) {
                        info = computeBlockInfo(rx, ry, rz, bx, by, bz);
                        posCache[idx] = info;
                    }
                }

                // ---- 碰撞处理（三路分支：FULL / 部分形状 / EMPTY） ----
                if (info.isFull) {
                    // FULL 方块：解析法 Slab Method 求入射面，零对象分配
                    int sxi = Mth.floor(sx), syi = Mth.floor(sy), szi = Mth.floor(sz);
                    if (sxi != bx || syi != by || szi != bz) {
                        // 从外部进入 → 计算入射面并反弹
                        int nid;
                        if (rnd.nextInt3()== 0) {
                            nid = rnd.nextInt(n);
                        } else {
                            nid = getOutboundSpeedFrom(
                                    computeEntryFace(sx, sy, sz, vx, vy, vz, bx, by, bz));
                        }
                        vx = speedVX[nid];
                        vy = speedVY[nid];
                        vz = speedVZ[nid];
                    }
                    // 粒子已在同一 FULL 方块内部 → 不反弹（与原代码行为一致：
                    //   shape.clip 对两端点都在 AABB 内部时返回 null → 不进入反弹分支）

                } else if (!info.isEmpty) {
                    // 部分形状（楼梯、半砖等，占比 <10%）：回退到原版 clip
                    // 此处不可避免需要创建 Vec3/BlockPos，因为 MC API 要求
                    Vec3 svec = new Vec3(sx, sy, sz);
                    Vec3 dvec = new Vec3(dx, dy, dz);
                    BlockPos bpos = new BlockPos(bx, by, bz);
                    BlockHitResult bhr = info.shape.clip(svec, dvec, bpos);
                    // 原代码逻辑：对 partial shape，条件为 bhr != null && bhr.isInside()
                    // （shape == FULL 在此分支中不可能为 true，已被上方 isFull 拦截）
                    if (bhr != null && bhr.isInside()) {
                        BlockHitResult brtr = AABB.clip(info.aabbList, svec, dvec, bpos);
                        int nid;
                        if (brtr != null) {
                            nid = rnd.nextInt3() == 0
                                    ? rnd.nextInt(n)
                                    : getOutboundSpeedFrom(brtr.getDirection());
                        } else {
                            nid = rnd.nextInt(n);
                        }

                        vx = speedVX[nid];
                        vy = speedVY[nid];
                        vz = speedVZ[nid];
                    }
                }
                // EMPTY：无碰撞，速度不变，零开销

                // ---- 更新粒子状态（仅局部变量） ----
                sx = dx;
                sy = dy;
                sz = dz;

                // ---- 温度累积 ----
                final float curheat = info.temperature;
                if (curheat != 0f) {
                    if (curheat < minTemp) minTemp = curheat;
                    else if (curheat > maxTemp) maxTemp = curheat;
                    // 展开自：curheat * Mth.lerp(Mth.clamp(-signum(curheat)*vy, 0, 0.4) * 2.5, 1, 0.5)
                    // 数学推导：lerp(t, 1, 0.5) = 1 - 0.5*t, 其中 t = clamp(...) * 2.5
                    //         → 1 - 1.25 * clamp(yInfluence, 0, 0.4)
                    // 物理含义：热气上升（curheat>0 时 -vy 越大权重越低），冷气下沉
                    double yInfluence = curheat > 0 ? -vy : vy;
                    if (yInfluence < 0) yInfluence = 0;
                    else if (yInfluence > 0.4) yInfluence = 0.4;
                    heat += (float) (curheat * (1.0 - 1.25 * yInfluence));
                }

                // ---- 风力计算 ----
                // 注意：topY 仅要求 xz 在界内（与 y 无关），不能与上方三轴检查合并，
                // 否则 y 越界、xz 界内的粒子会从"真实地形高度"误变为 -32767 强风。
                int topY;
                if (((px | pz) >>> 5) == 0) {
                    topY = topYCache[(px << 5) | pz];
                } else {
                    // 保持与 getTopY 越界时一致
                    topY = -32767;
                }

                if (topY <= by) {
                    // 方块位于天空下方 → 强风
                    wind += 2f;
                } else if (info.isEmpty) {
                    // 复用上方已查到的 info（原代码此处对同一 bpos 二次调用 getInfoCached）
                    double ddx = bx + 0.5 - qx0, ddy = by + 0.5 - qy0, ddz = bz + 0.5 - qz0;
                    if (ddx * ddx + ddy * ddy + ddz * ddz >= 16.0) {
                        // 粒子飞出 4 格以上且遇到空气 → 弱风
                        wind += 0.5f;
                    }
                }
            }
        }

        return new SimulationResult(Mth.clamp(heat / n, minTemp, maxTemp), wind / n);
    }

    // ======================== 缓存与方块访问 ========================
    /**
     * 计算方块的碰撞形状和温度。
     * 通过 stateCache（IdentityHashMap）对同一 BlockState 去重。
     *
     * @param localX/Y/Z 相对 origin 的坐标（用于 getBlock）
     * @param worldX/Y/Z 世界坐标（用于 getCollisionShape）
     */
    private CachedBlockInfo computeBlockInfo(int localX, int localY, int localZ,
                                             int worldX, int worldY, int worldZ) {
        BlockState bs = getBlock(localX, localY, localZ);

        // BlockState 在 MC 中是单例，IdentityHashMap 用 == 比较，最快
        CachedBlockInfo cached = stateCache.get(bs);
        if (cached != null) return cached;

        // 计算碰撞形状
        VoxelShape shape;
        if (bs.isAir()) {
            shape = EMPTY;
        } else if (bs.getBlock().hasDynamicShape()) {
            // 动态形状无法按 state 缓存，保守视为实心
            shape = FULL;
        } else {
            try {
                // null BlockGetter：原代码设计如此，vanilla 仅查询 shape 缓存
                shape = bs.getCollisionShape(null, new BlockPos(worldX, worldY, worldZ));
            } catch (Exception ex) {
                ex.printStackTrace();
                shape = FULL;
            }
        }

        // 计算温度
        float temp = 0f;
        BlockTempData b = BlockTempData.getData(bs.getBlock());
        if (b != null) {
            if (b.isLit()) {
                if (bs.hasProperty(BlockStateProperties.LIT) && bs.getValue(BlockStateProperties.LIT))
                    temp = b.getTemp();
            } else {
                temp = b.getTemp();
            }
            if (b.isLevel() && temp != 0f) {
                if (bs.hasProperty(BlockStateProperties.LEVEL))
                    temp *= (bs.getValue(BlockStateProperties.LEVEL) + 1) / 16f;
                else if (bs.hasProperty(BlockStateProperties.LEVEL_COMPOSTER))
                    temp *= (bs.getValue(BlockStateProperties.LEVEL_COMPOSTER) + 1) / 9f;
                else if (bs.hasProperty(BlockStateProperties.LEVEL_FLOWING))
                    temp *= bs.getValue(BlockStateProperties.LEVEL_FLOWING) / 8f;
                else if (bs.hasProperty(BlockStateProperties.LEVEL_CAULDRON))
                    temp *= (bs.getValue(BlockStateProperties.LEVEL_CAULDRON) + 1) / 4f;
            }
        }

        cached = new CachedBlockInfo(shape, temp);
        stateCache.put(bs, cached);
        return cached;
    }

    // ======================== 碰撞辅助 ========================

    /**
     * 解析法计算射线进入单位立方体 [bx,bx+1)×[by,by+1)×[bz,bz+1) 时的入射面。
     * 数学上等价于 AABB.clip 对完整方块的计算，但零对象分配。
     * <p>
     * 原理：对每个轴计算射线到达该轴边界面的参数 t，取最大 t 对应的面即为入射面。
     */
    private static Direction computeEntryFace(double sx, double sy, double sz,
                                              double vx, double vy, double vz,
                                              int bx, int by, int bz) {
        double tMax = Double.NEGATIVE_INFINITY;
        Direction result = Direction.UP; // 兜底值，正常输入不会使用

        // X 轴
        if (vx > 0) {
            double t = (bx - sx) / vx;
            if (t > tMax) {
                tMax = t;
                result = Direction.WEST;
            }
        } else if (vx < 0) {
            double t = (bx + 1 - sx) / vx;
            if (t > tMax) {
                tMax = t;
                result = Direction.EAST;
            }
        }

        // Y 轴
        if (vy > 0) {
            double t = (by - sy) / vy;
            if (t > tMax) {
                tMax = t;
                result = Direction.DOWN;
            }
        } else if (vy < 0) {
            double t = (by + 1 - sy) / vy;
            if (t > tMax) {
                tMax = t;
                result = Direction.UP;
            }
        }

        // Z 轴
        if (vz > 0) {
            double t = (bz - sz) / vz;
            if (t > tMax) {
                tMax = t;
                result = Direction.NORTH;
            }
        } else if (vz < 0) {
            double t = (bz + 1 - sz) / vz;
            if (t > tMax) {
                result = Direction.SOUTH;
            }
        }

        return result;
    }

    /**
     * 从指定方向的半球中随机选取一个速度向量索引。
     * 用于粒子反弹后的新方向。
     */
    public int getOutboundSpeedFrom(Direction dir) {
        if (dir == null) return rnd.nextInt(n);
        int[] iis = speedVectorByDirection[dir.ordinal()];
        return iis[rnd.nextInt(iis.length)];
    }

    // ======================== 方块与高度图访问 ========================

    /**
     * 获取相对 origin 的方块状态。
     * x, y, z 必须在 [-16, 16) 范围内，超出返回 AIR。
     */
    public BlockState getBlock(int x, int y, int z) {
        if (x >= 16 || y >= 16 || z >= 16 || x < -16 || y < -16 || z < -16)
            return Blocks.AIR.defaultBlockState();
        int i = 0;
        if (x >= 0) i |= 4;
        if (z >= 0) i |= 2;
        if (y >= 0) i |= 1;
        PalettedContainer<BlockState> current = sections[i];
        if (current == null) return Blocks.AIR.defaultBlockState();
        try {
            return current.get(x & 15, y & 15, z & 15);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get block at " + x + "," + y + "," + z);
        }
    }

    /**
     * 获取相对 origin 的最高方块 Y 坐标。
     */
    public int getTopY(int x, int z) {
        if (x >= 16 || z >= 16 || x < -16 || z < -16) return -32767;
        int i = 0;
        if (x >= 0) i |= 2;
        if (z >= 0) i |= 1;
        return maps[i].getFirstAvailable(x & 15, z & 15);
    }
    /**
     * 预计算当前 32×32 区域内的 topY。
     * 坐标覆盖相对 origin 的 [-16,16) × [-16,16)。
     */
    private void fillTopYCache(int[] topYCache) {
        for (int x = -CACHE_OFFSET; x < CACHE_OFFSET; x++) {
            for (int z = -CACHE_OFFSET; z < CACHE_OFFSET; z++) {
                topYCache[topYIndex(x, z)] = getTopY(x, z);
            }
        }
    }

    private static int topYIndex(int rx, int rz) {
        return ((rx + CACHE_OFFSET) << 5) | (rz + CACHE_OFFSET);
    }

    // ======================== 生命周期管理 ========================

    /**
     * 触发类加载，确保静态初始化在配置加载后执行。
     * 在 Mod 初始化阶段调用。
     */
    public static void init() {
    }

    /**
     * 清理当前线程的 ThreadLocal 缓冲区。
     * 在 ServerStoppingEvent 中调用。
     * <p>
     * 线程池中的工作线程缓冲区会在线程池关闭（shutdown + awaitTermination）后
     * 随线程终止自动释放。
     */
    public static void cleanup() {
        WORK_BUFFER.remove();
    }

    /**
     * xoroshiro128+ 伪随机数生成器（16B 状态，实例级，无共享无锁）。
     * <p>
     * 替代 SplittableRandom：后者的 nextInt(bound) 对非 2 的幂上界使用
     * 取模拒绝采样（idiv 数十周期，且可能多次拒绝）；本实现使用 Lemire
     * 高位乘法定界（Math.multiplyHigh，无除法、无拒绝循环）。
     * 偏差量级 2^-64，对蒙特卡洛温度估计在统计上无影响。
     */
    private static final class FastRandom {
        private long s0, s1;

        FastRandom(long seed) {
            // splitmix64 展开种子，避免弱种子
            long z = seed + 0x9E3779B97F4A7C15L;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            s0 = z ^ (z >>> 31);
            z = s0 + 0x9E3779B97F4A7C15L;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            s1 = z ^ (z >>> 31);
        }

        long nextLong() {
            long a = s0, b = s1;
            long r = a + b;
            b ^= a;
            s0 = Long.rotateLeft(a, 24) ^ b ^ (b << 16);
            s1 = Long.rotateLeft(b, 37);
            return r;
        }

        /**
         * 返回 [0, bound) 的均匀整数（Lemire 无拒绝采样）。
         * Java 17 无 Long.unsignedMultiplyHigh，用 multiplyHigh + 符号修正等效：
         * umulhi(r,b) = smulhi(r,b) + (r<0 ? b : 0)（b>0 恒成立）。
         */
        int nextInt(int bound) {
            long r = nextLong();
            long hi = Math.multiplyHigh(r, (long) bound);
            if (r < 0) hi += bound;
            return (int) hi;
        }

        /** 返回 {0,1,2} 均匀分布，无乘法/除法，平均 1.33 次迭代 */
        int nextInt3() {
            long r;
            do {
                r = nextLong() & 3;
            } while (r == 3);
            return (int) r;
        }
    }

    // ======================== 兼容性保留（未使用） ========================

    /**
     * 检查射线是否与方块碰撞并返回碰撞面。
     * 原代码保留方法，模拟循环中未使用。
     */
    @SuppressWarnings("unused")
    private Direction getHitingFace(double sx, double sy, double sz,
                                    double vx, double vy, double vz) {
        int bx = Mth.floor(sx + vx), by = Mth.floor(sy + vy), bz = Mth.floor(sz + vz);
        int rx = bx - ox, ry = by - oy, rz = bz - oz;
        BlockState bs = getBlock(rx, ry, rz);
        CachedBlockInfo ci = stateCache.computeIfAbsent(bs,
                s -> computeBlockInfo(rx, ry, rz, bx, by, bz));
        if (ci.isEmpty) return null;
        Vec3 svec = new Vec3(sx, sy, sz);
        Vec3 vvec = new Vec3(sx + vx, sy + vy, sz + vz);
        BlockHitResult brtr = AABB.clip(ci.aabbList, svec, vvec, new BlockPos(bx, by, bz));
        return brtr != null ? brtr.getDirection() : null;
    }
}


// ======================== 旧版原始逻辑（未经优化） ========================
// 保留作为注释，便于后续对照
//
// public class SurroundingTemperatureSimulator {
//     public static record SimulationResult(float blockTemp,float windStrength){
//
//     }
//
//     // Extract block data into shape and temperature, other data are disposed.
//
//     private static class CachedBlockInfo {
//         VoxelShape shape;
//         List<AABB> aabbList;
//         float temperature;
//         BlockState bs;
//
//         public CachedBlockInfo(VoxelShape shape, float temperature, BlockState bs) {
//             super();
//             this.shape = shape;
//             this.aabbList=shape.toAabbs();
//             this.temperature = temperature;
//
//             this.bs = bs;
//         }
//
//         public CachedBlockInfo(VoxelShape shape, BlockState bs) {
//             super();
//             this.shape = shape;
//             this.aabbList=shape.toAabbs();
//             this.bs = bs;
//         }
//     }
//
//     public static final int range = FHConfig.SERVER.SIMULATION.simulationRange.get();// through max range is 8, to avoid some rare issues, set it to 7 to keep count
//     private static final int n; //number of particles
//     private static final int rdiff = FHConfig.SERVER.SIMULATION.simulationDivision.get();//division of the unit square, changing this value would have no effect but improve precision
//     private static final double v0 = FHConfig.SERVER.SIMULATION.simulationParticleInitialSpeed.get();//initial particle speed
//     private static final VoxelShape EMPTY = Shapes.empty();
//     private static final VoxelShape FULL = Shapes.block();
//     private static Vec3[] speedVectors;// Vp, speed vector list, this list is constant and considered a distributed ball mesh.
//     private static final int num_rounds = 20;//THIS VALUE MUST NOT BE CONFIGURABLE AS THIS WOULD AFFECT RESULT//FHConfig.SERVER.simulationParticleLife.get();//propagate time-to-live for each particles
//     private static int[][] speedVectorByDirection = new int[6][];// index: ordinal value of outbounding facing
//
//     static {// generate speed vector list
//         Map<Direction, List<Integer>> lis = new EnumMap<>(Direction.class);
//         List<Vec3> v3fs = new ArrayList<>();
//         for (Direction dr : Direction.values()) {
//             lis.put(dr, new ArrayList<>());
//         }
//         int o = 0;
//         for (int i = -rdiff; i <= rdiff; ++i)
//             for (int j = -rdiff; j <= rdiff; ++j)
//                 for (int k = -rdiff; k <= rdiff; ++k) {
//                     if (i == 0 && j == 0 && k == 0)
//                         continue; // ignore zero vector
//                     float x = i * 1f / rdiff, y = j * 1f / rdiff, z = k * 1f / rdiff;
//                     float r = Mth.sqrt(x * x + y * y + z * z);
//                     if (r > 1)
//                         continue; // ignore vectors out of the unit ball
//                     Vec3 v3 = new Vec3(x / r * v0, y / r * v0, z / r * v0);
//                     v3fs.add(v3);
//                     if (v3.x > +0)
//                         lis.get(Direction.EAST).add(o);
//                     if (v3.x < -0)
//                         lis.get(Direction.WEST).add(o);
//                     if (v3.y > +0)
//                         lis.get(Direction.UP).add(o);
//                     if (v3.y < -0)
//                         lis.get(Direction.DOWN).add(o);
//                     if (v3.z > +0)
//                         lis.get(Direction.SOUTH).add(o);
//                     if (v3.z < -0)
//                         lis.get(Direction.NORTH).add(o);
//                     o++;
//                 }
//         n = o;
//         speedVectors = v3fs.toArray(new Vec3[o]);
//         for (Direction dr : Direction.values()) {
//             speedVectorByDirection[dr.ordinal()] = lis.get(dr).stream().mapToInt(t -> t).toArray();
//         }
//     }
//
//     @SuppressWarnings("unchecked")
//     public PalettedContainer<BlockState>[] sections = new PalettedContainer[8];// index: bitset of xzy(1 stands for +)
//     public Heightmap[] maps = new Heightmap[4]; // index: bitset of xz(1 stands for +)
//     BlockPos origin;
//     Random rnd;
//     //RandomSequence rrnd;
//     private Vec3[] Qpos = new Vec3[n];// Qpos, position of particle.
//     private int[] vid = new int[n];// IDv, particle speed index in speed vector list, this lower random cost.
//     //private double[] factor=
//     private Level level;
//
//
//     public Map<BlockState, CachedBlockInfo> info = new HashMap<>();// state to info cache
//
//     public Map<BlockPos, CachedBlockInfo> posinfo = new HashMap<>();// position to info cache
//
//     public static void init() {
//
//     }
//
//     public SurroundingTemperatureSimulator(ServerLevel world,double sx,double sy,double sz,boolean threadSafe) {
//         int sourceX = Mth.floor(sx), sourceY = Mth.floor(sy), sourceZ = Mth.floor(sz);
//         // these are block position offset
//         int offsetN = sourceZ - range;
//         int offsetW = sourceX - range;
//         int offsetD = sourceY - range;
//         // these are chunk position offset
//         int chunkOffsetW = offsetW >> 4;
//         int chunkOffsetN = offsetN >> 4;
//         int chunkOffsetD = offsetD >> 4;
//         // get origin point(center of 8 sections)
//         origin = new BlockPos((chunkOffsetW + 1) << 4, (chunkOffsetD + 1) << 4, (chunkOffsetN + 1) << 4);
//         // fetch all sections to lower calculation cost
//         int i = 0;
//
//         for (int x = chunkOffsetW; x <= chunkOffsetW + 1; x++)
//             for (int z = chunkOffsetN; z <= chunkOffsetN + 1; z++) {
//                 LevelChunk cnk = world.getChunk(x, z);
//                 int maxIndex=cnk.getSectionsCount();
//                 int index0=cnk.getSectionIndexFromSectionY(chunkOffsetD);
//                 int index1=index0+1;
//                 //cause chunk to prime heightmap
//                 cnk.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
//                 maps[i / 2] = cnk.getOrCreateHeightmapUnprimed(Types.MOTION_BLOCKING_NO_LEAVES);
//
//                 if(index0>=0&&index0<maxIndex)
//                     sections[i] = cnk.getSection(index0).getStates();
//                 //System.out.println(sections[i].get(0, 0, 0));
//                 if(index1>=0&&index1<maxIndex)
//                     sections[i + 1] = cnk.getSection(index1).getStates();
//                 //System.out.println(sections[i+1].get(0, 0, 0));
//                 i += 2;
//             }
//         //copy to avoid threading issue
//         if(threadSafe) {
//             for(int j=0;j<sections.length;j++)
//                 if(sections[j]!=null)
//                     sections[j]=sections[j].copy();
//
//         }
//         rnd = new Random(new BlockPos(sourceX,sourceY,sourceZ).asLong() ^ (world.getGameTime() >> 6));
//         level = world.getLevel();
//     }
//
//     // This fetch block in a delta location to origin,
//     // x,y,z must be in range [-16,16)
//
//     public BlockState getBlock(int x, int y, int z) {
//         if (x >= 16 || y >= 16 || z >= 16 || x < -16 || y < -16 || z < -16) // out of bounds
//             return Blocks.AIR.defaultBlockState();
//         int i = 0;
//         if (x >= 0)
//             i += 4;
//         if (z >= 0)
//             i += 2;
//         if (y >= 0)
//             i += 1;
//         PalettedContainer<BlockState> current = sections[i];
//         if (current == null)
//             return Blocks.AIR.defaultBlockState();
//         try {
//             return current.get(x & 15, y & 15, z & 15);
//         } catch (Exception ex) {
//             throw new RuntimeException("Failed to get block at" + x + "," + y + "," + z);
//         }
//     }
//
//     public int getTopY(int x, int z) {
//         if (x >= 16 || z >= 16 || x < -16 || z < -16) // out of bounds
//             return -32767;
//         int i = 0;
//         if (x >= 0)
//             i += 2;
//         if (z >= 0)
//             i += 1;
//         return maps[i].getFirstAvailable(x & 15, z & 15);
//     }
//
//     public int getOutboundSpeedFrom(Direction dir) {
//         if (dir == null)
//             return rnd.nextInt(speedVectors.length);
//         int[] iis = speedVectorByDirection[dir.ordinal()];
//         return iis[rnd.nextInt(iis.length)];
//     }
//
//     public SimulationResult getBlockTemperatureAndWind(double qx0, double qy0, double qz0) {
//         float wind = 0;
//         Vec3 q0 = new Vec3(qx0, qy0, qz0);
//         for (int i = 0; i < n; ++i)
//         {
//             Qpos[i] = q0;
//             vid[i] = i;
//         }
//
//         float heat = 0;
//         float minTemp=0;
//         float maxTemp=0;
//         for (int round = 0; round < num_rounds; ++round)
//         {
//             for (int i = 0; i < n; ++i)
//             {
//                 int nid = vid[i];
//                 Vec3 curspeed = speedVectors[vid[i]];
//                 Vec3 svec = Qpos[i];
//                 Vec3 dvec = svec.add(curspeed);
//                 BlockPos bpos = CUtils.vec2AlignedPos(dvec);
//                 CachedBlockInfo info = getInfoCached(bpos);
//
//                 VoxelShape shape = info.shape;
//                 BlockHitResult bhr = shape.clip(svec, dvec, bpos);
//                 if (bhr != null && (shape == FULL || bhr.isInside())) {
//                     BlockHitResult brtr = AABB.clip(info.aabbList, svec, dvec, bpos);
//                     if (brtr != null) {
//                         if (rnd.nextDouble() < 0.33f) {
//                             nid = rnd.nextInt(speedVectors.length);
//                         } else {
//                             nid = getOutboundSpeedFrom(brtr.getDirection());
//                         }
//                     } else {
//                         nid = rnd.nextInt(speedVectors.length);
//                     }
//                 }
//                 Qpos[i] = dvec;
//                 vid[i] = nid;
//                 float curheat=getHeat(bpos);
//                 if(curheat!=0) {
//                     minTemp=Math.min(minTemp, curheat);
//                     maxTemp=Math.max(maxTemp, curheat);
//                     heat += (float) (curheat * Mth.lerp(Mth.clamp(-(Math.signum(curheat))*curspeed.y(), 0, 0.4) * 2.5, 1, 0.5));
//                 }
//                 if(getAir(bpos)) {
//                     wind +=  2;
//                 }else if(bpos.distToCenterSqr(qx0, qy0, qz0)>=16&&getInfoCached(bpos).shape.isEmpty()) {
//                     wind+=.5;
//                 }
//             }
//         }
//         return new SimulationResult(Mth.clamp(heat / n, minTemp, maxTemp), wind / n);
//     }
//
//     // Get location temperature
//
//     private float getHeat(BlockPos bp) {
//         return getInfoCached(bp).temperature;
//     }
//
//     private boolean getAir(BlockPos pos) {
//         int topY=getTopY(pos.getX()-origin.getX(), pos.getZ()-origin.getZ());
//         return topY <= pos.getY();
//     }
//
//     private class CachedBlockInfoGetter implements Function<BlockState,CachedBlockInfo> {
//         BlockPos pos;
//         @Override
//         public CachedBlockInfo apply(BlockState t) {
//             CachedBlockInfo info= getInfo(pos, t);
//             return info;
//         }
//
//     }
//
//     CachedBlockInfoGetter generator=new CachedBlockInfoGetter();
//
//     // fetch without position cache, but with blockstate cache, blocks with the same
//     // state should have same collider and heat.
//
//     private CachedBlockInfo getInfo(BlockPos pos) {
//         generator.pos=pos;
//         BlockPos ofregion = pos.subtract(origin);
//         BlockState bs = getBlock(ofregion.getX(), ofregion.getY(), ofregion.getZ());
//         return info.computeIfAbsent(bs, generator);
//     }
//
//     // Just fetch block temperature and collision without cache.
//     // Position is only for getCollisionShape method, to avoid some TE based shape.
//
//     private CachedBlockInfo getInfo(BlockPos pos, BlockState bs) {
//         BlockTempData b = BlockTempData.getData(bs.getBlock());
//         VoxelShape shape;
//
//         if(bs.getBlock().hasDynamicShape()) {
//             shape=Shapes.block();
//         }else {
//             try {
//                 shape=bs.getCollisionShape(null, pos);
//             }catch(Exception ex) {
//                 ex.printStackTrace();
//                 shape=Shapes.block();
//             }
//         }
//         if (b == null)
//             return new CachedBlockInfo(shape, bs);
//         float cblocktemp = 0;
//         if (b.isLit()) {
//             boolean litOrActive = bs.hasProperty(BlockStateProperties.LIT) && bs.getValue(BlockStateProperties.LIT);
//             if (litOrActive)
//                 cblocktemp += b.getTemp();
//         } else
//             cblocktemp += b.getTemp();
//         if (b.isLevel()) {
//             if (bs.hasProperty(BlockStateProperties.LEVEL)) {
//                 cblocktemp *= (float) (bs.getValue(BlockStateProperties.LEVEL) + 1) / 16;
//             } else if (bs.hasProperty(BlockStateProperties.LEVEL_COMPOSTER)) {
//                 cblocktemp *= (float) (bs.getValue(BlockStateProperties.LEVEL_COMPOSTER) + 1) / 9;
//             } else if (bs.hasProperty(BlockStateProperties.LEVEL_FLOWING)) {
//                 cblocktemp *= (float) (bs.getValue(BlockStateProperties.LEVEL_FLOWING)) / 8;
//             } else if (bs.hasProperty(BlockStateProperties.LEVEL_CAULDRON)) {
//                 cblocktemp *= (float) (bs.getValue(BlockStateProperties.LEVEL_CAULDRON) + 1) / 4;
//             }
//         }
//         return new CachedBlockInfo(shape, cblocktemp, bs);
//     }
//
//     // Since a position is highly possible to be fetched for multiple times, add
//     // cache in normal fetch
//
//     private CachedBlockInfo getInfoCached(BlockPos pos) {
//         return posinfo.computeIfAbsent(pos, this::getInfo);
//     }
//
//     // Check if this location collides with block.
//
//     private Direction getHitingFace(double sx, double sy, double sz, double vx, double vy, double vz) {
//         BlockPos bpos = new BlockPos((int) (sx + vx), (int) (sy + vy), (int) (sz + vz));
//         CachedBlockInfo info = getInfoCached(bpos);
//         if (info.shape == EMPTY)
//             return null;
//         Vec3 svec = new Vec3(sx, sy, sz);
//         Vec3 vvec = new Vec3(sx + vx, sy + vy, sz + vz);
//         BlockHitResult brtr = AABB.clip(info.shape.toAabbs(), svec, vvec, bpos);
//         if (brtr != null)
//             return brtr.getDirection();
//         return null;
//     }
// }
//}
