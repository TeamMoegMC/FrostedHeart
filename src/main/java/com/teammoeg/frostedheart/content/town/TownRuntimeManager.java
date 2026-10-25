package com.teammoeg.frostedheart.content.town;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClockSource;
import com.teammoeg.frostedheart.content.town.ai_town.AITownManager;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimScheduler;
import com.teammoeg.frostedheart.content.town.model.TownScheduleModel;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import java.util.HashMap;

/** Server-thread lifecycle and ordering. Algorithms stay in their owning helpers. */
public final class TownRuntimeManager {
    private static TownRuntimeManager current;
    private final CTeamDataManager teams;
    private final Map<UUID, TownRuntime> towns = new LinkedHashMap<>();
    private final ArrayDeque<Runnable> lifecycleRequests = new ArrayDeque<>();
    private record CoreKey(TeamDataHolder holder, BlockPos pos) {}
    private final Map<CoreKey, Runnable> coreRequests = new LinkedHashMap<>();
    private boolean drainingRequests;
    private boolean stopping;
    private final Map<UUID, net.minecraft.nbt.CompoundTag> replacementSnapshots = new HashMap<>();

    public TownRuntimeManager(CTeamDataManager teams) {
        this.teams = teams;
        current = this;
    }
    public static TownRuntimeManager current() {
        return current;
    }

    public boolean isBusy(TeamDataHolder holder) {
        TownRuntime runtime = towns.get(holder.getId());
        return runtime != null && (runtime.binding || runtime.data.inSettlement);
    }

    public boolean deferIfBusy(TeamDataHolder holder, Runnable request) {
        TownRuntime runtime = towns.get(holder.getId());
        if (runtime == null || (!runtime.binding && !runtime.data.inSettlement)) {
            return false;
        }
        lifecycleRequests.add(() -> {
            if (teams.findByHolderId(holder.getId()) == holder && towns.get(holder.getId()) == runtime) {
                request.run();
            }
        });
        return true;
    }

    public boolean deferBuildingIfBusy(TeamDataHolder holder, BlockPos pos, TeamTownData expected, Runnable operation) {
        if (holder == null || !isBusy(holder)) {
            return false;
        }
        coreRequests.put(new CoreKey(holder, pos.immutable()), () -> {
            if (teams.findByHolderId(holder.getId()) == holder
                    && holder.getOptional(FHSpecialDataTypes.TOWN_DATA).orElse(null) == expected) {
                operation.run();
            }
        });
        return true;
    }

    public void finishOperations() {
        if (drainingRequests) {
            return;
        }
        drainingRequests = true;
        try {
            int count = lifecycleRequests.size();
            for (int index = 0; index < count; index++) {
                try {
                    lifecycleRequests.remove().run();
                } catch (RuntimeException failure) {
                    FHMain.LOGGER.error("Deferred town lifecycle operation failed", failure);
                }
            }
            var cores = new ArrayList<>(coreRequests.values());
            coreRequests.clear();
            for (Runnable operation : cores) {
                try {
                    operation.run();
                } catch (RuntimeException failure) {
                    FHMain.LOGGER.error("Deferred building lifecycle failed", failure);
                }
            }
        } finally {
            drainingRequests = false;
        }
    }

    public void install(TeamDataHolder holder) {
        install(holder, false);
    }

