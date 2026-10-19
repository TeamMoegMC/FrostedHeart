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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.experiment;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import java.util.*;

/**
 * 实验台：为队伍某次研究的一项实验需求投入材料并持续运行。条件不满足时暂停，不清进度、不退材料；
 * 运行不需要玩家在场，只需要区块保持加载。
 * An experiment table runs one experiment requirement of a team's research. A failing condition pauses it without
 * losing progress or returning materials; it needs no player nearby, only a loaded chunk.
 */
public class ExperimentTableBlockEntity extends CBlockEntity implements CTickableBlockEntity, MenuProvider {
    public static final int MATERIAL_SLOTS = ProjectRequirement.MAX_MATERIALS;
    /** Ticks between condition checks while running; progress advances by the same step. */
    public static final int CHECK_INTERVAL = 20;
    /** Room scans happen on every fifth check; temperature is read on every check. */
    public static final int SCAN_EVERY = 5;

    public enum Status {IDLE, RUNNING, PAUSED}

    private final ItemStackHandler inventory = new ItemStackHandler(MATERIAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private UUID run;
    private String requirement = "";
    private Status status = Status.IDLE;
    private int progress, duration, checks, completed;
    private ExperimentReport report = ExperimentReport.EMPTY;
    private List<ExperimentReport.Line> spaceLines = List.of();
    /** Last notable outcome for the screen, such as a finished run; a suffix of gui.frostedresearch.experiment.message. */
    private String message = "";

    public ExperimentTableBlockEntity(BlockPos pos, BlockState state) {
        super(FRContents.BlockEntityTypes.EXPERIMENT_TABLE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public UUID run() {
        return run;
    }

    public String requirement() {
        return requirement;
    }

    public Status status() {
        return status;
    }

    public int progress() {
        return progress;
    }

    public int duration() {
        return duration;
    }

    public ExperimentReport report() {
        return report;
    }

    public String message() {
        return message;
    }

    /**
     * Experiments this table has finished, for display only.
     */
    public int completed() {
        return completed;
    }

    private Optional<KnowledgeService> service() {
        UUID owner = IOwnerTile.getOwner(this);
        TeamDataHolder team = owner == null || level == null || CTeamDataManager.INSTANCE == null ? null : CTeamDataManager.INSTANCE.get(owner);
        return team == null ? Optional.empty() : Optional.of(KnowledgeService.forTeam(team, level.getGameTime()));
    }

    private Optional<ProjectRequirement> target(KnowledgeService service) {
        return run == null ? Optional.empty() : service.experimentRequirement(run, requirement);
    }

    /**
     * Points the table at one experiment requirement and checks it. A running experiment is abandoned.
     */
    public boolean select(ServerPlayer player, UUID run, String requirement) {
        var definition = KnowledgeService.forPlayer(player).experimentRequirement(run, requirement);
        if (definition.isEmpty()) {
            message = "unavailable";
            return false;
        }
        this.run = run;
        this.requirement = requirement;
        status = Status.IDLE;
        progress = 0;
        duration = definition.get().duration();
        message = "";
        refresh(definition.get(), true);
        return true;
    }

    public void check(ServerPlayer player) {
        var definition = target(KnowledgeService.forPlayer(player));
        if (definition.isEmpty()) {
            clearTarget("unavailable");
            return;
        }
        message = "";
        refresh(definition.get(), true);
    }

    /**
     * Starts when every condition and material is present, consuming the materials.
     */
    public boolean start(ServerPlayer player) {
        var definition = target(KnowledgeService.forPlayer(player));
        if (definition.isEmpty()) {
            clearTarget("unavailable");
            return false;
        }
        if (status != Status.IDLE) return false;
        refresh(definition.get(), true);
        if (!report.ok() || !ExperimentConditions.consume(inventory, definition.get().materials())) {
            message = "not_ready";
            return false;
        }
        status = Status.RUNNING;
        progress = 0;
        duration = definition.get().duration();
        checks = 0;
        message = "started";
        refresh(definition.get(), false);
        return true;
    }

    /**
     * Stops a running experiment. Its materials are already spent and are not returned.
     */
    public void stop() {
        if (status == Status.IDLE) return;
        status = Status.IDLE;
        progress = 0;
        message = "stopped";
    }

    private void clearTarget(String reason) {
        run = null;
        requirement = "";
        status = Status.IDLE;
        progress = 0;
        duration = 0;
        report = ExperimentReport.EMPTY;
        spaceLines = List.of();
        message = reason;
    }

    /**
     * Rebuilds the report. Before a run starts it lists the materials the slots can supply; while running the
     * materials are already in the experiment. The room is scanned only when {@code scan} asks for it.
     */
    private void refresh(ProjectRequirement definition, boolean scan) {
        if (scan || spaceLines.isEmpty() && definition.space().enclosed())
            spaceLines = ExperimentConditions.space(level, worldPosition, definition.space());
        List<ExperimentReport.Line> lines = new ArrayList<>(spaceLines);
        ExperimentConditions.temperature(level, worldPosition, definition.space()).ifPresent(lines::add);
        if (status == Status.IDLE) lines.addAll(ExperimentConditions.materials(inventory, definition.materials()));
        report = new ExperimentReport(lines);
    }

    @Override
    public void tick() {
        if (level == null || level.isClientSide || run == null || level.getGameTime() % CHECK_INTERVAL != 0) return;
        var service = service();
        // Without the owner team's data there is nothing to judge against; keep the run and look again next check.
        if (service.isEmpty()) return;
        var definition = target(service.get());
        if (definition.isEmpty()) {
            clearTarget("unavailable");
            setChanged();
            syncData();
            return;
        }
        if (status == Status.IDLE) {
            // Materials and temperature follow the slots and the weather; the room is rescanned only on request.
            // Reading the slots here also covers changes the item handler is not told about, such as merged stacks.
            ExperimentReport before = report;
            refresh(definition.get(), false);
            if (!report.equals(before)) syncData();
            return;
        }
        refresh(definition.get(), ++checks % SCAN_EVERY == 0);
        // A failing condition only pauses; progress and spent materials stay with the experiment.
        status = report.ok() ? Status.RUNNING : Status.PAUSED;
        if (status == Status.RUNNING) progress += CHECK_INTERVAL;
        if (progress >= duration) {
            service.get().recordExperiment(run, requirement);
            completed++;
            status = Status.IDLE;
            progress = 0;
            message = "completed";
            // The next experiment for the same requirement starts from a fresh check; a satisfied one clears the target.
            if (service.get().experimentRequirement(run, requirement).isEmpty()) clearTarget("completed");
            else refresh(definition.get(), true);
        }
        setChanged();
        syncData();
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        if (!descPacket && nbt.contains("inventory")) inventory.deserializeNBT(nbt.getCompound("inventory"));
        run = nbt.hasUUID("run") ? nbt.getUUID("run") : null;
        requirement = nbt.getString("requirement");
        try {
            status = Status.valueOf(nbt.getString("status"));
        } catch (IllegalArgumentException unknown) {
            status = Status.IDLE;
        }
        progress = nbt.getInt("progress");
        duration = nbt.getInt("duration");
        completed = nbt.getInt("completed");
        message = nbt.getString("message");
        report = ExperimentReport.read(nbt.getCompound("report"));
        spaceLines = List.of();
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        if (!descPacket) nbt.put("inventory", inventory.serializeNBT());
        if (run != null) nbt.putUUID("run", run);
        nbt.putString("requirement", requirement);
        nbt.putString("status", status.name());
        nbt.putInt("progress", progress);
        nbt.putInt("duration", duration);
        nbt.putInt("completed", completed);
        nbt.putString("message", message);
        nbt.put("report", report.write());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ExperimentTableMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.frostedresearch.experiment_table");
    }
}
