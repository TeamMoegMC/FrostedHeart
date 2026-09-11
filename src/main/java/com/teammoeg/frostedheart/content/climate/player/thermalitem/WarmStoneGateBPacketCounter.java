/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.player.thermalitem;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Default-off client packet observer used to collect Gate B evidence.
 */
public final class WarmStoneGateBPacketCounter {
    private static boolean enabled;
    private static long startedAtNanos;
    private static long stoppedAfterNanos;
    private static final Counts COUNTS = new Counts();

    private WarmStoneGateBPacketCounter() {
    }

    public static synchronized Snapshot start() {
        clearCounts();
        enabled = true;
        startedAtNanos = System.nanoTime();
        stoppedAfterNanos = 0L;
        Snapshot snapshot = snapshotAt(startedAtNanos);
        FHMain.LOGGER.info("FH_GATE_B_START {}", snapshot.toLogLine());
        return snapshot;
    }

    public static synchronized Snapshot reset() {
        boolean wasEnabled = enabled;
        clearCounts();
        enabled = wasEnabled;
        startedAtNanos = System.nanoTime();
        stoppedAfterNanos = 0L;
        Snapshot snapshot = snapshotAt(startedAtNanos);
        FHMain.LOGGER.info("FH_GATE_B_RESET {}", snapshot.toLogLine());
        return snapshot;
    }

    public static synchronized Snapshot stop() {
        long now = System.nanoTime();
        if (enabled) {
            stoppedAfterNanos = Math.max(0L, now - startedAtNanos);
        }
        enabled = false;
        Snapshot snapshot = snapshotAt(now);
        FHMain.LOGGER.info("FH_GATE_B_SUMMARY {}", snapshot.toLogLine());
        return snapshot;
    }

    public static synchronized Snapshot snapshot() {
        return snapshotAt(System.nanoTime());
    }

    public static synchronized boolean isEnabled() {
        return enabled;
    }

    public static synchronized void onCuriosStackPacket(
            int entityId,
            String curioId,
            int slotId,
            ItemStack stack
    ) {
        if (!enabled) {
            return;
        }
        ThermalStack thermal = inspectThermalStack(stack);
        COUNTS.recordCurios(thermal != null);
        if (thermal == null) {
            return;
        }
        logThermalEvent("curios_stack", entityId, curioId, slotId, thermal);
    }

    public static synchronized void onContainerSlotPacket(
            int containerId,
            int slotId,
            ItemStack stack
    ) {
        if (!enabled) {
            return;
        }
        ThermalStack thermal = inspectThermalStack(stack);
        COUNTS.recordContainerSlot(thermal != null);
        if (thermal == null) {
            return;
        }
        logThermalEvent("container_slot", containerId, "vanilla", slotId, thermal);
    }

    public static synchronized void onContainerContentPacket(
            int containerId,
            List<ItemStack> stacks
    ) {
        if (!enabled) {
            return;
        }
        int thermalStackCount = 0;
        for (int slotId = 0; slotId < stacks.size(); slotId++) {
            ThermalStack thermal = inspectThermalStack(stacks.get(slotId));
            if (thermal == null) {
                continue;
            }
            thermalStackCount++;
            logThermalEvent("container_content", containerId, "vanilla", slotId, thermal);
        }
        COUNTS.recordContainerContent(thermalStackCount);
    }

    public static synchronized void onProbeError(String detail) {
        if (!enabled) {
            return;
        }
        long errorCount = COUNTS.recordProbeError();
        if (errorCount == 1L) {
            FHMain.LOGGER.warn("FH_GATE_B_PROBE_ERROR detail={}", detail);
        }
    }

