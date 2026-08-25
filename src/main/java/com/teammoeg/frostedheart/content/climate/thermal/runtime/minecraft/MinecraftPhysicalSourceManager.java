/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.MissingPortPolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.Port;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.PortKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Main-thread producer for the Minecraft physical source profiles. */
public final class MinecraftPhysicalSourceManager implements AutoCloseable {
    private final MinecraftThermalInput input;
    private final ThermalSourceTimeline timeline;
    private final int maximumColdSourcePages;
    private final Map<Long, LiveSource> sources = new HashMap<>();
    private final Map<Long, Set<Long>> sourcesByTargetSection = new HashMap<>();
    private final LinkedHashSet<Long> dirtySources = new LinkedHashSet<>();

    private int nextLifecycleGeneration = 1;
    private long observedTopologyGeneration = -1L;
    private boolean closed;

    MinecraftPhysicalSourceManager(
            MinecraftThermalInput input,
            ThermalSourceTimeline timeline,
            int maximumColdSourcePages
    ) {
        if (maximumColdSourcePages <= 0) {
            throw new IllegalArgumentException("maximumColdSourcePages must be positive");
        }
        this.input = input;
        this.timeline = timeline;
        this.maximumColdSourcePages = maximumColdSourcePages;
    }

    public void observeCampfire(BlockPos position, boolean lit) {
        requireOpen();
        observe(
                position.asLong(),
                position,
                position,
                MinecraftPhysicalSourceProfile.CAMPFIRE,
                MinecraftPhysicalSourceProfile.CAMPFIRE.ratedPowerW(),
                lit);
    }

    public void observeGenerator(
            BlockPos sourcePosition,
            BlockPos exhaustTarget,
            double thermalLevel,
            boolean active
    ) {
        requireOpen();
        observe(
                sourcePosition.asLong(),
                sourcePosition,
                exhaustTarget,
                MinecraftPhysicalSourceProfile.GENERATOR,
                MinecraftPhysicalSourceProfile.GENERATOR.powerForLevel(thermalLevel),
                active);
    }

    public void observeFountain(
            BlockPos sourcePosition,
            BlockPos steamTarget,
            double thermalLevel,
            boolean active
    ) {
        requireOpen();
        observe(
                sourcePosition.asLong(),
                sourcePosition,
                steamTarget,
                MinecraftPhysicalSourceProfile.FOUNTAIN,
                MinecraftPhysicalSourceProfile.FOUNTAIN.powerForLevel(thermalLevel),
                active);
    }

    public void observeRadiator(
            BlockPos sourcePosition,
            BlockPos exhaustTarget,
            double thermalLevel,
            boolean active
    ) {
        requireOpen();
        observe(
                sourcePosition.asLong(),
                sourcePosition,
                exhaustTarget,
                MinecraftPhysicalSourceProfile.RADIATOR,
                MinecraftPhysicalSourceProfile.RADIATOR.powerForLevel(thermalLevel),
                active);
    }

    public void removeSource(BlockPos sourcePosition) {
        requireOpen();
        LiveSource source = sources.get(sourcePosition.asLong());
        if (source != null) {
            source.present = false;
            input.removeRadiationSource(source.sourceId);
            dirtySources.add(source.sourceId);
            input.requestUrgentSolve();
        }
    }

    void onBlockMutation(
            BlockPos position,
            BlockState oldState,
            BlockState newState
    ) {
        boolean oldCampfire = isCampfire(oldState);
        boolean newCampfire = isCampfire(newState);
        if (newCampfire) {
            observeCampfire(position, newState.getValue(CampfireBlock.LIT));
        } else if (oldCampfire) {
            removeSource(position);
        }
    }

    void onPageInvalidated(long sectionKey) {
        Set<Long> affected = sourcesByTargetSection.get(sectionKey);
        if (affected != null) {
            dirtySources.addAll(affected);
        }
    }

    void onPageWithdrawn(long sectionKey) {
        Set<Long> affected = sourcesByTargetSection.get(sectionKey);
        if (affected == null) {
            return;
        }
        for (long sourceId : affected) {
            LiveSource source = sources.get(sourceId);
            if (source != null) {
                source.retainedSections.remove(sectionKey);
                dirtySources.add(sourceId);
            }
        }
    }

