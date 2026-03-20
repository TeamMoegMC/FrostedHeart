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
public class SurroundingTemperatureSimulatorAIOptimization {


    public static record SimulationResult(float blockTemp, float windStrength) {
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
     * 避免每次模拟调用分配大数组，通过世代标记实现 O(1) 重置。
     */
    private static final class WorkBuffer {
        // 粒子状态数组（SoA 布局）
        final double[] qx = new double[n];
        final double[] qy = new double[n];
        final double[] qz = new double[n];

        final double[] pvx = new double[n];
        final double[] pvy = new double[n];
        final double[] pvz = new double[n];

        // 位置缓存：32^3 = 32768 个槽位，覆盖 [-16,16) 三个轴
        final CachedBlockInfo[] posCache = new CachedBlockInfo[CACHE_SIZE];
        final int[] gen = new int[CACHE_SIZE]; // 世代标记
        int currentGen = 1;

        //优化：预计算高度图（32×32 = 1024 个 int）
        final int[] topYCache = new int[CACHE_DIM * CACHE_DIM];

        /**
         * O(1) 重置：仅递增世代号，旧缓存自动失效
         */
        void reset() {
            currentGen++;
            // int 溢出保护：溢出到 ≤0 时强制清空（约 21 亿次调用才触发一次）
            if (currentGen <= 0) {
                Arrays.fill(gen, 0);
                currentGen = 1;
            }
        }

        /**
         * 读取缓存：仅当世代匹配时返回，否则视为未缓存
         */
        CachedBlockInfo getCached(int idx) {
            return (gen[idx] == currentGen) ? posCache[idx] : null;
        }

        /**
         * 写入缓存并标记当前世代
         */
        void putCached(int idx, CachedBlockInfo info) {
            posCache[idx] = info;
            gen[idx] = currentGen;
        }
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
    private SplittableRandom rnd;  // 无锁随机数（替代 java.util.Random 的 CAS）

    // BlockState → CachedBlockInfo 去重缓存（实例级，避免跨世界污染）
    private final IdentityHashMap<BlockState, CachedBlockInfo> stateCache =
            new IdentityHashMap<>(256);

    // ======================== 构造器 ========================

    public SurroundingTemperatureSimulatorAIOptimization(ServerLevel world, double sx, double sy, double sz,
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

        rnd = new SplittableRandom(
                BlockPos.asLong(sourceX, sourceY, sourceZ) ^ (world.getGameTime() >> 6));
    }

    // ======================== 核心模拟 ========================

    public SimulationResult getBlockTemperatureAndWind(double qx0, double qy0, double qz0) {
        // 获取当前线程的工作缓冲区并重置（O(1)）
        WorkBuffer buf = WORK_BUFFER.get();
        buf.reset();

        // 初始化所有粒子：位置 = 玩家位置，速度索引 = 序号
        Arrays.fill(buf.qx, qx0);
        Arrays.fill(buf.qy, qy0);
        Arrays.fill(buf.qz, qz0);
        System.arraycopy(speedVX, 0, buf.pvx, 0, n);
        System.arraycopy(speedVY, 0, buf.pvy, 0, n);
        System.arraycopy(speedVZ, 0, buf.pvz, 0, n);

        //预计算高度图
        fillTopYCache(buf.topYCache);

        float heat = 0f, wind = 0f, minTemp = 0f, maxTemp = 0f;
        // 局部变量缓存，JIT 优化更友好
        final int lox = this.ox, loy = this.oy, loz = this.oz;
        final double[] localPvx = buf.pvx, localPvy = buf.pvy, localPvz = buf.pvz;
        final double[] localQx = buf.qx, localQy = buf.qy, localQz = buf.qz;
        final int[] topYCache = buf.topYCache;

        for (int round = 0; round < num_rounds; ++round) {
            for (int i = 0; i < n; ++i) {
                final double vx = localPvx[i];
                final double vy = localPvy[i];
                final double vz = localPvz[i];

                final double sx = localQx[i], sy = localQy[i], sz = localQz[i];
                final double dx = sx + vx, dy = sy + vy, dz = sz + vz;
                final int bx = Mth.floor(dx), by = Mth.floor(dy), bz = Mth.floor(dz);

                // ---- 缓存查找（世代标记 + 直接数组索引） ----
                final CachedBlockInfo info = getInfoFast(buf, bx, by, bz);

                // ---- 碰撞处理（三路分支：FULL / 部分形状 / EMPTY） ----
                if (info.isFull) {
                    // FULL 方块：解析法 Slab Method 求入射面，零对象分配
                    int sxi = Mth.floor(sx), syi = Mth.floor(sy), szi = Mth.floor(sz);
                    if (sxi != bx || syi != by || szi != bz) {
                        // 从外部进入 → 计算入射面并反弹
                        int nid;
                        if (rnd.nextInt(3) == 0) {
                            nid = rnd.nextInt(n);
                        } else {
                            nid = getOutboundSpeedFrom(
                                    computeEntryFace(sx, sy, sz, vx, vy, vz, bx, by, bz));
                        }
                        // ★ 仅在反弹时写入 3 个 double（替代每轮写入 1 个 int）
                        localPvx[i] = speedVX[nid];
                        localPvy[i] = speedVY[nid];
                        localPvz[i] = speedVZ[nid];
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
                            nid = rnd.nextInt(3) == 0
                                    ? rnd.nextInt(n)
                                    : getOutboundSpeedFrom(brtr.getDirection());
                        } else {
                            nid = rnd.nextInt(n);
                        }

                        localPvx[i] = speedVX[nid];
                        localPvy[i] = speedVY[nid];
                        localPvz[i] = speedVZ[nid];
                    }
                }
                // EMPTY：无碰撞，nid 不变，零开销

                // ---- 更新粒子状态 ----
                localQx[i] = dx;
                localQy[i] = dy;
                localQz[i] = dz;

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
                int rx = bx - lox;
                int rz = bz - loz;

                int topY;
                if (rx >= -CACHE_OFFSET && rx < CACHE_OFFSET &&
                        rz >= -CACHE_OFFSET && rz < CACHE_OFFSET) {
                    topY = topYCache[topYIndex(rx, rz)];
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
     * 快速缓存查找：世代标记 + 直接数组索引。
     * 坐标范围 [-16,16) 映射到 [0,32) 再编码为一维索引。
     */
    private CachedBlockInfo getInfoFast(WorkBuffer buf, int bx, int by, int bz) {
        int rx = bx - ox, ry = by - oy, rz = bz - oz;

        // 边界检查：超出已加载区段范围则视为空气
        if (rx < -CACHE_OFFSET || rx >= CACHE_OFFSET ||
                ry < -CACHE_OFFSET || ry >= CACHE_OFFSET ||
                rz < -CACHE_OFFSET || rz >= CACHE_OFFSET) {
            return AIR_INFO;
        }

        // 编码为一维索引：每轴 5 bit，共 15 bit，范围 [0, 32767]
        int idx = ((rx + CACHE_OFFSET) << 10) | ((ry + CACHE_OFFSET) << 5) | (rz + CACHE_OFFSET);

        CachedBlockInfo ci = buf.getCached(idx);
        if (ci == null) {
            ci = computeBlockInfo(rx, ry, rz, bx, by, bz);
            buf.putCached(idx, ci);
        }
        return ci;
    }

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