    private static ThermalStack inspectThermalStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof WearableThermalReservoir)) {
            return null;
        }
        return new ThermalStack(stack,
                WearableThermalState.read(stack));
    }

    private static void logThermalEvent(
            String path,
            int ownerId,
            String slotType,
            int slotId,
            ThermalStack thermal
    ) {
        Optional<WearableThermalState> state = thermal.state();
        String temperatures = state
                .map(value -> String.format(Locale.ROOT,
                        "initialized=true core_c=%.3f surface_c=%.3f",
                        value.coreTemperatureC(), value.surfaceTemperatureC()))
                .orElse("initialized=false core_c=na surface_c=na");
        FHMain.LOGGER.info(
                "FH_GATE_B_PACKET path={} owner_id={} slot_type={} slot_id={} item={} {}",
                path,
                ownerId,
                slotType,
                slotId,
                BuiltInRegistries.ITEM.getKey(thermal.stack().getItem()),
                temperatures
        );
    }

    private static void clearCounts() {
        COUNTS.clear();
    }

    private static Snapshot snapshotAt(long now) {
        long elapsedNanos = enabled
                ? Math.max(0L, now - startedAtNanos)
                : stoppedAfterNanos;
        return COUNTS.snapshot(enabled, elapsedNanos);
    }

    private record ThermalStack(
            ItemStack stack,
            Optional<WearableThermalState> state
    ) {
    }

    static final class Counts {
        private long curiosStackPackets;
        private long thermalCuriosStackPackets;
        private long containerSlotPackets;
        private long thermalContainerSlotPackets;
        private long containerContentPackets;
        private long thermalContainerContentPackets;
        private long thermalStacksInContentPackets;
        private long probeErrors;

        void recordCurios(boolean thermal) {
            curiosStackPackets++;
            if (thermal) {
                thermalCuriosStackPackets++;
            }
        }

        void recordContainerSlot(boolean thermal) {
            containerSlotPackets++;
            if (thermal) {
                thermalContainerSlotPackets++;
            }
        }

        void recordContainerContent(int thermalStackCount) {
            containerContentPackets++;
            if (thermalStackCount > 0) {
                thermalContainerContentPackets++;
                thermalStacksInContentPackets += thermalStackCount;
            }
        }

        long recordProbeError() {
            return ++probeErrors;
        }

        void clear() {
            curiosStackPackets = 0L;
            thermalCuriosStackPackets = 0L;
            containerSlotPackets = 0L;
            thermalContainerSlotPackets = 0L;
            containerContentPackets = 0L;
            thermalContainerContentPackets = 0L;
            thermalStacksInContentPackets = 0L;
            probeErrors = 0L;
        }

        Snapshot snapshot(boolean enabled, long elapsedNanos) {
            return new Snapshot(
                    enabled,
                    elapsedNanos,
                    curiosStackPackets,
                    thermalCuriosStackPackets,
                    containerSlotPackets,
                    thermalContainerSlotPackets,
                    containerContentPackets,
                    thermalContainerContentPackets,
                    thermalStacksInContentPackets,
                    probeErrors
            );
        }
    }

    public record Snapshot(
            boolean enabled,
            long elapsedNanos,
            long curiosStackPackets,
            long thermalCuriosStackPackets,
            long containerSlotPackets,
            long thermalContainerSlotPackets,
            long containerContentPackets,
            long thermalContainerContentPackets,
            long thermalStacksInContentPackets,
            long probeErrors
    ) {
        public String toLogLine() {
            return String.format(Locale.ROOT,
                    "enabled=%s elapsed_seconds=%.3f curios_stack_packets=%d"
                            + " thermal_curios_stack_packets=%d container_slot_packets=%d"
                            + " thermal_container_slot_packets=%d container_content_packets=%d"
                            + " thermal_container_content_packets=%d thermal_stacks_in_content_packets=%d"
                            + " probe_errors=%d",
                    enabled,
                    elapsedNanos / 1_000_000_000.0D,
                    curiosStackPackets,
                    thermalCuriosStackPackets,
                    containerSlotPackets,
                    thermalContainerSlotPackets,
                    containerContentPackets,
                    thermalContainerContentPackets,
                    thermalStacksInContentPackets,
                    probeErrors
            );
        }
    }
}