    void onChunkLoad(LevelChunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        for (Map.Entry<Long, Set<Long>> entry : sourcesByTargetSection.entrySet()) {
            long sectionKey = entry.getKey();
            if (SectionPos.x(sectionKey) == chunkX && SectionPos.z(sectionKey) == chunkZ) {
                dirtySources.addAll(entry.getValue());
            }
        }
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof CampfireBlockEntity) {
                BlockState state = blockEntity.getBlockState();
                if (isCampfire(state)) {
                    observeCampfire(blockEntity.getBlockPos(), state.getValue(CampfireBlock.LIT));
                }
            }
        }
    }

    void beforeChunkUnload(LevelChunk chunk, long effectiveTick) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        for (LiveSource source : sources.values()) {
            if ((source.sourcePosition.getX() >> 4) == chunkX
                    && (source.sourcePosition.getZ() >> 4) == chunkZ) {
                source.present = false;
                input.removeRadiationSource(source.sourceId);
                dirtySources.add(source.sourceId);
            }
        }
        flush(effectiveTick);
    }

    void flush(long effectiveTick) {
        requireOpen();
        long topologyGeneration = input.topologyGeneration();
        if (topologyGeneration != observedTopologyGeneration) {
            observedTopologyGeneration = topologyGeneration;
            dirtySources.addAll(sources.keySet());
        }
        if (dirtySources.isEmpty()) {
            return;
        }

        List<Long> pending = new ArrayList<>(dirtySources);
        for (long sourceId : pending) {
            LiveSource source = sources.get(sourceId);
            if (source == null) {
                dirtySources.remove(sourceId);
                continue;
            }
            if (!source.present) {
                input.removeRadiationSource(source.sourceId);
                if (!source.registered
                        || timeline.offerUnload(
                                source.sourceId,
                                source.lifecycleGeneration,
                                effectiveTick) != ThermalSourceTimeline.OFFER_REJECTED) {
                    releaseTargets(source);
                    unindexTargets(source);
                    sources.remove(sourceId);
                    dirtySources.remove(sourceId);
                }
                continue;
            }

            if (source.registrationStale && source.registered) {
                if (timeline.offerUnload(
                        source.sourceId,
                        source.lifecycleGeneration,
                        effectiveTick) == ThermalSourceTimeline.OFFER_REJECTED) {
                    continue;
                }
                source.registered = false;
                source.registrationStale = false;
                source.lifecycleGeneration = nextLifecycleGeneration();
                source.offeredBindings = null;
                syncRadiationSource(source);
            }

            retainTargets(source);
            SourceBinding[] bindings = resolveBindings(source);
            if (!source.registered) {
                EmissionPort[] ports = emissionPorts(source.profile, bindings);
                long offered = timeline.offerRegister(
                        source.sourceId,
                        source.sourcePosition.asLong(),
                        source.profile.profileId(),
                        source.lifecycleGeneration,
                        ThermalSourceMode.POWER_SOURCE,
                        source.desiredPowerW,
                        source.desiredEnabled,
                        effectiveTick,
                        ports);
                if (offered == ThermalSourceTimeline.OFFER_REJECTED) {
                    continue;
                }
                source.registered = true;
                source.offeredPowerW = source.desiredPowerW;
                source.offeredEnabled = source.desiredEnabled;
                source.offeredBindings = bindings;
                dirtySources.remove(sourceId);
                continue;
            }

            boolean complete = true;
            if (Double.compare(source.offeredPowerW, source.desiredPowerW) != 0) {
                if (timeline.offerPowerChange(
                        source.sourceId,
                        source.desiredPowerW,
                        effectiveTick) == ThermalSourceTimeline.OFFER_REJECTED) {
                    complete = false;
                } else {
                    source.offeredPowerW = source.desiredPowerW;
                }
            }
            if (complete && source.offeredEnabled != source.desiredEnabled) {
                if (timeline.offerEnabledChange(
                        source.sourceId,
                        source.desiredEnabled,
                        effectiveTick) == ThermalSourceTimeline.OFFER_REJECTED) {
                    complete = false;
                } else {
                    source.offeredEnabled = source.desiredEnabled;
                }
            }
            if (complete) {
                Port[] profilePorts = source.profile.ports();
                for (int index = 0; index < bindings.length; index++) {
                    if (bindings[index].equals(source.offeredBindings[index])) {
                        continue;
                    }
                    if (timeline.offerRebind(
                            source.sourceId,
                            profilePorts[index].portId(),
                            bindings[index],
                            effectiveTick) == ThermalSourceTimeline.OFFER_REJECTED) {
                        complete = false;
                        break;
                    }
                    source.offeredBindings[index] = bindings[index];
                }
            }
            if (complete) {
                dirtySources.remove(sourceId);
            }
        }
    }

    public int sourceCount() {
        return sources.size();
    }

    BlockPos nearestEnabledGenerator(BlockPos position, double maximumDistanceSqr) {
        requireOpen();
        BlockPos nearest = null;
        double nearestDistanceSqr = maximumDistanceSqr;
        for (LiveSource source : sources.values()) {
            if (!source.present
                    || !source.desiredEnabled
                    || source.desiredPowerW <= 0.0D
                    || source.profile != MinecraftPhysicalSourceProfile.GENERATOR) {
                continue;
            }
            double distanceSqr = position.distSqr(source.sourcePosition);
            if (distanceSqr <= nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearest = source.sourcePosition;
            }
        }
        return nearest;
    }

    int appendInfraredFields(
            float[] output,
            int count,
            int maximumFields,
            int minimumX,
            int maximumX,
            int minimumZ,
            int maximumZ
    ) {
        requireOpen();
        for (LiveSource source : sources.values()) {
            if (count >= maximumFields) {
                break;
            }
            BlockPos position = source.sourcePosition;
            if (!source.present
                    || !source.desiredEnabled
                    || source.desiredPowerW <= 0.0D
                    || position.getX() < minimumX
                    || position.getX() > maximumX
                    || position.getZ() < minimumZ
                    || position.getZ() > maximumZ) {
                continue;
            }
            int offset = count * 8;
            output[offset] = position.getX() + 0.5F;
            output[offset + 1] = position.getY() + 0.5F;
            output[offset + 2] = position.getZ() + 0.5F;
            output[offset + 3] = 2.0F;
            output[offset + 4] = (float) Math.min(
                    20.0D,
                    10.0D * source.desiredPowerW / source.profile.ratedPowerW());
            output[offset + 5] = 16.0F;
            output[offset + 6] = 0.0F;
            output[offset + 7] = 0.0F;
            count++;
        }
        return count;
    }

    void replayRadiationSources() {
        requireOpen();
        for (LiveSource source : sources.values()) {
            if (source.present) {
                syncRadiationSource(source);
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (long sourceId : sources.keySet()) {
            input.removeRadiationSource(sourceId);
        }
        sources.clear();
        sourcesByTargetSection.clear();
        dirtySources.clear();
    }

    private void observe(
            long sourceId,
            BlockPos sourcePosition,
            BlockPos portAnchor,
            MinecraftPhysicalSourceProfile profile,
            double powerW,
            boolean enabled
    ) {
        LiveSource source = sources.get(sourceId);
        boolean changed = source == null;
        if (source == null) {
            source = new LiveSource(
                    sourceId,
                    nextLifecycleGeneration(),
                    sourcePosition.immutable(),
                    portAnchor.immutable(),
                    profile);
            sources.put(sourceId, source);
            indexTargets(source);
        } else if (!source.profile.equals(profile)
                || !source.portAnchor.equals(portAnchor)) {
            changed = true;
            releaseTargets(source);
            unindexTargets(source);
            source.portAnchor = portAnchor.immutable();
            source.profile = profile;
            source.registrationStale = source.registered;
            indexTargets(source);
        }
        changed |= !source.present
                || Double.compare(source.desiredPowerW, powerW) != 0
                || source.desiredEnabled != enabled;
        source.present = true;
        source.desiredPowerW = powerW;
        source.desiredEnabled = enabled;
        if (changed) {
            syncRadiationSource(source);
            dirtySources.add(sourceId);
            input.requestUrgentSolve();
        }
    }

    private void syncRadiationSource(LiveSource source) {
        input.upsertRadiationSource(
                source.sourceId,
                source.lifecycleGeneration,
                source.sourcePosition,
                source.profile,
                source.desiredPowerW,
                source.desiredEnabled);
    }

    private void retainTargets(LiveSource source) {
        for (Port port : source.profile.ports()) {
            if (port.kind() != PortKind.AIR_FACE) {
                continue;
            }
            BlockPos target = target(source, port);
            long sectionKey = sectionKey(target);
            if (!source.retainedSections.contains(sectionKey)
                    && input.retainPhysicalSourcePage(target, maximumColdSourcePages)) {
                source.retainedSections.add(sectionKey);
            }
        }
    }

    private void releaseTargets(LiveSource source) {
        for (long sectionKey : List.copyOf(source.retainedSections)) {
            input.releasePhysicalSourcePage(sectionKey);
        }
        source.retainedSections.clear();
    }

    private SourceBinding[] resolveBindings(LiveSource source) {
        Port[] ports = source.profile.ports();
        SourceBinding[] resolved = new SourceBinding[ports.length];
        SourceBinding redistributionTarget = null;
        for (int index = 0; index < ports.length; index++) {
            Port port = ports[index];
            if (port.kind() == PortKind.INTERNAL_HEAT) {
                resolved[index] = SourceBinding.internalReservoir(sinkId(source.sourceId, port));
            } else if (port.kind() == PortKind.DECLARED_LOSS) {
                resolved[index] = SourceBinding.declaredLoss(sinkId(source.sourceId, port));
            } else {
                BlockPos target = target(source, port);
                MinecraftThermalTopologyApplier.PortResolution resolution =
                        input.resolvePhysicalSourcePort(target, port.targetFace());
                if (resolution.status()
                        == MinecraftThermalTopologyApplier.PortResolutionStatus.RESOLVED) {
                    resolved[index] = resolution.binding();
                    if (redistributionTarget == null) {
                        redistributionTarget = resolution.binding();
                    }
                } else if (resolution.status()
                        == MinecraftThermalTopologyApplier.PortResolutionStatus.TOPOLOGY_UNAVAILABLE) {
                    resolved[index] = SourceBinding.degradedLoss(
                            sinkId(source.sourceId, port));
                }
            }
        }
        for (int index = 0; index < ports.length; index++) {
            if (resolved[index] != null) {
                continue;
            }
            Port port = ports[index];
            MissingPortPolicy policy = source.profile.missingPortPolicy();
            if (policy == MissingPortPolicy.REDISTRIBUTE_TO_VALID_PORTS
                    && redistributionTarget != null) {
                resolved[index] = redistributionTarget;
            } else if (policy == MissingPortPolicy.INTERNAL_HEAT) {
                resolved[index] = SourceBinding.internalReservoir(
                        sinkId(source.sourceId, port));
            } else {
                resolved[index] = SourceBinding.declaredLoss(
                        sinkId(source.sourceId, port));
            }
        }
        return resolved;
    }

    private static EmissionPort[] emissionPorts(
            MinecraftPhysicalSourceProfile profile,
            SourceBinding[] bindings
    ) {
        Port[] profilePorts = profile.ports();
        EmissionPort[] result = new EmissionPort[profilePorts.length];
        for (int index = 0; index < profilePorts.length; index++) {
            Port port = profilePorts[index];
            result[index] = EmissionPort.of(
                    port.portId(), port.channel(), port.powerShare(), bindings[index]);
        }
        return result;
    }

    private void indexTargets(LiveSource source) {
        for (Port port : source.profile.ports()) {
            if (port.kind() == PortKind.AIR_FACE) {
                long sectionKey = sectionKey(target(source, port));
                sourcesByTargetSection
                        .computeIfAbsent(sectionKey, ignored -> new HashSet<>())
                        .add(source.sourceId);
            }
        }
    }

    private void unindexTargets(LiveSource source) {
        for (Set<Long> indexed : sourcesByTargetSection.values()) {
            indexed.remove(source.sourceId);
        }
        sourcesByTargetSection.values().removeIf(Set::isEmpty);
    }

    private static BlockPos target(LiveSource source, Port port) {
        return source.portAnchor.offset(port.offsetX(), port.offsetY(), port.offsetZ());
    }

    private static long sectionKey(BlockPos position) {
        return SectionPos.asLong(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getY()),
                SectionPos.blockToSectionCoord(position.getZ()));
    }

    private static long sinkId(long sourceId, Port port) {
        return Long.rotateLeft(sourceId, 17) ^ Integer.toUnsignedLong(port.portId() + 1);
    }

    private static boolean isCampfire(BlockState state) {
        return state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private int nextLifecycleGeneration() {
        int generation = nextLifecycleGeneration;
        nextLifecycleGeneration = Math.incrementExact(nextLifecycleGeneration);
        return generation;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("physical source manager is closed");
        }
    }

    private static final class LiveSource {
        private final long sourceId;
        private int lifecycleGeneration;
        private final BlockPos sourcePosition;
        private BlockPos portAnchor;
        private MinecraftPhysicalSourceProfile profile;
        private final Set<Long> retainedSections = new HashSet<>();
        private boolean present;
        private boolean registered;
        private boolean registrationStale;
        private double desiredPowerW;
        private boolean desiredEnabled;
        private double offeredPowerW;
        private boolean offeredEnabled;
        private SourceBinding[] offeredBindings;

        private LiveSource(
                long sourceId,
                int lifecycleGeneration,
                BlockPos sourcePosition,
                BlockPos portAnchor,
                MinecraftPhysicalSourceProfile profile
        ) {
            this.sourceId = sourceId;
            this.lifecycleGeneration = lifecycleGeneration;
            this.sourcePosition = sourcePosition;
            this.portAnchor = portAnchor;
            this.profile = profile;
        }
    }
}