    public void install(TeamDataHolder holder, boolean restoring) {
        TeamTownData data = holder.getOptional(FHSpecialDataTypes.TOWN_DATA).orElse(null);
        TownRuntime previous = towns.get(holder.getId());
        if (previous != null && previous.data == data) {
            previous.nextRepairTick = 0;
            previous.data.needsRuntimeRepair = true;
            repair(previous);
            return;
        }
        if (data == null) {
            detach(holder);
            return;
        }
        data.bindBuildingOwner(holder);
        TownRuntime next = new TownRuntime(holder, data);
        var sim = AITownManager.peekPlayerSim(holder.getId());
        if (restoring) {
            var snapshot = replacementSnapshots.remove(holder.getId());
            if (snapshot != null && sim != null) {
                sim.detach();
                sim.loadFromNbt(snapshot);
            }
        } else if (previous != null && sim != null) {
            replacementSnapshots.put(holder.getId(), com.teammoeg.frostedheart.content.town.citizen.sim.TownSimData.toNbt(sim));
        }
        if (previous != null) {
            data.replaceAutoSettlement(data.getAutoSettlement().mergeForReplacement(previous.data.getAutoSettlement()));
            next.observedWindow = previous.observedWindow;
            next.pendingDevices.putAll(previous.data.takeWarehouseListeners());
            next.pendingDevices.putAll(previous.pendingDevices);
            previous.data.setResidentListener(null);
            if (previous.sim != null) {
                previous.sim.detach();
            }
        }
        TownAssignments.reconcile(data);
        data.replaceAutoSettlement(data.getAutoSettlement().normalize());
        towns.put(holder.getId(), next);
        observeInitial(next, logicalTick());
        OptionalLong time = logicalTick();
        if (time.isPresent() && next.observedWindow != null) {
            next.observedWindow = Math.max(next.observedWindow, TownScheduleModel.window(time.getAsLong(), holder.getId()));
        }
        next.data.needsRuntimeRepair = true;
        repair(next);
        if (previous != null && !restoring && executionLevel(next) != null && next.data.needsRuntimeRepair) {
            throw new IllegalStateException("Replacement binding for town " + holder.getId() + " did not complete");
        }
        replacementSnapshots.remove(holder.getId());
        TownSync.invalidate(holder);
    }

    public void detach(TeamDataHolder holder) {
        TownRuntime old = towns.remove(holder.getId());
        if (old == null) {
            return;
        }
        old.data.setResidentListener(null);
        if (old.sim != null) {
            old.sim.detach();
        }
        old.pendingDevices.putAll(old.data.takeWarehouseListeners());
        TownTransportService.rebind(old.pendingDevices, null);
        TownSync.invalidate(holder);
    }

    private OptionalLong logicalTick() {
        ServerLevel overworld = teams.server().overworld();
        if (overworld == null) {
            return OptionalLong.empty();
        }
        WorldClimate clock = WorldClimate.getCapability(overworld).orElse(null);
        return clock == null ? OptionalLong.empty() : OptionalLong.of(clock.getSec() * 20L
                + WorldClockSource.elapsedDayTimeTicks(clock.getClockDayTime(), overworld.getDayTime()));
    }

    private void observeInitial(TownRuntime runtime, OptionalLong logicalTick) {
        if (logicalTick.isEmpty()) {
            return;
        }
        long window = TownScheduleModel.window(logicalTick.getAsLong(), runtime.holder.getId());
        AutoSettlementState state = runtime.data.getAutoSettlement().resolveInitialWindow(window,
                TownScheduleModel.calendarDay(logicalTick.getAsLong()));
        runtime.data.replaceAutoSettlement(state);
        if (runtime.observedWindow == null) {
            runtime.observedWindow = Math.max(window, state.processedThrough().orElse(window));
        } else {
            runtime.observedWindow = Math.max(runtime.observedWindow, state.processedThrough().orElse(window));
        }
    }

    private ServerLevel executionLevel(TownRuntime runtime) {
        return TeamTownData.executionLevel(runtime.holder);
    }

