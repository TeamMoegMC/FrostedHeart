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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.Objects;

/** Main-thread application and acknowledgement of worker phase requests. */
final class MinecraftPhaseController {
    private final ServerLevel level;
    private final MinecraftPageManager pages;
    private final MinecraftSignatureCapture capture;
    private final ThermalSignatureRegistry signatures;
    private final MaterialBoundaryRegistry materials;
    private DimensionInputAccumulator accumulator;
    private final MinecraftPhaseTransitionHandler customHandler;
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
            MinecraftPhaseTransitionHandler customHandler,
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
        this.customHandler = customHandler;
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

    private MinecraftPhaseTransitionHandler.Outcome apply(
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
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(request.blockX()),
                SectionPos.blockToSectionCoord(request.blockZ()));
        if (chunk == null || level.isOutsideBuildHeight(request.blockY())) {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        MaterialBoundaryRegistry.Profile profile =
                materials.profileOrNull(request.profileId());
        if (profile == null || profile.model()
                != MaterialBoundaryRegistry.Model.PHASE_RESERVOIR) {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        int signatureId = capture.resolveSignatureId(
                request.blockX(), request.blockY(), request.blockZ());
        ResolvedThermalSignature signature =
                signatures.signatureOrNull(signatureId);
        if (signature == null
                || signature.materialProfileId() != profile.id()) {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        int randomTickSpeed = level.getGameRules().getInt(
                GameRules.RULE_RANDOMTICKING);
        if (!allowsAutomaticMutation(
                profile.transitionMutationPolicy(), randomTickSpeed)) {
            return profile.transitionMutationPolicy()
                    == MaterialBoundaryRegistry.TransitionMutationPolicy.NONE
                    ? MinecraftPhaseTransitionHandler.Outcome.REJECTED
                    : MinecraftPhaseTransitionHandler.Outcome.RETRY;
        }
        BlockState state = chunk.getBlockState(position);
        return switch (profile.transitionAction()) {
            case REMOVE_ONE_SNOW_LAYER -> removeSnow(position, state);
            case MELT_ICE_TO_WATER -> meltIce(position, state);
            case APPLY_STATE_TRANSITION_RECIPE ->
                    applyRecipe(position, state, profile);
            case CUSTOM -> customHandler.apply(
                    level, position, state, profile);
            case NONE -> MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        };
    }

    private MinecraftPhaseTransitionHandler.Outcome applyRecipe(
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
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        if (state.is(BlockTags.ICE)
                && level.getBiome(position).is(
                        FHTags.Biomes.ICE_DO_NOT_SMELT.tag)) {
            return MinecraftPhaseTransitionHandler.Outcome.RETRY;
        }
        return level.setBlockAndUpdate(position, transition.targetBlock())
                ? MinecraftPhaseTransitionHandler.Outcome.APPLIED
                : MinecraftPhaseTransitionHandler.Outcome.REJECTED;
    }

    private MinecraftPhaseTransitionHandler.Outcome removeSnow(
            BlockPos position,
            BlockState state
    ) {
        BlockState replacement;
        if (state.is(Blocks.SNOW)
                && state.hasProperty(SnowLayerBlock.LAYERS)) {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            replacement = layers > 1
                    ? state.setValue(SnowLayerBlock.LAYERS, layers - 1)
                    : Blocks.AIR.defaultBlockState();
        } else if (state.is(Blocks.SNOW_BLOCK)) {
            replacement = Blocks.AIR.defaultBlockState();
        } else {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        return level.setBlockAndUpdate(position, replacement)
                ? MinecraftPhaseTransitionHandler.Outcome.APPLIED
                : MinecraftPhaseTransitionHandler.Outcome.REJECTED;
    }

    private MinecraftPhaseTransitionHandler.Outcome meltIce(
            BlockPos position,
            BlockState state
    ) {
        if (!state.is(Blocks.ICE) && !state.is(Blocks.FROSTED_ICE)) {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        return level.setBlockAndUpdate(
                position, Blocks.WATER.defaultBlockState())
                ? MinecraftPhaseTransitionHandler.Outcome.APPLIED
                : MinecraftPhaseTransitionHandler.Outcome.REJECTED;
    }

    static boolean allowsAutomaticMutation(
            MaterialBoundaryRegistry.TransitionMutationPolicy policy,
            int randomTickSpeed
    ) {
        Objects.requireNonNull(policy, "policy");
        if (randomTickSpeed < 0) {
            throw new IllegalArgumentException(
                    "randomTickSpeed must be non-negative");
        }
        return switch (policy) {
            case IGNORE_RANDOM_TICK_SPEED -> true;
            case RESPECT_RANDOM_TICK_SPEED -> randomTickSpeed > 0;
            case NONE, SCRIPT_CONTROLLED -> false;
        };
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
