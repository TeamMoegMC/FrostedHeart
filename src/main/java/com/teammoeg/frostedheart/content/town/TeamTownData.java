/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.town;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.base.team.SpecialData;
import com.teammoeg.frostedheart.base.team.SpecialDataHolder;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.town.hunting.HuntingBaseTileEntity;
import com.teammoeg.frostedheart.content.town.hunting.HuntingCampTileEntity;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.UUIDCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;

/**
 * Town data for a whole team.
 * <p>
 * It maintains town resources, worker data, and holds a team data
 * when initialized.
 * <p>
 * Everything permanent should be saved in this class.
 */
public class TeamTownData implements SpecialData{
	public static final Codec<TeamTownData> CODEC=RecordCodecBuilder.create(t->t.group(
			Codec.STRING.fieldOf("name").forGetter(o->o.name),
			CodecUtil.mapCodec(TownResourceType.CODEC, Codec.INT).fieldOf("resource").forGetter(o->o.resources),
			CodecUtil.mapCodec(TownResourceType.CODEC, Codec.INT).fieldOf("backupResource").forGetter(o->o.backupResources),
			CodecUtil.mapCodec("pos", CodecUtil.BLOCKPOS, "data", TownWorkerData.CODEC).fieldOf("blocks").forGetter(o->o.blocks),
			CodecUtil.defaultValue(CodecUtil.mapCodec("uuid",UUIDCodec.CODEC,"data",Resident.CODEC), ImmutableMap.of()).fieldOf("residents").forGetter(o->o.residents)
		).apply(t, TeamTownData::new));
    /**
     * The town name.
     */
	String name;
    /**
     * The town residents.
     */
    Map<UUID, Resident> residents = new LinkedHashMap<>();
	/**
     * Resource generated from resident
     */
    Map<TownResourceType, Integer> resources = new EnumMap<>(TownResourceType.class);
    /**
     * Resource provided by player
     */
    Map<TownResourceType, Integer> backupResources = new EnumMap<>(TownResourceType.class);
    /**
     * Town blocks and their worker data
     */
    Map<BlockPos, TownWorkerData> blocks = new LinkedHashMap<>();
    
	public TeamTownData(String name, Map<TownResourceType, Integer> resources, Map<TownResourceType, Integer> backupResources, Map<BlockPos, TownWorkerData> blocks, Map<UUID, Resident> residents) {
		super();
		this.name = name;
		this.resources.putAll(resources);
		this.backupResources.putAll(backupResources);
		this.blocks.putAll(blocks);
		this.residents.putAll(residents);
	}
    public TeamTownData(SpecialDataHolder teamData) {
        super();
        if(teamData instanceof TeamDataHolder) {
        	TeamDataHolder data=(TeamDataHolder) teamData;
	        if (data.getTeam().isPresent()) {
	            this.name = data.getTeam().get().getDisplayName() + "'s Town";
	        } else {
	            this.name = data.getOwnerName() + "'s Town";
            }
        }
    }

    /**
     * Town logic update (every 20 ticks).
     * This method first validates the town blocks, then sorts them by priority and calls the work methods.
     *
     * @param world server world instance
     */
    public void tick(ServerWorld world) {
        int randomInt = world.getRandom().nextInt(64);//used to do some non-urgent check
        if(randomInt == 1){//check if occupied area overlapped
            checkOccupiedAreaOverlap(world);
        }else removeAllInvalidTiles(world);
        PriorityQueue<TownWorkerData> pq = new PriorityQueue<>(Comparator.comparingLong(TownWorkerData::getPriority).reversed());
        pq.addAll(blocks.values());
        TeamTown itt = new TeamTown(this);
        for (TownWorkerData t : pq) {
            t.firstWork(itt);
        }
        for (TownWorkerData t : pq) {
            t.beforeWork(itt);
        }
        for (TownWorkerData t : pq) {
            t.work(itt);
        }
        for (TownWorkerData t : pq) {
            t.afterWork(itt);
        }
        for (TownWorkerData t : pq) {
            t.lastWork(itt);
        }
        for (TownWorkerData t : pq) {
            t.setData(world);
        }
        itt.finishWork();
    }

