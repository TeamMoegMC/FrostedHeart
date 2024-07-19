/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.util.LazyOptional;

public class ChunkHeatData{
	public static final MapCodec<List<IHeatArea>> LIST_CODEC=CodecUtil.optionalFieldOfs(Codec.list(
		CodecUtil.dispatch(IHeatArea.class)
		.type("cubic", CubicHeatArea.class,CubicHeatArea.CODEC)
		.type("pillar", PillarHeatArea.class, PillarHeatArea.CODEC)
		.buildByInt()
		),o->o.emptyList(),"adjs","temperature");
	public static final Codec<ChunkHeatData> CODEC=RecordCodecBuilder.create(t->t.group(
		LIST_CODEC.forGetter(o->o.adjusters.values().stream().collect(Collectors.toList()))).apply(t, ChunkHeatData::new));

    private Map<BlockPos,IHeatArea> adjusters = new LinkedHashMap<>();


    /**
     * Used on a ServerWorld context to add temperature in certain 3D region in a
     * ChunkData instance
     * Updates server side cache first.
     */
    private static void addChunkAdjust(IWorld world, ChunkPos chunkPos, IHeatArea adjx) {
        if (world != null && !world.isRemote()) {
            IChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
            ChunkHeatData data = ChunkHeatData.getCapability(chunk).orElseGet(() -> null);
            if (data != null) {
                data.adjusters.remove(adjx.getCenter());
                data.adjusters.put(adjx.getCenter(),adjx);
            }
        }
    }

    /**
     * Used on a ServerWorld context to add temperature in a cubic region
     *
     * @param world   must be server side
     * @param heatPos the position of the heating block, at the center of the cube
     * @param range   the distance from the heatPos to the boundary
     * @param tempMod the temperature added
     */
    public static void addCubicTempAdjust(IWorld world, BlockPos heatPos, int range, int tempMod) {
        removeTempAdjust(world, heatPos);//remove current first
        int sourceX = heatPos.getX(), sourceZ = heatPos.getZ();

        // these are block position offset
        int offsetN = sourceZ - range;
        int offsetS = sourceZ + range + 1;
        int offsetW = sourceX - range;
        int offsetE = sourceX + range + 1;

        // these are chunk position offset
        int chunkOffsetW = offsetW >> 4;
        int chunkOffsetE = offsetE >> 4;
        int chunkOffsetN = offsetN >> 4;
        int chunkOffsetS = offsetS >> 4;
        // add adjust to effected chunks
        IHeatArea adj = new CubicHeatArea(heatPos, range, tempMod);
        for (int x = chunkOffsetW; x <= chunkOffsetE; x++)
            for (int z = chunkOffsetN; z <= chunkOffsetS; z++) {
                ChunkPos cp = new ChunkPos(x, z);
                addChunkAdjust(world, cp, adj);
            }
    }

    /**
     * Used on a ServerWorld context to add temperature in a piller region
     *
     * @param world   must be server side
     * @param heatPos the position of the heating block, at the center of the cube
     * @param range   the distance from the heatPos to the boundary
     * @param up      y range above the plane
     * @param down    y range below the plane
     * @param tempMod the temperature added
     */
    public static void addPillarTempAdjust(IWorld world, BlockPos heatPos, int range, int up, int down, int tempMod) {
        removeTempAdjust(world, heatPos);
        int sourceX = heatPos.getX(), sourceZ = heatPos.getZ();

        // these are block position offset
        int offsetN = sourceZ - range;
        int offsetS = sourceZ + range + 1;
        int offsetW = sourceX - range;
        int offsetE = sourceX + range + 1;

        // these are chunk position offset
        int chunkOffsetW = offsetW >> 4;
        int chunkOffsetE = offsetE >> 4;
        int chunkOffsetN = offsetN >> 4;
        int chunkOffsetS = offsetS >> 4;
        // add adjust to effected chunks
        IHeatArea adj = new PillarHeatArea(heatPos, range, up, down, tempMod);
        for (int x = chunkOffsetW; x <= chunkOffsetE; x++)
            for (int z = chunkOffsetN; z <= chunkOffsetS; z++) {
                ChunkPos cp = new ChunkPos(x, z);
                addChunkAdjust(world, cp, adj);
            }
    }

    /**
     * Used on a ServerWorld context to add temperature adjust.
     *
     * @param world must be server side
     * @param adj   adjust
     */
    public static void addTempAdjust(IWorld world, IHeatArea adj) {

        int sourceX = adj.getCenter().getX(), sourceZ = adj.getCenter().getZ();
        removeTempAdjust(world, new BlockPos(sourceX, adj.getCenter().getY(), sourceZ));
        // these are block position offset
        int offsetN = sourceZ - adj.getRadius();
        int offsetS = sourceZ + adj.getRadius() + 1;
        int offsetW = sourceX - adj.getRadius();
        int offsetE = sourceX + adj.getRadius() + 1;

        // these are chunk position offset
        int chunkOffsetW = offsetW >> 4;
        int chunkOffsetE = offsetE >> 4;
        int chunkOffsetN = offsetN >> 4;
        int chunkOffsetS = offsetS >> 4;
        // add adjust to effected chunks
        for (int x = chunkOffsetW; x <= chunkOffsetE; x++)
            for (int z = chunkOffsetN; z <= chunkOffsetS; z++) {
                ChunkPos cp = new ChunkPos(x, z);
                addChunkAdjust(world, cp, adj);
            }
    }

