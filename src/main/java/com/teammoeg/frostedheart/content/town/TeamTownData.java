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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.content.town.building.TownBuildingStore;
import com.teammoeg.frostedheart.content.town.building.BuildingRecordsCodec;
import com.mojang.serialization.MapCodec;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WeatherForecast;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.TownHousingMealService;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.event.ITownBuildingChangeEventListener;
import com.teammoeg.frostedheart.content.town.event.ITownDataUpdateListener;
import com.teammoeg.frostedheart.content.town.event.ITownResidentChangeEventListener;
import com.teammoeg.frostedheart.content.town.event.ITownResidentListener;
import com.teammoeg.frostedheart.content.town.event.ITownResourceChangeEventListener;
import com.teammoeg.frostedheart.content.town.event.TownBuildingChangeEvent;
import com.teammoeg.frostedheart.content.town.event.TownBuildingChangeEvent.ChangeKind;
import com.teammoeg.frostedheart.content.town.event.TownResidentChangeEvent;
import com.teammoeg.frostedheart.content.town.event.TownResourceChangeEvent;
import com.teammoeg.frostedheart.content.town.model.TownAssignmentModel;
import com.teammoeg.frostedheart.content.town.model.TownResidentCareModel;
import com.teammoeg.frostedheart.content.town.network.TownBuildingUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownHistoryUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownPolicyStateUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownResidentUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownResourceUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownSignalNotificationPacket;
import com.teammoeg.frostedheart.content.town.network.TownTransportShortageNotificationPacket;
import com.teammoeg.frostedheart.content.town.observation.TownHistoryModel;
import com.teammoeg.frostedheart.content.town.observation.TownNutritionHistory;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalHistory;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusModel;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusProvider;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEventModel;
import com.teammoeg.frostedheart.content.town.observation.TownSignalNotice;
import com.teammoeg.frostedheart.content.town.observation.TownTowerTipThrottle;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentActivity;
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeChange;
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentDailyModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionSupportModel;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceData;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingState;
import com.teammoeg.frostedheart.content.town.transport.TownTransportShortageNotice;
import com.teammoeg.frostedheart.content.town.transport.TownTransportShortageNotificationModel;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSnapshot;
import com.teammoeg.frostedheart.content.town.transport.TownTransportState;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportConsumerParameters;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointId;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointKind;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import com.teammoeg.frostedheart.content.town.transport.TransportReservationModel;
import com.teammoeg.frostedheart.content.town.transport.WarehouseTopologyEntry;
import com.teammoeg.frostedheart.content.town.transport.WarehouseTopologyListener;
import com.teammoeg.frostedheart.content.town.transport.WarehouseTopologySnapshot;
import com.teammoeg.frostedheart.content.town.transport.device.P2PFilterSummaryState;
import com.teammoeg.frostedheart.content.town.util.ObservableTownMap;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig.Server.Town.RefugeeSpawn;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig.Server.Town.ResidentAging;

import blusunrize.immersiveengineering.common.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.Collections;

/**
 * ITown data for a whole team.
 * <p>
 * It maintains town resources, worker data, and holds a team data when
 * initialized.
 * <p>
 * Everything permanent should be saved in this class.
 */