    private void repair(TownRuntime runtime) {
        ServerLevel level = executionLevel(runtime);
        var dimension = level == null ? null : level.dimension();
        if (!Objects.equals(runtime.dimension, dimension)) {
            if (runtime.sim != null) {
                runtime.sim.detach();
            }
            runtime.dimension = dimension;
            runtime.data.markWarehouseTopologyDirty();
            runtime.data.needsRuntimeRepair = true;
            runtime.nextRepairTick = 0;
        }
        if (teams.server().getTickCount() < runtime.nextRepairTick) {
            return;
        }
        if (!runtime.data.needsRuntimeRepair && runtime.pendingDevices.isEmpty()) {
            return;
        }
        runtime.binding = true;
        try {
            if (runtime.pendingDevices.isEmpty()) {
                runtime.pendingDevices.putAll(runtime.data.takeWarehouseListeners());
            }
            runtime.data.prepareBuildingGeometry();
            TownAssignments.reconcile(runtime.data);
            if (level != null) {
                runtime.sim = AITownManager.getPlayerSim(runtime.holder.getId());
                // Refresh calibrates all restored IDs before adopt can allocate one.
                if (runtime.sim.isAdopted()) {
                    runtime.sim.detach();
                }
                CitizenSimScheduler.get(level).refreshRegistryNow(level);
            }
            TownTransportService.prepare(runtime.data.createTeamTown(), dimension);
            boolean rebound = TownTransportService.rebind(runtime.pendingDevices, runtime.data.createTeamTown());
            runtime.data.needsRuntimeRepair = !rebound;
            runtime.repairDelay = rebound ? 20 : Math.min(200, runtime.repairDelay * 2);
        } catch (RuntimeException failure) {
            if (runtime.sim != null) {
                runtime.sim.detach();
            }
            runtime.data.needsRuntimeRepair = true;
            if (runtime.repairDelay == 20) {
                FHMain.LOGGER.error("Town {} runtime needs repair", runtime.holder.getId(), failure);
            }
            runtime.repairDelay = Math.min(200, runtime.repairDelay * 2);
        } finally {
            runtime.nextRepairTick = teams.server().getTickCount() + runtime.repairDelay;
            runtime.binding = false;
        }
    }

    public void tickEnd() {
        if (stopping) {
            return;
        }
        OptionalLong time = logicalTick();
        var candidates = new ArrayList<>(towns.values());
        for (TownRuntime runtime : candidates) {
            if (towns.get(runtime.holder.getId()) != runtime) {
                continue;
            }
            repair(runtime);
            observeInitial(runtime, time);
            if (time.isEmpty() || runtime.data.inSettlement) {
                continue;
            }
            long window = TownScheduleModel.window(time.getAsLong(), runtime.holder.getId());
            if (window <= runtime.observedWindow) {
                continue;
            }
            runtime.observedWindow = window;
            ServerLevel level = executionLevel(runtime);
            if (level == null || runtime.holder.getOnlineMembers().isEmpty()
                    || !FHConfig.SERVER.TOWN.enableTownTickMorning.get()) continue;
            runtime.data.replaceAutoSettlement(runtime.data.getAutoSettlement().afterAttempt(window));
            TownSettlement.advanceOneDay(runtime.data, level, runtime.holder, OptionalLong.of(window));
        }
        finishOperations();
        TownSync.tick(teams.server());
        for (TownRuntime runtime : candidates) {
            if (towns.get(runtime.holder.getId()) != runtime) {
                continue;
            }
            ServerLevel level = executionLevel(runtime);
            if (level != null) {
                runtime.data.tick(level, runtime.holder);
            }
        }
    }

    public void tickLevel(ServerLevel level) {
        if (stopping) {
            return;
        }
        int interval = Math.max(1, FHConfig.SERVER.TOWN.townUpdateIntervalGameTicks.get());
        for (TownRuntime runtime : new ArrayList<>(towns.values())) {
            if (executionLevel(runtime) != level || runtime.holder.getOnlineMembers().isEmpty()) {
                continue;
            }
            if (Math.floorMod(level.getGameTime(), interval) == Math.floorMod(runtime.holder.getId().hashCode(), interval)) {
                try {
                    runtime.data.tickSecond(level, runtime.holder);
                } catch (RuntimeException failure) {
                    runtime.data.needsRuntimeRepair = true;
                    FHMain.LOGGER.error("Town {} low-frequency update failed", runtime.holder.getId(), failure);
                }
            }
        }
    }

    public void stopping() {
        stopping = true;
        finishOperations();
    }
    public void close() {
        towns.clear();
        lifecycleRequests.clear();
        coreRequests.clear();
        replacementSnapshots.clear();
        TownSync.reset();
        if (current == this) {
            current = null;
        }
    }
}
