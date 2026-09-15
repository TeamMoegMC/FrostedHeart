/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
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
            int targetStateId, byte materialBranch
    ) {}

    private final ThermalCellArena arena;
    private final int requestCapacity;
    private final ArrayDeque<Request> requests = new ArrayDeque<>();
    private final MaterialSlotIndex materialSlots = new MaterialSlotIndex();
    private long nextRequestSequence;

    public PhaseTransitionRuntime(ThermalCellArena arena, int requestCapacity) {
        this.arena = Objects.requireNonNull(arena, "arena");
        if (requestCapacity <= 0) throw new IllegalArgumentException("phase request capacity must be positive");
        this.requestCapacity = requestCapacity;
        materialSlots.defaultReturnValue(-1);
    }

    /** Visit phase-capable bodies only. Requests are allocated only when offered. */
    public void collectMaterialRequests() {
        var slots = materialSlots.values().iterator();
        while (slots.hasNext()) {
            int slot = slots.nextInt();
            if (arena.materialLayoutPending(slot)) continue;
            if (!arena.phaseRequestOutstanding(slot)) {
                var transition = arena.materialTransition(slot);
                if (transition == null || !transition.complete(arena.enthalpyJ(slot))) continue;
                arena.beginPhaseRequest(slot, ++nextRequestSequence);
            }
            if (requests.size() == requestCapacity || !arena.phaseRequestNeedsOffer(slot)) continue;
            var transition = arena.materialTransition(slot);
            requests.addLast(new Request(slot, arena.lifecycleGeneration(slot),
                    arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2),
                    arena.materialProfileId(slot), arena.phaseRequestSequence(slot),
                    transition.targetStateId(), arena.materialBranch(slot)));
            arena.markPhaseRequestEnqueued(slot, arena.phaseRequestSequence(slot));
        }
    }

    public boolean applyAck(Request request, AckOutcome outcome) {
        int slot = request.fastSlot();
        if (!matches(slot, request)) {
            slot = materialSlots.get(BlockPos.asLong(request.blockX(), request.blockY(), request.blockZ()));
        }
        if (!matches(slot, request) || !arena.phaseRequestOutstanding(slot)
                || arena.materialTransitionAcknowledged(slot)
                || arena.phaseRequestSequence(slot) != request.requestSequence()) return false;
        if (outcome == AckOutcome.RETRY) {
            arena.retryPhaseRequest(slot, request.requestSequence());
        } else {
            arena.completePhaseRequest(slot, request.requestSequence(), outcome == AckOutcome.APPLIED);
        }
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
        materialSlots.reserve(additional);
    }

    public void registerMaterial(int slot) {
        materialSlots.put(position(slot), slot);
    }

    public void unregisterMaterial(int slot) {
        materialSlots.remove(position(slot), slot);
    }

    private long position(int slot) {
        return BlockPos.asLong(arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2));
    }

    private boolean matches(int slot, Request request) {
        return arena.isLive(slot) && arena.hasMaterialTransition(slot)
                && arena.lifecycleGeneration(slot) == request.lifecycleGeneration()
                && arena.minimum(slot, 0) == request.blockX()
                && arena.minimum(slot, 1) == request.blockY()
                && arena.minimum(slot, 2) == request.blockZ()
                && arena.materialProfileId(slot) == request.profileId()
                && arena.materialBranch(slot) == request.materialBranch();
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