public class TeamTownData implements com.teammoeg.chorda.dataholders.PersistentComponent {
    public static final MapCodec<TeamTownData> MAP_CODEC = createMapCodec(BuildingRecordsCodec.DISK);
    public static final Codec<TeamTownData> MANAGEMENT_CODEC = createMapCodec(BuildingRecordsCodec.MANAGEMENT).codec();
    private static MapCodec<TeamTownData> createMapCodec(MapCodec<TownBuildingStore> buildingFields) {
        return CodecUtil.mapCodec(t->t
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(Codec.STRING, () -> "Default Town")
    	    .fieldOf("name").forGetter(o -> o.name)))
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(TeamTownResourceHolder.CODEC), TeamTownResourceHolder::new)
    	    .fieldOf("resources").forGetter(o -> o.resources)))
        .add(CodecUtil.wrap(buildingFields.forGetter(o -> o.buildingStore)))
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(CodecUtil.mapCodec("uuid", UUIDUtil.CODEC, "data", Resident.CODEC)), ObservableTownMap::new)
    	    .fieldOf("residents").forGetter(o -> o.residents)))
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(CodecUtil.mapCodec("type", CodecUtil.enumCodec(TerrainResourceType.values()), "data", TerrainResourceData.CODEC)), HashMap::new)
    	    .fieldOf("terrainResource").forGetter(o -> o.terrainResource)))
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(TownHistoryEntry.CODEC.listOf()), ArrayList::new)
    	    .fieldOf("history").forGetter(o -> o.history)))
        // Older saves did not persist town age. A negative sentinel lets the
        // constructor migrate them from the retained settlement count.
        .add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(Codec.LONG), () -> -1L)
    	    .fieldOf("townDay").forGetter(o -> o.townDay)))
        	// Old saves have no staffingPlan field. Decode them as EMPTY; the
        // constructor then derives a deterministic plan from surviving work
        // buildings and their existing rosters.
        .add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(TownStaffingPlan.CODEC), () -> TownStaffingPlan.EMPTY)
    	    .fieldOf("staffingPlan").forGetter(TeamTownData::getStaffingPlan)))
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(TownHousingPlan.CODEC), () -> TownHousingPlan.EMPTY)
    	    .fieldOf("housingPlan").forGetter(TeamTownData::getHousingPlan)))
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(TownPolicyState.CODEC), () -> TownPolicyState.DEFAULT)
    	    .fieldOf("policyState").forGetter(TeamTownData::getPolicyState)))
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(TownTransportState.CODEC), TownTransportState::new)
    	    .fieldOf("transportState").forGetter(TeamTownData::getTransportState)))
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(P2PBindingState.CODEC), () -> P2PBindingState.EMPTY)
    	    .fieldOf("p2pBindingState").forGetter(TeamTownData::getP2PBindingState)))
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(P2PFilterSummaryState.CODEC),() -> P2PFilterSummaryState.EMPTY)
    		.fieldOf("p2pFilterSummaryState").forGetter(TeamTownData::getP2PFilterSummaryState)))
    	.add(CodecUtil.wrap(CodecUtil.defaultSupply(CodecUtil.catchingCodec(Codec.LONG), () -> -1L)
    	    .fieldOf("lastRefugeeSpawnDay").forGetter(o -> o.lastRefugeeSpawnDay)))
        .add(CodecUtil.wrap(AutoSettlementState.CODEC.optionalFieldOf("autoSettlement", AutoSettlementState.pendingLegacy())
            .forGetter(TeamTownData::getAutoSettlement)))
        .apply(TeamTownData::new));
    }

    
    public static final Codec<TeamTownData> CODEC=MAP_CODEC.codec();
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
    private TownBuildingStore buildingStore = new TownBuildingStore();
    Map<BlockPos, AbstractTownBuilding> buildings = buildingStore.resolvedMap();
    private transient TeamDataHolder buildingOwner;


    Map<TerrainResourceType, TerrainResourceData> terrainResource=new EnumMap<>(TerrainResourceType.class);
    /**
     * 城镇结算快照历史，最新条目在末尾，按观测配置裁剪。
     * 随存档持久化，并随城镇数据全量同步下发客户端。
     * <p>
     * Settlement snapshot history of the town, newest entry last, capped at
     * the configured number of entries. Persisted with the save and synced
     * to clients with the full town data sync.
     */
    List<TownHistoryEntry> history = new ArrayList<>();

    public List<TownHistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /** Number of completed town settlements since the town data was created. */
    @Getter
    long townDay;

    /**
     * 玩家编辑的工作建筑队列与保障人数。列表顺序是完整优先关系；保障人数是
     * 第一轮优先补足的目标而非硬上限。通过 {@link #getStaffingPlan()} 读取时会与
     * 当前建筑表对齐，以清理已拆除建筑并补入新建筑。
     */
    private TownStaffingPlan staffingPlan = TownStaffingPlan.EMPTY;

    /** Player-authored residential-care queue and guaranteed-ration targets. */
    private TownHousingPlan housingPlan = TownHousingPlan.EMPTY;

    /** Extensible, globally cooled-down town law selections. */
    private TownPolicyState policyState = TownPolicyState.DEFAULT;

    /** Town-owned aggregate transport service state. */
    @Getter
    private TownTransportState transportState = new TownTransportState();

    /** Town-owned authority for P2P connection relationships and indexes. */
    private P2PBindingState p2pBindingState = P2PBindingState.EMPTY;

    /** Persistent endpoint filter summaries for unloaded-peer inspection and network snapshots. */
    private P2PFilterSummaryState p2pFilterSummaryState = P2PFilterSummaryState.EMPTY;

    private transient P2PBindingState reconciledP2PBindingState;
    private transient TransportConsumerParameters reconciledP2PParameters;

    private transient boolean warehouseTopologyDirty = true;
    private transient boolean warehouseTopologyInitialized;
    private transient boolean warehouseTopologyRefreshInProgress;
    private transient WarehouseTopologySnapshot appliedWarehouseTopology =
            WarehouseTopologySnapshot.UNAVAILABLE;
    private transient final Map<GlobalPos, WarehouseTopologyListener> loadedWarehouseAutomationDevices =
            new HashMap<>();
    private transient long warehouseTopologyBuildCount;
    private transient long warehouseEndpointRecomputeCount;
    private transient long warehouseListenerNotificationCount;

    /** Threshold events accumulated during the current daily settlement. */
    transient final List<TownSignalEvent> pendingDailySignals = new ArrayList<>();
    /** Settlement events waiting for one per-server-tick player brief. */
    transient final List<TownSignalEvent> pendingTownTipSignals = new ArrayList<>();
    /** Tower notifications emitted by the immediate/debounce state machine. */
    transient final List<TownSignalNotice> pendingTowerTipSignals = new ArrayList<>();
    /** Morning transport shortages waiting for the shared per-tick notification flush. */
    transient final List<TownTransportShortageNotice> pendingTransportShortageTips =
            new ArrayList<>();
    /** Last per-second service state; null until the first generator observation. */
    transient Boolean lastObservedTowerActive;
    transient TownTowerTipThrottle.State towerTipState = TownTowerTipThrottle.INITIAL;
    transient TownTransportShortageNotificationModel.State transportShortageTipState =
            TownTransportShortageNotificationModel.INITIAL;
    transient long nextTownTipNotificationId;
    /**
     * 上一次日结算后的保障岗位缺口，仅用于识别“出现缺口/完全恢复”的状态穿越。
     * 它不是城镇玩法状态，不写入存档；重新载入后的第一次结算会建立新的运行时
     * 基线，若当时已有缺口则会发出一次当前风险警告。
     */
    transient Integer lastStaffingTargetShortfall;

    /**
     * 最近一次按世界日结算难民刷新的日期（服务端持久化）。
     * 防止同一天内重复结算（含 /town tick 手动调用）导致重复刷新。
     */
    long lastRefugeeSpawnDay = -1L;

    /** Local change wiring is installed once; only a bound server holder tracks saves and deltas. */
    private boolean listenerInitialized = false;
    private transient Runnable persistenceListener = () -> {};
    private transient boolean serverTracking;
    private AutoSettlementState autoSettlement = AutoSettlementState.pendingNew();

    @Override
    public void setPersistenceListener(Runnable listener) {
        persistenceListener = listener == null ? () -> {} : listener;
        serverTracking = listener != null;
        if (!serverTracking) dataSyncCache.clearChanged();
        initializeChangeTracking();
        if (serverTracking) buildingStore.reportInitialChanges();
    }

    void persistentChanged() { persistenceListener.run(); }

    public AutoSettlementState getAutoSettlement() { return autoSettlement; }

    public void replaceAutoSettlement(AutoSettlementState state) {
        if (!Objects.equals(autoSettlement, state)) {
            autoSettlement = state;
            persistentChanged();
        }
    }

    public void prepareAssignments() {
        normalizeStaffingPlan();
        normalizeHousingPlan();
    }
    @Getter
    final DataSyncCache dataSyncCache = new DataSyncCache();

    /**
     * 居民生命周期监听器（服务端单订阅）：居民增删/每日结算事件的门面出口。
     * 事件通道与 DataSyncCache 的 {@code ObservableTownMap} 钩子链完全分离——
     * 那三个钩子是增量更新专用（见 {@link ObservableTownMap} javadoc），居民模拟
     * 不注册其上，而是经 {@link TeamTown} 门面方法 fire：added 只在房屋分配完成后
     * 触发（锚点必已就绪，单次通知无双触发）。仅在服务端设置，客户端实例无人注册。
     * <p>
     * Resident lifecycle listener (single subscriber, server side): the facade
     * outlet for resident add/remove and daily-settlement events. The channel is
     * fully separate from DataSyncCache's ObservableTownMap hook chain — those
     * hooks are incremental-sync-only (see {@link ObservableTownMap} javadoc),
     * and the citizen simulation does not register there; it listens via facade
     * fires from {@link TeamTown}: added fires only after house allocation
     * (anchor guaranteed ready; single notification, no double-fire).
     * Never set on client instances.
     */
    private ITownResidentListener residentListener;
    transient boolean inSettlement;
    transient boolean needsRuntimeRepair;
    final transient List<Runnable> dayEffects = new ArrayList<>();

    public boolean isInSettlement() { return inSettlement; }

    private void notifyResidentEffect(Runnable effect) {
        if (inSettlement) dayEffects.add(effect);
        else {
            try { effect.run(); }
            catch (RuntimeException failure) {
                needsRuntimeRepair = true;
                FHMain.LOGGER.error("Town resident notification requires rebinding", failure);
            }
        }
    }
    

    /**
     * 设置居民生命周期监听器（服务端，模拟 adopt 时调用）。
     * 单订阅者——居民模拟是唯一消费者；重复设置覆盖（同实例幂等）。
     * <p>
     * Sets the resident lifecycle listener (server, called on adopt). Single
     * subscriber — the citizen simulation is the only consumer; re-setting
     * overwrites (idempotent for the same instance).
     *
     * @param listener 监听器 / the listener
     */
    public void setResidentListener(ITownResidentListener listener) {
        this.residentListener = listener;
    }

    /**
     * 通知居民生命周期监听器：居民已加入城镇。
     * 触发点：{@link TeamTown#addResident} 成功路径与 {@code debugAddResident}
     * （put 后锚点已就绪；单次通知，无双触发）。
     * <p>
     * Fires "resident added" to the lifecycle listener (after house allocation;
     * single notification, no double-fire).
     *
     * @param resident 加入的居民 / the added resident
     */
    public void fireResidentAdded(Resident resident) {
        if (this.residentListener != null)
            notifyResidentEffect(() -> { if (residentListener != null) residentListener.onResidentAdded(resident); });
    }

    /**
     * 通知居民生命周期监听器：居民已移出城镇。
     * 触发点：{@link TeamTown#removeResident}（集合移除完成后）。
     * <p>
     * Fires "resident removed" to the lifecycle listener (after collection removal).
     *
     * @param resident 移出的居民 / the removed resident
     */
    public void fireResidentRemoved(Resident resident) {
        if (this.residentListener != null)
            notifyResidentEffect(() -> { if (residentListener != null) residentListener.onResidentRemoved(resident); });
    }

    /**
     * 通知居民生命周期监听器：每日结算完成（tickMorning 末尾）。
     * <p>
     * Fires "daily settlement done" to the lifecycle listener (end of tickMorning).
     */
    public void fireMorningDone() {
        if (this.residentListener != null)
            this.residentListener.onTownMorningDone(this);
    }

    /**
     * 客户端 GUI 监听器集合（static 而非实例字段）。
     * <p>
     * 原因：全量同步包 {@code TeamTownDataS2CPacket} 会用新解码出的实例替换客户端
     * TeamTownData；若监听器挂在实例上，GUI 打开后收到全量包就会丢失监听。static 集合
     * 与实例替换解耦；客户端同一时刻只有一个本地玩家队伍，全局唯一无歧义。所有增删与触发
     * 都发生在主线程（包处理经 {@code NetworkEvent.Context#enqueueWork}），
     * ConcurrentHashMap 仅作额外的并发保护。
     */

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
    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource, List<TownHistoryEntry> history, long townDay, TownStaffingPlan staffingPlan, TownHousingPlan housingPlan, TownPolicyState policyState, TownTransportState transportState, P2PBindingState p2pBindingState, P2PFilterSummaryState p2pFilterSummaryState, long lastRefugeeSpawnDay, AutoSettlementState autoSettlement) {
        this(name, resources, storeOf(buildings), residents, terrainResource, history, townDay, staffingPlan, housingPlan, policyState, transportState, p2pBindingState, p2pFilterSummaryState, lastRefugeeSpawnDay, autoSettlement);
    }
    private static TownBuildingStore storeOf(Map<BlockPos, ITownBuilding> input) {
        TownBuildingStore result = new TownBuildingStore();
        input.values().forEach(value -> { if (value instanceof AbstractTownBuilding building) result.install(building); });
        return result;
    }

    public TeamTownData(String name, TeamTownResourceHolder resources, TownBuildingStore decodedBuildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource, List<TownHistoryEntry> history, long townDay, TownStaffingPlan staffingPlan, TownHousingPlan housingPlan, TownPolicyState policyState, TownTransportState transportState, P2PBindingState p2pBindingState, P2PFilterSummaryState p2pFilterSummaryState, long lastRefugeeSpawnDay, AutoSettlementState autoSettlement) {
        super();
        this.history = new ArrayList<>(history);
        this.townDay = townDay >= 0L ? townDay : history.size();
        this.name = name;
        this.resources = resources;
        this.buildingStore = decodedBuildings;
        this.buildings = buildingStore.resolvedMap();
        this.residents.putAll(residents);
        this.terrainResource.putAll(terrainResource);

        this.staffingPlan = staffingPlan == null ? TownStaffingPlan.EMPTY : staffingPlan;
        this.housingPlan = housingPlan == null ? TownHousingPlan.EMPTY : housingPlan;
        this.policyState = policyState == null ? TownPolicyState.DEFAULT : policyState;
        this.transportState = transportState == null ? new TownTransportState() : transportState;
        this.p2pBindingState = p2pBindingState == null ? P2PBindingState.EMPTY : p2pBindingState;
        this.p2pFilterSummaryState = p2pFilterSummaryState == null
                ? P2PFilterSummaryState.EMPTY : p2pFilterSummaryState;
        this.lastRefugeeSpawnDay = lastRefugeeSpawnDay;
        this.autoSettlement = autoSettlement;
        initializeChangeTracking();
    }

    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings,
                        Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource,
                        List<TownHistoryEntry> history, long townDay, TownStaffingPlan staffingPlan,
                        TownHousingPlan housingPlan, TownPolicyState policyState, TownTransportState transportState,
                        P2PBindingState p2pBindingState, P2PFilterSummaryState p2pFilterSummaryState, long lastRefugeeSpawnDay) {
        this(name, resources, buildings, residents, terrainResource, history, townDay, staffingPlan,
                housingPlan, policyState, transportState, p2pBindingState, p2pFilterSummaryState,
                lastRefugeeSpawnDay, AutoSettlementState.pendingLegacy());
    }
    /** Source-compatible constructor for callers predating P2P filter summaries. */
    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource, List<TownHistoryEntry> history, long townDay, TownStaffingPlan staffingPlan, TownHousingPlan housingPlan, TownPolicyState policyState, TownTransportState transportState, P2PBindingState p2pBindingState, long lastRefugeeSpawnDay) {
        this(name, resources, buildings, residents, terrainResource,
                history, townDay, staffingPlan, housingPlan, policyState,
                transportState, p2pBindingState, P2PFilterSummaryState.EMPTY,
                lastRefugeeSpawnDay);
    }

    /** Source-compatible constructor for callers predating P2P binding state. */
    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource, List<TownHistoryEntry> history, long townDay, TownStaffingPlan staffingPlan, TownHousingPlan housingPlan, TownPolicyState policyState, TownTransportState transportState, long lastRefugeeSpawnDay) {
        this(name, resources, buildings, residents, terrainResource,
                history, townDay, staffingPlan, housingPlan, policyState,
                transportState, P2PBindingState.EMPTY, P2PFilterSummaryState.EMPTY,
                lastRefugeeSpawnDay);
    }

    /** Source-compatible constructor for callers predating town transport state. */
    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource, List<TownHistoryEntry> history, long townDay, TownStaffingPlan staffingPlan, TownHousingPlan housingPlan, TownPolicyState policyState, long lastRefugeeSpawnDay) {
        this(name, resources, buildings, residents, terrainResource,
                history, townDay, staffingPlan, housingPlan, policyState,
                new TownTransportState(), lastRefugeeSpawnDay);
    }

    /** Source-compatible constructor for callers predating housing and policy plans. */
    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource, List<TownHistoryEntry> history, long townDay, TownStaffingPlan staffingPlan, long lastRefugeeSpawnDay) {
        this(name, resources, buildings, residents, terrainResource,
                history, townDay, staffingPlan, TownHousingPlan.EMPTY,
                TownPolicyState.DEFAULT, lastRefugeeSpawnDay);
    }

    /** Source-compatible constructor for callers predating the persistent town-day field. */
    public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource, List<TownHistoryEntry> history, TownStaffingPlan staffingPlan, long lastRefugeeSpawnDay) {
        this(name, resources, buildings, residents, terrainResource,
                history, -1L, staffingPlan, lastRefugeeSpawnDay);
    }

    public TownHousingPlan getHousingPlan() {
        return housingPlan;
    }

    private void normalizeHousingPlan() {
        TownHousingPlan normalized = housingPlan.normalize(buildings, buildingStore::preservesReference);
        if (!normalized.equals(housingPlan)) { housingPlan = normalized; persistentChanged(); }
    }

    public boolean setHousingGuarantee(BlockPos building, int target) {
        Optional<TownHousingPlan> changed = getHousingPlan().withGuarantee(
                building, target, buildings);
        if (changed.isEmpty() || changed.get().equals(housingPlan)) return false;
        housingPlan = changed.get();
        persistentChanged();
        return true;
    }

    public boolean moveHousingEntry(BlockPos building, Optional<BlockPos> before) {
        Optional<TownHousingPlan> changed = getHousingPlan().move(
                building, before, buildings);
        if (changed.isEmpty() || changed.get().equals(housingPlan)) return false;
        housingPlan = changed.get();
        persistentChanged();
        return true;
    }

    public void applyHousingPlan(TownHousingPlan updated) {
        housingPlan = updated == null ? TownHousingPlan.EMPTY : updated;
        normalizeHousingPlan();
        fireHousingChanged();
    }

    public TownPolicyState getPolicyState() {
        return policyState;
    }

    public boolean requestCareLaw(TownCareLaw law) {
        TownPolicyState.EditResult result = policyState.requestCareLaw(
                law, townDay,
                FHConfig.SERVER.TOWN.RESIDENT_RULES.townPolicyCooldownDays.get());
        if (!result.changed()) return false;
        policyState = result.state();
        persistentChanged();
        return true;
    }

    public void applyPolicyState(TownPolicyState updated) {
        policyState = updated == null ? TownPolicyState.DEFAULT : updated;
        firePoliciesChanged();
    }

    public TeamTownData(SpecialDataHolder teamData) {
        super();
        initializeChangeTracking();
        if (teamData instanceof TeamDataHolder data && data.getTeam() != null) {
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

    public P2PBindingState getP2PBindingState() {
        return p2pBindingState;
    }

    void setP2PBindingState(P2PBindingState state) {
        p2pBindingState = state == null ? P2PBindingState.EMPTY : state;
        persistentChanged();
        reconciledP2PBindingState = null;
        reconciledP2PParameters = null;
    }

    public P2PFilterSummaryState getP2PFilterSummaryState() {
        return p2pFilterSummaryState;
    }

    void setP2PFilterSummaryState(P2PFilterSummaryState state) {
        p2pFilterSummaryState = state == null ? P2PFilterSummaryState.EMPTY : state;
        persistentChanged();
    }

    boolean isP2PTransportReconciled(TransportConsumerParameters parameters) {
        return p2pBindingState == reconciledP2PBindingState
                && Objects.equals(parameters, reconciledP2PParameters);
    }

    void markP2PTransportReconciled(TransportConsumerParameters parameters) {
        reconciledP2PBindingState = p2pBindingState;
        reconciledP2PParameters = parameters;
    }

    public TownBuildingStore getBuildingStore() { return buildingStore; }
    TeamDataHolder buildingOwner() { return buildingOwner; }
    void removeBuildingPlans(BlockPos pos) {
        staffingPlan = staffingPlan.withoutBuilding(pos);
        housingPlan = housingPlan.withoutBuilding(pos);
        residents.values().forEach(resident -> resident.clearPendingAt(pos));
        persistentChanged();
    }
    public void bindBuildingOwner(TeamDataHolder owner) {
        buildingOwner = owner;
        if (buildingStore.dimension() == null) {
            owner.getOptional(FHSpecialDataTypes.GENERATOR_DATA).ifPresent(generator -> buildingStore.setDimension(generator.dimension));
        }
        buildingStore.reportInitialChanges();
    }
    public static ServerLevel executionLevel(TeamDataHolder owner) {
        CTeamDataManager teams = CTeamDataManager.INSTANCE;
        if (owner == null || teams == null || teams.findByHolderId(owner.getId()) != owner) return null;
        var generator = owner.getOptional(FHSpecialDataTypes.GENERATOR_DATA).orElse(null);
        ResourceKey<Level> dimension = resolveTownDimension(owner);
        if (generator == null || dimension == null || !dimension.equals(generator.dimension)) return null;
        return teams.server().getLevel(dimension);
    }

    boolean isCurrentExecutionContext(ServerLevel level, TeamDataHolder owner) {
        return level != null && owner != null
                && owner.getOptional(FHSpecialDataTypes.TOWN_DATA).orElse(null) == this
                && executionLevel(owner) == level;
    }
    public static ResourceKey<Level> executionDimension(TeamDataHolder owner) {
        ServerLevel level = executionLevel(owner);
        return level == null ? null : level.dimension();
    }

    public static ResourceKey<Level> resolveTownDimension(TeamDataHolder teamData) {
        if (teamData == null) {
            return null;
        }
        TeamTownData town = teamData.getOptional(FHSpecialDataTypes.TOWN_DATA).orElse(null);
        if (town != null && town.buildingStore.dimension() != null) return town.buildingStore.dimension();
        return teamData.getOptional(FHSpecialDataTypes.GENERATOR_DATA).map(generator -> generator.dimension).orElse(null);
    }

    WarehouseTopologySnapshot getAppliedWarehouseTopology() {
        return appliedWarehouseTopology;
    }

    void markWarehouseTopologyDirty() {
        warehouseTopologyDirty = true;
    }

    void registerWarehouseTopologyListener(
            GlobalPos devicePos,
            WarehouseTopologyListener listener
    ) {
        loadedWarehouseAutomationDevices.put(
                Objects.requireNonNull(devicePos, "devicePos"),
                Objects.requireNonNull(listener, "listener"));
    }

    void unregisterWarehouseTopologyListener(
            GlobalPos devicePos,
            WarehouseTopologyListener listener
    ) {
        if (devicePos != null && listener != null) {
            loadedWarehouseAutomationDevices.remove(devicePos, listener);
        }
    }

    public Map<GlobalPos, WarehouseTopologyListener> takeWarehouseListeners() {
        Map<GlobalPos, WarehouseTopologyListener> listeners = new HashMap<>(loadedWarehouseAutomationDevices);
        loadedWarehouseAutomationDevices.clear();
        return listeners;
    }

    WarehouseTopologySnapshot refreshWarehouseTopologyIfDirty(
            TransportConsumerParameters parameters,
            ResourceKey<Level> authoritativeTownDimension
    ) {
        Objects.requireNonNull(parameters, "parameters");
        if (warehouseTopologyRefreshInProgress) {
            return appliedWarehouseTopology;
        }
        if (warehouseTopologyInitialized
                && Objects.equals(appliedWarehouseTopology.townDimension(), authoritativeTownDimension)
                && !warehouseTopologyDirty) {
            return appliedWarehouseTopology;
        }

        warehouseTopologyRefreshInProgress = true;
        try {
            reloadMaxCapacity();
            warehouseTopologyBuildCount++;
            List<WarehouseTopologyEntry> entries = buildings.values().stream()
                    .filter(WarehouseBuilding.class::isInstance)
                    .map(WarehouseBuilding.class::cast)
                    .filter(WarehouseBuilding::isBuildingWorkable)
                    .filter(warehouse -> Double.isFinite(warehouse.getCapacity())
                            && warehouse.getCapacity() > 0.0)
                    .map(warehouse -> new WarehouseTopologyEntry(
                            warehouse.getPos(), warehouse.getCapacity()))
                    .sorted(WarehouseTopologyEntry.CORE_POS_ORDER)
                    .toList();
            WarehouseTopologySnapshot candidate = WarehouseTopologySnapshot.of(
                    authoritativeTownDimension, entries);
            warehouseTopologyDirty = false;
            if (warehouseTopologyInitialized && candidate.equals(appliedWarehouseTopology)) {
                return appliedWarehouseTopology;
            }

            appliedWarehouseTopology = candidate;
            warehouseTopologyInitialized = true;
            Map<TransportEndpointId, TransportReservation> replacements = new TreeMap<>(
                    TransportEndpointId.STABLE_COMPARATOR);
            for (Map.Entry<TransportEndpointId, TransportReservation> entry
                    : transportState.getReservations().entrySet()) {
                if (entry.getValue().endpointKind() != TransportEndpointKind.WAREHOUSE_INTERFACE) {
                    continue;
                }
                warehouseEndpointRecomputeCount++;
                replacements.put(entry.getKey(), reservationForTopology(
                        entry.getKey(), entry.getValue(), candidate, parameters));
            }
            if (transportState.replaceReservations(replacements, parameters)) {
                dataSyncCache.markTransportStateChanged();
            }

            List<WarehouseTopologyListener> listeners = List.copyOf(
                    loadedWarehouseAutomationDevices.values());
            for (WarehouseTopologyListener listener : listeners) {
                try {
                    listener.onWarehouseTopologyChanged(candidate);
                    warehouseListenerNotificationCount++;
                } catch (RuntimeException exception) {
                    needsRuntimeRepair = true;
                    FHMain.LOGGER.warn("Warehouse topology listener failed for town {}.",
                            name, exception);
                }
            }
            return candidate;
        } finally {
            warehouseTopologyRefreshInProgress = false;
        }
    }

    private static TransportReservation reservationForTopology(
            TransportEndpointId endpointId,
            TransportReservation old,
            WarehouseTopologySnapshot topology,
            TransportConsumerParameters parameters
    ) {
        if (!topology.isUsable()
                || !endpointId.endpointPos().dimension().equals(topology.townDimension())) {
            return unavailableReservation(old);
        }
        double metric = TransportReservationModel.warehouseWeightedDistance(
                endpointId.endpointPos().pos(), topology.entries());
        if (!TransportReservationModel.isFiniteNonNegative(metric)) {
            return unavailableReservation(old);
        }
        if (old.rateItemsPerSecond() == 0) {
            return new TransportReservation(old.endpointKind(), 0, metric, 0.0,
                    TransportAdmissionStatus.DISABLED);
        }
        double reserved = TransportReservationModel.capacityForStoredRate(
                old.endpointKind(), old.rateItemsPerSecond(), metric, parameters);
        if (!TransportReservationModel.isFiniteNonNegative(reserved)) {
            return unavailableReservation(old);
        }
        return new TransportReservation(old.endpointKind(), old.rateItemsPerSecond(),
                metric, reserved, TransportAdmissionStatus.ACTIVE);
    }

    private static TransportReservation unavailableReservation(TransportReservation old) {
        return new TransportReservation(old.endpointKind(), old.rateItemsPerSecond(),
                0.0, 0.0, TransportAdmissionStatus.UNAVAILABLE);
    }

    long getWarehouseTopologyBuildCount() {
        return warehouseTopologyBuildCount;
    }

    long getWarehouseEndpointRecomputeCount() {
        return warehouseEndpointRecomputeCount;
    }

    long getWarehouseListenerNotificationCount() {
        return warehouseListenerNotificationCount;
    }

    /**
     * Returns the current logical client's synchronized town snapshot, if the
     * initial full synchronization has arrived.
     * <p>
     * Client town-building block entities must use this accessor instead of
     * {@code CTeamDataManager}: the latter is server-only and only appears to
     * work in an integrated server because both logical sides share one JVM.
     */
    public static Optional<TeamTown> getClientTown() {
        return com.teammoeg.frostedheart.content.town.client.TownClientUpdates.currentTown().map(TeamTownData::createTeamTown);
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
        return staffingPlan;
    }

    /**
     * 将岗位计划与当前 {@link #buildings} 对齐，并仅在内容实际变化时替换对象。
     * 已存在条目的玩家顺序和保障人数保持不变；缺失条目的迁移规则由
     * {@link TownStaffingPlan#normalize(Map)} 统一定义。
     */
    private void normalizeStaffingPlan() {
        TownStaffingPlan normalized = staffingPlan.normalize(buildings, buildingStore::preservesReference);
        if (!normalized.equals(staffingPlan)) { staffingPlan = normalized; persistentChanged(); }
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
        persistentChanged();
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
        persistentChanged();
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
    private void initializeChangeTracking() {
        if(!this.listenerInitialized){
            this.buildingStore.setListeners(this.dataSyncCache, () -> {
                persistentChanged();
                markWarehouseTopologyDirty();
                if (serverTracking && buildingOwner != null) TownSync.invalidate(buildingOwner);
            });
            this.residents.setOnAttach(r -> r.setChangeEventListener(this.dataSyncCache));
            this.residents.setOnDetach(r -> r.setChangeEventListener(null));
            this.resources.setChangeListener(this.dataSyncCache);
            this.resources.setNotificationDispatcher(this::notifyResidentEffect);
            this.terrainResource.values().forEach(value -> value.setChangeListener(this::persistentChanged));
            this.residents.setOnChange((uuid)-> {this.dataSyncCache.onResidentChange(new TownResidentChangeEvent(this.residents, uuid));});
            this.residents.values().forEach(resident -> resident.setChangeEventListener(this.dataSyncCache));
            this.listenerInitialized = true;
        }

    }

    void prepareView(ServerLevel level, TeamDataHolder teamData) {
        prepareBuildingGeometry();
        TeamTown town = new TeamTown(this);
        town.prepareWarehouseTopology(executionDimension(teamData));
        town.prepareTransportState();
    }

    void prepareBuildingGeometry() {
        if (dataSyncCache.hasChangedZone()) {
            // Recompute the entire affected relation once: moving one room can
            // release both its old neighbours and its new neighbours.
            checkOccupiedAreaOverlap();
            dataSyncCache.drainChangedBuildingZones();
        }
    }

    public void tick(ServerLevel level, TeamDataHolder teamData) { TownSync.tick(this, level, teamData); }

    private void flushChanges(ServerLevel level, TeamDataHolder teamData) { TownSync.flushChanges(this, level, teamData); }

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

    private void flushTownTipNotifications(TeamDataHolder teamData) { TownSync.flushTownTipNotifications(this, teamData); }

    public TownSettlement.Result tickMorning(ServerLevel world, TeamDataHolder teamData) {
        return TownSettlement.advanceOneDay(this, world, teamData, java.util.OptionalLong.empty());
    }

    void activatePendingPolicies() {
        TownPolicyState activated = policyState.activatePending();
        if (!activated.equals(policyState)) {
            policyState = activated;
            persistentChanged();
        }
    }
    long nextTownDay() {
        return townDay < Long.MAX_VALUE ? townDay + 1L : Long.MAX_VALUE;
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
    record PreparedDayCommit(List<TownHistoryEntry> history, TownHistoryEntry entry, long townDay,
                             int signalCount, List<TownSignalEvent> tips) {}

    PreparedDayCommit prepareDayCommit(ServerLevel world, TeamDataHolder teamData) {

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

        if (previous != null) {
            addDailyThresholdSignals(day, previous, current, signals, hasPerSecondTowerSignal);
        }
        List<TownSignalEvent> tips = new ArrayList<>();
        for (TownSignalEvent signal : signals) {
            if (!TownSignalEventModel.isTowerService(signal.type()) || !hasPerSecondTowerSignal) {
                tips.add(signal);
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
                climateLevel, signals, TownOperationalHistory.from(current),
                TownNutritionHistory.capture(residents.values().stream()
                        .map(Resident::getNutrition).toList()));
        List<TownHistoryEntry> updated = new ArrayList<>(history);
        updated.add(entry);
        int limit = Math.max(0, FHConfig.SERVER.TOWN.OBSERVATION.historyDays.get());
        if (updated.size() > limit) updated = new ArrayList<>(updated.subList(updated.size() - limit, updated.size()));
        return new PreparedDayCommit(updated, entry, nextTownDay(), pendingDailySignals.size(), List.copyOf(tips));
    }

    void commitDay(PreparedDayCommit prepared, java.util.OptionalLong automaticWindow) {
        history = prepared.history();
        townDay = prepared.townDay();
        if (automaticWindow.isPresent()) autoSettlement = autoSettlement.afterCompletion(automaticWindow.getAsLong());
        pendingDailySignals.subList(0, Math.min(prepared.signalCount(), pendingDailySignals.size())).clear();
        pendingTownTipSignals.addAll(prepared.tips());
        persistentChanged();
    }

    void publishDayEffects(PreparedDayCommit prepared, TeamDataHolder holder) {
        for (Runnable effect : List.copyOf(dayEffects)) {
            try { effect.run(); }
            catch (RuntimeException failure) {
                needsRuntimeRepair = true;
                FHMain.LOGGER.error("Town {} resident notification requires rebinding", holder.getId(), failure);
            }
        }
        fireMorningDone();
        holder.sendToOnline(FHNetwork.INSTANCE, new TownHistoryUpdatePacket(prepared.entry(), townDay));
        holder.sendToOnline(FHNetwork.INSTANCE, new TownPolicyStateUpdatePacket(policyState));
    }

    void recordDailySnapshot(ServerLevel world, TeamDataHolder holder) {
        PreparedDayCommit prepared = prepareDayCommit(world, holder);
        commitDay(prepared, java.util.OptionalLong.empty());
        publishDayEffects(prepared, holder);
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
        List<BlockPos> invalidBuildings = new ArrayList<>();
        for (AbstractTownBuilding building : buildings.values()) {
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
                invalidBuildings.add(pos);
            }
        }
        invalidBuildings.forEach(pos -> town.removeMissingCore(level, pos));
    }
    void checkOccupiedAreaOverlap() {
        Set<AbstractTownBuilding> overlapped = new HashSet<>();
        Map<BlockPos, AbstractTownBuilding> roomsBySignature = new HashMap<>();
        for (AbstractTownBuilding building : buildings.values()) {
            OccupiedZoneInfo geometry = building.getOccupiedVolume();
            if (!geometry.isValid() || geometry.getOccupiedCells().isEmpty()) {
                continue;
            }
            AbstractTownBuilding previous = roomsBySignature.put(geometry.getSignature(), building);
            if (previous != null) {
                overlapped.add(previous);
                overlapped.add(building);
            }
        }
        for (AbstractTownBuilding building : buildings.values()) {
            building.setOverlapped(overlapped.contains(building));
        }
    }

    /**
     * 处理村民死亡
     */
    void tickResidentsMorning() { TownSettlement.tickResidentsMorning(this); }

    /**
     * Settles daily strength/intelligence with the resident's pre-transition age,
     * then advances age-days and applies infant/child age transitions.
     */
    void tickResidentsAging() { TownSettlement.tickResidentsAging(this); }

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
    void tickRefugeeSpawnAndDespawn(ServerLevel world, TeamDataHolder teamData) { TownSettlement.tickRefugeeSpawnAndDespawn(this, world, teamData); }

    /**
     * 强制刷一批难民（调试命令 /town spawn_refugees 用），并更新按日结算标记。
     */
    public void debugSpawnRefugeeBatch(ServerLevel world, TeamDataHolder teamData) { TownSettlement.debugSpawnRefugeeBatch(this, world, teamData); }

    /**
     * 在能量塔周围按天气参数刷一批难民。
     *
     * @return 实际生成数量
     */
    int spawnRefugeeBatch(ServerLevel world, TeamDataHolder teamData, RefugeeSpawnWeather weather) { return TownSettlement.spawnRefugeeBatch(this, world, teamData, weather); }

    /**
     * 判定当天的刷新天气：暖流 = 温度级别≥1 且晴天；寒流 = 温度级别≤-1 或暴风雪；其余平稳。
     * 温度取塔位置的气候基线（与预报系统同源）。
     */
    private RefugeeSpawnWeather getSpawnWeather(ServerLevel world, BlockPos towerPos) { return TownSettlement.getSpawnWeather(this, world, towerPos); }

    /**
     * 在塔周围 [minR, maxR] 随机距离与角度找可落地生成点，最多尝试 16 次。
     */
    private BlockPos findRefugeeSpawnPos(ServerLevel world, BlockPos towerPos) { return TownSettlement.findRefugeeSpawnPos(this, world, towerPos); }

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


    // Rebuild the complete residential roster in player-authored care order.
    void allocateHouse() { TownAssignments.allocateHouse(this); }

    /**
     * Places one newly-added resident without rebuilding every household.
     * The next morning settlement will include them in the full care-priority pass.
     */
    void allocateNewResident(Resident resident) { TownAssignments.allocateNewResident(this, resident); }

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
    void assignWork() { TownAssignments.assignWork(this); }

    /**
     * 根据本日保障岗位总缺口记录状态穿越，而不是每天重复记录持续状态。
     * <p>
     * 缺口从零变为正数时产生 {@code STAFFING_TARGET_UNMET}；从正数回到零时产生
     * {@code STAFFING_TARGET_RECOVERED}。正数之间的大小变化只更新运行时基线，当前
     * 界面会直接显示最新缺口，不额外制造重复 Tip。
     *
     * @param shortfall 所有有效工作建筑的 {@code max(target-assigned, 0)} 之和
     */
    void queueStaffingTargetCrossing(int shortfall) {
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

    void linkMinesToBases() { TownSettlement.linkMinesToBases(this); }


    void recalcOreChunkResources() { TownSettlement.recalcOreChunkResources(this); }

    /**
     * execute work method of buildings.
     */
    void buildingsWork(ServerLevel world){ TownSettlement.buildingsWork(this, world); }

    /** Finalizes the aggregate report after every station has produced. */
    void finishDailyTransportSettlement() { TownSettlement.finishDailyTransportSettlement(this); }

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

    private TerrainResourceData getOrCreateTerrainResource(TerrainResourceType type) {
        return terrainResource.computeIfAbsent(type, key -> {
            TerrainResourceData created = RESOURCE_DATA_SUPPLIER.apply(key);
            created.setChangeListener(this::persistentChanged);
            persistentChanged();
            return created;
        });
    }

    public double pickTerrainResource(TerrainResourceType type,double maxPick) {
        TerrainResourceData rd=getOrCreateTerrainResource(type);
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
        TerrainResourceData rd=getOrCreateTerrainResource(type);
        rd.recoverResource(maxPick);
    }

    public double maypickTerrainResource(TerrainResourceType type, double d) {
        return Math.min(d, getRemainingTerrainResource(type));
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
        TerrainResourceData data = getOrCreateTerrainResource(type);
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
        TerrainResourceData rd = getOrCreateTerrainResource(type);
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
    public static void addClientListener(ITownDataUpdateListener listener) { com.teammoeg.frostedheart.content.town.client.TownClientUpdates.addClientListener(listener); }
    public static void removeClientListener(ITownDataUpdateListener listener) { com.teammoeg.frostedheart.content.town.client.TownClientUpdates.removeClientListener(listener); }
    public static boolean isInClientBatchFire() { return com.teammoeg.frostedheart.content.town.client.TownClientUpdates.isInClientBatchFire(); }
    public static long getClientSyncBatchId() { return com.teammoeg.frostedheart.content.town.client.TownClientUpdates.getClientSyncBatchId(); }
    public static void fireClientDataChanged() { com.teammoeg.frostedheart.content.town.client.TownClientUpdates.fireClientDataChanged(); }
    public static void fireBuildingsChanged() { com.teammoeg.frostedheart.content.town.client.TownClientUpdates.fireBuildingsChanged(); }
    public static void fireResidentsChanged() { com.teammoeg.frostedheart.content.town.client.TownClientUpdates.fireResidentsChanged(); }
    public static void fireResourcesChanged() { com.teammoeg.frostedheart.content.town.client.TownClientUpdates.fireResourcesChanged(); }
    public static void fireHistoryChanged() { com.teammoeg.frostedheart.content.town.client.TownClientUpdates.fireHistoryChanged(); }
    public static void fireStaffingChanged() { com.teammoeg.frostedheart.content.town.client.TownClientUpdates.fireStaffingChanged(); }
    public static void fireHousingChanged() { com.teammoeg.frostedheart.content.town.client.TownClientUpdates.fireHousingChanged(); }
    public static void firePoliciesChanged() { com.teammoeg.frostedheart.content.town.client.TownClientUpdates.firePoliciesChanged(); }

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
                buildingStore.install(building);
            }
        }
        for (BlockPos pos : removed) {
            buildingStore.removeAt(pos);
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
    public void applyResourceUpdate(
            Map<ITownResourceKey, Double> changes,
            double occupiedCapacity,
            TownTransportSnapshot transportSnapshot
    ) {
        for (Map.Entry<ITownResourceKey, Double> entry : changes.entrySet()) {
            resources.applySyncEntry(entry.getKey(), entry.getValue());
        }
        resources.setOccupiedCapacity(occupiedCapacity);
        applyTransportSnapshot(transportSnapshot);
        fireResourcesChanged();
    }

    public void applyTransportSnapshot(TownTransportSnapshot transportSnapshot) {
        TownTransportSnapshot snapshot = transportSnapshot == null
                ? TownTransportSnapshot.EMPTY : transportSnapshot;
        transportState.applySnapshot(snapshot);
        setP2PBindingState(snapshot.p2pBindingState());
        setP2PFilterSummaryState(snapshot.p2pFilterSummaryState());
    }

    public TownTransportSnapshot createTransportSnapshot() {
        double totalCapacity = resources.get(
                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0));
        if (!warehouseTopologyInitialized) {
            return TownTransportSnapshot.from(totalCapacity, transportState,
                    transportState.getEffectiveWarehouseCount(),
                    transportState.getWarehouseDistanceCostPerBlock(),
                    currentP2PDistanceCost(),
                    p2pBindingState, p2pFilterSummaryState);
        }
        double distanceCost = transportState.getWarehouseDistanceCostPerBlock();
        try {
            distanceCost = FHConfig.SERVER.TOWN.TRANSPORT_CONSUMERS
                    .warehouseDistanceCostPerBlock.get();
        } catch (IllegalArgumentException | IllegalStateException ignored) {
        }
        return TownTransportSnapshot.from(
                totalCapacity,
                transportState,
                appliedWarehouseTopology.entries().size(),
                distanceCost,
                currentP2PDistanceCost(),
                p2pBindingState,
                p2pFilterSummaryState);
    }

    private static double currentP2PDistanceCost() {
        try {
            return FHConfig.SERVER.TOWN.TRANSPORT_CONSUMERS.p2pDistanceCostPerBlock.get();
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return com.teammoeg.frostedheart.content.town.model.TownModelParameters.Defaults
                    .TRANSPORT_CONSUMER_P2P_DISTANCE_COST_PER_BLOCK;
        }
    }

    /** Source-compatible client helper for callers predating transport snapshots. */
    public void applyResourceUpdate(
            Map<ITownResourceKey, Double> changes,
            double occupiedCapacity,
            TownTransportState.DailyReport transportDailyReport
    ) {
        applyResourceUpdate(changes, occupiedCapacity,
                new TownTransportSnapshot(transportDailyReport,
                        resources.get(VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0)), List.of()));
    }

    /** Source-compatible client helper for callers predating transport reports. */
    public void applyResourceUpdate(Map<ITownResourceKey, Double> changes, double occupiedCapacity) {
        applyResourceUpdate(changes, occupiedCapacity,
                TownTransportSnapshot.from(
                        resources.get(VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0)),
                        transportState));
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
        Set<ITownResourceKey> changedResourceKey = new HashSet<>();
        Set<UUID> changedResidentUUID = new HashSet<>();
        Set<BlockPos> changedBuildingPos = new HashSet<>();
        Set<BlockPos> changedBuildingZonePos = new HashSet<>();
        boolean transportStateChanged;

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

        public boolean hasChangedZone() {
			return !changedBuildingZonePos.isEmpty();
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
            persistentChanged();
            if (!serverTracking) return;
            this.changedResourceKey.add(changedResourceKey);
        }

        public void addChanged(UUID changedResidentUUID){
            persistentChanged();
            if (!serverTracking) return;
            this.changedResidentUUID.add(changedResidentUUID);
        }

        public void addChanged(BlockPos changedBuildingPos){
            persistentChanged();
            if (!serverTracking) return;
            this.changedBuildingPos.add(changedBuildingPos);
        }

        public Set<BlockPos> drainChangedBuildingZones() {
            if (changedBuildingZonePos.isEmpty()) return Set.of();
            Set<BlockPos> out = new HashSet<>(changedBuildingZonePos);
            changedBuildingZonePos.clear();
            return out;
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

        public boolean hasChangedResources() {
            return !changedResourceKey.isEmpty();
        }

        public void markTransportStateChanged() {
            persistentChanged();
            if (!serverTracking) return;
            transportStateChanged = true;
        }

        public boolean hasTransportStateChange() {
            return transportStateChanged;
        }

        public boolean drainTransportStateChange() {
            boolean changed = transportStateChanged;
            transportStateChanged = false;
            return changed;
        }

        /**
         * 是否还有未同步的脏数据。
         */
        public boolean hasChanges() {
            return !changedBuildingPos.isEmpty() || !changedResidentUUID.isEmpty()
                    || !changedResourceKey.isEmpty() || transportStateChanged;
        }

        /**
         * 清空所有脏键记录（如加载存档后已完成一次全量同步时调用）。
         */
        public void clearChanged() {
            changedBuildingPos.clear();
            changedResidentUUID.clear();
            changedResourceKey.clear();
            transportStateChanged = false;
        }



        @Override
        public void onBuildingChange(TownBuildingChangeEvent event) {
            if (!serverTracking) {
                return;
            }
            if (event.includes(ChangeKind.REGISTRATION)) {
                prepareAssignments();
            }
            boolean warehouseAvailabilityChanged = event.getSource() instanceof WarehouseBuilding
                    && event.affectsAvailability();
            if (event.includes(ChangeKind.REGISTRATION) || warehouseAvailabilityChanged) {
                markWarehouseTopologyDirty();
            }
            if (event.requiresSpaceRecheck()) {
                changedBuildingZonePos.add(event.changedBuildingPos);
                needsRuntimeRepair = true;
            }
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

		@Override
		public void onBuildingZoneChange(TownBuildingChangeEvent event) {
			if (!serverTracking) return;
			changedBuildingZonePos.add(event.changedBuildingPos);
		}
    }
}
