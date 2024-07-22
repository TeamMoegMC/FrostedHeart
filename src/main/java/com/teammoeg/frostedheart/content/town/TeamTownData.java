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
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.base.team.SpecialData;
import com.teammoeg.frostedheart.base.team.SpecialDataHolder;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.UUIDCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.server.ServerWorld;

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
            CodecUtil.defaultValue(Codec.STRING, "Default Town").fieldOf("name").forGetter(o->o.name),
			CodecUtil.defaultValue(CodecUtil.mapCodec(TownResourceType.CODEC, Codec.INT), ImmutableMap.of()).fieldOf("resource").forGetter(o->o.resources),
			CodecUtil.defaultValue(CodecUtil.mapCodec(TownResourceType.CODEC, Codec.INT), ImmutableMap.of()).fieldOf("backupResource").forGetter(o->o.backupResources),
			CodecUtil.defaultValue(CodecUtil.mapCodec("pos", CodecUtil.BLOCKPOS, "data", TownWorkerData.CODEC), ImmutableMap.of()).fieldOf("blocks").forGetter(o->o.blocks),
			CodecUtil.defaultValue(CodecUtil.mapCodec("uuid",UUIDCodec.CODEC,"data",Resident.CODEC), ImmutableMap.of()).fieldOf("residents").forGetter(o->o.residents),
            CodecUtil.defaultValue(CodecUtil.mapCodec("pos", CodecUtil.BLOCKPOS, "residents", Codec.list(UUIDCodec.CODEC)), ImmutableMap.of()).fieldOf("workAssignStatus").forGetter(o->o.workAssigningStatus),
            CodecUtil.defaultValue(CodecUtil.mapCodec("pos", CodecUtil.BLOCKPOS, "residents", Codec.list(UUIDCodec.CODEC)), ImmutableMap.of()).fieldOf("houseAllocatingStatus").forGetter(o->o.houseAllocatingStatus)

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
    /**
     * Saves what residents are working in a worker block
     */
    public Map<BlockPos, List<UUID>> workAssigningStatus = new HashMap<>();
    /**
     * Saves what residents are living in a house
     */
    Map<BlockPos, List<UUID>> houseAllocatingStatus = new HashMap<>();
    
	public TeamTownData(String name, Map<TownResourceType, Integer> resources, Map<TownResourceType, Integer> backupResources, Map<BlockPos, TownWorkerData> blocks, Map<UUID, Resident> residents, Map<BlockPos, List<UUID>> houseAllocatingStatus, Map<BlockPos, List<UUID>> workAssigningStatus) {
		super();
		this.name = name;
		this.resources.putAll(resources);
		this.backupResources.putAll(backupResources);
		this.blocks.putAll(blocks);
		this.residents.putAll(residents);
        this.houseAllocatingStatus.putAll(houseAllocatingStatus);
        this.workAssigningStatus.putAll(workAssigningStatus);
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
        PriorityQueue<TownWorkerData> pq = new PriorityQueue<>(Comparator.comparingLong(TownWorkerData::getPriority).reversed());
        for(TownWorkerData workerData : blocks.values()){
            if(world.isAreaLoaded(workerData.getPos(), 1)){
                workerData.setWorkData(world);
            }
            if(AbstractTownWorkerTileEntity.isValid(workerData)){
                //由于已经使用了自动刷新城镇方块的功能，已经不需要通过isWorkValid来在获取合法性信息时刷新。
                //在抽象类AbstractTownWorkerTileEntity中已经定义了townWorkerState来确定和保存合法性，因此可以直接使用静态方法isValid判断是否是合法的数据
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

    public void tickMorning(ServerWorld world){
        this.removeNonTownBlocks(world);
        this.checkOccupiedAreaOverlap(world);
        this.residentAllocatingCheck(world);
        this.allocateHouse();
        this.assignWork();
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
                OccupiedArea comparingWorkerOccupiedArea = AbstractTownWorkerTileEntity.getOccupiedArea(comparingWorkerData);
                OccupiedArea workerOccupiedArea = AbstractTownWorkerTileEntity.getOccupiedArea(workerData);
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
                if(AbstractTownWorkerTileEntity.getWorkerState(data) != TownWorkerState.OCCUPIED_AREA_OVERLAPPED){
                    TownTileEntity townTileEntity = (TownTileEntity) Utils.getExistingTileEntity(world, data.getPos());
                    townTileEntity.setWorkerState(TownWorkerState.OCCUPIED_AREA_OVERLAPPED);
                }
            }else if(AbstractTownWorkerTileEntity.getWorkerState(data) != TownWorkerState.OCCUPIED_AREA_OVERLAPPED){
                TownTileEntity townTileEntity = (TownTileEntity) Utils.getExistingTileEntity(world, data.getPos());
                townTileEntity.setWorkerState(TownWorkerState.NOT_INITIALIZED);
            }
        }
    }
    //1 每天早上检查存在blocks里面的TownTileEntity是否存在
    // 2 每天早上检查house/workAllocatingStatus里面不存在的blocks，以及大于上限的Resident
    // 3 在house/workAllocatingStatus里面添加存在于blocks里面的合适worker
    private void residentAllocatingCheck(ServerWorld world){
        ArrayList<TownWorkerData> availableWorkersConsumingResidents = new ArrayList<>();
        ArrayList<TownWorkerData> availableHouse = new ArrayList<>();

        for(BlockPos key_pos : blocks.keySet()){
            //检查block对应区块是否加载，如果已加载，检查TE是否存在。不存在，从blocks里删掉。若存在，更新workData。
            if(world.isAreaLoaded(key_pos, 1)){
                AbstractTownWorkerTileEntity te = (AbstractTownWorkerTileEntity) Utils.getExistingTileEntity(world, key_pos);
                if(te == null){
                    blocks.remove(key_pos);
                    continue;
                }
                else{
                    blocks.get(key_pos).fromTileEntity(te);
                }
            }
            //挑出所有合法的house和需要居民的worker，如果有houseAllocatingStatus和workAssigningStatus中未包含的worker，加进去。
            TownWorkerData data = blocks.get(key_pos);
            if(data.getType() == TownWorkerType.HOUSE){
                if(AbstractTownWorkerTileEntity.isValid(data.getWorkData())){
                    availableHouse.add(data);
                    if(!houseAllocatingStatus.containsKey(key_pos)){
                        houseAllocatingStatus.put(key_pos, new ArrayList<>());
                    }
                }
            }
            if(data.getType().needsResident()){
                if(AbstractTownWorkerTileEntity.isValid(data.getWorkData())){
                    availableWorkersConsumingResidents.add(data);
                    if(!workAssigningStatus.containsKey(key_pos)){
                        workAssigningStatus.put(key_pos, new ArrayList<>());
                    }
                }
            }
        }
        //检查houseAllocatingStatus和workAssigningStatus，移除里面多余的或不存在的居民；移除里面不存在的block。
        ArrayList<BlockPos> toRemoveHouse = new ArrayList<>();
        for(Map.Entry<BlockPos, List<UUID>> entry : houseAllocatingStatus.entrySet()){
            if(!availableHouse.contains(blocks.get(entry.getKey()))){
                toRemoveHouse.add(entry.getKey());
                continue;
            }
            entry.getValue().removeIf(uuid -> !residents.containsKey(uuid));
            int exceedingResidents = entry.getValue().size() - blocks.get(entry.getKey()).getWorkData().getInt("maxResidents");
            if(exceedingResidents > 0){
                for(int i = 0; i < exceedingResidents; i++){
                    residents.get(entry.getValue().get(entry.getValue().size() - 1)).setHousePos(null);
                    entry.getValue().remove(entry.getValue().size() - 1);
                }
            }
        }
        for(BlockPos pos : toRemoveHouse){
            houseAllocatingStatus.get(pos).forEach(uuid -> residents.get(uuid).setHousePos(null));
            houseAllocatingStatus.remove(pos);
        }
        ArrayList<BlockPos> toRemoveWorker = new ArrayList<>();
        for(Map.Entry<BlockPos, List<UUID> > entry : workAssigningStatus.entrySet()){
            if(!availableWorkersConsumingResidents.contains(blocks.get(entry.getKey()))){
                toRemoveWorker.add(entry.getKey());
                continue;
            }
            entry.getValue().removeIf(uuid -> !residents.containsKey(uuid));
            int exceedingResidents = entry.getValue().size() - blocks.get(entry.getKey()).getWorkData().getInt("maxResidents");
            if(exceedingResidents > 0){
                for(int i = 0; i < exceedingResidents; i++){
                    residents.get(entry.getValue().get(entry.getValue().size() - 1)).setWorkPos(null);
                    entry.getValue().remove(entry.getValue().size() - 1);
                }
            }
        }
        for(BlockPos pos : toRemoveWorker){
            workAssigningStatus.get(pos).forEach(uuid -> residents.get(uuid).setHousePos(null));
            workAssigningStatus.remove(pos);
        }
    }

    private void allocateHouse(){
        Set<BlockPos> availableHouses = houseAllocatingStatus.keySet();
        Iterator<BlockPos> iterator = availableHouses.iterator();
        BlockPos currentHousePos;
        if(iterator.hasNext()) currentHousePos = iterator.next();
        else return;
        for(Resident resident : residents.values()){//遍历所有的居民，对每个居民进行分配
            TownWorkerData currentHouseData = blocks.get(currentHousePos);
            while(resident.getHousePos() == null){//仅分配无住房居民
                List<UUID> currentHouseAllocatingStatus = houseAllocatingStatus.getOrDefault(currentHouseData.getPos(), new ArrayList<>());
                if(currentHouseData.getWorkData().getInt("maxResident") > currentHouseAllocatingStatus.size()){//若有空位，则将resident分配到此房屋内
                    currentHouseAllocatingStatus.add(resident.getUUID());
                    resident.setHousePos(currentHouseData.getPos());
                }
                if(iterator.hasNext()) currentHousePos = iterator.next();//若此房屋已满，切换到下一个房间
                else return;//若没有更多的房间了，停止遍历居民。
            }
        }
    }



    public void assignWork(){
        Set<BlockPos> availableWorkers = workAssigningStatus.keySet();
        ArrayList<UUID> availableResidents = new ArrayList<>(residents.keySet());
        for(int i=0;i<1024;i++){
            TownWorkerData topPriorityWorker = null;
            double topPriority = Double.NEGATIVE_INFINITY;
            for(BlockPos key_pos : availableWorkers){
                TownWorkerData data = blocks.get(key_pos);
                double priority = data.getType().getResidentPriority(workAssigningStatus.get(key_pos), data);
                if(topPriorityWorker == null){
                    topPriorityWorker = data;
                    topPriority = priority;
                    continue;
                }
                if(priority >= topPriority){
                    topPriorityWorker = data;
                    topPriority = priority;
                }
            }
            if(topPriorityWorker == null || topPriority == Double.NEGATIVE_INFINITY || availableResidents.isEmpty()){
                break;
            }
            UUID bestResident = null;
            TownWorkerType topPriorityWorkerType = topPriorityWorker.getType();
            Double bestResidentScore = Double.NEGATIVE_INFINITY;
            for(int j = 0; j< availableResidents.size(); j++){
                UUID uuid = availableResidents.get(j);
                Resident resident = residents.get(uuid);
                Double score = resident.getWorkScore(topPriorityWorkerType);
                if(score == Double.NEGATIVE_INFINITY){
                    availableResidents.remove(j);
                    j--;
                }
                if(score == 0.0){
                    continue;
                }
                if(bestResidentScore <= resident.getWorkScore(topPriorityWorkerType)){
                    bestResident = uuid;
                    bestResidentScore = resident.getWorkScore(topPriorityWorkerType);
                }
            }
            if(bestResident != null){
                availableResidents.remove(bestResident);
                workAssigningStatus.getOrDefault(topPriorityWorker.getPos(), new ArrayList<>());
                workAssigningStatus.get(topPriorityWorker.getPos()).add(bestResident);
                residents.get(bestResident).setWorkPos(topPriorityWorker.getPos());
            }
        }
    }


}
