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

import blusunrize.immersiveengineering.common.util.Utils;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.base.team.SpecialData;
import com.teammoeg.frostedheart.base.team.SpecialDataHolder;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.town.mine.MineTileEntity;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.UUIDCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;

import java.util.*;
import java.util.stream.Collectors;

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
        PriorityQueue<TownWorkerData> pq = new PriorityQueue<>(Comparator.comparingLong(TownWorkerData::getPriority).reversed());
        for(TownWorkerData workerData : blocks.values()){
            if(world.isAreaLoaded(workerData.getPos(), 1)){
                workerData.toTileEntity(world);
                workerData.updateFromTileEntity(world);
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
        //for (TownWorkerData t : pq) {
        //    t.setData(world);
        //}
        //在目前的运行逻辑中，work方法不会改变任何应存储在TileEntity中的信息，因此暂时将此内容放在所有work之前。
        itt.finishWork();
    }

    public void tickMorning(ServerWorld world){
        this.updateAllBlocks(world);
        this.checkOccupiedAreaOverlap(world);
        this.connectMineAndBase();
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

    void updateAllBlocks(ServerWorld world){
        Iterator<TownWorkerData> iterator = blocks.values().iterator();
        while(iterator.hasNext()){
            TownWorkerData data = iterator.next();
            BlockPos pos = data.getPos();
            data.loaded = false;
            if (world.isBlockLoaded(pos)) {
                data.loaded = true;
                BlockState bs = world.getBlockState(pos);
                TileEntity te = Utils.getExistingTileEntity(world, pos);
                TownWorkerType type = data.getType();
                if(type.getBlock() != bs.getBlock() || !(te instanceof TownTileEntity)){
                    iterator.remove();
                } else{
                    data.updateFromTileEntity((TownTileEntity)te);
                }
            }
        }
    }

    //这个方法会直接将不能正常工作(包括结构不完整、温度不适合等情况)的worker直接踢出town，难以再找回
    @Deprecated
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
            data.setOverlappingState(overlappedWorkers.contains(data));
        }
    }

    private void residentAllocatingCheck(ServerWorld world){
        //清空residents里所有居民存储的的house和work位置，之后再加回来，以刷新居民的工作和房屋
        residents.values().forEach(resident -> {
            resident.setHousePos(null);
            resident.setWorkPos(null);
        });
        //移除house/worker里超过上限，或已不存在的的resident
        for(TownWorkerData data : blocks.values()){
            if(data.getType() == TownWorkerType.HOUSE || data.getType().needsResident()){
                ListNBT residentListNBT = data.getResidents();
                residentListNBT.removeIf(residentNBT -> !residents.containsKey(UUID.fromString(residentNBT.getString())));
                int maxResident = data.getWorkData().getCompound("tileEntity").getInt("maxResident");
                while(residentListNBT.size() > maxResident){
                    residentListNBT.remove(residentListNBT.size() - 1);
                }
                residentListNBT.stream()
                        .map(nbt -> UUID.fromString(nbt.getString()))
                        .forEach(resident -> {
                            if(data.getType() == TownWorkerType.HOUSE) residents.get(resident).setHousePos(data.getPos());
                            else residents.get(resident).setWorkPos(data.getPos());
                        });
            }
        }
    }

    private void connectMineAndBase(){
        ArrayList<TownWorkerData> mineBases = new ArrayList<>();
        HashMap<TownWorkerData, BlockPos> mineMap = new HashMap<>();        //TownWorkerData: mine, BlockPos: basePos
        for(TownWorkerData data : blocks.values()){
            if(data.getType() == TownWorkerType.MINE_BASE && AbstractTownWorkerTileEntity.isValid(data)){
                mineBases.add(data);
            }
            if(data.getType() == TownWorkerType.MINE){
                mineMap.put(data, null);
            }
        }
        for(TownWorkerData mineBase : mineBases){
            mineBase.getWorkData().getCompound("tileEntity").getList("linkedMines", Constants.NBT.TAG_LONG).stream()
                    .map(nbt -> BlockPos.fromLong(((LongNBT)nbt).getLong()))
                    .forEach(pos -> mineMap.put(blocks.get(pos), mineBase.getPos()));
        }
        mineMap.forEach(MineTileEntity::setLinkedBase);
    }

    //distribute homeless residents to house
    private void allocateHouse() {
        ArrayList<TownWorkerData> availableHouses = blocks.values().stream()
                .filter(data -> data.getType() == TownWorkerType.HOUSE && data.getMaxResident() > data.getResidents().size())
                .sorted(Comparator.comparingDouble(data -> -data.getWorkData().getCompound("tileEntity").getDouble("rating")))//优先分配评分最高的house。因此在rating前面加了负号。
                .collect(Collectors.toCollection(ArrayList::new));
        if(availableHouses.isEmpty()) return;
        Iterator<TownWorkerData> houseIterator = availableHouses.iterator();
        TownWorkerData currentHouseData = houseIterator.next();
        ListNBT residentListNBT = currentHouseData.getResidents();
        int maxResident = currentHouseData.getMaxResident();
        Iterator<Resident> residentIterator = residents.values().iterator();
        while (true) {//遍历所有居民
            Resident resident = residentIterator.next();
            if (resident.getHousePos() == null) {//为没有house的居民分配进当前的house(暂存在ListNBT中)
                residentListNBT.add(StringNBT.valueOf(resident.getUUID().toString()));
                resident.setHousePos(currentHouseData.getPos());
            }
            if (residentListNBT.size() >= maxResident) {//如果当前house满了，将暂存在ListNBT中的居民信息存入TownWorkerData，然后尝试进入下一个house
                currentHouseData.setDataFromTown("residents", residentListNBT);
                if (houseIterator.hasNext()) {
                    currentHouseData = houseIterator.next();
                    residentListNBT = currentHouseData.getResidents();
                    maxResident = currentHouseData.getMaxResident();
                } else {
                    return;
                }
            }
            if(!residentIterator.hasNext()){//如果全部居民遍历完成，将暂存在ListNBT中的居民信息存入TownWorkerData，然后退出循环
                currentHouseData.setDataFromTown("residents", residentListNBT);
                return;
            }
        }
    }

    class TempDataHolder{//一个暂时存储TownWorkerData、优先级和居民的类，减少对NBT的读写
        public TempDataHolder(TownWorkerData data){
            this.townWorkerData = data;
            this.residentPriority = townWorkerData.getResidentPriority();
            this.residentListNBT = null;
        }
        public TownWorkerData townWorkerData;
        public double residentPriority;
        public ListNBT residentListNBT;
        public void saveResidents(){
            if(residentListNBT != null) townWorkerData.setDataFromTown("residents", residentListNBT);
        }
        public void addResident(UUID uuid){
            if(residentListNBT == null){
                residentListNBT = townWorkerData.getResidents();
            }
            residentListNBT.add(StringNBT.valueOf(uuid.toString()));
            residents.get(uuid).setWorkPos(townWorkerData.getPos());
            residentPriority = townWorkerData.getResidentPriority(residentListNBT.size());
        }
    }

    public void assignWork(){
        ArrayList<UUID> availableResidents = new ArrayList<>(residents.keySet());

        ArrayList<TempDataHolder> availableWorkers = (ArrayList<TempDataHolder>) blocks.values().stream()
                .filter(data -> data.getType().needsResident())
                .map(TempDataHolder::new)
                .sorted(Comparator.comparingDouble(o -> -o.residentPriority))//降序排列
                .collect(Collectors.toList());
        if(availableWorkers.size() == 0) return;
        for(int i=0;i<1024;i++){//无意义，仅用于限制循环次数
            TempDataHolder topPriorityWorker = availableWorkers.get(0);//由于已经降序排列，取集合最靠前的为最高优先级
            if(topPriorityWorker.residentPriority == Double.NEGATIVE_INFINITY || availableResidents.isEmpty()){
                break;//没有可分的工作或居民，则退出循环，进入保存数据阶段
            }
            UUID bestResident = null;
            TownWorkerType topPriorityWorkerType = topPriorityWorker.townWorkerData.getType();
            Double bestResidentScore = Double.NEGATIVE_INFINITY;
            for(int j = 0; j< availableResidents.size(); j++){
                UUID uuid = availableResidents.get(j);
                Resident resident = residents.get(uuid);
                Double score = resident.getWorkScore(topPriorityWorkerType);
                if(score == Double.NEGATIVE_INFINITY){//score为负无穷大时，代表这个居民本身存在问题，无法进行任何工作。
                    availableResidents.remove(j);
                    j--;
                }
                if(score == 0.0){//score为0时，无法进行此类工作，但有可能进行其它工作。
                    continue;
                }
                if(bestResidentScore <= resident.getWorkScore(topPriorityWorkerType)){
                    bestResident = uuid;
                    bestResidentScore = resident.getWorkScore(topPriorityWorkerType);
                }
            }
            if(bestResident != null){
                topPriorityWorker.addResident(bestResident);//将居民加入worker（暂存，所有循环结束后存入TownWorkerData）
                availableResidents.remove(bestResident);
                for(int j = 0; j< availableWorkers.size()-1; j++){//居民的存入改变了worker的优先级，需要重新排列。
                    //除了之前优先级最高的worker，其余worker本身已经排序好，优先级也没有改变。
                    //因此，要完成排序，只需要将之前优先级最高的worker从位置0挪到合适的位置即可。
                    //所以这里只有一层循环，并且在j+1的优先级不小于j的时候，也就是之前优先级最高的worker从位置0挪到合适的位置的时候，直接退出循环
                    if(availableWorkers.get(j).residentPriority < availableWorkers.get(j+1).residentPriority){
                        Collections.swap(availableWorkers, j, j+1);
                    } else break;
                }
            }
        }
        availableWorkers.forEach(TempDataHolder::saveResidents);
    }


}
