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

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.content.town.transport.*;
import com.teammoeg.frostedheart.content.town.transport.device.P2PFilterSnapshot;
import com.teammoeg.frostedheart.content.town.transport.device.P2PFilterSummaryState;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * The town for a player team.
 * <p>
 * The TeamTown is only an interface of the underlying TeamTownData.
 * You may use this to access or modify town data.
 */
public class TeamTown implements ITown, ITownWithResidents, ITownWithBuildings {

    /** The town data, actual data stored on disk. */
    TeamTownData data;

    /**
     * Create a new town based on data.
     * @param data can be taken from a player, team, etc.
     *             can also be experimental data.
     * @return the town
     */
    public static TeamTown create(TeamTownData data) {
        return new TeamTown(data);
    }

    /**
     * Get the town for a player.
     * @param player the player
     * @return the town
     */
    public static TeamTown from(Player player) {
        TeamTownData data = CTeamDataManager.get(player).getData(FHSpecialDataTypes.TOWN_DATA);
        return new TeamTown(data);
    }


    /**
     * Default constructor links storage to the town data.
     * @param td the town data
     */
    public TeamTown(TeamTownData td) {
        super();
        this.data = td;
    }



    /**
     * Get the blocks and their worker data.
     */
    public Map<BlockPos, AbstractTownBuilding> getTownBuildings() {
        return data.buildings;
    }

    /**
     * Get the work data of the town block.
     *
     * @param pos position of the block
     * @return the work data
     */
    public Optional<AbstractTownBuilding> getTownBuilding(BlockPos pos) {
        return Optional.ofNullable(data.buildings.get(pos));
    }

    /**
     * Initializes new AbstractTownBuilding from the townBlockEntity entity.
     * Put the data into the map.
     *
     * @param pos position of the block
     * @param townBlockEntity the townBlockEntity entity associated with the block
     */
    public void addTownBlock(BlockPos pos, TownBlockEntity<?> townBlockEntity) {
        ITownBuilding building =townBlockEntity.createBuilding();
        if(building instanceof AbstractTownBuilding abstractTownBuilding){
            ServerLevel level = townBlockEntity instanceof BlockEntity blockEntity
                    && blockEntity.getLevel() instanceof ServerLevel serverLevel ? serverLevel : null;
            removeTownBlockInternal(level, pos);
            data.buildings.put(pos, abstractTownBuilding); // put 时由 ObservableTownMap.onAttach 自动接线
            data.markWarehouseTopologyDirty();
        }
    }

    /**
     * Remove the town block from the map.
     *
     * @param pos position of the block
     */
    public void removeTownBlock(ServerLevel sl,BlockPos pos) {
        removeTownBlockInternal(sl, pos);
    }

    /**
     * Idempotent common building-removal path used by active block teardown,
     * same-position replacement, and the morning reconciliation fallback.
     */
    private void removeTownBlockInternal(ServerLevel level, BlockPos pos) {
        AbstractTownBuilding building = data.buildings.get(pos);
        if (building == null) {
            return;
        }

        data.buildings.remove(pos);
        data.markWarehouseTopologyDirty();
        building.onRemoved(this);

        // Building rosters are normally authoritative, but older or partially
        // repaired saves may disagree with Resident fields. Clear exact
        // position references defensively without reallocating until morning.
        for (Resident resident : data.residents.values()) {
            if (pos.equals(resident.getHousePos())) {
                resident.setHousePos(null);
            }
            if (pos.equals(resident.getWorkPos())) {
                resident.setWorkPos(null);
            }
        }

        if (building instanceof MineBuilding) {
            for (AbstractTownBuilding remaining : data.buildings.values()) {
                if (remaining instanceof MineBaseBuilding mineBase) {
                    mineBase.removeLinkedMine(pos);
                }
            }
            data.recalcOreChunkResources();
        } else if (building instanceof MineBaseBuilding) {
            data.recalcOreChunkResources();
        }

        if (building instanceof WarehouseBuilding) {
            // MAX_CAPACITY is derived from surviving physical warehouses. The
            // stored resources and occupied-capacity counter are untouched.
            data.reloadMaxCapacity();
        }

        data.checkOccupiedAreaOverlap();
    }

    public Map<UUID, Resident> getResidents() {
        return data.residents;
    }

    public Collection<Resident> getAllResidents(){
        return data.residents.values();
    }

    public Optional<Resident> getResident(UUID id){
        return Optional.ofNullable(data.residents.get(id));
    }

    public boolean addResident(Resident resident) {
        // 无空房位短路：满员时直接拒绝，跳过 put+全量 allocateHouse+回滚（失败路径从
        // O(H log H)+每房评分降为 O(R+H)）。短路需双条件：canAddResident()（occupancy
        // 口径）与 hasFreeHouseSlot()（residentsID 口径，镜像 allocateHouse filter）
        // 同时判满才成立——正常态两口径一致（一致性由 residentAllocatingCheck 每日重建
        // 与 addResident/removeResident 双写维护），双条件等价于旧路径必然失败；任一
        // 不一致窗口不满足双条件 → 不短路，走旧路径与旧代码逐字节一致。不一致窗口：
        // 建筑同位置替换（setPlacedBy→addTownBlock 的 put 覆盖旧条目不调 onRemoved，
        // 旧居民 housePos 悬空而新实例 residentsID 为空，至次日结算恢复）、旧存档重载
        // 至次日结算前。竞争路径（两个 homeless 抢最后槽位）同样不拦截，仍走
        // put→allocateHouse→无房回滚。
        if (!canAddResident() && !hasFreeHouseSlot()) return false;
        data.residents.put(resident.getUUID(), resident);
        // 白天加入的居民只占用一个现有空位；全镇照护排序仍统一留到次日晨间，
        // 避免一次出生/招募立即打乱所有家庭与玩家刚看到的分配结果。
        data.allocateNewResident(resident);
        // 仅在 occupancy 与住宅 UUID 名册暂时不一致时保留旧的全量修复路径。
        // 正常有空床时不会进入这里；正常满员已被上方双条件短路。
        if (resident.getHousePos() == null) data.allocateHouse();
        if(resident.getHousePos() == null){
            removeResident(resident);
            return false;
        }
        // 门面 fire（唯一触发点）：put 先于房屋分配，map 钩子链（DataSyncCache 专用）
        // 首次触发时 housePos 尚为空，模拟不为其建条目；分配完成后这里经门面单次
        // 通知（锚点必已就绪、无双触发），模拟立即出生居民（事件驱动，替代原 1Hz
        // 对齐延迟）。
        data.fireResidentAdded(resident);
        return true;
    }

