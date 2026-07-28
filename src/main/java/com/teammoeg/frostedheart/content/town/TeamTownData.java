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

package com.teammoeg.frostedheart.content.town;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.event.*;
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import com.teammoeg.frostedheart.content.town.util.ObservableTownMap;
import lombok.Getter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceData;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ITown data for a whole team.
 * <p>
 * It maintains town resources, worker data, and holds a team data when
 * initialized.
 * <p>
 * Everything permanent should be saved in this class.
 */
public class TeamTownData implements SpecialData{
    public static final Codec<TeamTownData> CODEC = RecordCodecBuilder.create(t -> t.group(
        //Only prevent decoding failures in this field from breaking the whole object.
        CodecUtil.defaultSupply(Codec.STRING, () -> "Default Town")
        .fieldOf("name").forGetter(o -> o.name),

        CodecUtil.defaultSupply(CodecUtil.catchingCodec(TeamTownResourceHolder.CODEC), TeamTownResourceHolder::new)
        .fieldOf("resources").forGetter(o -> o.resources),

        CodecUtil.defaultSupply(CodecUtil.catchingCodec(CodecUtil.mapCodec("pos", BlockPos.CODEC, "building", AbstractTownBuilding.CODEC)), ObservableTownMap::new)
        .fieldOf("blocks").forGetter(o -> new HashMap<>(o.buildings)),

        CodecUtil.defaultSupply(CodecUtil.catchingCodec(CodecUtil.mapCodec("uuid", UUIDUtil.CODEC, "data", Resident.CODEC)), ObservableTownMap::new)
        .fieldOf("residents").forGetter(o -> o.residents),

        CodecUtil.defaultSupply(CodecUtil.catchingCodec(CodecUtil.mapCodec("type", CodecUtil.enumCodec(TerrainResourceType.values()), "data", TerrainResourceData.CODEC)), HashMap::new)
        .fieldOf("terrainResource").forGetter(o -> o.terrainResource),

        CodecUtil.defaultSupply(CodecUtil.catchingCodec(Codec.INT), () -> 0)
        .fieldOf("labour").forGetter(o -> o.labour),

        CodecUtil.defaultSupply(CodecUtil.catchingCodec(Codec.INT), () -> 0)
        .fieldOf("maxLabour").forGetter(o -> o.maxLabour)

        )

        .apply(t, TeamTownData::new));
//    public static final Codec<TeamTownData> CODEC = CodecUtil.debugCodec(CODEC_TOWN);
    /**
     * The town name.
     */
    String name = "Default ITown";
    /**
     * The town residents.
     */
    ObservableTownMap<UUID, Resident> residents = new ObservableTownMap<>();
    /**
     * ITown resources. Including normal resources and town services. Including
     * resources gathered from town and resources gathered from player. Must be
     * changed by TownResourceManager.
     */
    TeamTownResourceHolder resources = new TeamTownResourceHolder();
    /**
     * ITown blocks and their worker data
     */
    ObservableTownMap<BlockPos, AbstractTownBuilding> buildings = new ObservableTownMap<>();


    Map<TerrainResourceType, TerrainResourceData> terrainResource=new EnumMap<>(TerrainResourceType.class);
    @Getter
    int labour=0;
    @Getter
    int maxLabour=0;

