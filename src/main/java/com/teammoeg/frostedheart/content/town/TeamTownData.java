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
import com.teammoeg.frostedheart.content.town.house.HouseTileEntity;
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
        removeNonTownBlocks(world);
        int randomInt = world.getRandom().nextInt(64);//used to do some non-urgent check
        if(randomInt == 1) checkOccupiedAreaOverlap(world);
        if(randomInt == 2) distributeResidents(world);
        PriorityQueue<TownWorkerData> pq = new PriorityQueue<>(Comparator.comparingLong(TownWorkerData::getPriority).reversed());
        for(TownWorkerData workerData : blocks.values()){
            if(TownBuildingCoreBlockTileEntity.isValid(workerData)){
                //由于已经使用了自动刷新城镇方块的功能，已经不需要通过isWorkValid来在获取合法性信息时刷新。
                //在抽象类TownBuildingCoreBlockTileEntity中已经定义了townWorkerState来确定和保存合法性，因此可以直接使用静态方法isValid判断是否是合法的数据
                // 不再使用isWorkValid，从而减少获取TileEntity的次数
                pq.add(workerData);
            }
        }
        //pq.addAll(blocks.values());
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
        //removeNonTownBlocks(world);
        Collection<TownWorkerData> workerDataCollection =  new ArrayList<>(blocks.values());
        List<TownWorkerData> workerDataList = new ArrayList<>(workerDataCollection);
        Map<TownWorkerData, OccupiedArea> workersMightOverlap = new HashMap<>();
        for (TownWorkerData workerData : workerDataList) {
            Iterator<TownWorkerData> subIterator = workerDataList.iterator();
            if (subIterator.hasNext()) {
                subIterator.next();
                subIterator.remove();
            }
            while (subIterator.hasNext()) {
                TownWorkerData comparingWorkerData = subIterator.next();
                OccupiedArea comparingWorkerOccupiedArea = TownBuildingCoreBlockTileEntity.getOccupiedArea(comparingWorkerData);
                OccupiedArea workerOccupiedArea = TownBuildingCoreBlockTileEntity.getOccupiedArea(workerData);
                if (workerOccupiedArea.doRectanglesIntersect(comparingWorkerOccupiedArea)) {
                    workersMightOverlap.put(workerData, workerOccupiedArea);
                    workersMightOverlap.put(comparingWorkerData, comparingWorkerOccupiedArea);
                }
            }
        }
        Map<ColumnPos, TownWorkerData> occupiedAreaCollectingMap = new HashMap<>();
        Set<TownWorkerData> overlappedWorkers = new HashSet<>();

        //利用townTileEntity的OccupiedArea判断是否有重叠
        for(Map.Entry<TownWorkerData, OccupiedArea> entry : workersMightOverlap.entrySet()){
            for(ColumnPos columnPos : entry.getValue().getOccupiedArea()){
                if(occupiedAreaCollectingMap.containsKey(columnPos)){
                    overlappedWorkers.add(entry.getKey());
                    overlappedWorkers.add(occupiedAreaCollectingMap.get(columnPos));
                }
                occupiedAreaCollectingMap.put(columnPos, entry.getKey());
            }
        }
        for(TownWorkerData data : workerDataCollection){
            if(overlappedWorkers.contains(data)){
                if(TownBuildingCoreBlockTileEntity.getWorkerState(data) != TownWorkerState.OCCUPIED_AREA_OVERLAPPED){
                    TownTileEntity townTileEntity = (TownTileEntity) Utils.getExistingTileEntity(world, data.getPos());
                    townTileEntity.setWorkerState(TownWorkerState.OCCUPIED_AREA_OVERLAPPED);
                }
            }else if(TownBuildingCoreBlockTileEntity.getWorkerState(data) != TownWorkerState.OCCUPIED_AREA_OVERLAPPED){
                TownTileEntity townTileEntity = (TownTileEntity) Utils.getExistingTileEntity(world, data.getPos());
                townTileEntity.setWorkerState(TownWorkerState.NOT_INITIALIZED);
            }
        }
    }

    //distribute homeless residents to house
    private void distributeResidents(ServerWorld world) {
        List<TownWorkerData> availableHouses = new ArrayList<>();
        //List<Resident> residentsHasHouse = new ArrayList<>();
        LinkedList<Resident> residentsHomeless = new LinkedList<>(residents.values());//this is a Queue
        for(TownWorkerData data : blocks.values()){//get all houses that can accommodate more residents
            if(data.getType() != TownWorkerType.HOUSE) continue;//remove non house blocks
            CompoundNBT houseNBT = data.getWorkData();
            if(!TownBuildingCoreBlockTileEntity.isValid(houseNBT)) continue;//remove invalid houses
            int houseMaxResident = houseNBT.getInt("maxResident");
            if(houseMaxResident <= 0) continue;//remove houses that can't accommodate residents
            ListNBT residentListNBT = houseNBT.getList("resident", Constants.NBT.TAG_COMPOUND);
            for(INBT inbt : residentListNBT){//遍历house中的居民，以判断城镇中的居民是否有房
                Resident resident = new Resident(inbt);
                if(!this.residents.containsKey(resident.getUUID())){//检测到错误居民，立即清除
                    ((HouseTileEntity) Objects.requireNonNull(world.getTileEntity(data.getPos()))).removeResident(resident);
                }
                residentsHomeless.remove(resident);
            }
            int houseResident = residentListNBT.size();
            if(houseResident >= houseMaxResident) continue;//remove full houses
            if(!world.isBlockLoaded(data.getPos())) continue;//remove unloaded houses.对于未加载的房间，我们没法获取TileEntity，也就做不到向其中添加居民
            availableHouses.add(data);
        }
        if(residentsHomeless.isEmpty() || availableHouses.isEmpty()) return;
        availableHouses.sort(Comparator.comparingDouble(house -> - house.getWorkData().getDouble("rating")));//将用于比较的rating加上了负号，以将rating大的排在前面
        Iterator<TownWorkerData> availableHousesIterator = availableHouses.iterator();
        HouseTileEntity currentHouseTileEntity = (HouseTileEntity) Utils.getExistingTileEntity(world, availableHousesIterator.next().getPos());
        for(Resident resident : residentsHomeless){
            while(!currentHouseTileEntity.addResident(resident)){
                if(!availableHousesIterator.hasNext()) return;
                currentHouseTileEntity = (HouseTileEntity) Utils.getExistingTileEntity(world, availableHousesIterator.next().getPos());
            }
        }
    }

}
