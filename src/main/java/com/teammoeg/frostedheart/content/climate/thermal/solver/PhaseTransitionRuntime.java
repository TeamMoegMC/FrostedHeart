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
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialStateAccess;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.Objects;

/** Worker-owned handoff of completed material transitions; owns no energy. */
public final class PhaseTransitionRuntime {
    private static final Request[] NO_REQUESTS = new Request[0];

    public enum AckOutcome { APPLIED, REJECTED, RETRY }

    public record Request(
            int fastSlot, int lifecycleGeneration,
            int blockX, int blockY, int blockZ,
            int profileId, long requestSequence,
            int targetStateId, byte materialBranch, long incarnation
    ) {}

    private final ThermalCellArena arena;
    private final MaterialStateAccess materialStates;
    private final MaterialSample material = new MaterialSample();
    private final int requestCapacity;
    private final ArrayDeque<Request> requests = new ArrayDeque<>();
    private final MaterialSlotIndex materialSlots = new MaterialSlotIndex();
    private record StagedSlot(com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token, int next) { }
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<StagedSlot> stagedSlots = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    private it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet dirty = new it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet();
    private com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation storage;
    private int admittedCapacity;
    public void storageOwner(com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation owner) { storage = owner; }
    public void recordBacking(com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter meter) {
        meter.container(materialSlots, admittedCapacity, 12); meter.container(dirty, admittedCapacity, 16);
        meter.container(requests, requestCapacity, 8);
        meter.container(stagedSlots, stagedSlots.size(), 16); meter.bytes(48L * stagedSlots.size());
    }

    public void markMaterialDirty(int slot) { dirty.add(slot); }
    public boolean hasReadyRequests() { return !dirty.isEmpty() && requests.size() < requestCapacity; }
    private long nextRequestSequence;
    private java.util.function.IntConsumer materialControlChanged = slot -> { };
    public void onMaterialControlChanged(java.util.function.IntConsumer listener) {
        materialControlChanged = Objects.requireNonNull(listener, "listener");
    }

    public PhaseTransitionRuntime(ThermalCellArena arena, int requestCapacity) {
        this(arena, arena, requestCapacity);
    }

