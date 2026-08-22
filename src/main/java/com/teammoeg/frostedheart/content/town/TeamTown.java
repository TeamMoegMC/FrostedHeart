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
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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

        if (building instanceof WarehouseBuilding warehouse) {
            if (level != null) {
                unregisterTransportEndpointsBoundTo(GlobalPos.of(level.dimension(), pos));
            } else {
                unregisterTransportEndpointsBoundTo(pos);
            }
        }

        if (level != null && building instanceof WarehouseBuilding warehouse) {
            // Release watcher-backed devices while the exact warehouse instance
            // is still resolvable through its provider. Unloaded devices retain
            // their saved binding but remain inert once the map entry is gone.
            warehouse.unbindLoadedDevices(level);
        }

        data.buildings.remove(pos);
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

    public Optional<TransportReservation> getTransportReservation(TransportEndpointId endpointId) {
        prepareTransportState();
        return Optional.ofNullable(data.getTransportState().getReservation(endpointId));
    }

    public TownTransportSnapshot getTransportSnapshot() {
        prepareTransportState();
        return TownTransportSnapshot.from(getCurrentTransportCapacity(), data.getTransportState());
    }

    public TransportReservationResult registerOrUpdateTransportEndpoint(TransportEndpointRequest request) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (request == null || parametersResult.isEmpty()) {
            return result(TransportReservationDecision.INVALID_REQUEST, null, 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        TransportReservation old = data.getTransportState().getReservation(request.endpointId());

        if (!request.endpointId().endpointPos().dimension().equals(request.boundWarehouseCorePos().dimension())
                || old != null && (old.endpointKind() != request.endpointKind()
                || !old.boundWarehouseCorePos().equals(request.boundWarehouseCorePos()))) {
            return result(TransportReservationDecision.INVALID_BINDING, old, 0.0);
        }
        if (!parameters.isRateValid(request.rateItemsPerSecond())
                || !TransportReservationModel.isFiniteNonNegative(request.scaleMetric())) {
            return result(TransportReservationDecision.INVALID_REQUEST, old, 0.0);
        }

        int rate = request.rateItemsPerSecond();
        if (rate == 0) {
            TransportReservation disabled = new TransportReservation(
                    request.endpointKind(), request.boundWarehouseCorePos(),
                    0, request.scaleMetric(), 0.0, TransportAdmissionStatus.DISABLED);
            replaceAndMark(request.endpointId(), disabled);
            return result(TransportReservationDecision.ACCEPTED, disabled, 0.0);
        }

        double candidateReserved = TransportReservationModel.requiredCapacity(
                request.endpointKind(), rate, request.scaleMetric(), parameters);
        if (!TransportReservationModel.isFiniteNonNegative(candidateReserved)) {
            return result(TransportReservationDecision.INVALID_REQUEST, old, 0.0);
        }
        double oldReserved = old == null ? 0.0 : old.reservedTransportCapacity();
        TransportReservationModel.AdmissionEvaluation admission = TransportReservationModel.evaluateAdmission(
                getCurrentTransportCapacity(), data.getTransportState().getReservedTransportCapacity(),
                oldReserved, candidateReserved);
        if (!admission.valid()) {
            return result(TransportReservationDecision.INVALID_REQUEST, old, 0.0);
        }

        if (admission.accepted()) {
            TransportReservation accepted = new TransportReservation(
                    request.endpointKind(), request.boundWarehouseCorePos(),
                    rate, request.scaleMetric(), candidateReserved,
                    TransportAdmissionStatus.ACTIVE);
            replaceAndMark(request.endpointId(), accepted);
            return result(TransportReservationDecision.ACCEPTED, accepted,
                    admission.requiredAdditionalCapacity());
        }

        if (old == null) {
            TransportReservation disabled = new TransportReservation(
                    request.endpointKind(), request.boundWarehouseCorePos(),
                    0, request.scaleMetric(), 0.0, TransportAdmissionStatus.DISABLED);
            replaceAndMark(request.endpointId(), disabled);
            return result(TransportReservationDecision.INSUFFICIENT_CAPACITY, disabled,
                    admission.requiredAdditionalCapacity());
        }
        return result(TransportReservationDecision.INSUFFICIENT_CAPACITY, old,
                admission.requiredAdditionalCapacity());
    }

    public TransportReservationResult refreshTransportEndpointMetric(
            TransportEndpointId endpointId,
            GlobalPos boundWarehouseCorePos,
            double scaleMetric
    ) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (endpointId == null || boundWarehouseCorePos == null || parametersResult.isEmpty()) {
            return result(TransportReservationDecision.INVALID_REQUEST, null, 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        TransportReservation old = data.getTransportState().getReservation(endpointId);
        if (old == null || !old.boundWarehouseCorePos().equals(boundWarehouseCorePos)) {
            return result(TransportReservationDecision.INVALID_BINDING, old, 0.0);
        }
        if (!TransportReservationModel.isFiniteNonNegative(scaleMetric)) {
            return result(TransportReservationDecision.INVALID_REQUEST, old, 0.0);
        }
        double refreshedCapacity = TransportReservationModel.capacityForStoredRate(
                old.endpointKind(), old.rateItemsPerSecond(), scaleMetric, parameters);
        if (!TransportReservationModel.isFiniteNonNegative(refreshedCapacity)) {
            return result(TransportReservationDecision.INVALID_REQUEST, old, 0.0);
        }
        TransportReservation refreshed = new TransportReservation(
                old.endpointKind(), old.boundWarehouseCorePos(),
                old.rateItemsPerSecond(), scaleMetric, refreshedCapacity, old.admissionStatus());
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

    /** Releases every endpoint logically owned by one warehouse core, including unloaded interfaces. */
    public int unregisterTransportEndpointsBoundTo(GlobalPos boundWarehouseCorePos) {
        if (boundWarehouseCorePos == null) {
            return 0;
        }
        return unregisterTransportEndpointsMatching(reservation ->
                reservation.boundWarehouseCorePos().equals(boundWarehouseCorePos));
    }

    /** Dimension-agnostic fallback for repair/removal paths that have no loaded level. */
    public int unregisterTransportEndpointsBoundTo(BlockPos boundWarehouseCorePos) {
        if (boundWarehouseCorePos == null) {
            return 0;
        }
        return unregisterTransportEndpointsMatching(reservation ->
                reservation.boundWarehouseCorePos().pos().equals(boundWarehouseCorePos));
    }

    private int unregisterTransportEndpointsMatching(
            java.util.function.Predicate<TransportReservation> predicate
    ) {
        prepareTransportState();
        List<TransportEndpointId> endpoints = data.getTransportState().getReservations().entrySet().stream()
                .filter(entry -> predicate.test(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        for (TransportEndpointId endpoint : endpoints) {
            data.getTransportState().removeReservation(endpoint);
        }
        if (!endpoints.isEmpty()) {
            data.getDataSyncCache().markTransportStateChanged();
        }
        return endpoints.size();
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
        if (data.getTransportState().recalculateReservedCapacities(parameters)) {
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
                    config.warehouseScaleCostPerMetric.get()));
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
