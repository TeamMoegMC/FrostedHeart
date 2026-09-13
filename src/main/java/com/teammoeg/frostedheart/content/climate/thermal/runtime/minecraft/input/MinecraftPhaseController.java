/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftStateThermalTable;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.MaterialChanges;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.GameRules;

import java.util.ArrayDeque;

/** Main-thread application and acknowledgement of worker phase requests. */
public final class MinecraftPhaseController {
    private record Mutation(long position, byte cause) {}
    private static final ThreadLocal<Mutation> APPLYING = new ThreadLocal<>();

    public static byte materialChangeCause(BlockState before, BlockState after, int x, int y, int z) {
        Mutation mutation = APPLYING.get();
        if (mutation != null && mutation.position() == BlockPos.asLong(x, y, z)) return mutation.cause();
        if (before.getBlock() != after.getBlock()) return MaterialChanges.REPLACE;
        return before.getFluidState() != after.getFluidState() ? MaterialChanges.MASS_CHANGE : 0;
    }

    public static boolean applyGameplayTransition(ServerLevel level, BlockPos position, BlockState target) {
        return setMaterialBlock(level, position, target, MaterialChanges.GAMEPLAY_TRANSITION);
    }

    private static boolean setMaterialBlock(ServerLevel level, BlockPos position, BlockState target, byte cause) {
        Mutation previous = APPLYING.get();
        APPLYING.set(new Mutation(position.asLong(), cause));
        try {
            return level.setBlockAndUpdate(position, target);
        } finally {
            if (previous == null) APPLYING.remove();
            else APPLYING.set(previous);
        }
    }
    private enum Outcome {
        APPLIED,
        REJECTED,
        RETRY
    }

    private final ServerLevel level;
    private final MinecraftPageManager pages;
    private final MinecraftStateThermalTable states;
    private final ThermalSignatureTable signatures;
    private final MaterialBoundaryRegistry materials;
    private DimensionInputAccumulator accumulator;
    private final int maximumPerTick;
    private final ArrayDeque<PhaseTransitionRuntime.Request> pending =
            new ArrayDeque<>();
    private final com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticFieldIndex.Sample fieldSample =
            new com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticFieldIndex.Sample();
    private final BlockPos.MutableBlockPos neighborPosition = new BlockPos.MutableBlockPos();

    public MinecraftPhaseController(
            ServerLevel level,
            MinecraftPageManager pages,
            MinecraftStateThermalTable states,
            ThermalSignatureTable signatures,
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
        this.states = states;
        this.signatures = signatures;
        this.materials = materials;
        this.accumulator = accumulator;
        this.maximumPerTick = maximumPerTick;
    }

    public void accept(PhaseTransitionRuntime.Request[] requests) {
        for (PhaseTransitionRuntime.Request request : requests) {
            pending.addLast(request);
        }
    }

    public void replaceAccumulator(DimensionInputAccumulator next) {
        accumulator = next;
        pending.clear();
    }

    public void tick() {
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

    public boolean ownsHeatingTransition(
            BlockPos position,
            int profileId
    ) {
        ThermalPageHandle page = pages.handle(sectionKey(position));
        if (page == null) {
            return false;
        }
        PagePublication publication = page.currentPublication();
        if (publication == null) publication = page.lastPublication();
        return publication != null && publication.hasPhaseCandidate(
                position.getX(), position.getY(), position.getZ(), profileId);
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
                || !pages.matchesMaterialRequest(request)) {
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
        if (profile == null) {
            return Outcome.REJECTED;
        }
        BlockState state = chunk.getBlockState(position);
        int signatureId = states.signatureId(state);
        if (!signatures.valid(signatureId)
                || signatures.materialProfileId(signatureId) != profile.id()) {
            return Outcome.REJECTED;
        }
        int randomTickSpeed = level.getGameRules().getInt(
                GameRules.RULE_RANDOMTICKING);
        if (randomTickSpeed <= 0) {
            return Outcome.RETRY;
        }
        var transition = profile.thermalLaw().transition(request.materialBranch());
        if (transition == null || transition.targetStateId() != request.targetStateId()) return Outcome.REJECTED;
        if (transition.heating() && state.is(BlockTags.ICE)
                && level.getBiome(position).is(FHTags.Biomes.ICE_DO_NOT_SMELT.tag)) return Outcome.RETRY;
        if (!transition.heating()) {
            double natural = WorldTemperature.naturalBlock(level, position);
            MinecraftThermalInput.gameplayPassiveEnvironment(level, position, natural, fieldSample);
            if (fieldSample.present() && fieldSample.guaranteedFloor(natural) >= transition.temperatureC()) return Outcome.RETRY;
            if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock
                    && state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                boolean edge = !level.isWaterAt(neighborPosition.set(position).move(net.minecraft.core.Direction.WEST))
                        || !level.isWaterAt(neighborPosition.set(position).move(net.minecraft.core.Direction.EAST))
                        || !level.isWaterAt(neighborPosition.set(position).move(net.minecraft.core.Direction.NORTH))
                        || !level.isWaterAt(neighborPosition.set(position).move(net.minecraft.core.Direction.SOUTH));
                if (!edge) return Outcome.RETRY;
            }
        }
        BlockState target = net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.byId(transition.targetStateId());
        if (target == null) return Outcome.REJECTED;
        return setMaterialBlock(level, position, target, MaterialChanges.THERMAL_TRANSITION)
                ? Outcome.APPLIED : Outcome.REJECTED;
    }

    private static long sectionKey(BlockPos position) {
        return SectionPos.asLong(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getY()),
                SectionPos.blockToSectionCoord(position.getZ()));
    }
}
