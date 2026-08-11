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
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WeatherForecast;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.event.*;
import com.teammoeg.frostedheart.content.town.network.TownBuildingUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownResidentUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownResourceUpdatePacket;
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import com.teammoeg.frostedheart.content.town.util.ObservableTownMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
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
import com.teammoeg.frostedheart.content.town.resident.ResidentDailyModel;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceData;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig.Server.Town.RefugeeSpawn;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig.Server.Town.ResidentAging;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

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
        .fieldOf("history").forGetter(o -> o.history),

        CodecUtil.defaultSupply(CodecUtil.catchingCodec(Codec.LONG), () -> -1L)
        .fieldOf("lastRefugeeSpawnDay").forGetter(o -> o.lastRefugeeSpawnDay)

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
     * 最近一次按世界日结算难民刷新的日期（服务端持久化）。
     * 防止同一天内重复结算（含 /town tick 手动调用）导致重复刷新。
     */
    long lastRefugeeSpawnDay = -1L;

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



    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource,int labour,int maxlabour, List<TownHistoryEntry> history, long lastRefugeeSpawnDay) {
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
        this.lastRefugeeSpawnDay = lastRefugeeSpawnDay;
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

    public void tickMorning(ServerLevel world, TeamDataHolder teamData) {
        if (!FHConfig.SERVER.TOWN.enableTownTickMorning.get()) return;
        FHMain.LOGGER.debug("Ticking morning for {}...", name);
        TeamTown town = this.createTeamTown();
        this.checkBlocks(world, town);
        this.checkOccupiedAreaOverlap();
        this.tickResidentsMorning();
        this.tickResidentsAging();
        this.residentAllocatingCheck(town);
        this.allocateHouse();
        this.assignWork();
        this.tickRefugeeSpawnAndDespawn(world, teamData);
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
        long day = WorldClimate.getWorldDay(world);
        // DoubleSummaryStatistics 与 DoubleStream.average() 同源（Kahan 补偿求和），位级一致；空集 getAverage()=0.0
        DoubleSummaryStatistics healthStat = new DoubleSummaryStatistics();
        DoubleSummaryStatistics mentalStat = new DoubleSummaryStatistics();
        for (Resident resident : residents.values()) {
            healthStat.accept(resident.getHealth());
            mentalStat.accept(resident.getMental());
        }
        TownHistoryEntry entry = new TownHistoryEntry(day, residents.size(), healthStat.getAverage(), mentalStat.getAverage(), buildings.size());
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
        FHConfig.Server.Town.ResidentRules config = FHConfig.SERVER.TOWN.RESIDENT_RULES;
        List<Resident> deadResidents = new ArrayList<>();
        for (Resident resident : residents.values()) {
            ResidentDailyModel.MorningResult result = ResidentDailyModel.settleMorning(
                    resident.getHealth(),
                    resident.getMental(),
                    resident.getHousePos() != null,
                    config.homelessHealthLossPerDay.get(),
                    config.removalHealthThreshold.get(),
                    config.removalMentalThreshold.get());
            resident.setHealth(result.healthAfterHomelessPenalty());
            if (result.removed()) {
                deadResidents.add(resident);
            }
        }
        TeamTown town = TeamTown.create(this);
        deadResidents.forEach(resident -> resident.setDeath(town));
    }

    /**
     * 每日老化结算：ageDays+1，幼儿/儿童达标后成长为下一个年龄组（属性保留），
     * 各年龄组按配置每日增减属性并封顶。
     */
    private void tickResidentsAging() {
        ResidentAging aging = FHConfig.SERVER.TOWN.RESIDENT_AGING;
        for (Resident resident : residents.values()) {
            resident.setAgeDays(resident.getAgeDays() + 1);
            switch (resident.getAge()) {
                case Resident.AGE_INFANT -> {
                    if (resident.getAgeDays() >= aging.infantToChildDays.get()) {
                        resident.setAge(Resident.AGE_CHILD);
                    } else {
                        resident.growStrengthDaily(aging.infantStrengthGainPerDay.get(), aging.infantAttributeCap.get());
                        resident.growIntelligenceDaily(aging.infantIntelligenceGainPerDay.get(), aging.infantAttributeCap.get());
                    }
                }
                case Resident.AGE_CHILD -> {
                    if (resident.getAgeDays() >= aging.childToAdultDays.get()) {
                        resident.setAge(Resident.AGE_ADULT);
                    } else {
                        resident.growStrengthDaily(aging.childStrengthGainPerDay.get(), aging.childStrengthCap.get());
                        resident.growIntelligenceDaily(aging.childIntelligenceGainPerDay.get(), aging.childIntelligenceCap.get());
                    }
                }
                case Resident.AGE_ADULT -> {
                    resident.growStrengthDaily(aging.adultStrengthGainPerDay.get(), aging.adultAttributeCap.get());
                    resident.growIntelligenceDaily(aging.adultIntelligenceGainPerDay.get(), aging.adultAttributeCap.get());
                }
                case Resident.AGE_ELDER -> resident.decayStrengthDaily(aging.elderStrengthDecayPerDay.get(), aging.elderStrengthFloor.get());
            }
        }
    }

    /**
     * 难民刷新时的天气判定结果。
     */
    enum RefugeeSpawnWeather {
        WARM, NORMAL, COLD
    }

    /**
     * 每天早晨按天气概率在开启的能量塔附近刷新一批流浪难民；无视容量照刷，塔旁等待。
     * 同一天只结算一次（按世界日持久化）；已刷难民的清场由实体自身按日界结算，不在此处理。
     */
    private void tickRefugeeSpawnAndDespawn(ServerLevel world, TeamDataHolder teamData) {
        RefugeeSpawn config = FHConfig.SERVER.TOWN.REFUGEE_SPAWN;
        if (!config.enableRefugeeSpawn.get()) return;
        // 队伍无人在线时不刷新；不置位当天标记，有人上线当天仍可刷。
        // 防御：getTeam() 可为 null（旧存档恢复已解散队伍的 holder），getOnlineMembers() 内部不判空
        if (teamData.getTeam() == null || teamData.getTeam().getOnlineMembers().isEmpty()) return;
        // WorldClockSource 已在本次城镇 tick 前更新：睡觉跳时会推进日期，/time set 回退不会让日期倒退。
        long day = WorldClimate.getWorldDay(world);
        // "当天只结算一次"守卫：/town tick 同日多次调用也不会重复刷批
        if (day == this.lastRefugeeSpawnDay) return;
        Optional<GeneratorData> genDataOpt = teamData.getOptional(FHSpecialDataTypes.GENERATOR_DATA);
        if (genDataOpt.isEmpty() || genDataOpt.get().actualPos == null || !genDataOpt.get().isWorking) {
            // 塔不存在/未开启：置位当天，防止同日反复调用重复判定
            this.lastRefugeeSpawnDay = day;
            return;
        }
        GeneratorData genData = genDataOpt.get();
        BlockPos towerPos = genData.actualPos;
        RefugeeSpawnWeather weather = getSpawnWeather(world, towerPos);
        double chance = config.baseSpawnChancePerDay.get()
            + (weather == RefugeeSpawnWeather.WARM ? config.warmSpawnChanceBonus.get()
            : weather == RefugeeSpawnWeather.COLD ? -config.coldSpawnChancePenalty.get() : 0);
        chance = Math.max(0.0, Math.min(1.0, chance));
        if (CMath.RANDOM.nextDouble() >= chance) {
            FHMain.LOGGER.debug("No refugee batch this morning, weather={}, chance={}", weather, chance);
            this.lastRefugeeSpawnDay = day;
            return;
        }
        FHMain.LOGGER.debug("Spawning refugee batch, weather={}", weather);
        int spawned = this.spawnRefugeeBatch(world, teamData, weather);
        // 全部生成失败时不置位，当天可重试
        if (spawned > 0) {
            this.lastRefugeeSpawnDay = day;
        }
    }

    /**
     * 强制刷一批难民（调试命令 /town spawn_refugees 用），并更新按日结算标记。
     */
    public void debugSpawnRefugeeBatch(ServerLevel world, TeamDataHolder teamData) {
        Optional<GeneratorData> genDataOpt = teamData.getOptional(FHSpecialDataTypes.GENERATOR_DATA);
        if (genDataOpt.isEmpty() || genDataOpt.get().actualPos == null) return;
        RefugeeSpawnWeather weather = getSpawnWeather(world, genDataOpt.get().actualPos);
        int spawned = spawnRefugeeBatch(world, teamData, weather);
        if (spawned > 0) {
            this.lastRefugeeSpawnDay = WorldClimate.getWorldDay(world);
        }
        FHMain.LOGGER.info("Debug-spawned {} refugee(s), weather={}", spawned, weather);
    }

    /**
     * 在能量塔周围按天气参数刷一批难民。
     *
     * @return 实际生成数量
     */
    int spawnRefugeeBatch(ServerLevel world, TeamDataHolder teamData, RefugeeSpawnWeather weather) {
        RefugeeSpawn config = FHConfig.SERVER.TOWN.REFUGEE_SPAWN;
        Optional<GeneratorData> genDataOpt = teamData.getOptional(FHSpecialDataTypes.GENERATOR_DATA);
        if (genDataOpt.isEmpty() || genDataOpt.get().actualPos == null) return 0;
        GeneratorData genData = genDataOpt.get();
        int sizeMod = weather == RefugeeSpawnWeather.WARM ? config.warmSpawnBatchBonus.get()
            : weather == RefugeeSpawnWeather.COLD ? -config.coldSpawnBatchPenalty.get() : 0;
        int min = Math.max(1, config.batchSizeMin.get() + sizeMod);
        int max = Math.max(min, config.batchSizeMax.get() + sizeMod);
        int batchSize = min + CMath.RANDOM.nextInt(max - min + 1);
        int spawned = 0;
        for (int i = 0; i < batchSize; i++) {
            BlockPos pos = findRefugeeSpawnPos(world, genData.actualPos);
            if (pos == null) continue;
            WanderingRefugee refugee = FHEntityTypes.WANDERING_REFUGEE.get().create(world);
            if (refugee == null) continue;
            refugee.setAgeGroup(pickAgeByWeight());
            refugee.markTownSpawned(teamData.getId());
            if (weather == RefugeeSpawnWeather.COLD && CMath.RANDOM.nextDouble() < config.coldQualityChance.get()) {
                refugee.setColdSurvivor(true);
            }
            refugee.setPersistenceRequired();
            refugee.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            // 生成可能被世界拒绝（如实体数量上限），成功才计数并寻路，避免计数虚高使"全部失败也置位当天"
            if (world.addFreshEntity(refugee)) {
                // 默默寻路走到能量塔旁，聚拢在塔附近等待（无任何特效）
                refugee.getNavigation().moveTo(genData.actualPos.getX() + 0.5D, genData.actualPos.getY(), genData.actualPos.getZ() + 0.5D, 1.0D);
                spawned++;
            }
        }
        return spawned;
    }

    /**
     * 按配置权重轮盘随机一个年龄组。
     */
    private int pickAgeByWeight() {
        RefugeeSpawn config = FHConfig.SERVER.TOWN.REFUGEE_SPAWN;
        double infant = config.weightInfant.get();
        double child = config.weightChild.get();
        double adult = config.weightAdult.get();
        double elder = config.weightElder.get();
        if (infant + child + adult + elder <= 0.0D) {
            // 全零权重回退默认分布，避免轮盘恒落最后一项
            infant = 10.0D;
            child = 20.0D;
            adult = 50.0D;
            elder = 20.0D;
        }
        double roll = CMath.RANDOM.nextDouble() * (infant + child + adult + elder);
        if (roll < infant) return Resident.AGE_INFANT;
        roll -= infant;
        if (roll < child) return Resident.AGE_CHILD;
        roll -= child;
        if (roll < adult) return Resident.AGE_ADULT;
        return Resident.AGE_ELDER;
    }

    /**
     * 判定当天的刷新天气：暖流 = 温度级别≥1 且晴天；寒流 = 温度级别≤-1 或暴风雪；其余平稳。
     * 温度取塔位置的气候基线（与预报系统同源）。
     */
    private RefugeeSpawnWeather getSpawnWeather(ServerLevel world, BlockPos towerPos) {
        int tempLevel = WeatherForecast.getTemperatureLevel(WorldTemperature.climate(world, towerPos));
        if (tempLevel >= 1 && WorldClimate.isSun(world)) {
            return RefugeeSpawnWeather.WARM;
        }
        if (tempLevel <= -1 || WorldClimate.isBlizzard(world)) {
            return RefugeeSpawnWeather.COLD;
        }
        return RefugeeSpawnWeather.NORMAL;
    }

    /**
     * 在塔周围 [minR, maxR] 随机距离与角度找可落地生成点，最多尝试 16 次。
     */
    private BlockPos findRefugeeSpawnPos(ServerLevel world, BlockPos towerPos) {
        RefugeeSpawn config = FHConfig.SERVER.TOWN.REFUGEE_SPAWN;
        int minR = config.spawnRadiusMinBlocks.get();
        int maxR = Math.max(minR, config.spawnRadiusMaxBlocks.get());
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = CMath.RANDOM.nextDouble() * Math.PI * 2.0D;
            double dist = minR + CMath.RANDOM.nextDouble() * (maxR - minR);
            int x = towerPos.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = towerPos.getZ() + (int) Math.round(Math.sin(angle) * dist);
            // 未加载区块直接跳过：getHeight 对未加载区块会同步加载，塔区块卸载时每天最多 16×batchSize 次
            if (!world.hasChunkAt(x, z)) continue;
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockState ground = world.getBlockState(pos.below());
            if (!ground.isSolid()) continue;
            BlockState standing = world.getBlockState(pos);
            BlockState overhead = world.getBlockState(pos.above());
            if (!standing.isAir() && !standing.canBeReplaced()) continue;
            if (!overhead.isAir() && !overhead.canBeReplaced()) continue;
            return pos;
        }
        return null;
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
        Map<UUID, Resident> availableResidents = residents.values().stream().filter(resident->resident.getWorkPos() == null && resident.getHousePos() != null && resident.getAge() != Resident.AGE_INFANT)
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
                .filter(AbstractTownBuilding::shouldRunDailySettlement)
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
     * 全量同步包批内 fire 标记 + 单调递增批次号：批内连续回调（建筑→居民→资源）之间
     * 数据不变（实例替换在 fireClientDataChanged 之前完成），监听器可据此对同一批的
     * 重复回调去重一次；批号供监听器区分「当前批已求值」与「新一轮批」（纯布尔在
     * 连续两次全量包之间无增量回调时会残留为真，跳过新一轮求值）。
     * 仅客户端主线程访问（全量包 enqueueWork / 增量包 handler），无需并发保护。
     */
    private static boolean inClientBatchFire = false;
    private static long clientSyncBatchId = 0;

    public static boolean isInClientBatchFire() {
        return inClientBatchFire;
    }

    public static long getClientSyncBatchId() {
        return clientSyncBatchId;
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
        inClientBatchFire = true;
        clientSyncBatchId++;
        try {
            fireBuildingsChanged();
            fireResidentsChanged();
            fireResourcesChanged();
        } finally {
            inClientBatchFire = false;
        }
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
        private final Object2DoubleOpenHashMap<ITownResourceKey> lastSyncedResources = new Object2DoubleOpenHashMap<>();

        /**
         * 判断该资源自上次增量同步后是否实际未变。
         *
         * @param key          资源键
         * @param currentValue 当前值
         * @return true 表示当前值与上次同步值相同且该键此前已同步过，应跳过发包
         */
        public boolean isResourceUnchanged(ITownResourceKey key, double currentValue) {
            if (!lastSyncedResources.containsKey(key)) {
                return false; // 键从未同步过（或此前已归零移除）：视为变化
            }
            return Math.abs(lastSyncedResources.getDouble(key) - currentValue) < TeamTownResourceHolder.DELTA;
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