    void removeNonTownBlocks(ServerWorld world) {
        blocks.values().removeIf(v -> {
            BlockPos pos = v.getPos();
            v.loaded = false;
            if (world.isBlockLoaded(pos)) {
                v.loaded = true;
                BlockState bs = world.getBlockState(pos);
                TileEntity te = Utils.getExistingTileEntity(world, pos);
                TownWorkerType twt = v.getType();
                return twt.getBlock() != bs.getBlock() || !(te instanceof TownTileEntity);
            }
            return false;
        });
    }
    void removeAllInvalidTiles(ServerWorld world){
        blocks.values().removeIf(v -> {
            BlockPos pos = v.getPos();
            v.loaded = false;
            if (world.isBlockLoaded(pos)) {
                v.loaded = true;
                BlockState bs = world.getBlockState(pos);
                TileEntity te = Utils.getExistingTileEntity(world, pos);
                TownWorkerType twt = v.getType();
                return twt.getBlock() != bs.getBlock() || !(te instanceof TownTileEntity) || !(((TownTileEntity)te).isWorkValid());
            }
            return false;
        });
    }

	@Override
	public void setHolder(SpecialDataHolder holder) {

	}

    private void checkOccupiedAreaOverlap(ServerWorld world){
        removeNonTownBlocks(world);
        List<TownTileEntity> tileEntityList = blocks.keySet().stream().map(pos -> (TownTileEntity)Utils.getExistingTileEntity(world, pos)).collect(Collectors.toList());
        List<TownTileEntity> tileEntityListCopy = new ArrayList<>(tileEntityList);
        Set<TownTileEntity> tileEntitiesMightOverlap = new HashSet<>();
        for (TownTileEntity townTileEntity : tileEntityListCopy) {
            Iterator<TownTileEntity> subIterator = tileEntityListCopy.iterator();
            if (subIterator.hasNext()) {
                subIterator.next();
                subIterator.remove();
            }
            while (subIterator.hasNext()) {
                TownTileEntity comparingTE = subIterator.next();
                if (townTileEntity.getOccupiedArea().doRectanglesIntersect(comparingTE.getOccupiedArea())) {
                    tileEntitiesMightOverlap.add(comparingTE);
                    tileEntitiesMightOverlap.add(townTileEntity);
                }
            }
        }
        Map<ColumnPos, TownTileEntity> occupiedAreaCollectingMap = new HashMap<>();
        Set<TownTileEntity> overlappedTileEntities = new HashSet<>();

        //利用townTileEntity的OccupiedArea判断是否有重叠
        for(TownTileEntity townTileEntity : tileEntitiesMightOverlap){
            for(ColumnPos columnPos : townTileEntity.getOccupiedArea().getOccupiedArea()){
                if(occupiedAreaCollectingMap.containsKey(columnPos)){
                    overlappedTileEntities.add(townTileEntity);
                    overlappedTileEntities.add(occupiedAreaCollectingMap.get(columnPos));
                }
                occupiedAreaCollectingMap.put(columnPos, townTileEntity);
            }
        }
        for(TownTileEntity townTileEntity : tileEntityList){
            if(overlappedTileEntities.contains(townTileEntity)){
                townTileEntity.setWorkerState(TownWorkerState.OCCUPIED_AREA_OVERLAPPED);
            } else if(townTileEntity.getWorkerState() == TownWorkerState.OCCUPIED_AREA_OVERLAPPED){
                townTileEntity.setWorkerState(TownWorkerState.NOT_INITIALIZED);
            }
        }
        blocks.keySet().removeIf(pos -> !((TownTileEntity)Utils.getExistingTileEntity(world, pos)).isWorkValid());
    }

}