    public boolean addResident(String firstName, String lastName) {
        return addResident(new Resident(firstName, lastName));
    }

    /**
     * 调试/非玩家镇专用：直接加入一名居民并预置房屋锚点
     * （绕过 canAddResident/allocateHouse——无需任何建筑，housePos 即生成锚点）。
     * 直写 residents map 后经门面 fire 单次通知——housePos 已就绪，模拟立即按锚点
     * 出生条目（模拟 adopt 注册后）；未接管时由调度器接管的全量对账补建。
     * <p>
     * Debug / non-player-town helper: adds a resident directly with a preset
     * house anchor (bypasses canAddResident/allocateHouse — no buildings
     * needed; housePos is the spawn anchor). After the direct map write the
     * facade fires a single notification — the anchor is already set, so the
     * attached simulation spawns the entry at the anchor immediately (once
     * adopted); before adoption the scheduler's takeover reconciliation
     * rebuilds it.
     *
     * @param firstName 名 / first name
     * @param lastName 姓 / last name
     * @param anchor 生成锚点（房屋位置） / spawn anchor (house position)
     * @return 新居民 / the new resident
     */
    public Resident debugAddResident(String firstName, String lastName, BlockPos anchor) {
        Resident resident = new Resident(firstName, lastName);
        resident.setHousePos(anchor);
        data.residents.put(resident.getUUID(), resident);
        data.fireResidentAdded(resident);
        return resident;
    }

