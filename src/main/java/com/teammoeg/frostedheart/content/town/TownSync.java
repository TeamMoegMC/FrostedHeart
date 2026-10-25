package com.teammoeg.frostedheart.content.town;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.network.TeamTownDataS2CPacket;
import com.teammoeg.frostedheart.content.town.network.TownBuildingUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownResidentUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownResourceUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownSignalNotificationPacket;
import com.teammoeg.frostedheart.content.town.network.TownTransportShortageNotificationPacket;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEventModel;
import com.teammoeg.frostedheart.content.town.observation.TownSignalNotice;
import com.teammoeg.frostedheart.content.town.observation.TownTowerTipThrottle;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import com.teammoeg.frostedheart.content.town.transport.TownTransportShortageNotice;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Ordered full-view baselines and bounded retries for the current server session. */
public final class TownSync {
    private static final Map<ServerGamePacketListenerImpl, Baseline> connections = new IdentityHashMap<>();
    private static final class Baseline {
        UUID holderId;
        TeamTownData data;
        boolean ready;
        boolean invalidationSent;
        long nextAttempt;
        int retryTicks = 20;
    }
    private TownSync() {}

    public static void requestFull(ServerPlayer player) {
        Baseline state = connections.computeIfAbsent(player.connection, ignored -> new Baseline());
        state.ready = false;
        state.invalidationSent = false;
        state.nextAttempt = 0;
        state.retryTicks = 20;
        tryFull(player, state);
    }

    public static void invalidate(TeamDataHolder holder) {
        holder.forEachOnline(player -> {
            Baseline state = connections.computeIfAbsent(player.connection, ignored -> new Baseline());
            state.ready = false;
            state.invalidationSent = false;
            state.nextAttempt = 0;
            // Ownership may still be changing. Invalidate now; resolve the final
            // team at the next boundary rather than creating a temporary holder.
            try {
                FHNetwork.INSTANCE.sendPlayer(player, TeamTownDataS2CPacket.pending(null));
                state.invalidationSent = true;
            } catch (RuntimeException failure) { sendFailed(player, failure); }
        });
    }

    public static boolean allowDelta(ServerPlayer player) {
        Baseline state = connections.get(player.connection);
        TeamDataHolder holder = CTeamDataManager.get(player);
        TeamTownData data = holder == null ? null : holder.getOptional(FHSpecialDataTypes.TOWN_DATA).orElse(null);
        UUID id = holder == null ? null : holder.getId();
        if (state == null || !Objects.equals(state.holderId, id) || state.data != data) {
            requestFull(player);
            state = connections.get(player.connection);
        }
        return state != null && state.ready && state.data != null;
    }

    public static void sendFailed(ServerPlayer player, RuntimeException failure) {
        Baseline state = connections.computeIfAbsent(player.connection, ignored -> new Baseline());
        state.ready = false;
        state.invalidationSent = false;
        state.nextAttempt = player.server.getTickCount() + 20L;
        FHMain.LOGGER.error("Town view delivery failed for {}", player.getUUID(), failure);
    }