    /**
     * Used on a ServerWorld context to add temperature in a cubic region
     *
     * @param world   must be server side
     * @param heatPos the position of the heating block, at the center of the cube
     * @param range   the distance from the heatPos to the boundary
     * @param tempMod the temperature added
     * @deprecated use {@link ChunkHeatData#addCubicTempAdjust}
     */
    @Deprecated
    public static void addTempToCube(IWorld world, BlockPos heatPos, int range, byte tempMod) {
        addCubicTempAdjust(world, heatPos, range, tempMod);
    }

    public static ChunkHeatData get(IWorld world, BlockPos pos) {
        return get(world, new ChunkPos(pos)).orElse(null);
    }

    /**
     * Called to get chunk data when a world context is available.
     * On client, will query capability, falling back to cache, and send request
     * packets if necessary
     * On server, will either query capability falling back to cache, or query
     * provider to generate the data.
     */
    @SuppressWarnings("deprecation")
	public static Optional<ChunkHeatData> get(IWorldReader world, ChunkPos pos) {
        // Query cache first, picking the correct cache for the current logical side
        //ChunkData data = ChunkDataCache.get(world).get(pos);
        //if (data == null) {
        //System.out.println("no cache at"+pos);
        if (world instanceof IWorld)
            return ((IWorld) world).getChunkProvider().isChunkLoaded(pos) ? getCapability(world.getChunk(pos.asBlockPos()))
                    .resolve() : Optional.empty();
        return world.chunkExists(pos.x, pos.z) ? getCapability(world.getChunk(pos.asBlockPos())).resolve() : Optional.empty();
        //}
        //return data;
    }

    /**
     * Called to get temperature when a world context is available.
     * on server, will either query capability falling back to cache, or query
     * provider to generate the data.
     * This method directly get temperature at any positions.
     */
    public static float getAdditionTemperature(IWorldReader world, BlockPos pos) {
        return get(world, new ChunkPos(pos)).map(t->t.getAdditionTemperatureAtBlock(world, pos)).orElse(0f);
    }

    /**
     * Called to get temperature adjusts at location when a world context is available.
     * on server, will either query capability falling back to cache, or query
     * provider to generate the data.
     * This method directly get temperature adjusts at any positions.
     */
    public static Collection<IHeatArea> getAdjust(IWorldReader world, BlockPos pos) {
        ArrayList<IHeatArea> al = new ArrayList<>(get(world, new ChunkPos(pos)).map(ChunkHeatData::getAdjusters).orElseGet(Arrays::asList));
        al.removeIf(adj -> !adj.isEffective(pos));
        return al;
    }

    /**
     * If there is any adjust at the position.
     */
    public static boolean hasAdjust(IWorldReader world, BlockPos pos) {
        return !getAdjust(world, pos).isEmpty();
    }

    /**
     * Helper method, since lazy optionals and instanceof checks together are ugly
     */
    public static LazyOptional<ChunkHeatData> getCapability(@Nullable IChunk chunk) {
    	return FHCapabilities.CHUNK_HEAT.getCapability(chunk);
    }

    /**
     * Called to get temperature when a world context is available.
     * on server, will either query capability falling back to cache, or query
     * provider to generate the data.
     * This method directly get temperature at any positions.
     */
    public static float getTemperature(IWorldReader world, BlockPos pos) {
        return get(world, new ChunkPos(pos)).map(t->t.getTemperatureAtBlock(world, pos)).orElseGet(()->WorldTemperature.getTemperature(world, pos));
    }
    public static String toDisplaySoil(float temp) {
    	temp=Math.max(temp, -20);
    	temp=Math.min(temp, 30);
    	temp+=20;
    	temp*=2;
    	return (int)temp+"%";
    }
    /**
     * Used on a ServerWorld context to set temperature in certain 3D region in a
     * ChunkData instance
     * Updates server side cache first. Then send a sync packet to every client.
     */
    private static void removeChunkAdjust(IWorld world, ChunkPos chunkPos, BlockPos src) {
        if (world != null && !world.isRemote()) {
            IChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
            ChunkHeatData data = ChunkHeatData.getCapability(chunk).orElseGet(() -> null);
            // TODO: should use isPresent some how
            if (data != null)
                data.adjusters.remove(src);
        }
    }