    /**
     * 客户端/服务端通用：判断城镇是否还能再容纳一名居民。
     * 逻辑镜像 {@link TeamTownData#allocateHouse()} 的空闲槽位判定：
     * 存在任一可工作的 {@code HouseBuilding} 仍有空余房屋槽位即可。
     * <p>
     * 容量按居民实际住房归属（{@code Resident.housePos}）计数而非
     * {@code HouseBuilding.getResidentsID()}：residentsUUID 已随 CODEC 序列化
     * （2026-08-02 起），与 housePos 的一致性由 residentAllocatingCheck 每日重建、
     * addResident/removeResident 双写维护，正常态两者等价；不一致窗口（建筑同位置
     * 替换、旧存档重载）至次日结算修复，见 {@link #addResident(Resident)} 短路注释；
     * 此口径与城镇 GUI 显示、{@link TeamTownData#allocateHouse()} 的真实分配结果保持一致。
     * <p>
     * Client/server shared: whether the town can still accommodate one more resident.
     * Counts actual occupancy by {@code Resident.housePos} instead of
     * {@code HouseBuilding.getResidentsID()}: the two stay consistent via the daily
     * residentAllocatingCheck rebuild and the addResident/removeResident double-writes,
     * so they are equivalent in the normal state (inconsistency windows — same-pos
     * building replacement, old-save reload — are repaired at the next daily settlement,
     * see the {@link #addResident(Resident)} short-circuit comment); this matches the
     * town GUI display and the real allocation of {@link TeamTownData#allocateHouse()}.
     *
     * @return 可容纳则返回 true / true if another resident can be accommodated
     */
    public boolean canAddResident() {
        // 单次遍历统计各房屋实际入住人数（仅计已分配房屋的居民；null housePos 与原
        // housePos.equals(...) 恒 false 的计数口径一致，不占槽位），再单次遍历建筑
        // 检查空余槽位：O(居民+建筑)，替代原 O(建筑×居民) 的嵌套 stream。
        Map<BlockPos, Integer> occupancy = new HashMap<>();
        for (Resident resident : data.residents.values()) {
            BlockPos housePos = resident.getHousePos();
            if (housePos != null) {
                occupancy.merge(housePos, 1, Integer::sum);
            }
        }
        for (AbstractTownBuilding building : data.buildings.values()) {
            if (building instanceof HouseBuilding house && house.isBuildingWorkable()) {
                if (occupancy.getOrDefault(house.getPos(), 0) < house.getMaxResidents()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否存在空余房屋槽位：按 residentsID 口径，镜像 {@link TeamTownData#allocateHouse()}
     * 的候选过滤（workable 且 {@code maxResidents > getResidentsID().size()}），O(H) 纯读。
     * 供 {@link #addResident(Resident)} 短路与 canAddResident() 双条件判满：
     * 任一不一致窗口下两口径结果不同 → 短路不触发，保证与旧路径严格等价。
     */
    private boolean hasFreeHouseSlot() {
        for (AbstractTownBuilding building : data.buildings.values()) {
            if (building instanceof HouseBuilding house
                    && house.isBuildingWorkable()
                    && house.getMaxResidents() > house.getResidentsID().size()) {
                return true;
            }
        }
        return false;
    }

    public boolean removeResident(UUID id) {
        if(!data.residents.containsKey(id)){
            return false;
        }
        Resident resident = data.residents.get(id);
        if(resident.getHousePos() != null){
            this.getTownBuilding(resident.getHousePos()).ifPresent(
                    building -> {
                        if(building instanceof HouseBuilding houseBuilding){
                            houseBuilding.removeResident(resident);
                        }
                    }
            );
        }
        if(resident.getWorkPos() != null){
            this.getTownBuilding(resident.getWorkPos()).ifPresent(
                    building -> {

                        if(building instanceof AbstractTownResidentWorkBuilding workBuilding){
                            workBuilding.removeResident(resident);
                        }
                    }
            );
        }
        data.residents.remove(id);
        // 门面 fire：集合移除完成后单次通知（模拟立即 despawn 条目；幂等——
        // 未见过的居民忽略，如 addResident 无房回滚时从未出生过条目）。
        data.fireResidentRemoved(resident);
        return true;
    }

    /**
     * Remove all resident matching the first and last name.
     * @param firstName the first name
     * @param lastName the last name
     * @return true if the resident was removed
     */
    public boolean removeResident(String firstName, String lastName) {
        // 先收集再删除：避免遍历 entrySet 时直接 remove 导致 ConcurrentModificationException；
        // 并且走 removeResident(UUID) 完整流程，同步清理住房/工作建筑中的引用。
        List<UUID> toRemove = new ArrayList<>();
        for (Entry<UUID, Resident> entry : getResidents().entrySet()) {
            if (entry.getValue().getFirstName().equals(firstName) && entry.getValue().getLastName().equals(lastName)) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(this::removeResident);
        return !toRemove.isEmpty();
    }

    /**
     * Get the town name.
     */
    public String getName() {
        return data.name;
    }

    /**
     * Set the town name.
     * @param name the new name
     */
    public void setName(String name) {
        this.data.name = name;
    }

    @Override
    public TeamTownResourceActionExecutorHandler getActionExecutorHandler() {
        return data.resources.actionExecutor;
    }

    public TeamTownResourceHolder getResourceHolder() {
        return data.resources;
    }

    public TownTransportState getTransportState() {
        return data.getTransportState();
    }

    public TownTransportSummary getTransportSummary() {
        prepareTransportState();
        return TownTransportSummary.from(getCurrentTransportCapacity(), data.getTransportState());
    }

    public Map<TransportEndpointId, TransportReservation> getTransportReservations() {
        prepareTransportState();
        return data.getTransportState().getReservations();
    }

    public P2PBindingState getP2PBindingState() {
        prepareTransportState();
        return data.getP2PBindingState();
    }

    public P2PFilterSummaryState getP2PFilterSummaryState() {
        return data.getP2PFilterSummaryState();
    }

    public void updateP2PFilterSummary(
            GlobalPos endpoint,
            P2PFilterSnapshot sendFilter,
            P2PFilterSnapshot receiveFilter
    ) {
        if (endpoint == null || sendFilter == null || receiveFilter == null) {
            return;
        }
        P2PFilterSummaryState previous = data.getP2PFilterSummaryState();
        P2PFilterSummaryState updated;
        try {
            updated = previous.with(endpoint, sendFilter, receiveFilter);
        } catch (IllegalArgumentException exception) {
            return;
        }
        if (updated != previous) {
            data.setP2PFilterSummaryState(updated);
            data.getDataSyncCache().markTransportStateChanged();
        }
    }

    public void removeP2PFilterSummary(GlobalPos endpoint) {
        P2PFilterSummaryState previous = data.getP2PFilterSummaryState();
        P2PFilterSummaryState updated = previous.without(endpoint);
        if (updated != previous) {
            data.setP2PFilterSummaryState(updated);
            data.getDataSyncCache().markTransportStateChanged();
        }
    }

    public Optional<TransportReservation> getTransportReservation(TransportEndpointId endpointId) {
        prepareTransportState();
        return Optional.ofNullable(data.getTransportState().getReservation(endpointId));
    }

    public TownTransportSnapshot getTransportSnapshot() {
        prepareTransportState();
        return data.createTransportSnapshot();
    }

    public WarehouseTopologySnapshot prepareWarehouseTopology(
            ResourceKey<Level> authoritativeTownDimension
    ) {
        return currentTransportParameters()
                .map(parameters -> data.refreshWarehouseTopologyIfDirty(
                        parameters, authoritativeTownDimension))
                .orElse(data.getAppliedWarehouseTopology());
    }

    public WarehouseTopologySnapshot getWarehouseTopology() {
        return data.getAppliedWarehouseTopology();
    }

    public void registerWarehouseTopologyListener(
            GlobalPos devicePos,
            WarehouseTopologyListener listener
    ) {
        data.registerWarehouseTopologyListener(devicePos, listener);
    }

    public void unregisterWarehouseTopologyListener(
            GlobalPos devicePos,
            WarehouseTopologyListener listener
    ) {
        data.unregisterWarehouseTopologyListener(devicePos, listener);
    }

    public TransportReservationResult registerOrUpdateTransportEndpoint(TransportEndpointRequest request) {
        return registerOrUpdateWarehouseInterface(request);
    }

    public TransportReservationResult registerOrUpdateWarehouseInterface(TransportEndpointRequest request) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (request == null || parametersResult.isEmpty()) {
            return result(TransportReservationDecision.INVALID_REQUEST, null, 0.0);
        }
        if (request.endpointKind() != TransportEndpointKind.WAREHOUSE_INTERFACE) {
            return result(TransportReservationDecision.INVALID_BINDING,
                    data.getTransportState().getReservation(request.endpointId()), 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        data.refreshWarehouseTopologyIfDirty(
                parameters, data.getAppliedWarehouseTopology().townDimension());
        TransportReservation old = data.getTransportState().getReservation(request.endpointId());

        if (old == null && data.getTransportState().getReservations().size()
                >= TownTransportSnapshot.MAX_RESERVATIONS) {
            return result(TransportReservationDecision.INVALID_REQUEST, null, 0.0);
        }

        if (old != null && old.endpointKind() != request.endpointKind()) {
            return result(TransportReservationDecision.INVALID_BINDING, old, 0.0);
        }
        if (!parameters.isRateValid(request.rateItemsPerSecond())) {
            return result(TransportReservationDecision.INVALID_REQUEST, old, 0.0);
        }

        OptionalDouble metricResult = currentWarehouseDistance(request.endpointId());
        if (metricResult.isEmpty()) {
            if (old != null && request.rateItemsPerSecond() != 0) {
                return result(TransportReservationDecision.INVALID_BINDING, old, 0.0);
            }
            TransportReservation unavailable = new TransportReservation(
                    request.endpointKind(), 0, 0.0, 0.0,
                    TransportAdmissionStatus.UNAVAILABLE);
            replaceAndMark(request.endpointId(), unavailable);
            return result(TransportReservationDecision.ACCEPTED, unavailable, 0.0);
        }
        double scaleMetric = metricResult.getAsDouble();
        ResolvedTransportAdmission admission = admitResolvedEndpoint(
                request.endpointId(), request.endpointKind(), request.rateItemsPerSecond(),
                scaleMetric, parameters);
        if (admission.decision() == TransportReservationDecision.ACCEPTED) {
            TransportReservation accepted = admission.acceptedReservation().orElseThrow();
            replaceAndMark(request.endpointId(), accepted);
            return result(admission.decision(), accepted,
                    admission.requiredAdditionalCapacity());
        }
        if (admission.decision() == TransportReservationDecision.INSUFFICIENT_CAPACITY
                && old == null) {
            TransportReservation disabled = new TransportReservation(
                    TransportEndpointKind.WAREHOUSE_INTERFACE, 0, scaleMetric, 0.0,
                    TransportAdmissionStatus.DISABLED);
            replaceAndMark(request.endpointId(), disabled);
            return result(admission.decision(), disabled,
                    admission.requiredAdditionalCapacity());
        }
        return result(admission.decision(), old,
                admission.requiredAdditionalCapacity());
    }

    ResolvedTransportAdmission evaluateP2PTransportAdmission(
            GlobalPos sender,
            GlobalPos receiver,
            int rateItemsPerSecond
    ) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (sender == null || receiver == null || parametersResult.isEmpty()) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_REQUEST, null);
        }
        double scaleMetric = TransportReservationModel.p2pManhattanDistance(sender, receiver);
        if (!TransportReservationModel.isFiniteNonNegative(scaleMetric)) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_BINDING,
                    data.getTransportState().getReservation(new TransportEndpointId(sender)));
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        return admitResolvedEndpoint(new TransportEndpointId(sender),
                TransportEndpointKind.P2P_DIRECT_LINK, rateItemsPerSecond,
                scaleMetric, parameters);
    }

    private ResolvedTransportAdmission admitResolvedEndpoint(
            TransportEndpointId endpointId,
            TransportEndpointKind endpointKind,
            int rateItemsPerSecond,
            double scaleMetric,
            TransportConsumerParameters parameters
    ) {
        TransportReservation old = endpointId == null
                ? null
                : data.getTransportState().getReservation(endpointId);
        if (endpointId == null || endpointKind == null || parameters == null
                || !parameters.isRateValid(rateItemsPerSecond)
                || !TransportReservationModel.isFiniteNonNegative(scaleMetric)) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_REQUEST, old);
        }
        if (old != null && old.endpointKind() != endpointKind) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_BINDING, old);
        }
        if (rateItemsPerSecond == 0) {
            return ResolvedTransportAdmission.accepted(old, new TransportReservation(
                    endpointKind, 0, scaleMetric, 0.0,
                    TransportAdmissionStatus.DISABLED), 0.0);
        }

        double candidateReserved = TransportReservationModel.requiredCapacity(
                endpointKind, rateItemsPerSecond, scaleMetric, parameters);
        if (!TransportReservationModel.isFiniteNonNegative(candidateReserved)) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_REQUEST, old);
        }
        double oldReserved = old == null ? 0.0 : old.reservedTransportCapacity();
        TransportReservationModel.AdmissionEvaluation evaluation =
                TransportReservationModel.evaluateAdmission(
                        getCurrentTransportCapacity(),
                        data.getTransportState().getReservedTransportCapacity(),
                        oldReserved, candidateReserved);
        if (!evaluation.valid()) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_REQUEST, old);
        }
        if (!evaluation.accepted()) {
            return ResolvedTransportAdmission.rejected(old,
                    evaluation.requiredAdditionalCapacity());
        }
        return ResolvedTransportAdmission.accepted(old, new TransportReservation(
                endpointKind, rateItemsPerSecond, scaleMetric, candidateReserved,
                TransportAdmissionStatus.ACTIVE), evaluation.requiredAdditionalCapacity());
    }

    record ResolvedTransportAdmission(
            TransportReservationDecision decision,
            Optional<TransportReservation> previousReservation,
            Optional<TransportReservation> acceptedReservation,
            double requiredAdditionalCapacity
    ) {
        ResolvedTransportAdmission {
            Objects.requireNonNull(decision, "decision");
            previousReservation = previousReservation == null
                    ? Optional.empty() : previousReservation;
            acceptedReservation = acceptedReservation == null
                    ? Optional.empty() : acceptedReservation;
            requiredAdditionalCapacity = TransportReservationModel.isFiniteNonNegative(
                    requiredAdditionalCapacity) ? requiredAdditionalCapacity : 0.0;
        }

        private static ResolvedTransportAdmission invalid(
                TransportReservationDecision decision,
                TransportReservation previous
        ) {
            return new ResolvedTransportAdmission(decision, Optional.ofNullable(previous),
                    Optional.empty(), 0.0);
        }

        private static ResolvedTransportAdmission rejected(
                TransportReservation previous,
                double requiredAdditionalCapacity
        ) {
            return new ResolvedTransportAdmission(
                    TransportReservationDecision.INSUFFICIENT_CAPACITY,
                    Optional.ofNullable(previous), Optional.empty(),
                    requiredAdditionalCapacity);
        }

        private static ResolvedTransportAdmission accepted(
                TransportReservation previous,
                TransportReservation accepted,
                double requiredAdditionalCapacity
        ) {
            return new ResolvedTransportAdmission(TransportReservationDecision.ACCEPTED,
                    Optional.ofNullable(previous), Optional.of(accepted),
                    requiredAdditionalCapacity);
        }
    }

    public P2PBindingResult bindOrRebindP2PTerminals(
            P2PTerminalEndpoint first,
            P2PTerminalEndpoint second
    ) {
        return bindOrRebindP2PTerminals(first, second, false, false);
    }

    public P2PBindingResult bindOrRebindP2PTerminals(
            P2PTerminalEndpoint first,
            P2PTerminalEndpoint second,
            boolean firstRedstonePowered,
            boolean secondRedstonePowered
    ) {
        if (first == null || second == null) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                    null, Set.of(), 0.0);
        }
        if (first.pos().equals(second.pos())) {
            return p2pResult(P2PBindingDecision.SELF_LINK,
                    null, Set.of(), 0.0);
        }
        if (!first.pos().dimension().equals(second.pos().dimension())) {
            return p2pResult(P2PBindingDecision.CROSS_DIMENSION,
                    null, Set.of(), 0.0);
        }
        boolean hasDirection = first.role().canSend() && second.role().canReceive()
                || second.role().canSend() && first.role().canReceive();
        if (!hasDirection) {
            return p2pResult(P2PBindingDecision.INCOMPATIBLE_ENDPOINTS,
                    null, Set.of(), 0.0);
        }
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (parametersResult.isEmpty()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                    null, Set.of(), 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);

        P2PBindingState currentBindings = data.getP2PBindingState();
        UUID connectionId = UUID.randomUUID();
        P2PBindingState.BindingPlan plan;
        P2PBindingState candidateBindings;
        try {
            plan = currentBindings.planConnection(first, second,
                    parameters.defaultRateItemsPerSecond(), connectionId);
            candidateBindings = currentBindings.apply(plan);
            candidateBindings = candidateBindings.withEndpointRedstonePowered(
                    first.pos(), firstRedstonePowered);
            candidateBindings = candidateBindings.withEndpointRedstonePowered(
                    second.pos(), secondRedstonePowered);
        } catch (IllegalArgumentException exception) {
            return p2pResult(P2PBindingDecision.INVALID_ENDPOINT,
                    null, Set.of(), 0.0);
        }

        Map<TransportEndpointId, TransportReservation> candidateReservations =
                new TreeMap<>(TransportEndpointId.STABLE_COMPARATOR);
        candidateReservations.putAll(data.getTransportState().getReservations());
        double removedCapacity = 0.0;
        for (UUID removedConnectionId : plan.removedConnectionIds()) {
            for (P2PDirectedBinding oldBinding : currentBindings.connection(removedConnectionId)
                    .orElse(List.of())) {
                TransportReservation removed = candidateReservations.remove(
                        oldBinding.sender().transportEndpointId());
                if (removed != null) {
                    removedCapacity += removed.reservedTransportCapacity();
                }
            }
        }

        double addedCapacity = 0.0;
        for (P2PDirectedBinding binding : candidateBindings.connection(connectionId)
                .orElse(List.of())) {
            if (!parameters.isRateValid(binding.rateItemsPerSecond())) {
                return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                        null, Set.of(), 0.0);
            }
            TransportEndpointId senderId = binding.sender().transportEndpointId();
            TransportReservation collision = candidateReservations.get(senderId);
            if (collision != null
                    && collision.endpointKind() != TransportEndpointKind.P2P_DIRECT_LINK) {
                return p2pResult(P2PBindingDecision.INVALID_ENDPOINT,
                        null, Set.of(), 0.0);
            }
            TransportReservation reservation = reservationForBinding(binding, parameters);
            if (reservation == null) {
                return p2pResult(P2PBindingDecision.INVALID_ENDPOINT,
                        null, Set.of(), 0.0);
            }
            candidateReservations.put(senderId, reservation);
            addedCapacity += reservation.reservedTransportCapacity();
        }
        if (candidateReservations.size() > TownTransportSnapshot.MAX_RESERVATIONS) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                    null, Set.of(), 0.0);
        }

        TransportReservationModel.AdmissionEvaluation admission =
                TransportReservationModel.evaluateAdmission(
                        getCurrentTransportCapacity(),
                        data.getTransportState().getReservedTransportCapacity(),
                        removedCapacity, addedCapacity);
        if (!admission.valid()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                    null, Set.of(), 0.0);
        }
        if (!admission.accepted()) {
            return p2pResult(P2PBindingDecision.INSUFFICIENT_CAPACITY,
                    null, Set.of(), admission.requiredAdditionalCapacity());
        }

        data.setP2PBindingState(candidateBindings);
        boolean reservationsChanged = data.getTransportState().replaceAllReservations(
                candidateReservations, parameters);
        data.markP2PTransportReconciled(parameters);
        if (reservationsChanged || !candidateBindings.bindings().equals(currentBindings.bindings())) {
            data.getDataSyncCache().markTransportStateChanged();
        }
        return p2pResult(P2PBindingDecision.ACCEPTED, connectionId,
                plan.removedConnectionIds(), admission.requiredAdditionalCapacity());
    }

    public P2PBindingResult unbindP2PConnection(UUID connectionId) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (connectionId == null || parametersResult.isEmpty()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                    null, Set.of(), 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        P2PBindingState currentBindings = data.getP2PBindingState();
        List<P2PDirectedBinding> removed = currentBindings.connection(connectionId)
                .orElse(null);
        if (removed == null) {
            return p2pResult(P2PBindingDecision.STALE_CONNECTION,
                    null, Set.of(), 0.0);
        }

        Map<TransportEndpointId, TransportReservation> candidateReservations =
                new TreeMap<>(TransportEndpointId.STABLE_COMPARATOR);
        candidateReservations.putAll(data.getTransportState().getReservations());
        for (P2PDirectedBinding binding : removed) {
            TransportEndpointId senderId = binding.sender().transportEndpointId();
            TransportReservation reservation = candidateReservations.get(senderId);
            if (reservation != null
                    && reservation.endpointKind() == TransportEndpointKind.P2P_DIRECT_LINK) {
                candidateReservations.remove(senderId);
            }
        }
        P2PBindingState candidateBindings = currentBindings.withoutConnection(connectionId);
        data.setP2PBindingState(candidateBindings);
        data.getTransportState().replaceAllReservations(candidateReservations, parameters);
        data.markP2PTransportReconciled(parameters);
        data.getDataSyncCache().markTransportStateChanged();
        return p2pResult(P2PBindingDecision.ACCEPTED,
                connectionId, Set.of(connectionId), 0.0);
    }

    public P2PBindingResult setP2PTransportRate(GlobalPos sender, int rateItemsPerSecond) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (sender == null || parametersResult.isEmpty()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        P2PBindingState currentBindings = data.getP2PBindingState();
        P2PDirectedBinding current = currentBindings.outgoing(sender).orElse(null);
        if (current == null || !parameters.isRateValid(rateItemsPerSecond)) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        P2PBindingState candidateBindings;
        try {
            candidateBindings = currentBindings.withRate(sender, rateItemsPerSecond);
        } catch (IllegalArgumentException exception) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        P2PDirectedBinding candidate = candidateBindings.outgoing(sender).orElseThrow();
        TransportReservation reservation = reservationForBinding(candidate, parameters);
        if (reservation == null) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        TransportReservation previous = data.getTransportState().getReservation(
                candidate.sender().transportEndpointId());
        double previousCapacity = previous == null ? 0.0
                : previous.reservedTransportCapacity();
        TransportReservationModel.AdmissionEvaluation admission =
                TransportReservationModel.evaluateAdmission(
                        getCurrentTransportCapacity(),
                        data.getTransportState().getReservedTransportCapacity(),
                        previousCapacity, reservation.reservedTransportCapacity());
        if (!admission.valid()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        if (!admission.accepted()) {
            return p2pResult(P2PBindingDecision.INSUFFICIENT_CAPACITY,
                    null, Set.of(), admission.requiredAdditionalCapacity());
        }
        data.setP2PBindingState(candidateBindings);
        replaceAndMark(candidate.sender().transportEndpointId(), reservation);
        data.markP2PTransportReconciled(parameters);
        return p2pResult(P2PBindingDecision.ACCEPTED,
                current.connectionId(), Set.of(), admission.requiredAdditionalCapacity());
    }

    /** Compatibility facade for sender-side redstone state updates. */
    public P2PBindingResult setP2PRedstonePaused(GlobalPos sender, boolean paused) {
        return setP2PEndpointRedstonePowered(sender, paused);
    }

    /** Redstone resumption restores accepted reservations even if it creates town shortage. */
    public P2PBindingResult setP2PEndpointRedstonePowered(
            GlobalPos endpoint,
            boolean powered
    ) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (endpoint == null || parametersResult.isEmpty()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        P2PBindingState currentBindings = data.getP2PBindingState();
        Set<UUID> connectionIds = currentBindings.connectionIdsAt(endpoint);
        if (connectionIds.isEmpty()) {
            return p2pResult(P2PBindingDecision.STALE_CONNECTION, null, Set.of(), 0.0);
        }
        P2PBindingState candidateBindings = currentBindings.withEndpointRedstonePowered(
                endpoint, powered);
        if (candidateBindings != currentBindings) {
            Map<TransportEndpointId, TransportReservation> candidateReservations =
                    new TreeMap<>(TransportEndpointId.STABLE_COMPARATOR);
            candidateReservations.putAll(data.getTransportState().getReservations());
            for (P2PDirectedBinding binding : candidateBindings.bindings()) {
                if (!binding.sender().pos().equals(endpoint)
                        && !binding.receiver().pos().equals(endpoint)) {
                    continue;
                }
                TransportReservation reservation = reservationForBinding(binding, parameters);
                if (reservation == null) {
                    return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
                }
                candidateReservations.put(binding.sender().transportEndpointId(), reservation);
            }
            data.setP2PBindingState(candidateBindings);
            data.getTransportState().replaceAllReservations(candidateReservations, parameters);
            data.markP2PTransportReconciled(parameters);
            data.getDataSyncCache().markTransportStateChanged();
        }
        return p2pResult(P2PBindingDecision.ACCEPTED,
                connectionIds.iterator().next(), Set.of(), 0.0);
    }

    private TransportReservation reservationForBinding(
            P2PDirectedBinding binding,
            TransportConsumerParameters parameters
    ) {
        double metric = TransportReservationModel.p2pManhattanDistance(
                binding.sender().pos(), binding.receiver().pos());
        if (!TransportReservationModel.isFiniteNonNegative(metric)) {
            return null;
        }
        if (binding.rateItemsPerSecond() == 0) {
            return new TransportReservation(TransportEndpointKind.P2P_DIRECT_LINK,
                    0, metric, 0.0, TransportAdmissionStatus.DISABLED);
        }
        if (binding.redstonePaused()) {
            return new TransportReservation(TransportEndpointKind.P2P_DIRECT_LINK,
                    binding.rateItemsPerSecond(), metric, 0.0,
                    TransportAdmissionStatus.REDSTONE_PAUSED);
        }
        double capacity = TransportReservationModel.requiredCapacity(
                TransportEndpointKind.P2P_DIRECT_LINK,
                binding.rateItemsPerSecond(), metric, parameters);
        if (!TransportReservationModel.isFiniteNonNegative(capacity)) {
            return null;
        }
        return new TransportReservation(TransportEndpointKind.P2P_DIRECT_LINK,
                binding.rateItemsPerSecond(), metric, capacity,
                TransportAdmissionStatus.ACTIVE);
    }

    private P2PBindingResult p2pResult(
            P2PBindingDecision decision,
            UUID connectionId,
            Set<UUID> replacedConnectionIds,
            double requiredAdditionalCapacity
    ) {
        return new P2PBindingResult(decision, Optional.ofNullable(connectionId),
                replacedConnectionIds, requiredAdditionalCapacity,
                TownTransportSummary.from(getCurrentTransportCapacity(),
                        data.getTransportState()));
    }

    public TransportReservationResult refreshTransportEndpointMetric(TransportEndpointId endpointId) {
        return refreshWarehouseInterfaceMetric(endpointId);
    }

    public TransportReservationResult refreshWarehouseInterfaceMetric(TransportEndpointId endpointId) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (endpointId == null || parametersResult.isEmpty()) {
            return result(TransportReservationDecision.INVALID_REQUEST, null, 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        TransportReservation old = data.getTransportState().getReservation(endpointId);
        if (old == null) {
            return result(TransportReservationDecision.INVALID_BINDING, old, 0.0);
        }
        if (old.endpointKind() != TransportEndpointKind.WAREHOUSE_INTERFACE) {
            return result(TransportReservationDecision.INVALID_BINDING, old, 0.0);
        }
        data.refreshWarehouseTopologyIfDirty(
                parameters, data.getAppliedWarehouseTopology().townDimension());
        OptionalDouble metricResult = currentWarehouseDistance(endpointId);
        if (metricResult.isEmpty()) {
            TransportReservation unavailable = new TransportReservation(
                    old.endpointKind(), old.rateItemsPerSecond(), 0.0, 0.0,
                    TransportAdmissionStatus.UNAVAILABLE);
            replaceAndMark(endpointId, unavailable);
            return result(TransportReservationDecision.ACCEPTED, unavailable, 0.0);
        }
        double scaleMetric = metricResult.getAsDouble();
        double refreshedCapacity = TransportReservationModel.capacityForStoredRate(
                old.endpointKind(), old.rateItemsPerSecond(), scaleMetric, parameters);
        if (!TransportReservationModel.isFiniteNonNegative(refreshedCapacity)) {
            return result(TransportReservationDecision.INVALID_REQUEST, old, 0.0);
        }
        TransportReservation refreshed = new TransportReservation(
                old.endpointKind(), old.rateItemsPerSecond(), scaleMetric, refreshedCapacity,
                old.rateItemsPerSecond() == 0
                        ? TransportAdmissionStatus.DISABLED
                        : TransportAdmissionStatus.ACTIVE);
        replaceAndMark(endpointId, refreshed);
        return result(TransportReservationDecision.ACCEPTED, refreshed, 0.0);
    }

    public TransportReservationResult unregisterTransportEndpoint(TransportEndpointId endpointId) {
        prepareTransportState();
        if (endpointId == null) {
            return result(TransportReservationDecision.INVALID_REQUEST, null, 0.0);
        }
        if (data.getTransportState().removeReservation(endpointId)) {
            data.getDataSyncCache().markTransportStateChanged();
        }
        return result(TransportReservationDecision.ACCEPTED, null, 0.0);
    }

    private OptionalDouble currentWarehouseDistance(TransportEndpointId endpointId) {
        if (endpointId == null) {
            return OptionalDouble.empty();
        }
        WarehouseTopologySnapshot topology = data.getAppliedWarehouseTopology();
        if (!topology.isUsable()
                || !endpointId.endpointPos().dimension().equals(topology.townDimension())) {
            return OptionalDouble.empty();
        }
        double metric = TransportReservationModel.warehouseWeightedDistance(
                endpointId.endpointPos().pos(), topology.entries());
        return TransportReservationModel.isFiniteNonNegative(metric)
                ? OptionalDouble.of(metric)
                : OptionalDouble.empty();
    }

    private void replaceAndMark(TransportEndpointId endpointId, TransportReservation reservation) {
        if (data.getTransportState().replaceReservation(endpointId, reservation)) {
            data.getDataSyncCache().markTransportStateChanged();
        }
    }

    private TransportReservationResult result(
            TransportReservationDecision decision,
            TransportReservation reservation,
            double requiredAdditionalCapacity
    ) {
        return new TransportReservationResult(decision, Optional.ofNullable(reservation),
                TownTransportSummary.from(getCurrentTransportCapacity(), data.getTransportState()),
                requiredAdditionalCapacity);
    }

    private double getCurrentTransportCapacity() {
        return data.resources.get(VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0));
    }

    private void prepareTransportState() {
        currentTransportParameters().ifPresent(this::prepareTransportState);
    }

    private void prepareTransportState(TransportConsumerParameters parameters) {
        reconcileP2PTransportState(parameters);
        if (data.getTransportState().recalculateReservedCapacities(parameters)) {
            data.getDataSyncCache().markTransportStateChanged();
        }
    }

    private void reconcileP2PTransportState(TransportConsumerParameters parameters) {
        if (data.isP2PTransportReconciled(parameters)) {
            return;
        }
        P2PBindingState bindings = data.getP2PBindingState();
        Map<TransportEndpointId, TransportReservation> reconciled =
                new TreeMap<>(TransportEndpointId.STABLE_COMPARATOR);
        data.getTransportState().getReservations().forEach((endpointId, reservation) -> {
            if (reservation.endpointKind() != TransportEndpointKind.P2P_DIRECT_LINK) {
                reconciled.put(endpointId, reservation);
            }
        });

        Set<UUID> invalidConnections = new TreeSet<>();
        for (P2PDirectedBinding binding : bindings.bindings()) {
            TransportEndpointId senderId = binding.sender().transportEndpointId();
            if (reconciled.containsKey(senderId)
                    || reconciled.size() >= TownTransportSnapshot.MAX_RESERVATIONS) {
                invalidConnections.add(binding.connectionId());
                continue;
            }
            TransportReservation reservation = reservationForBinding(binding, parameters);
            if (reservation == null) {
                invalidConnections.add(binding.connectionId());
                continue;
            }
            reconciled.put(senderId, reservation);
        }
        for (UUID connectionId : invalidConnections) {
            bindings = bindings.withoutConnection(connectionId);
        }
        if (!invalidConnections.isEmpty()) {
            data.setP2PBindingState(bindings);
            P2PBindingState validBindings = bindings;
            reconciled.entrySet().removeIf(entry ->
                    entry.getValue().endpointKind() == TransportEndpointKind.P2P_DIRECT_LINK
                            && validBindings.outgoing(entry.getKey().endpointPos()).isEmpty());
        }
        boolean changed = data.getTransportState().replaceAllReservations(reconciled, parameters);
        data.markP2PTransportReconciled(parameters);
        if (changed || !invalidConnections.isEmpty()) {
            data.getDataSyncCache().markTransportStateChanged();
        }
    }

    private static Optional<TransportConsumerParameters> currentTransportParameters() {
        FHConfig.Server.Town.TransportConsumers config = FHConfig.SERVER.TOWN.TRANSPORT_CONSUMERS;
        try {
            return Optional.of(new TransportConsumerParameters(
                    config.defaultRateItemsPerSecond.get(),
                    config.minimumRateItemsPerSecond.get(),
                    config.maximumRateItemsPerSecond.get(),
                    config.warehouseDistanceCostPerBlock.get(),
                    config.p2pDistanceCostPerBlock.get()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /**
     * Get the settlement snapshot history of the town, newest entry last.
     * Used by information GUIs such as the Mayor's Seal.
     *
     * @return unmodifiable view is not guaranteed; treat as read-only
     */
    public List<TownHistoryEntry> getHistory() {
        return data.getHistory();
    }

    /** Player-visible work-building order and guaranteed staffing targets. */
    public TownStaffingPlan getStaffingPlan() {
        return data.getStaffingPlan();
    }

    public TownHousingPlan getHousingPlan() {
        return data.getHousingPlan();
    }

    public TownPolicyState getPolicyState() {
        return data.getPolicyState();
    }

    //@Override
    public Optional<TeamTownData> getTownData() {
        return Optional.of(data);
    }
	public double maypickTerrainResource(TerrainResourceType type, double d) {
		return data.maypickTerrainResource(type, d);
	}

    //全局地形资源
	public double pickTerrainResource(TerrainResourceType type,double maxPick) {
		return data.pickTerrainResource(type, maxPick);
	}

    //区块型资源
    public double pickTerrainResource(TerrainResourceType type, ChunkPos chunkPos,double maxPick) {
        return data.pickTerrainResource(type,chunkPos,maxPick);
    }

    public double getRemainingTerrainResource(TerrainResourceType type) {
        return data.getRemainingTerrainResource(type);
    }

    public double getRemainingTerrainResource(TerrainResourceType type, ChunkPos chunkPos) {
        return data.getRemainingTerrainResource(type, chunkPos);
    }

    public double getExtractedTerrainResource(TerrainResourceType type, ChunkPos chunkPos) {
        return data.getExtractedTerrainResource(type, chunkPos);
    }

	public void unpickTerrainResource(TerrainResourceType type,double maxPick) {
		data.unpickTerrainResource(type, maxPick);
	}
}