    public static void tick(MinecraftServer server) {
        connections.keySet().removeIf(connection -> server.getPlayerList().getPlayers().stream()
                .noneMatch(player -> player.connection == connection));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Baseline state = connections.computeIfAbsent(player.connection, ignored -> new Baseline());
            if (!state.ready && server.getTickCount() >= state.nextAttempt) tryFull(player, state);
        }
    }

    private static void tryFull(ServerPlayer player, Baseline state) {
        TeamDataHolder holder = CTeamDataManager.get(player);
        state.holderId = holder == null ? null : holder.getId();
        state.data = holder == null ? null : holder.getOptional(FHSpecialDataTypes.TOWN_DATA).orElse(null);
        try {
            if (!state.invalidationSent) {
                FHNetwork.INSTANCE.sendPlayer(player, TeamTownDataS2CPacket.pending(state.holderId));
                state.invalidationSent = true;
            }
            TownRuntimeManager runtime = TownRuntimeManager.current();
            if (holder != null && runtime != null && runtime.isBusy(holder)) {
                state.nextAttempt = player.server.getTickCount() + 1L;
                return;
            }
            TeamTownDataS2CPacket packet;
            if (state.data == null) packet = TeamTownDataS2CPacket.absent(state.holderId);
            else {
                state.data.prepareView(TeamTownData.executionLevel(holder), holder);
                packet = TeamTownDataS2CPacket.present(state.holderId, state.data);
            }
            FHNetwork.INSTANCE.sendPlayer(player, packet);
            state.ready = true;
            state.retryTicks = 20;
        } catch (Exception failure) {
            state.ready = false;
            state.nextAttempt = player.server.getTickCount() + state.retryTicks;
            if (state.retryTicks == 20) FHMain.LOGGER.error("Town full snapshot remains pending for {}", player.getUUID(), failure);
            state.retryTicks = Math.min(200, state.retryTicks * 2);
        }
    }

    public static void reset() { connections.clear(); }

    static void tick(TeamTownData data, ServerLevel level, TeamDataHolder teamData) {
        try { data.prepareView(level, teamData); }
        catch (RuntimeException failure) {
            data.needsRuntimeRepair = true;
            FHMain.LOGGER.error("Town {} view preparation requires repair", teamData.getId(), failure);
            return;
        }
        Set<UUID> residentsBefore = Set.copyOf(data.dataSyncCache.changedResidentUUID);
        Set<BlockPos> buildingsBefore = Set.copyOf(data.dataSyncCache.changedBuildingPos);
        Set<ITownResourceKey> resourcesBefore = Set.copyOf(data.dataSyncCache.changedResourceKey);
        boolean transportBefore = data.dataSyncCache.transportStateChanged;
        try { flushChanges(data, level, teamData); }
        catch (RuntimeException failure) {
            data.dataSyncCache.changedResidentUUID.addAll(residentsBefore);
            data.dataSyncCache.changedBuildingPos.addAll(buildingsBefore);
            data.dataSyncCache.changedResourceKey.addAll(resourcesBefore);
            data.dataSyncCache.transportStateChanged |= transportBefore;
            TownSync.invalidate(teamData);
            FHMain.LOGGER.error("Town {} incremental capture remains pending", teamData.getId(), failure);
        }
    }

    static void flushChanges(TeamTownData data, ServerLevel level, TeamDataHolder teamData) {
        if(data.dataSyncCache.hasChangedResources() || data.dataSyncCache.hasTransportStateChange()){
            Map<ITownResourceKey, Double> changedResource = new HashMap<>();
            for(ITownResourceKey resourceKey : data.dataSyncCache.drainChangedResources()){
                double current = data.resources.get(resourceKey);
                // 发送端值级去重：当前值与上次已同步值相同（且此前已同步过）时跳过，
                // 消除"操作级 fire"（如 reloadMaxCapacity 的清零+加回）造成的空转资源包。
                if(data.dataSyncCache.isResourceUnchanged(resourceKey, current)){
                    continue;
                }
                changedResource.put(resourceKey, current);
            }
            boolean transportStateChanged = data.dataSyncCache.drainTransportStateChange();
            if(!changedResource.isEmpty() || transportStateChanged){
                teamData.sendToOnline(FHNetwork.INSTANCE, new TownResourceUpdatePacket(
                        changedResource,
                        data.resources.getOccupiedCapacity(),
                        data.createTransportSnapshot()));
                changedResource.forEach(data.dataSyncCache::markResourceSynced);
            }
        }

        if(!data.dataSyncCache.changedResidentUUID.isEmpty()){
            Map<UUID, Resident> changedResidents = new HashMap<>();
            Set<UUID> removedResidents = new HashSet<>();
            for(UUID uuid : data.dataSyncCache.drainChangedResidents()){
                Resident resident = data.residents.get(uuid);
                if(resident == null){
                    removedResidents.add(uuid);
                } else{
                    changedResidents.put(uuid, resident);
                }
            }
            teamData.sendToOnline(FHNetwork.INSTANCE, new TownResidentUpdatePacket(changedResidents, removedResidents));
        }

        if(!data.dataSyncCache.changedBuildingPos.isEmpty()){
            Map<BlockPos, ITownBuilding> changedBuildings = new HashMap<>();
            Set<BlockPos> removedBuildings = new HashSet<>();
            for(BlockPos pos : data.dataSyncCache.drainChangedBuildings()){
                ITownBuilding building = data.buildings.get(pos);
                if(building == null){
                    removedBuildings.add(pos);
                } else{
                    changedBuildings.put(pos, building);
                }
            }
            teamData.sendToOnline(FHNetwork.INSTANCE, new TownBuildingUpdatePacket(changedBuildings, removedBuildings));
        }

        TownTowerTipThrottle.Result deferredTower = TownTowerTipThrottle.onTick(
                data.towerTipState, level.getGameTime());
        data.towerTipState = deferredTower.state();
        data.pendingTowerTipSignals.addAll(deferredTower.emitted());
        flushTownTipNotifications(data, teamData);
    }

    static void flushTownTipNotifications(TeamTownData data, TeamDataHolder teamData) {
        if (!data.pendingTowerTipSignals.isEmpty()) {
            teamData.sendToOnline(FHNetwork.INSTANCE, new TownSignalNotificationPacket(
                    ++data.nextTownTipNotificationId, List.copyOf(data.pendingTowerTipSignals)));
            data.pendingTowerTipSignals.clear();
        }
        if (!data.pendingTownTipSignals.isEmpty()) {
            List<TownSignalNotice> compacted = TownSignalEventModel.compactNotifications(
                    data.pendingTownTipSignals);
            data.pendingTownTipSignals.clear();
            if (!compacted.isEmpty()) {
                teamData.sendToOnline(FHNetwork.INSTANCE, new TownSignalNotificationPacket(
                        ++data.nextTownTipNotificationId, compacted));
            }
        }
        while (!data.pendingTransportShortageTips.isEmpty()) {
            int packetSize = Math.min(
                    data.pendingTransportShortageTips.size(),
                    TownTransportShortageNotificationPacket.MAX_NOTICES);
            List<TownTransportShortageNotice> packetNotices = List.copyOf(
                    data.pendingTransportShortageTips.subList(0, packetSize));
            data.pendingTransportShortageTips.subList(0, packetSize).clear();
            teamData.sendToOnline(FHNetwork.INSTANCE,
                    new TownTransportShortageNotificationPacket(
                            ++data.nextTownTipNotificationId, packetNotices));
        }
    }
}
