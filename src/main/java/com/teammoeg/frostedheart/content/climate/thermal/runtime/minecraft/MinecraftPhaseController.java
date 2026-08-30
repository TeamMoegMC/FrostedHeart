/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.Objects;

/** Main-thread application and acknowledgement of worker phase requests. */
final class MinecraftPhaseController {
    private enum Outcome {
        APPLIED,
        REJECTED,
        RETRY
    }

    private final ServerLevel level;
    private final MinecraftPageManager pages;
    private final MinecraftSignatureCapture capture;
    private final ThermalSignatureRegistry signatures;
    private final MaterialBoundaryRegistry materials;
    private DimensionInputAccumulator accumulator;
    private final int maximumPerTick;
    private final ArrayDeque<PhaseTransitionRuntime.Request> pending =
            new ArrayDeque<>();

    MinecraftPhaseController(
            ServerLevel level,
            MinecraftPageManager pages,
            MinecraftSignatureCapture capture,
            ThermalSignatureRegistry signatures,
            MaterialBoundaryRegistry materials,
            DimensionInputAccumulator accumulator,
            int maximumPerTick
    ) {
        if (maximumPerTick <= 0) {
            throw new IllegalArgumentException(
                    "phase mutation limit must be positive");
        }
        this.level = level;
        this.pages = pages;
        this.capture = capture;
        this.signatures = signatures;
        this.materials = materials;
        this.accumulator = accumulator;
        this.maximumPerTick = maximumPerTick;
    }

    void accept(PhaseTransitionRuntime.Request[] requests) {
        for (PhaseTransitionRuntime.Request request : requests) {
            pending.addLast(request);
        }
    }

    void replaceAccumulator(DimensionInputAccumulator next) {
        accumulator = next;
        pending.clear();
    }

    void tick() {
        int remaining = maximumPerTick;
        while (remaining-- > 0 && !pending.isEmpty()) {
            PhaseTransitionRuntime.Request request = pending.removeFirst();
            accumulator.acknowledgePhase(
                    request,
                    switch (apply(request)) {
                        case APPLIED ->
                                PhaseTransitionRuntime.AckOutcome.APPLIED;
                        case REJECTED ->
                                PhaseTransitionRuntime.AckOutcome.REJECTED;
                        case RETRY ->
                                PhaseTransitionRuntime.AckOutcome.RETRY;
                    });
        }
    }

    boolean ownsHeatingTransition(
            BlockPos position,
            int profileId
    ) {
        ThermalPageHandle page = pages.handle(sectionKey(position));
        return page != null && hasCandidate(
                page, position.getX(), position.getY(), position.getZ(),
                profileId);
    }

    private Outcome apply(
            PhaseTransitionRuntime.Request request
    ) {
        BlockPos position = new BlockPos(
                request.blockX(), request.blockY(), request.blockZ());
        ThermalPageHandle page = pages.handle(sectionKey(position));
        if (page == null
                || page.lifecycleGeneration()
                        != request.lifecycleGeneration()
                || !hasCandidate(
                        page,
                        request.blockX(), request.blockY(), request.blockZ(),
                        request.profileId())) {
            return Outcome.REJECTED;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(request.blockX()),
                SectionPos.blockToSectionCoord(request.blockZ()));
        if (chunk == null || level.isOutsideBuildHeight(request.blockY())) {
            return Outcome.REJECTED;
        }
        MaterialBoundaryRegistry.Profile profile =
                materials.profileOrNull(request.profileId());
        if (profile == null || profile.model()
                != MaterialBoundaryRegistry.Model.PHASE_RESERVOIR) {
            return Outcome.REJECTED;
        }
        int signatureId = capture.resolveSignatureId(
                request.blockX(), request.blockY(), request.blockZ());
        ResolvedThermalSignature signature =
                signatures.signatureOrNull(signatureId);
        if (signature == null
                || signature.materialProfileId() != profile.id()) {
            return Outcome.REJECTED;
        }
        int randomTickSpeed = level.getGameRules().getInt(
                GameRules.RULE_RANDOMTICKING);
        if (randomTickSpeed <= 0) {
            return Outcome.RETRY;
        }
        BlockState state = chunk.getBlockState(position);
        return applyRecipe(position, state, profile);
    }

    private Outcome applyRecipe(
            BlockPos position,
            BlockState state,
            MaterialBoundaryRegistry.Profile profile
    ) {
        StateTransitionData data = StateTransitionData.getData(state);
        StateTransitionData.HeatingTransition transition = data == null
                ? null : data.heatingTransition(state);
        if (data == null || !data.willTransit() || data.heatCapacity() <= 0
                || transition == null
                || Double.compare(
                        transition.temperatureC(),
                        profile.transitionTemperatureC()) != 0) {
            return Outcome.REJECTED;
        }
        if (state.is(BlockTags.ICE)
                && level.getBiome(position).is(
                        FHTags.Biomes.ICE_DO_NOT_SMELT.tag)) {
            return Outcome.RETRY;
        }
        return level.setBlockAndUpdate(position, transition.targetBlock())
                ? Outcome.APPLIED
                : Outcome.REJECTED;
    }

    private static boolean hasCandidate(
            ThermalPageHandle page,
            int blockX,
            int blockY,
            int blockZ,
            int profileId
    ) {
        PagePublication publication = page.currentPublication();
        return publication != null && publication.hasPhaseCandidate(
                blockX, blockY, blockZ, profileId);
    }

    private static long sectionKey(BlockPos position) {
        return SectionPos.asLong(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getY()),
                SectionPos.blockToSectionCoord(position.getZ()));
    }
}
