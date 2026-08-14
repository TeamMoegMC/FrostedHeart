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
import com.teammoeg.frostedheart.content.town.network.TownHistoryUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownSignalNotificationPacket;
import com.teammoeg.frostedheart.content.town.observation.TownObservationModel;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalHistory;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusModel;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusProvider;
import com.teammoeg.frostedheart.content.town.observation.TownHistoryModel;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEventModel;
import com.teammoeg.frostedheart.content.town.observation.TownSignalNotice;
import com.teammoeg.frostedheart.content.town.observation.TownTowerTipThrottle;
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
import com.teammoeg.frostedheart.content.town.resident.ResidentAgingModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentGenerationModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentDailyModel;
import com.teammoeg.frostedheart.content.town.model.TownAssignmentModel;
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

        // Older saves did not persist town age. A negative sentinel lets the
        // constructor migrate them from the retained settlement count.
        CodecUtil.defaultSupply(CodecUtil.catchingCodec(Codec.LONG), () -> -1L)
        .fieldOf("townDay").forGetter(o -> o.townDay),

        // Old saves have no staffingPlan field. Decode them as EMPTY; the
        // constructor then derives a deterministic plan from surviving work
        // buildings and their existing rosters.
        CodecUtil.defaultSupply(CodecUtil.catchingCodec(TownStaffingPlan.CODEC), () -> TownStaffingPlan.EMPTY)
        .fieldOf("staffingPlan").forGetter(TeamTownData::getStaffingPlan),

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
     * 城镇结算快照历史，最新条目在末尾，按观测配置裁剪。
     * 随存档持久化，并随城镇数据全量同步下发客户端。
     * <p>
     * Settlement snapshot history of the town, newest entry last, capped at
     * the configured number of entries. Persisted with the save and synced
     * to clients with the full town data sync.
     */
    @Getter
    List<TownHistoryEntry> history = new ArrayList<>();

    /** Number of completed town settlements since the town data was created. */
    @Getter
    long townDay;

    /**
     * 玩家编辑的工作建筑队列与保障人数。列表顺序是完整优先关系；保障人数是
     * 第一轮优先补足的目标而非硬上限。通过 {@link #getStaffingPlan()} 读取时会与
     * 当前建筑表对齐，以清理已拆除建筑并补入新建筑。
     */
    private TownStaffingPlan staffingPlan = TownStaffingPlan.EMPTY;

    /** Threshold events accumulated during the current daily settlement. */
    private transient final List<TownSignalEvent> pendingDailySignals = new ArrayList<>();
    /** Settlement events waiting for one per-server-tick player brief. */
    private transient final List<TownSignalEvent> pendingTownTipSignals = new ArrayList<>();
    /** Tower notifications emitted by the immediate/debounce state machine. */
    private transient final List<TownSignalNotice> pendingTowerTipSignals = new ArrayList<>();
    /** Last per-second service state; null until the first generator observation. */
    private transient Boolean lastObservedTowerActive;
    private transient TownTowerTipThrottle.State towerTipState = TownTowerTipThrottle.INITIAL;
    private transient long nextTownTipNotificationId;
    /**
     * 上一次日结算后的保障岗位缺口，仅用于识别“出现缺口/完全恢复”的状态穿越。
     * 它不是城镇玩法状态，不写入存档；重新载入后的第一次结算会建立新的运行时
     * 基线，若当时已有缺口则会发出一次当前风险警告。
     */
    private transient Integer lastStaffingTargetShortfall;

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
    /**
     * Codec 解码构造器。所有建筑必须先装入本实例，再规范化岗位计划，这样旧存档的
     * 空计划才能从仍存在的工作建筑和旧名册中迁移出稳定的初始队列与保障人数。
     *
     * @param name 城镇名称
     * @param resources 城镇资源仓储
     * @param buildings 已持久化的城镇建筑
     * @param residents 已持久化的居民
     * @param terrainResource 地形资源状态
     * @param labour 旧劳动字段；当前构造逻辑保持历史行为并重置为零
     * @param maxlabour 旧最大劳动字段；当前构造逻辑保持历史行为并重置为零
     * @param history 城镇结算历史
     * @param townDay 已完成的城镇日；旧存档使用负数哨兵并从保留历史数量迁移
     * @param staffingPlan 已保存的岗位计划；旧存档缺失时由 Codec 提供空计划
     * @param lastRefugeeSpawnDay 最近一次难民自然刷新所用的稳定世界日
     */
    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource,int labour,int maxlabour, List<TownHistoryEntry> history, long townDay, TownStaffingPlan staffingPlan, long lastRefugeeSpawnDay) {
        super();
        this.history = new ArrayList<>(history);
        this.townDay = townDay >= 0L ? townDay : history.size();
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
        this.staffingPlan = staffingPlan == null ? TownStaffingPlan.EMPTY : staffingPlan;
        normalizeStaffingPlan();
        this.lastRefugeeSpawnDay = lastRefugeeSpawnDay;
    }

    /** Source-compatible constructor for callers predating the persistent town-day field. */
    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource,int labour,int maxlabour, List<TownHistoryEntry> history, TownStaffingPlan staffingPlan, long lastRefugeeSpawnDay) {
        this(name, resources, buildings, residents, terrainResource, labour, maxlabour,
                history, -1L, staffingPlan, lastRefugeeSpawnDay);
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
     * 返回与当前建筑集合一致的权威岗位计划。
     * <p>
     * 读取前会执行轻量规范化：移除已拆除或不再属于工作建筑的条目，并把尚未进入
     * 计划的新工作建筑追加到队尾。旧存档中的空计划也在这里完成惰性迁移。因此调用方
     * 不应缓存旧的计划对象，而应在需要显示或编辑时重新读取。
     *
     * @return 当前规范化后的不可变岗位计划
     */
    public TownStaffingPlan getStaffingPlan() {
        normalizeStaffingPlan();
        return staffingPlan;
    }

    /**
     * 将岗位计划与当前 {@link #buildings} 对齐，并仅在内容实际变化时替换对象。
     * 已存在条目的玩家顺序和保障人数保持不变；缺失条目的迁移规则由
     * {@link TownStaffingPlan#normalize(Map)} 统一定义。
     */
    private void normalizeStaffingPlan() {
        TownStaffingPlan normalized = staffingPlan.normalize(buildings);
        if (!normalized.equals(staffingPlan)) staffingPlan = normalized;
    }

    /**
     * 在服务端最新计划上设置一栋工作建筑的保障人数。
     * <p>
     * 请求中的建筑位置必须仍对应工作建筑；目标值会在编辑当下限制到
     * {@code [0, getMaxResidents()]}。本方法只修改下一次日结算使用的计划，不会在
     * 白天中途移动居民。
     *
     * @param building 要编辑的工作建筑位置
     * @param target 新保障人数
     * @return 内容实际改变时为 {@code true}；建筑无效或结果未变化时为 {@code false}
     */
    public boolean setStaffingTarget(BlockPos building, int target) {
        Optional<TownStaffingPlan> changed = getStaffingPlan().withTarget(
                building, target, buildings);
        if (changed.isEmpty() || changed.get().equals(staffingPlan)) return false;
        staffingPlan = changed.get();
        return true;
    }

    /**
     * 在服务端最新计划上移动一栋工作建筑。
     * <p>
     * 使用“移到某条目之前”的相对操作而不是客户端提交整张列表，可避免两个队员
     * 同时编辑时用旧快照覆盖彼此的其他修改。{@code before} 为空表示移到队尾。
     *
     * @param building 要移动的工作建筑位置
     * @param before 移动后紧随其后的锚点；为空表示队尾
     * @return 内容实际改变时为 {@code true}；建筑/锚点无效或结果未变化时为 {@code false}
     */
    public boolean moveStaffingEntry(BlockPos building, Optional<BlockPos> before) {
        Optional<TownStaffingPlan> changed = getStaffingPlan().move(
                building, before, buildings);
        if (changed.isEmpty() || changed.get().equals(staffingPlan)) return false;
        staffingPlan = changed.get();
        return true;
    }

    /**
     * 客户端应用服务端下发的完整权威岗位计划。
     * <p>
     * 该入口只用于 S2C 增量同步。替换后仍会针对客户端当前建筑快照规范化，并通知
     * 已打开的城镇界面；游戏逻辑不得通过此方法在服务端编辑计划。
     *
     * @param updated 服务端权威计划；空值按空计划处理
     */
    public void applyStaffingPlan(TownStaffingPlan updated) {
        staffingPlan = updated == null ? TownStaffingPlan.EMPTY : updated;
        normalizeStaffingPlan();
        fireStaffingChanged();
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

        TownTowerTipThrottle.Result deferredTower = TownTowerTipThrottle.onTick(
                towerTipState, level.getGameTime());
        towerTipState = deferredTower.state();
        pendingTowerTipSignals.addAll(deferredTower.emitted());
        flushTownTipNotifications(teamData);
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
            // An unformed tower cannot heat even if GeneratorData still carries
            // the last active flag from before the multiblock was broken.
            queueTowerServiceCrossing(world, genData.actualPos != null && genData.isActive);
        }
    }

    private void queueTowerServiceCrossing(ServerLevel world, boolean active) {
        if (lastObservedTowerActive != null && lastObservedTowerActive != active) {
            int hour = (int) ((world.getDayTime() % 24000L) / 1000L);
            pendingDailySignals.add(new TownSignalEvent(-1L, hour,
                    active ? TownSignalEvent.Type.TOWER_SERVICE_RESTORED
                            : TownSignalEvent.Type.TOWER_SERVICE_LOST,
                    active ? TownSignalEvent.Severity.INFORMATION
                            : TownSignalEvent.Severity.CRITICAL,
                    1, "per-second GeneratorData.isActive crossing"));
            TownTowerTipThrottle.Result result = TownTowerTipThrottle.onCrossing(
                    towerTipState, world.getGameTime(), active);
            towerTipState = result.state();
            pendingTowerTipSignals.addAll(result.emitted());
        }
        lastObservedTowerActive = active;
    }

    private void flushTownTipNotifications(TeamDataHolder teamData) {
        if (!pendingTowerTipSignals.isEmpty()) {
            teamData.sendToOnline(FHNetwork.INSTANCE, new TownSignalNotificationPacket(
                    ++nextTownTipNotificationId, List.copyOf(pendingTowerTipSignals)));
            pendingTowerTipSignals.clear();
        }
        if (!pendingTownTipSignals.isEmpty()) {
            List<TownSignalNotice> compacted = TownSignalEventModel.compactNotifications(
                    pendingTownTipSignals);
            pendingTownTipSignals.clear();
            if (!compacted.isEmpty()) {
                teamData.sendToOnline(FHNetwork.INSTANCE, new TownSignalNotificationPacket(
                        ++nextTownTipNotificationId, compacted));
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
        this.recordDailySnapshot(world, teamData);
    }

    /**
     * 每次城镇结算完成后记录一条快照。连续执行 /town tick 时，即使世界时间
     * 没有前进，每次也会分配一个新的城镇结算日；超过配置条数时丢弃最旧记录。
     * <p>
     * Records one snapshot after every town settlement. Repeated /town tick
     * commands receive distinct town settlement days even when world time does
     * not advance; oldest entries are dropped at the configured history cap.
     *
     * @param world 服务端世界 / server world instance
     */
    void recordDailySnapshot(ServerLevel world, TeamDataHolder teamData) {
        if (townDay < Long.MAX_VALUE) townDay++;
        long day = TownHistoryModel.nextSettlementDay(history, WorldClimate.getWorldDay(world));
        Optional<GeneratorData> generator = teamData.getOptional(FHSpecialDataTypes.GENERATOR_DATA);
        BlockPos fallback = generator.filter(value -> value.actualPos != null)
                .map(value -> value.actualPos)
                .orElseGet(() -> buildings.keySet().stream().findFirst().orElse(BlockPos.ZERO));
        TownOperationalStatus current = TownOperationalStatusProvider.capture(
                world, teamData, createTeamTown(), fallback);
        boolean towerWorking = current.tower().active();
        int climateLevel = current.climateLevel();
        TownHistoryEntry previous = history.isEmpty() ? null : history.get(history.size() - 1);
        List<TownSignalEvent> signals = new ArrayList<>();
        boolean hasPerSecondTowerSignal = pendingDailySignals.stream()
                .anyMatch(signal -> TownSignalEventModel.isTowerService(signal.type()));
        for (TownSignalEvent signal : pendingDailySignals) {
            addUniqueSignal(signals, new TownSignalEvent(
                    day, signal.hour(), signal.type(), signal.severity(),
                    signal.affectedCount(), signal.episodeId(), signal.detail()));
        }
        pendingDailySignals.clear();
        if (previous != null) {
            addDailyThresholdSignals(day, previous, current, signals, hasPerSecondTowerSignal);
        }
        for (TownSignalEvent signal : signals) {
            if (!TownSignalEventModel.isTowerService(signal.type()) || !hasPerSecondTowerSignal) {
                pendingTownTipSignals.add(signal);
            }
        }
        TownHistoryEntry entry = new TownHistoryEntry(
                day, current.population(), current.averageHealth(),
                current.averageMental(), buildings.size(), current.p10Health(),
                current.population() == 0 ? 0.0 : residents.values().stream()
                        .mapToDouble(Resident::getHealth).min().orElse(0.0),
                current.p10Mental(), current.population() == 0 ? 0.0 : residents.values().stream()
                        .mapToDouble(Resident::getMental).min().orElse(0.0),
                current.unableToWorkCount(), current.exitRiskCount(), towerWorking,
                climateLevel, signals, TownOperationalHistory.from(current));
        history.add(entry);
        int configuredHistoryDays = FHConfig.SERVER.TOWN.OBSERVATION.historyDays.get();
        while (history.size() > configuredHistoryDays) {
            history.remove(0);
        }
        teamData.sendToOnline(FHNetwork.INSTANCE, new TownHistoryUpdatePacket(entry, townDay));
    }

    private static void addDailyThresholdSignals(
            long day,
            TownHistoryEntry previous,
            TownOperationalStatus current,
            List<TownSignalEvent> signals,
            boolean skipTowerFallback
    ) {
        int climateLevel = current.climateLevel();
        if (previous.climateLevel() >= 0 && climateLevel < 0) {
            addUniqueSignal(signals, new TownSignalEvent(day, 0, TownSignalEvent.Type.CLIMATE_COLD_WARNING,
                    TownSignalEvent.Severity.WARNING, 1, "current temperature category below normal"));
        } else if (previous.climateLevel() < 0 && climateLevel >= 0) {
            addUniqueSignal(signals, new TownSignalEvent(day, 0, TownSignalEvent.Type.CLIMATE_COLD_ENDED,
                    TownSignalEvent.Severity.INFORMATION, 1, "current temperature category returned to normal"));
        }
        addCountCrossing(day, previous.unableToWorkCount(), current.unableToWorkCount(),
                TownSignalEvent.Type.WORK_CAPACITY_LOST,
                TownSignalEvent.Type.WORK_CAPACITY_RECOVERED, signals);
        addCountCrossing(day, previous.exitRiskCount(), current.exitRiskCount(),
                TownSignalEvent.Type.EXIT_RISK_ENTERED,
                TownSignalEvent.Type.EXIT_RISK_RECOVERED, signals);

        TownOperationalHistory priorOperational = previous.operational();
        if (!priorOperational.equals(TownOperationalHistory.EMPTY)) {
            boolean towerWorking = current.tower().active();
            boolean previousTowerWorking = priorOperational.tower().active();
            if (!skipTowerFallback && previousTowerWorking != towerWorking) {
                addUniqueSignal(signals, new TownSignalEvent(day, 0,
                        towerWorking ? TownSignalEvent.Type.TOWER_SERVICE_RESTORED
                                : TownSignalEvent.Type.TOWER_SERVICE_LOST,
                        towerWorking ? TownSignalEvent.Severity.INFORMATION
                                : TownSignalEvent.Severity.CRITICAL,
                        1, "daily GeneratorData.isActive state crossed"));
            }
            FHConfig.Server.Town.Observation config = FHConfig.SERVER.TOWN.OBSERVATION;
            addReserveCrossing(day, priorOperational.foodReserveDays().toLiveMetric(),
                    current.foodReserveDays(), TownSignalEvent.Type.FOOD_RESERVE_WARNING,
                    TownSignalEvent.Type.FOOD_SHORTAGE, TownSignalEvent.Type.FOOD_RESERVE_RECOVERED,
                    Math.max(1, current.population()), config, signals);
            addReserveCrossing(day, priorOperational.fuelReserveDays().toLiveMetric(),
                    current.fuelReserveDays(), TownSignalEvent.Type.FUEL_RESERVE_WARNING,
                    TownSignalEvent.Type.FUEL_SHORTAGE, TownSignalEvent.Type.FUEL_RESERVE_RECOVERED,
                    1, config, signals);
            addCountCrossing(day, priorOperational.unsafeOccupiedHouseCount(),
                    current.unsafeOccupiedHouseCount(), TownSignalEvent.Type.HOUSE_TEMPERATURE_UNSAFE,
                    TownSignalEvent.Type.HOUSE_TEMPERATURE_RECOVERED, signals);
            addCountCrossing(day, priorOperational.stoppedStaffedHuntingCount(),
                    current.stoppedStaffedHuntingCount(), TownSignalEvent.Type.HUNTING_TEMPERATURE_STOP,
                    TownSignalEvent.Type.HUNTING_TEMPERATURE_RECOVERED, signals);
        }
    }

    private static void addReserveCrossing(
            long day,
            TownOperationalStatus.Metric previous,
            TownOperationalStatus.Metric current,
            TownSignalEvent.Type warning,
            TownSignalEvent.Type critical,
            TownSignalEvent.Type recovered,
            int affectedCount,
            FHConfig.Server.Town.Observation config,
            List<TownSignalEvent> signals
    ) {
        TownOperationalStatusModel.ReserveTransition transition =
                TownOperationalStatusModel.reserveTransition(previous, current,
                        config.reserveWarningDays.get(), config.reserveCriticalDays.get());
        TownSignalEvent event = switch (transition) {
            case WARNING -> new TownSignalEvent(day, 0, warning, TownSignalEvent.Severity.WARNING,
                    affectedCount, "reserve entered warning band");
            case CRITICAL -> new TownSignalEvent(day, 0, critical, TownSignalEvent.Severity.CRITICAL,
                    affectedCount, "reserve entered critical band");
            case RECOVERED -> new TownSignalEvent(day, 0, recovered, TownSignalEvent.Severity.INFORMATION,
                    affectedCount, "reserve recovered to safe band");
            case NONE -> null;
        };
        if (event != null) addUniqueSignal(signals, event);
    }

    private static void addCountCrossing(
            long day,
            int previous,
            int current,
            TownSignalEvent.Type increase,
            TownSignalEvent.Type decrease,
            List<TownSignalEvent> signals
    ) {
        int delta = current - previous;
        if (delta == 0) return;
        addUniqueSignal(signals, new TownSignalEvent(day, 0, delta > 0 ? increase : decrease,
                TownSignalEventModel.defaultSeverity(delta > 0 ? increase : decrease),
                Math.abs(delta), "daily threshold count crossed"));
    }

    private static void addUniqueSignal(List<TownSignalEvent> signals, TownSignalEvent candidate) {
        TownSignalEventModel.addToHistory(signals, candidate);
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
                TownSignalEvent.Type type = result.removedForHealth() && result.removedForMental()
                        ? TownSignalEvent.Type.RESIDENT_EXIT_BOTH
                        : result.removedForHealth()
                        ? TownSignalEvent.Type.RESIDENT_EXIT_HEALTH
                        : TownSignalEvent.Type.RESIDENT_EXIT_MENTAL;
                pendingDailySignals.add(new TownSignalEvent(
                        -1L, 0, type, TownSignalEvent.Severity.IRREVERSIBLE, 1,
                        resident.getUUID().toString()));
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
        ResidentAgingModel.Parameters parameters =
                new ResidentAgingModel.Parameters(
                        aging.infantToChildDays.get(),
                        aging.childToAdultDays.get(),
                        aging.infantStrengthGainPerDay.get(),
                        aging.infantIntelligenceGainPerDay.get(),
                        aging.infantAttributeCap.get(),
                        aging.childStrengthGainPerDay.get(),
                        aging.childIntelligenceGainPerDay.get(),
                        aging.childStrengthCap.get(),
                        aging.childIntelligenceCap.get(),
                        aging.adultStrengthGainPerDay.get(),
                        aging.adultIntelligenceGainPerDay.get(),
                        aging.adultAttributeCap.get(),
                        aging.elderStrengthDecayPerDay.get(),
                        aging.elderStrengthFloor.get());
        for (Resident resident : residents.values()) {
            ResidentAgingModel.AgingResult result = ResidentAgingModel.settleDay(
                    resident.getAge(), resident.getAgeDays(), resident.getStrength(),
                    resident.getIntelligence(), parameters);
            resident.setAgeDays(result.ageDays());
            resident.setAge(result.age());
            resident.setStrength(result.strength());
            resident.setIntelligence(result.intelligence());
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
        FHConfig.Server.Town.ResidentGeneration fallback =
                FHConfig.SERVER.TOWN.RESIDENT_GENERATION;
        return ResidentGenerationModel.pickAge(
                CMath.RANDOM::nextDouble,
                new ResidentGenerationModel.AgeWeights(
                        config.weightInfant.get(), config.weightChild.get(),
                        config.weightAdult.get(), config.weightElder.get()),
                new ResidentGenerationModel.AgeWeights(
                        fallback.fallbackWeightInfant.get(), fallback.fallbackWeightChild.get(),
                        fallback.fallbackWeightAdult.get(), fallback.fallbackWeightElder.get()));
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

    /**
     * 在每日正式分配前修复居民位置与建筑名册之间的双向一致性。
     * <p>
     * 方法先清空所有居民保存的住房/工作位置，再以各建筑保存的 UUID 名册重建反向
     * 引用，同时删除已经不存在的居民。住宅名册仍在这里按容量裁剪；工作建筑故意
     * 不裁剪，因为其 {@link Set} 没有玩家优先语义，随机删除会破坏岗位队列。全部
     * 工作名册稍后由 {@link #assignWork()} 按容量、资格、保障人数和队列原子重建。
     *
     * @param town 当前城镇门面；为兼容既有调用签名保留，当前实现不读取该参数
     */
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

                // 住宅在此修剪超额名册；工作建筑稍后由 assignWork 原子重建。
                // 不能在这里迭代 HashSet 随机删人，否则会绕过玩家队列与居民适配分数。
                int maxResident = residentBuilding.getMaxResidents();
                if (!(residentBuilding instanceof ITownResidentWorkBuilding)
                        && residentIDs.size() > maxResident) {
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

    /**
     * 为当天生产生成一份完整、确定性的工作名册。
     * <p>
     * 执行顺序如下：
     * <ol>
     *     <li>规范化玩家岗位计划，并严格按其中的建筑顺序构造当日岗位输入；</li>
     *     <li>在清空旧名册前保存上一日岗位，仅作为居民生产力完全相同时的稳定平局条件；</li>
     *     <li>调用共享纯函数 {@link TownAssignmentModel#plan(List, List, java.util.function.Function, java.util.function.BiPredicate, java.util.function.ToDoubleBiFunction, Comparator)}，先补保障人数，再按最低占用率分配剩余劳动力；</li>
     *     <li>规划成功后一次性清空居民工作位置和建筑名册，再提交全部新分配；</li>
     *     <li>汇总保障人数缺口并排入当日阈值事件。</li>
     * </ol>
     * 先规划后提交保证计算过程中旧状态保持完整，也确保不合格居民在本次日结算释放
     * 岗位。采矿和狩猎随后直接使用这份晨间快照，不再运行另一套资格过滤。
     */
    void assignWork() {
        TownStaffingPlan plan = getStaffingPlan();
        Map<BlockPos, ITownResidentWorkBuilding> workBuildings = new LinkedHashMap<>();
        for (TownStaffingPlan.Entry entry : plan.entries()) {
            AbstractTownBuilding building = buildings.get(entry.building());
            if (building instanceof ITownResidentWorkBuilding workBuilding) {
                workBuildings.put(entry.building(), workBuilding);
            }
        }

        Map<Resident, ITownResidentWorkBuilding> previous = new IdentityHashMap<>();
        for (Resident resident : residents.values()) {
            AbstractTownBuilding old = resident.getWorkPos() == null
                    ? null : buildings.get(resident.getWorkPos());
            if (old instanceof ITownResidentWorkBuilding workBuilding) {
                previous.put(resident, workBuilding);
            }
        }
        List<TownAssignmentModel.Workplace<ITownResidentWorkBuilding>> workplaces =
                new ArrayList<>();
        for (TownStaffingPlan.Entry entry : plan.entries()) {
            ITownResidentWorkBuilding building = workBuildings.get(entry.building());
            if (building != null) {
                workplaces.add(new TownAssignmentModel.Workplace<>(
                        building, building.getMaxResidents(), entry.targetWorkers(),
                        ((AbstractTownBuilding) building).isBuildingWorkable()));
            }
        }

        TownAssignmentModel.Plan<Resident, ITownResidentWorkBuilding> result =
                TownAssignmentModel.plan(
                        new ArrayList<>(residents.values()), workplaces,
                        previous::get,
                        ITownResidentWorkBuilding::canResidentWork,
                        ITownResidentWorkBuilding::getResidentScore,
                        Comparator.comparing(Resident::getUUID));

        residents.values().forEach(resident -> resident.setWorkPos(null));
        for (ITownResidentWorkBuilding building : workBuildings.values()) {
            if (building instanceof com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding residentWorkBuilding) {
                residentWorkBuilding.clearResidents();
            } else {
                building.getResidentsID().clear();
            }
        }
        result.assignments().forEach(assignment ->
                assignment.workplace().addResident(assignment.resident()));
        int targetShortfall = result.workplaces().values().stream()
                .mapToInt(TownAssignmentModel.WorkplaceStatus::targetShortfall)
                .sum();
        queueStaffingTargetCrossing(targetShortfall);
    }

    /**
     * 根据本日保障岗位总缺口记录状态穿越，而不是每天重复记录持续状态。
     * <p>
     * 缺口从零变为正数时产生 {@code STAFFING_TARGET_UNMET}；从正数回到零时产生
     * {@code STAFFING_TARGET_RECOVERED}。正数之间的大小变化只更新运行时基线，当前
     * 界面会直接显示最新缺口，不额外制造重复 Tip。
     *
     * @param shortfall 所有有效工作建筑的 {@code max(target-assigned, 0)} 之和
     */
    private void queueStaffingTargetCrossing(int shortfall) {
        int current = Math.max(0, shortfall);
        if (current > 0 && (lastStaffingTargetShortfall == null
                || lastStaffingTargetShortfall == 0)) {
            pendingDailySignals.add(new TownSignalEvent(
                    -1L, 0, TownSignalEvent.Type.STAFFING_TARGET_UNMET,
                    TownSignalEvent.Severity.WARNING, current,
                    "guaranteed staffing target shortfall"));
        } else if (current == 0 && lastStaffingTargetShortfall != null
                && lastStaffingTargetShortfall > 0) {
            pendingDailySignals.add(new TownSignalEvent(
                    -1L, 0, TownSignalEvent.Type.STAFFING_TARGET_RECOVERED,
                    TownSignalEvent.Severity.INFORMATION,
                    lastStaffingTargetShortfall,
                    "guaranteed staffing targets restored"));
        }
        lastStaffingTargetShortfall = current;
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
     * 通知所有已注册 GUI：建筑、居民、资源、历史和岗位计划均可能已变化。
     * 由全量同步包在替换实例后调用，
     * 保证 GUI 打开瞬间收到的最新全量数据能立即刷新到界面。
     * <p>
     * Notify all registered GUIs that any synchronized town category may have changed.
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
            fireHistoryChanged();
            fireStaffingChanged();
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

    private static void fireHistoryChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onHistoryChanged();
        }
    }

    /**
     * 通知客户端监听器重新读取权威岗位计划。岗位面板在渲染阶段读取最新数据，因此
     * 回调无需重建整个页面，也不会丢失滚动或拖拽以外的临时 UI 状态。
     */
    private static void fireStaffingChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onStaffingChanged();
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

    /** Client-side authoritative town-name update. */
    public void applyNameUpdate(String updatedName) {
        this.name = updatedName;
    }

    /** Client-side idempotent history merge; retransmitted entries replace by town day. */
    public void applyHistoryUpdate(TownHistoryEntry entry) {
        history = new ArrayList<>(TownHistoryModel.upsert(history, entry,
                FHConfig.SERVER.TOWN.OBSERVATION.historyDays.get()));
        fireHistoryChanged();
    }

    /** Client-side history merge paired with the server's persistent town age. */
    public void applyHistoryUpdate(TownHistoryEntry entry, long updatedTownDay) {
        townDay = Math.max(0L, updatedTownDay);
        applyHistoryUpdate(entry);
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
