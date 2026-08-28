package com.teammoeg.chorda.util;
import java.util.Arrays;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public class CachedLevel {
	private interface HeightProvider{
		int getFirstAvailable(int pX, int pZ);
	}
	public static class HeightmapCache implements HeightProvider{
	   private final BitStorage data;
	   private final int minBuildingHeight;
	   public HeightmapCache(ChunkAccess pChunk, long[] pData) {
	      this.minBuildingHeight = pChunk.getMinBuildHeight();
	      int i = Mth.ceillog2(pChunk.getHeight() + 1);
	      this.data = new SimpleBitStorage(i, 256, Arrays.copyOf(pData, pData.length));
	   }
	   @Override
	   public int getFirstAvailable(int pX, int pZ) {
	      return this.getFirstAvailable(getIndex(pX, pZ));
	   }
	   public int getHighestTaken(int pX, int pZ) {
	      return this.getFirstAvailable(getIndex(pX, pZ)) - 1;
	   }
	   private int getFirstAvailable(int pIndex) {
	      return this.data.get(pIndex) + minBuildingHeight;
	   }
	   public long[] getRawData() {
	      return this.data.getRaw();
	   }
	   private static int getIndex(int pX, int pZ) {
	      return pX + pZ * 16;
	   }
	}
	
    private final int minChunkX, maxChunkX, minChunkZ, maxChunkZ, minChunkY, maxChunkY;
    private final int chunkWidth, chunkDepth, chunkHeight;

    private final PalettedContainer<BlockState>[] sections;
    private final HeightProvider[] heightmaps;

    @SuppressWarnings("unchecked")
    public CachedLevel(int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ,
                      int minChunkY, int maxChunkY) {
        this.minChunkX = minChunkX;
        this.maxChunkX = maxChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkZ = maxChunkZ;
        this.minChunkY = minChunkY;
        this.maxChunkY = maxChunkY;
        this.chunkWidth = maxChunkX - minChunkX + 1;
        this.chunkDepth = maxChunkZ - minChunkZ + 1;
        this.chunkHeight = maxChunkY - minChunkY + 1;
        int totalChunks = chunkWidth * chunkDepth;
        int totalSections = totalChunks * chunkHeight;
        this.sections = new PalettedContainer[totalSections];
        this.heightmaps = new HeightProvider[totalChunks];
    }

    private int getSectionIndex(int chunkX, int chunkY, int chunkZ) {
        return getSectionIndex(getChunkIndex(chunkX, chunkZ), chunkY);
    }
    private int getSectionIndex(int chunkIdx, int chunkY) {
        return chunkIdx * chunkHeight + chunkY - minChunkY;
    }
    private int getChunkIndex(int chunkX, int chunkZ) {
        return (chunkX - minChunkX) * chunkDepth + (chunkZ - minChunkZ);
    } 
    /**
     * 通过世界坐标直接获取方块状态（O(1)）
     * @param x 世界X坐标
     * @param y 世界Y坐标
     * @param z 世界Z坐标
     * @return 该位置的 BlockState，若超出缓存范围则返回空气
     */
    public BlockState getBlockState(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx < minChunkX || cx > maxChunkX || cz < minChunkZ || cz > maxChunkZ || cy < minChunkY || cy > maxChunkY) {
            return Blocks.AIR.defaultBlockState();
        }
        int chunkIdx = getSectionIndex(cx, cy, cz);
        PalettedContainer<BlockState> container = sections[chunkIdx];
        if (container == null) {
            return Blocks.AIR.defaultBlockState();
        }
        int localX = x & 15;
        int localY = y & 15;   // section 内偏移（0~15）
        int localZ = z & 15;
        return container.get(localX, localY, localZ);
    }

    /**
     * 通过世界坐标获取 WORLD_SURFACE 高度（O(1)）
     * @param x 世界X坐标
     * @param z 世界Z坐标
     * @return 地表高度（最高非空气方块），若缓存中无该列数据则返回 -1
     */
    public int getSurfaceHeight(int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        if (cx < minChunkX || cx > maxChunkX || cz < minChunkZ || cz > maxChunkZ) {
            return -1;
        }
        int chunkIdx = getChunkIndex(cx, cz);
        HeightProvider hm = heightmaps[chunkIdx];
        return (hm == null) ? -1 : hm.getFirstAvailable(x & 15, z & 15);
    }
    /**
     * 从 Level 中提取指定 AABB 范围内的区块数据，生成 CachedLevel 缓存对象。
     * 
     * <p><b>关于 copy 参数：</b><br>
     * - 若 {@code copy == false}，则直接引用原世界对象。这种方式速度快、内存小，但缓存数据会随世界更新而改变，
     *   适用于瞬时查询（如在 tick 内使用）。<br>
     * - 若 {@code copy == true}，则执行深拷贝，生成一份独立快照。
     *   此时缓存不随原世界变化，适用于需要长期保留或多线程分析。
     * 
     * @param level  目标世界
     * @param aabb   轴对齐包围盒，用于确定提取的区域
     * @param copy   是否拷贝区块数据（true=深拷贝快照，false=直接引用）
     * @return 包含指定范围内所有相关 Section 和高度图的 CachedLevel 实例
     */
    public static CachedLevel getCachedLevel(Level level, AABB aabb, boolean copy) {
        // 1. 计算 AABB 覆盖的整数方块范围
        int minX = Mth.floor(aabb.minX);
        int maxX = Mth.ceil(aabb.maxX) - 1;
        int minY = Mth.floor(aabb.minY);
        int maxY = Mth.ceil(aabb.maxY) - 1;
        int minZ = Mth.floor(aabb.minZ);
        int maxZ = Mth.ceil(aabb.maxZ) - 1;

        // 2. 涉及的区块范围
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        // 3. 涉及的 Section 范围（全局索引）
        int minChunkY = (minY >> 4) - level.getMinSection();
        int maxChunkY = (maxY >> 4) - level.getMinSection();

        // 4. 创建数据容器
        CachedLevel data = new CachedLevel(minChunkX, maxChunkX, minChunkZ, maxChunkZ, minChunkY, maxChunkY);

        // 5. 遍历所有涉及的区块
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                int chunkIdx = data.getChunkIndex(cx, cz);
                // ---- 高度图 ----
                Heightmap hm = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE);
                HeightProvider hmp;
                if(copy) {
                	hmp = new HeightmapCache(chunk,hm.getRawData());
                }else {
                	hmp = hm::getFirstAvailable;
                }
                data.heightmaps[chunkIdx] = hmp;

                // ---- Section 数据 ----
                for (int cy = minChunkY; cy <= maxChunkY; cy++) {
                	LevelChunkSection section=chunk.getSection(cy);
                    if (section == null || section.hasOnlyAir()) continue;
                    PalettedContainer<BlockState> container;
                    if(copy) {
                    	container = section.getStates().copy();
                    }else {
                    	container = section.getStates();
                    }
                    data.sections[data.getSectionIndex(chunkIdx, cy)] = container;
                }
            }
        }

        return data;
    }
}