    /**
     * Used on a ServerWorld context to set temperature in certain 3D region in a
     * ChunkData instance
     * Updates server side cache first. Then send a sync packet to every client.
     */
    private static void removeChunkAdjust(IWorld world, ChunkPos chunkPos, IHeatArea adj) {
        if (world != null && !world.isRemote()) {
            IChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
            ChunkHeatData data = ChunkHeatData.getCapability(chunk).orElseGet(() -> null);
            if (data != null)
                data.adjusters.remove(adj);
        }
    }
    /**
     * Used on a ServerWorld context to reset a temperature area
     *
     * @param world   must be server side
     * @param heatPos the position of the heating block, at the center of the area
     */
    public static void removeTempAdjust(IWorld world, BlockPos heatPos) {
        int sourceX = heatPos.getX(), sourceZ = heatPos.getZ();
        ChunkHeatData cd = ChunkHeatData.get(world, heatPos);
        if(cd==null)return;
        IHeatArea oadj = cd.getAdjustAt(heatPos);
        if (oadj == null) return;
        int range = oadj.getRadius();

        // these are block position offset
        int offsetN = sourceZ - range;
        int offsetS = sourceZ + range + 1;
        int offsetW = sourceX - range;
        int offsetE = sourceX + range + 1;

        // these are chunk position offset
        int chunkOffsetW = offsetW >> 4;
        int chunkOffsetE = offsetE >> 4;
        int chunkOffsetN = offsetN >> 4;
        int chunkOffsetS = offsetS >> 4;

        for (int x = chunkOffsetW; x <= chunkOffsetE; x++)
            for (int z = chunkOffsetN; z <= chunkOffsetS; z++)
                removeChunkAdjust(world, new ChunkPos(x, z), heatPos);
    }

    /**
     * Used on a ServerWorld context to remove a temperature area
     *
     * @param world must be server side
     * @param adj   adjust
     */
    public static void removeTempAdjust(IWorld world, IHeatArea adj) {
        int sourceX = adj.getCenter().getX(), sourceZ = adj.getCenter().getZ();

        // these are block position offset
        int offsetN = sourceZ - adj.getRadius();
        int offsetS = sourceZ + adj.getRadius() + 1;
        int offsetW = sourceX - adj.getRadius();
        int offsetE = sourceX + adj.getRadius() + 1;

        // these are chunk position offset
        int chunkOffsetW = offsetW >> 4;
        int chunkOffsetE = offsetE >> 4;
        int chunkOffsetN = offsetN >> 4;
        int chunkOffsetS = offsetS >> 4;
        for (int x = chunkOffsetW; x <= chunkOffsetE; x++)
            for (int z = chunkOffsetN; z <= chunkOffsetS; z++)
                removeChunkAdjust(world, new ChunkPos(x, z), adj);
    }

    /**
     * Used on a ServerWorld context to reset a temperature area
     *
     * @param world   must be server side
     * @param heatPos the position of the heating block, at the center of the cube
     * @deprecated use {@link ChunkHeatData#removeTempAdjust}
     */
    @Deprecated
    public static void resetTempToCube(IWorld world, BlockPos heatPos) {
        removeTempAdjust(world, heatPos);
    }

    public ChunkHeatData() {
        reset();
    }



    /**
     * Get Temperature in a world at a location
     *
     * @param world world in
     * @param pos   position
     */
    float getAdditionTemperatureAtBlock(IWorldReader world, BlockPos pos) {
        if (adjusters.isEmpty()) return 0;
        float ret = 0, tmp;
        for (IHeatArea adj : adjusters.values()) {
            if (adj.isEffective(pos)) {
                tmp = adj.getValueAt(pos);
                if (tmp > ret)
                    ret = tmp;
            }
        }
        return ret;
    }

    public IHeatArea getAdjustAt(BlockPos pos) {
        return adjusters.get(pos);
    }

    public Collection<IHeatArea> getAdjusters() {
        return adjusters.values();
    }




    /**
     * Get Temperature in a world at a location
     *
     * @param world world in
     * @param pos   position
     */
    float getTemperatureAtBlock(IWorldReader world, BlockPos pos) {
        if (adjusters.isEmpty()) return WorldTemperature.getTemperature(world, pos);
        float ret = 0, tmp;
        for (IHeatArea adj : adjusters.values()) {
            if (adj.isEffective(pos)) {
                tmp = adj.getValueAt(pos);
                if (tmp > ret)
                    ret = tmp;
            }
        }
        return WorldTemperature.getTemperature(world, pos) + ret;
    }

    private void reset() {
        adjusters.clear();

    }
    public ChunkHeatData(List<IHeatArea> adjusters) {
		super();
		reset();
		setAdjusters(adjusters);
	}

	public void setAdjusters(List<IHeatArea> adjusters) {
		for(IHeatArea adjust:adjusters)
			this.adjusters.put(adjust.getCenter(), adjust);
    }

}