    public PhaseTransitionRuntime(ThermalCellArena arena, MaterialStateAccess materialStates, int requestCapacity) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.materialStates = Objects.requireNonNull(materialStates, "materialStates");
        if (requestCapacity <= 0) throw new IllegalArgumentException("phase request capacity must be positive");
        this.requestCapacity = requestCapacity;
        materialSlots.defaultReturnValue(-1);
    }

    /** Visit phase-capable bodies only. Requests are allocated only when offered. */
    public void collectMaterialRequests() {
        var slots = materialSlots.values().iterator();
        while (slots.hasNext()) markMaterialDirty(slots.nextInt());
        advanceMaterialRequests(Integer.MAX_VALUE);
    }

    public void advanceMaterialRequests(int maximum) {
        while (maximum-- > 0 && !dirty.isEmpty() && requests.size() < requestCapacity) {
            int slot = dirty.removeFirstInt();
            if (!arena.isLive(slot) || !arena.hasMaterialTransition(slot)) continue;
            if (arena.materialLayoutPending(slot)) continue;
            materialStates.readMaterial(slot, material);
            var transition = material.law().transition(material.branch());
            if (transition == null) continue;
            if (!arena.phaseRequestOutstanding(slot)) {
                if (!transition.complete(material.enthalpyJ())) continue;
                arena.beginPhaseRequest(slot, ++nextRequestSequence);
                materialControlChanged.accept(slot);
            }
            if (requests.size() == requestCapacity || !arena.phaseRequestNeedsOffer(slot)) continue;
            requests.addLast(new Request(slot, arena.lifecycleGeneration(slot),
                    arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2),
                    arena.materialProfileId(slot), arena.phaseRequestSequence(slot),
                    transition.targetStateId(), material.branch(), arena.materialIncarnation(slot)));
            arena.markPhaseRequestEnqueued(slot, arena.phaseRequestSequence(slot));
            materialControlChanged.accept(slot);
        }
    }

    /** Explicit gameplay control through the current material state owner. */
    public boolean forceMaterialTransition(int slot, byte branch) {
        materialStates.readMaterial(slot, material);
        var edge = material.law() == null ? null : material.law().transition(branch);
        if (edge == null) return false;
        if (arena.phaseRequestOutstanding(slot) && material.branch() != branch)
            arena.completePhaseRequest(slot, arena.phaseRequestSequence(slot), false);
        double next = edge.heating() ? Math.max(material.enthalpyJ(), edge.targetEnthalpyJ())
                : Math.min(material.enthalpyJ(), edge.targetEnthalpyJ());
        materialStates.applyMaterialControl(slot, next, branch);
        arena.recordExternalMaterialEnergy(next - material.enthalpyJ());
        materialControlChanged.accept(slot);
        markMaterialDirty(slot);
        return true;
    }

    public boolean applyAck(Request request, AckOutcome outcome) {
        int slot = request.fastSlot();
        if (!matches(slot, request)) {
            long position = BlockPos.asLong(request.blockX(), request.blockY(), request.blockZ());
            var staged = stagedSlots.get(position);
            slot = staged != null && staged.token.committed() ? staged.next : materialSlots.get(position);
        }
        if (!matches(slot, request) || !arena.phaseRequestOutstanding(slot)
                || arena.materialTransitionAcknowledged(slot)
                || arena.phaseRequestSequence(slot) != request.requestSequence()) return false;
        if (outcome == AckOutcome.RETRY) {
            arena.retryPhaseRequest(slot, request.requestSequence());
        } else {
            arena.completePhaseRequest(slot, request.requestSequence(), outcome == AckOutcome.APPLIED);
        }
        materialControlChanged.accept(slot);
        markMaterialDirty(slot);
        return true;
    }

    public Request[] drainRequests(int maximum) {
        int count = Math.min(maximum, requests.size());
        if (count == 0) return NO_REQUESTS;
        Request[] drained = new Request[count];
        for (int i = 0; i < count; i++) drained[i] = requests.removeFirst();
        return drained;
    }

    public void reserveMaterialChanges(int additional) {
        int required = materialSlots.size() + additional;
        if (required > admittedCapacity) {
            int next = Math.max(required, Math.max(16, admittedCapacity * 2));
            if (storage != null) storage.beforeAllocation(64L * next);
            materialSlots.reserve(next - materialSlots.size());
            var replacement = new it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet(next);
            replacement.addAll(dirty); dirty = replacement;
            if (storage != null) storage.afterReplacement(64L * admittedCapacity);
            admittedCapacity = next;
        }
        materialSlots.reserve(additional);
    }

    public void registerMaterial(int slot) {
        materialSlots.put(position(slot), slot);
        markMaterialDirty(slot);
    }

    public void stageMaterial(int slot, boolean addition, com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token) {
        long position = position(slot);
        var old = stagedSlots.get(position);
        if (!addition && old != null && old.token == token && old.next >= 0) return;
        stagedSlots.put(position, new StagedSlot(token, addition ? slot : -1));
    }
    public void finishMaterial(int slot, boolean addition, com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken token) {
        long position = position(slot);
        var staged = stagedSlots.get(position);
        if (token.committed()) {
            if (addition) registerMaterial(slot); else unregisterMaterial(slot);
        }
        if (staged != null && staged.token == token && (addition || staged.next < 0)) stagedSlots.remove(position);
    }

    public void unregisterMaterial(int slot) {
        materialSlots.remove(position(slot), slot);
        dirty.remove(slot);
    }

    private long position(int slot) {
        return BlockPos.asLong(arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2));
    }

    private boolean matches(int slot, Request request) {
        if (!(arena.isLive(slot) && arena.hasMaterialTransition(slot)
                && arena.lifecycleGeneration(slot) == request.lifecycleGeneration()
                && arena.materialIncarnation(slot) == request.incarnation()
                && arena.minimum(slot, 0) == request.blockX()
                && arena.minimum(slot, 1) == request.blockY()
                && arena.minimum(slot, 2) == request.blockZ()
                && arena.materialProfileId(slot) == request.profileId())) return false;
        materialStates.readMaterial(slot, material);
        return material.branch() == request.materialBranch();
    }

    /** Reserve before topology commit so registration cannot resize mid-commit. */
    private static final class MaterialSlotIndex extends Long2IntOpenHashMap {
        void reserve(int additional) {
            int required = size() + additional;
            if (required > maxFill) rehash(it.unimi.dsi.fastutil.HashCommon.arraySize(required, f));
        }

        @Override
        protected void rehash(int capacity) {
            // Removing old slots is also part of commit; retain backing storage.
            if (capacity > n) super.rehash(capacity);
        }
    }
}