    /**
     * 用于将城镇数据变化的监听器塞到各个地方。
     * 由于只需要在第一个tick进行，所以弄个boolean记一下、
     * 由于我希望只在服务端这么做，所以不放在构造方法中。
     */
    private boolean listenerInitialized = false;
    @Getter
    private final DataSyncCache dataSyncCache = new DataSyncCache();



    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource,int labour,int maxlabour) {
        super();
        this.name = name;
        this.resources = resources;
        buildings.forEach((pos, building) -> {
            if(building instanceof AbstractTownBuilding abstractTownBuilding){
                this.buildings.put(pos, abstractTownBuilding);
            }
        });
        this.residents.putAll(residents);
        this.terrainResource.putAll(terrainResource);
        this.labour=0;
        this.maxLabour=0;
    }

    public TeamTownData(SpecialDataHolder teamData) {
        super();
        if (teamData instanceof TeamDataHolder data) {
            this.name = data.getTeam().getName() + "'s Town";
        }
    }

    /**
     * 获取以本实例为data的TeamTown。
     *
     * @return 以本实例为data的TeamTown。
     */
    public TeamTown createTeamTown() {
        return TeamTown.create(this);
    }

    /**
     * process some town logic that needs to run every tick, like sync data to client
     * @param level server world instance
     * @param teamData tickSecond有这个参数，所以我也顺便加上了
     */
    public void tick(ServerLevel level, TeamDataHolder teamData) {
        if(!this.listenerInitialized){
            this.buildings.setOnAttach(b -> b.setChangeEventListener(this.dataSyncCache));
            this.residents.setOnAttach(r -> r.setChangeEventListener(this.dataSyncCache));
            this.buildings.setOnDetach(b -> b.setChangeEventListener(null));
            this.residents.setOnDetach(r -> r.setChangeEventListener(null));
            this.resources.setChangeListener(dataSyncCache);
            this.buildings.setOnChange((pos) -> {this.dataSyncCache.onBuildingChange(new TownBuildingChangeEvent(this.buildings, pos));});
            this.residents.setOnChange((uuid)-> {this.dataSyncCache.onResidentChange(new TownResidentChangeEvent(this.residents, uuid));});
            this.listenerInitialized = true;
        }
    }

    /**
     * ITown logic update (every 20 ticks). This method first validates the town
     * blocks, then sorts them by priority and calls the work methods.
     *
     * @param world server world instance
     */
    public void tickSecond(ServerLevel world, TeamDataHolder teamData) {
        //if (!FHConfig.SERVER.TOWN.enableTownTick.get()) return;
        Optional<GeneratorData> genDataOpt = teamData.getOptional(FHSpecialDataTypes.GENERATOR_DATA);
        if (genDataOpt.isPresent()) {
            GeneratorData genData = genDataOpt.get();
            if (genData.actualPos != null) {
                genData.townTick(world, teamData);
            }
        }
    }

    public void tickMorning(ServerLevel world) {
        if (!FHConfig.SERVER.TOWN.enableTownTickMorning.get()) return;
        TeamTown town = this.createTeamTown();
        this.checkBlocks(world, town);
        this.checkOccupiedAreaOverlap();
        this.tickResidentsMorning();
        this.residentAllocatingCheck(town);
        this.allocateHouse();
        this.assignWork();
        this.linkMinesToBases();
        this.recalcOreChunkResources();
        residents.values().forEach(Resident::resetDailyProficiencyGrowth);
        this.buildingsWork(world);
        this.recoverResources();
    }

    /**
     * 检查所有town blocks是否和当前储存的一致
     */
    void checkBlocks(ServerLevel level, TeamTown town) {
        Iterator<AbstractTownBuilding> iterator = buildings.values().iterator();
        while (iterator.hasNext()) {
            AbstractTownBuilding building = iterator.next();
            BlockPos pos = building.getPos();
            if (level.isLoaded(pos)) {
                //BlockState bs = level.getBlockState(pos);
                BlockEntity blockEntity = Utils.getExistingTileEntity(level, pos);
                if(blockEntity instanceof TownBlockEntity<?> townBlockEntity){
                    //这个getBuilding的作用是：当building符合类型时，转变类型，否则返回null。
                    // 因此通过它可以判断building是否为BlockEntity对应的Building
                    if(townBlockEntity.getBuilding(building) != null){
                        continue;
                    }
                }
                iterator.remove();
                building.onRemoved(town);
            }
        }
    }

    private void checkOccupiedAreaOverlap() {
        // removeNonTownBlocks(world);
        List<AbstractTownBuilding> buildingsWithOccupiedAreas = buildings.values().stream()
                .filter(building -> building.getOccupiedVolume() != null && building.getOccupiedVolume() != OccupiedVolume.EMPTY)
                .toList();
        // 两两比对，根据OccupiedArea的外接矩形是否重合初步筛选可能重叠的worker
        for (int i = 0; i < buildingsWithOccupiedAreas.size() - 1; i++) {
            AbstractTownBuilding building = buildingsWithOccupiedAreas.get(i);
            OccupiedVolume occupiedVolume = building.getOccupiedVolume();
            for (int j = i + 1; j < buildingsWithOccupiedAreas.size(); j++) {
                AbstractTownBuilding otherBuilding = buildingsWithOccupiedAreas.get(j);
                OccupiedVolume otherOccupiedVolume = otherBuilding.getOccupiedVolume();
                if (occupiedVolume.intersects(otherOccupiedVolume)) {
                    building.setOccupiedAreaOverlapped(true);
                    otherBuilding.setOccupiedAreaOverlapped(true);
                }
            }
        }
    }

    /**
     * 处理村民死亡
     */
    private void tickResidentsMorning() {
        if (ITown.DEBUG_MODE) {
            return;// 测试时村民不死
        }
        List<Resident> deadResidents = new ArrayList<>();
        for (Resident resident : residents.values()) {
            if (resident.getHousePos() == null) {
                resident.costHealth(10);
            }
            if (resident.getHealth() <= 5 || // 似了
                resident.getMental() <= 5) {// 跑了
                deadResidents.add(resident);
            }
        }
        TeamTown town = TeamTown.create(this);
        deadResidents.forEach(resident -> resident.setDeath(town));
    }

    private void residentAllocatingCheck(TeamTown town) {
        // 清空residents里所有居民存储的的house和work位置，之后再加回来，以刷新居民的工作和房屋
        residents.values().forEach(resident -> {
            resident.setHousePos(null);
            resident.setWorkPos(null);
        });
        // 移除house/worker里超过上限，或已不存在的的resident
        for (AbstractTownBuilding building : buildings.values()) {
            if (building instanceof ITownResidentBuilding residentBuilding) {
                Collection<UUID> residentIDs = residentBuilding.getResidentsID();
                //移除已不存在的居民
                residentIDs.removeIf(uuid -> !residents.containsKey(uuid));

                //移除超过上限的居民
                int maxResident = residentBuilding.getMaxResidents();
                if (residentIDs.size() > maxResident) {
                    Iterator<UUID> iterator = residentIDs.iterator();
                    int removeCount = residentIDs.size() - maxResident;
                    for (int i = 0; i < removeCount && iterator.hasNext(); i++) {
                        iterator.next();
                        iterator.remove();
                    }
                }
                for (UUID resident : residentBuilding.getResidentsID()) {
                    // 把清空的居民的house/work位置设为加回来
                    if (building instanceof HouseBuilding){
                        residents.get(resident).setHousePos(building.getPos());
                    }
                    else residents.get(resident).setWorkPos(building.getPos());
                }
            }
        }
    }

    // distribute homeless residents to house
    void allocateHouse() {
        Iterator<HouseBuilding> houseIterator = buildings.values().stream()
                .filter(building -> building instanceof HouseBuilding)
                .map(building -> (HouseBuilding) building)
                .filter(building ->building.isBuildingWorkable() && building.getMaxResidents() > building.getResidentsID().size())
            .sorted(Comparator.comparingDouble(building -> -building.getRating()))// 优先分配评分最高的house。因此在rating前面加了负号。
            .iterator();
        if (!houseIterator.hasNext()) return;
        HouseBuilding currentHouseData = houseIterator.next();
        for (Resident resident : residents.values()) {// 遍历所有居民
            if (resident.getHousePos() == null) {// 为没有house的居民分配进当前的house(暂存在ListNBT中)
                currentHouseData.addResident(resident);
            }
            if (currentHouseData.getResidentsID().size() >= currentHouseData.getMaxResidents()) {// 如果当前house满了，将暂存在ListNBT中的居民信息存入TownWorkerData，然后尝试进入下一个house
                if (houseIterator.hasNext()) {
                    currentHouseData = houseIterator.next();
                } else {
                    break;
                }
            }
        }
    }

    void assignWork() {
        Map<UUID, Resident> availableResidents = residents.values().stream().filter(resident->resident.getWorkPos() == null && resident.getHousePos() != null)
        .collect(Collectors.toMap(Resident::getUUID, t->t));
        PriorityQueue<ITownResidentWorkBuilding> availableBuildings = buildings.values().stream()
                .filter(AbstractTownBuilding::isBuildingWorkable)
                .filter(building -> building instanceof ITownResidentWorkBuilding)
                .map(building -> (ITownResidentWorkBuilding) building)
                //.sorted(Comparator.comparingDouble(o -> -o.getResidentPriority()))//PriorityQueue本身就有排序，不需要额外排序
                .collect(Collectors.toCollection(() -> new PriorityQueue<>(Comparator.comparingDouble(ITownResidentWorkBuilding::getResidentPriority).reversed())));

        Map<ITownResidentWorkBuilding, Map<Resident, Double/*score*/>> buildingResidentScoreCache = new HashMap<>();

        while(!availableBuildings.isEmpty()){
            ITownResidentWorkBuilding topPriorityBuilding = availableBuildings.poll();
            if(topPriorityBuilding.getResidentPriority() == Double.NEGATIVE_INFINITY) break;
            Resident bestResident = null;
            double bestResidentScore = 0;
            Map<Resident, Double> residentScoreCache = buildingResidentScoreCache.computeIfAbsent(topPriorityBuilding, a->new HashMap<>());
            if(availableResidents.isEmpty()){
                break;
            }
            for(Resident resident:availableResidents.values()){
                if (!topPriorityBuilding.canResidentWork(resident)) {
                    continue;
                }
                double residentScore = residentScoreCache.computeIfAbsent(resident, topPriorityBuilding::getResidentScore);
                if(residentScore > bestResidentScore){
                    bestResident = resident;
                    bestResidentScore = residentScore;
                }
            }
            if(bestResident != null){
                topPriorityBuilding.addResident(bestResident);
                availableResidents.remove(bestResident.getUUID());
                if(topPriorityBuilding.getResidentPriority() != Double.NEGATIVE_INFINITY){
                    availableBuildings.add(topPriorityBuilding);
                }
            }
        }
    }

    void linkMinesToBases() {
        Set<BlockPos> unassigned = new HashSet<>();
        for (AbstractTownBuilding b : buildings.values()) {
            if (b instanceof MineBuilding mine && mine.isStructureValid()) {
                unassigned.add(mine.getPos());
            }
        }

        for (AbstractTownBuilding b : buildings.values()) {
            if (!(b instanceof MineBaseBuilding base) || !base.isStructureValid()) continue;
            base.clearLinkedMines();
            BlockPos basePos = base.getPos();
            int radius = base.getConnectionRadius();
            unassigned.removeIf(minePos -> {
                if (minePos.distSqr(basePos) <= radius * radius) {
                    base.addLinkedMine(minePos);
                    return true;
                }
                return false;
            });
        }
    }


    void recalcOreChunkResources() {
        Set<ChunkPos> covered = new HashSet<>();
        for (AbstractTownBuilding b : buildings.values()) {
            if (b instanceof MineBaseBuilding base && base.isBuildingWorkable()) {
                for (BlockPos minePos : base.getLinkedMines()) {
                    covered.add(new ChunkPos(minePos));
                }
            }
        }
        this.setTerrainResourceTypeActiveChunks(TerrainResourceType.ORE, covered);
    }

    /**
     * execute work method of buildings.
     */
    private void buildingsWork(ServerLevel world){
        this.updateRadius();
        //updateAllBlocks(world);

        TeamTown teamTown = new TeamTown(this);
        reloadMaxCapacity();

        buildings.values().stream()
                .filter(AbstractTownBuilding::isBuildingWorkable)
                .sorted(Comparator.comparingInt(AbstractTownBuilding::getWorkPriority).reversed())
                .forEach(building -> building.work(teamTown,world));
    }

    /**
     * 清零MaxCapacity，并从仓库中重新读取和添加
     */
    public void reloadMaxCapacity(){
        resources.resetMaxCapacity();
        TeamTown teamTown = this.createTeamTown();
        buildings.values().stream().filter(building -> building instanceof WarehouseBuilding)
                .filter(AbstractTownBuilding::isBuildingWorkable)
                .forEach(building -> ((WarehouseBuilding) building).addCapacity(teamTown));
    }

    private static final Function<TerrainResourceType, TerrainResourceData> RESOURCE_DATA_SUPPLIER = type -> {
        TerrainResourceData data = new TerrainResourceData();
        data.recalculateRadius(type.getResourcePerSq(), TerrainResourceData.DEFAULT_MAX_RADIUS);
        return data;
    };

    public double pickTerrainResource(TerrainResourceType type,double maxPick) {
        TerrainResourceData rd=this.terrainResource.computeIfAbsent(type, RESOURCE_DATA_SUPPLIER);
        double total=Math.min(rd.getRemainResource(), maxPick);
        rd.costResource(total);
        return total;
    }
    public void recoverResources() {
        for(Entry<TerrainResourceType, TerrainResourceData> rd:this.terrainResource.entrySet()) {
            double recover=rd.getValue().getSize()*rd.getKey().getRecoverSpeed();
            rd.getValue().recoverResource(CMath.randomValue(recover));
        }
    }
    public void updateRadius() {
        for(Entry<TerrainResourceType, TerrainResourceData> rd:this.terrainResource.entrySet()) {
            rd.getValue().recalculateRadius(rd.getKey().getResourcePerSq(), TerrainResourceData.DEFAULT_MAX_RADIUS);
        }

    }

    public void unpickTerrainResource(TerrainResourceType type, double maxPick) {
        TerrainResourceData rd=this.terrainResource.computeIfAbsent(type, RESOURCE_DATA_SUPPLIER);
        rd.recoverResource(maxPick);
    }

    public double maypickTerrainResource(TerrainResourceType type, double d) {
        TerrainResourceData rd=this.terrainResource.computeIfAbsent(type, RESOURCE_DATA_SUPPLIER);
        return rd.mayCostResource(d);
    }

    /**
     * Read-only global-resource query. Unlike maypickTerrainResource this does
     * not create or mutate town data and is safe for client-side information UI.
     */
    public double getRemainingTerrainResource(TerrainResourceType type) {
        TerrainResourceData rd = terrainResource.get(type);
        double extracted = rd == null ? 0.0 : rd.getExtracted();
        return Math.max(0.0, TerrainResourceData.calculateTotalResource(
                type.getResourcePerSq(), TerrainResourceData.DEFAULT_MAX_RADIUS) - extracted);
    }

    /**
     * Read-only per-chunk resource query. The returned value is independent of
     * the server-only active chunk set.
     */
    public double getRemainingTerrainResource(TerrainResourceType type, ChunkPos chunk) {
        TerrainResourceData rd = terrainResource.get(type);
        double extracted = 0.0;
        if (rd != null && rd.getChunkResourceTracker() != null) {
            extracted = rd.getChunkResourceTracker().getExtracted(chunk);
        }
        return Math.max(0.0, type.getResourcePerSq() - extracted);
    }

    public double getExtractedTerrainResource(TerrainResourceType type, ChunkPos chunk) {
        TerrainResourceData rd = terrainResource.get(type);
        if (rd == null || rd.getChunkResourceTracker() == null) return 0.0;
        return rd.getChunkResourceTracker().getExtracted(chunk);
    }

    public void setTerrainResourceTypeActiveChunks(TerrainResourceType type, Set<ChunkPos> chunks) {
        TerrainResourceData data = terrainResource.computeIfAbsent(type, t -> {
            TerrainResourceData newData = new TerrainResourceData();
            newData.setChunkTracker(new TerrainResourceData.ChunkResourceTracker());
            return newData;
        });
        if (data.getChunkResourceTracker() == null) {
            data.setChunkTracker(new TerrainResourceData.ChunkResourceTracker());
        }
        data.getChunkResourceTracker().setActiveChunks(chunks);
    }

    // 区块检查是否可采，不扣减
    public double mayPickTerrainResource(TerrainResourceType type, ChunkPos chunk, double amount) {
        TerrainResourceData rd = terrainResource.get(type);
        if (rd != null) {
            return rd.mayCostResource(chunk, amount, type.getResourcePerSq());
        }
        return 0;
    }

    // 区块实际扣减并返回真实开采量
    public double pickTerrainResource(TerrainResourceType type, ChunkPos chunk, double amount) {
        TerrainResourceData rd = terrainResource.computeIfAbsent(type, t -> new TerrainResourceData());
        double actual = rd.mayCostResource(chunk, amount, type.getResourcePerSq());
        if (actual > 0) {
            rd.costChunkResource(chunk, actual);
        }
        return actual;
    }

    /**
     * 用于在服务端向客户端同步发生变化的数据
     */
    class DataSyncCache implements ITownBuildingChangeEventListener, ITownResourceChangeEventListener, ITownResidentChangeEventListener {
        /**
         * 记录发生变化但未同步到客户端的资源，通常将在下一tick同步。
         */
        private Set<ITownResourceKey> changedResourceKey = new HashSet<>();
        private Set<UUID> changedResidentUUID = new HashSet<>();
        private Set<BlockPos> changedBuildingPos = new HashSet<>();

        public void addChanged(ITownResourceKey changedResourceKey){
            this.changedResourceKey.add(changedResourceKey);
        }

        public void addChanged(UUID changedResidentUUID){
            this.changedResidentUUID.add(changedResidentUUID);
        }

        public void addChanged(BlockPos changedBuildingPos){
            this.changedBuildingPos.add(changedBuildingPos);
        }

        /**
         * 取出当前所有脏建筑键并清空。发包前调用。
         */
        public Set<BlockPos> drainChangedBuildings() {
            if (changedBuildingPos.isEmpty()) return Set.of();
            Set<BlockPos> out = new HashSet<>(changedBuildingPos);
            changedBuildingPos.clear();
            return out;
        }

        /**
         * 取出当前所有脏居民键并清空。发包前调用。
         */
        public Set<UUID> drainChangedResidents() {
            if (changedResidentUUID.isEmpty()) return Set.of();
            Set<UUID> out = new HashSet<>(changedResidentUUID);
            changedResidentUUID.clear();
            return out;
        }

        /**
         * 取出当前所有脏资源键并清空。发包前调用（资源层 fire 尚未接入时通常为空）。
         */
        public Set<ITownResourceKey> drainChangedResources() {
            if (changedResourceKey.isEmpty()) return Set.of();
            Set<ITownResourceKey> out = new HashSet<>(changedResourceKey);
            changedResourceKey.clear();
            return out;
        }

        /**
         * 是否还有未同步的脏数据。
         */
        public boolean hasChanges() {
            return !changedBuildingPos.isEmpty() || !changedResidentUUID.isEmpty() || !changedResourceKey.isEmpty();
        }

        /**
         * 清空所有脏键记录（如加载存档后已完成一次全量同步时调用）。
         */
        public void clearChanged() {
            changedBuildingPos.clear();
            changedResidentUUID.clear();
            changedResourceKey.clear();
        }



        @Override
        public void onBuildingChange(TownBuildingChangeEvent event) {
            this.addChanged(event.changedBuildingPos);
        }

        @Override
        public void onResidentChange(TownResidentChangeEvent event) {

        }

        @Override
        public void onResourceChange(TownResourceChangeEvent event) {
            this.addChanged(event.changedResourceKey);
        }
    }
}
