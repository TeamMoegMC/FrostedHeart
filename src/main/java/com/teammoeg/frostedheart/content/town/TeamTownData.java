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
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.event.*;
import com.teammoeg.frostedheart.content.town.network.TownBuildingUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownResidentUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownResourceUpdatePacket;
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
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.event.ITownBuildingChangeEventListener;
import com.teammoeg.frostedheart.content.town.event.ITownResidentChangeEventListener;
import com.teammoeg.frostedheart.content.town.event.ITownResourceChangeEventListener;
import com.teammoeg.frostedheart.content.town.event.TownBuildingChangeEvent;
import com.teammoeg.frostedheart.content.town.event.TownResidentChangeEvent;
import com.teammoeg.frostedheart.content.town.event.TownResourceChangeEvent;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceData;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
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
        .fieldOf("maxLabour").forGetter(o -> o.maxLabour),

        CodecUtil.defaultSupply(CodecUtil.catchingCodec(TownHistoryEntry.CODEC.listOf()), ArrayList::new)
        .fieldOf("history").forGetter(o -> o.history)

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
     * 城镇每日快照历史，最新条目在末尾，最多保留 {@link #MAX_HISTORY_ENTRIES} 条。
     * 随存档持久化，并随城镇数据全量同步下发客户端。
     * <p>
     * Daily snapshot history of the town, newest entry last, capped at
     * {@link #MAX_HISTORY_ENTRIES} entries. Persisted with the save and synced
     * to clients with the full town data sync.
     */
    @Getter
    List<TownHistoryEntry> history = new ArrayList<>();

    /**
     * 用于将城镇数据变化的监听器塞到各个地方。
     * 由于只需要在第一个tick进行，所以弄个boolean记一下、
     * 由于我希望只在服务端这么做，所以不放在构造方法中。
     */
    private boolean listenerInitialized = false;
    @Getter
    private final DataSyncCache dataSyncCache = new DataSyncCache();

    /**
     * 客户端 GUI 监听器集合（static 而非实例字段）。
     * <p>
     * 原因：全量同步包 {@code TeamTownDataS2CPacket} 会用新解码出的实例替换客户端
     * TeamTownData；若监听器挂在实例上，GUI 打开后收到全量包就会丢失监听。static 集合
     * 与实例替换解耦；客户端同一时刻只有一个本地玩家队伍，全局唯一无歧义。所有增删与触发
     * 都发生在主线程（包处理经 {@code NetworkEvent.Context#enqueueWork}），
     * ConcurrentHashMap 仅作额外的并发保护。
     */
    private static final Set<ITownDataUpdateListener> clientListeners = ConcurrentHashMap.newKeySet();



    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource,int labour,int maxlabour, List<TownHistoryEntry> history) {
        super();
        this.history = new ArrayList<>(history);
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
            this.resources.setChangeListener(this.dataSyncCache);
            this.buildings.setOnChange((pos) -> {this.dataSyncCache.onBuildingChange(new TownBuildingChangeEvent(this.buildings, pos));});
            this.residents.setOnChange((uuid)-> {this.dataSyncCache.onResidentChange(new TownResidentChangeEvent(this.residents, uuid));});
            this.buildings.values().forEach(building -> building.setChangeEventListener(this.dataSyncCache));
            this.residents.values().forEach(resident -> resident.setChangeEventListener(this.dataSyncCache));
            this.listenerInitialized = true;
        }

        if(!dataSyncCache.changedResourceKey.isEmpty()){
            Map<ITownResourceKey, Double> changedResource = new HashMap<>();
            for(ITownResourceKey resourceKey : this.dataSyncCache.drainChangedResources()){
                double current = this.resources.get(resourceKey);
                // 发送端值级去重：当前值与上次已同步值相同（且此前已同步过）时跳过，
                // 消除"操作级 fire"（如 reloadMaxCapacity 的清零+加回）造成的空转资源包。
                if(this.dataSyncCache.isResourceUnchanged(resourceKey, current)){
                    continue;
                }
                changedResource.put(resourceKey, current);
            }
            if(!changedResource.isEmpty()){
                teamData.sendToOnline(FHNetwork.INSTANCE, new TownResourceUpdatePacket(changedResource, resources.getOccupiedCapacity()));
                changedResource.forEach(this.dataSyncCache::markResourceSynced);
            }
        }

        if(!dataSyncCache.changedResidentUUID.isEmpty()){
            Map<UUID, Resident> changedResidents = new HashMap<>();
            Set<UUID> removedResidents = new HashSet<>();
            for(UUID uuid : this.dataSyncCache.drainChangedResidents()){
                Resident resident = residents.get(uuid);
                if(resident == null){
                    removedResidents.add(uuid);
                } else{
                    changedResidents.put(uuid, resident);
                }
            }
            teamData.sendToOnline(FHNetwork.INSTANCE, new TownResidentUpdatePacket(changedResidents, removedResidents));
        }

        if(!dataSyncCache.changedBuildingPos.isEmpty()){
            Map<BlockPos, ITownBuilding> changedBuildings = new HashMap<>();
            Set<BlockPos> removedBuildings = new HashSet<>();
            for(BlockPos pos : this.dataSyncCache.drainChangedBuildings()){
                ITownBuilding building = buildings.get(pos);
                if(building == null){
                    removedBuildings.add(pos);
                } else{
                    changedBuildings.put(pos, building);
                }
            }
            teamData.sendToOnline(FHNetwork.INSTANCE, new TownBuildingUpdatePacket(changedBuildings, removedBuildings));
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
        FHMain.LOGGER.debug("Ticking morning for {}...", name);
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
        this.recordDailySnapshot(world);
    }

    /**
     * 历史快照的最大保留条数。
     * <p>
     * Maximum number of retained history entries.
     */
    public static final int MAX_HISTORY_ENTRIES = 30;

    /**
     * 在每日结算完成后记录一条城镇快照。同一天重复结算时覆盖当天条目，
     * 超过 {@link #MAX_HISTORY_ENTRIES} 条时丢弃最旧的记录。
     * <p>
     * Records a daily town snapshot after settlement. Repeated settlements on
     * the same day overwrite that day's entry; oldest entries are dropped once
     * {@link #MAX_HISTORY_ENTRIES} is exceeded.
     *
     * @param world 服务端世界 / server world instance
     */
    void recordDailySnapshot(ServerLevel world) {
        long day = world.getDayTime() / 24000L;
        double avgHealth = residents.values().stream().mapToDouble(Resident::getHealth).average().orElse(0);
        double avgMental = residents.values().stream().mapToDouble(Resident::getMental).average().orElse(0);
        TownHistoryEntry entry = new TownHistoryEntry(day, residents.size(), avgHealth, avgMental, buildings.size());
        if (!history.isEmpty() && history.get(history.size() - 1).day() == day) {
            history.set(history.size() - 1, entry);
        } else {
            history.add(entry);
        }
        while (history.size() > MAX_HISTORY_ENTRIES) {
            history.remove(0);
        }
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
     * <p>
     * 净变化守卫：先累加所有可工作仓库的容量总和，与当前 max_capacity 相同则直接返回。
     * 否则才执行"清零→逐仓加回"。避免仓库被定时刷新（{@code WarehouseBlockEntity.refresh}
     * 无条件调用本方法）时，因"操作级 fire"（清零与加回各 fire 一次）而向客户端发送
     * 内容未变的资源增量包。
     */
    public void reloadMaxCapacity(){
        double totalCapacity = 0.0;
        for (AbstractTownBuilding b : buildings.values()) {
            if (b instanceof WarehouseBuilding warehouse && warehouse.isBuildingWorkable()) {
                totalCapacity += warehouse.getCapacity();
            }
        }
        if (Math.abs(totalCapacity - resources.get(VirtualResourceType.MAX_CAPACITY.generateAttribute(0))) < TeamTownResourceHolder.DELTA) {
            return;
        }
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

    // ===================== 客户端 GUI 监听器 =====================
    // 增量包 applyXxxUpdate 与全量包 TeamTownDataS2CPacket 在客户端调用下方方法，
    // 通知当前打开的城镇 GUI 刷新。GUI 在 onInit() 注册、onClosed() 移除
    // （见 AbstractTownWorkerBlockScreen 与 TownManagerScreen）。

    /**
     * 注册一个客户端 GUI 监听器（GUI 打开时调用）。
     * <p>Register a client-side GUI listener (called when the GUI opens).</p>
     */
    public static void addClientListener(ITownDataUpdateListener listener) {
        clientListeners.add(listener);
    }

    /**
     * 移除一个客户端 GUI 监听器（GUI 关闭时调用）。
     * <p>Remove a client-side GUI listener (called when the GUI closes).</p>
     */
    public static void removeClientListener(ITownDataUpdateListener listener) {
        clientListeners.remove(listener);
    }

    /**
     * 通知所有已注册 GUI：三类数据均可能已变化。由全量同步包在替换实例后调用，
     * 保证 GUI 打开瞬间收到的最新全量数据能立即刷新到界面。
     * <p>
     * Notify all registered GUIs that any of the three categories may have changed.
     * Called by the full-sync packet after the instance is replaced, so the freshest
     * full snapshot is reflected immediately.
     * </p>
     */
    public static void fireClientDataChanged() {
        fireBuildingsChanged();
        fireResidentsChanged();
        fireResourcesChanged();
    }

    private static void fireBuildingsChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onBuildingsChanged();
        }
    }

    private static void fireResidentsChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onResidentsChanged();
        }
    }

    private static void fireResourcesChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onResourcesChanged();
        }
    }

    // ===================== 客户端增量同步入口 =====================
    // 以下三个方法仅在【客户端】TeamTownData 实例上被对应的增量包调用。
    // 客户端实例的 buildings/residents 未绑定任何 onChange/onAttach/onDetach 回调
    // （那些回调只在服务端 tick() 中绑定），因此这里对 Map 的增删不会触发脏标记，
    // 也不会形成“客户端→服务端”的回环。每个方法都按服务端发来的当前权威值覆盖本地，
    // 并在返回前触发对应类别的客户端 GUI 监听器，使打开中的界面即时刷新。

    /**
     * 客户端增量同步：用服务端发来的变更（新增/修改）覆盖对应建筑，并移除已删除的建筑。
     * @param changed 发生变化的建筑（pos → 当前完整建筑对象）
     * @param removed 需要从客户端移除的建筑位置
     */
    public void applyBuildingUpdate(Map<BlockPos, ITownBuilding> changed, Set<BlockPos> removed) {
        for (Map.Entry<BlockPos, ITownBuilding> entry : changed.entrySet()) {
            if (entry.getValue() instanceof AbstractTownBuilding building) {
                buildings.put(entry.getKey(), building);
            }
        }
        for (BlockPos pos : removed) {
            buildings.remove(pos);
        }
        fireBuildingsChanged();
    }

    /**
     * 客户端增量同步：用服务端发来的变更（新增/修改）覆盖对应居民，并移除已删除的居民。
     * @param changed 发生变化的居民（uuid → 当前完整居民对象）
     * @param removed 需要从客户端移除的居民 uuid
     */
    public void applyResidentUpdate(Map<UUID, Resident> changed, Set<UUID> removed) {
        residents.putAll(changed);
        for (UUID uuid : removed) {
            residents.remove(uuid);
        }
        fireResidentsChanged();
    }

    /**
     * 客户端增量同步：用服务端发来的当前值覆盖对应资源，并刷新已占用容量。
     * @param changes 发生变化的资源（key → 当前权威数量）
     * @param occupiedCapacity 服务端下发的最新已占用容量
     */
    public void applyResourceUpdate(Map<ITownResourceKey, Double> changes, double occupiedCapacity) {
        for (Map.Entry<ITownResourceKey, Double> entry : changes.entrySet()) {
            resources.applySyncEntry(entry.getKey(), entry.getValue());
        }
        resources.setOccupiedCapacity(occupiedCapacity);
        fireResourcesChanged();
    }

    /**
     * 全量同步包（{@link com.teammoeg.frostedheart.content.town.network.TeamTownDataS2CPacket}）发出成功后调用：
     * 清空资源值级去重基线，使下一轮 flush 对所有脏资源键强制发包（全量包单播、基线全队共享，
     * 重建基线会吞掉其他玩家的窗口内增量，详见 {@link DataSyncCache#markFullSynced()}）。
     * 委托给 {@link DataSyncCache#markFullSynced()}（内部类跨包不可访问）。
     */
    public void markFullSynced() {
        this.dataSyncCache.markFullSynced();
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

        /**
         * 上次已通过增量包同步给客户端的资源值快照（仅服务端维护）。
         * <p>
         * 发送端值级去重：仅当资源"当前值"与上次同步值不同（或该键从未同步过）时才发包。
         * 只覆盖资源层（纯数值比对，每键仅一个 double，内存极小）；建筑/居民的空转
         * fire 由 setter 值守卫（{@link AbstractTownBuilding} / 各子类）在源头拦截。
         */
        private final Map<ITownResourceKey, Double> lastSyncedResources = new HashMap<>();

        /**
         * 判断该资源自上次增量同步后是否实际未变。
         *
         * @param key          资源键
         * @param currentValue 当前值
         * @return true 表示当前值与上次同步值相同且该键此前已同步过，应跳过发包
         */
        public boolean isResourceUnchanged(ITownResourceKey key, double currentValue) {
            Double last = lastSyncedResources.get(key);
            if (last == null) {
                return false; // 键从未同步过（或此前已归零移除）：视为变化
            }
            return Math.abs(last - currentValue) < TeamTownResourceHolder.DELTA;
        }

        /**
         * 记录某资源已通过增量包同步（发包后调用）。值近似为 0 时移除记录，
         * 防止快照随"增删交替"的键无限膨胀。
         */
        public void markResourceSynced(ITownResourceKey key, double value) {
            if (Math.abs(value) < TeamTownResourceHolder.DELTA) {
                lastSyncedResources.remove(key);
            } else {
                lastSyncedResources.put(key, value);
            }
        }

        /**
         * 全量同步包（{@link com.teammoeg.frostedheart.content.town.network.TeamTownDataS2CPacket}）发出成功后调用：
         * 清空资源值级去重基线，使下一轮 flush 对所有脏资源键强制发包（last==null 一律视为变化）。
         * <p>
         * 为何不按"当前值重建基线"：全量包只单播给单个玩家（登录/切维度/开 GUI/印章），
         * 而基线是全队共享的。若把基线推进到当前值，窗口内（已标记未 flush）的资源变更
         * 会在下一 tick 被值级去重误判为"未变化"而跳过，导致未收到全量包的其他在线玩家
         * 丢失该增量。改为清空基线则双向安全：① 其他玩家下一 tick 收到窗口内全部变更；
         * ② 收到全量包的玩家若快照值随后回跳（如 200 → 100），也会被强制发包修正。
         * 代价：全量包后首次 flush 会把窗口内脏键（含空转键）多发送一次冗余包，量小可接受。
         */
        public void markFullSynced() {
            lastSyncedResources.clear();
        }

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
            this.addChanged(event.changedResidentID);
        }

        @Override
        public void onResourceChange(TownResourceChangeEvent event) {
            this.addChanged(event.changedResourceKey);
        }
    }
}
