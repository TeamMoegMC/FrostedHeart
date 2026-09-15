/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.MaterialChanges;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;

import java.util.ArrayDeque;

/** Main-thread application and acknowledgement of worker phase requests. */
public final class MinecraftPhaseController {
    private static final QueryPublication.MutableMaterialSample DORMANT_SAMPLE = new QueryPublication.MutableMaterialSample();
    public enum PhaseAttempt { GAMEPLAY, DEFERRED, CHANGED }
    public static final class Mutation {
        private final long position, tick;
        private final byte cause;
        private final BlockState before, target;
        private final double energyJ;
        private boolean superseded;

        public Mutation(long position, byte cause, BlockState before, BlockState target, double energyJ, long tick) {
            this.position = position; this.cause = cause; this.before = before;
            this.target = target; this.energyJ = energyJ; this.tick = tick;
        }
        public double energyJ() { return energyJ; }
        public long tick() { return tick; }
    }
    private static final ThreadLocal<Mutation> APPLYING = new ThreadLocal<>();

    /** Main-thread random updates share the same ownership and phase rules as worker commits. */
    public static PhaseAttempt tryAtRandomTick(ServerLevel level, LevelChunk chunk, BlockPos position, BlockState state) {
        if (chunk == null || level.isOutsideBuildHeight(position)) return PhaseAttempt.DEFERRED;
        int sectionY = position.getY() >> 4;
        int block = (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8;
        var profiles = MinecraftThermalProfiles.prepare();
        int signature = profiles.states().signatureId(state);
        int profileId = profiles.signatures().materialProfileId(signature);
        var profile = profiles.materials().profileOrNull(profileId);
        if (profile == null) return PhaseAttempt.GAMEPLAY;
        var law = profile.thermalLaw();
        if (law.heating() == null && law.cooling() == null) return PhaseAttempt.GAMEPLAY;
        var section = chunk.getSection(chunk.getSectionIndex(position.getY()));
        var owner = ((MinecraftThermalSectionAttachment) (Object) section).frostedheart$getThermalInputOwner();
        if (owner != null && owner.ownsMaterialPosition(block)) {
            if (law.heating() != null
                    && MinecraftGameplayFields.guaranteedFloor(level, position) >= law.heating().temperatureC())
                MinecraftThermalInput.requestGameplayPhase(level, position, state, MaterialThermalLaw.HEATING);
            return PhaseAttempt.DEFERRED;
        }
        var stored = ((MinecraftThermalChunkAttachment) (Object) chunk).frostedheart$getDormantThermalState();
        var sample = DORMANT_SAMPLE;
        sample.clear();
        var read = stored == null ? DormantChunkThermalState.MaterialRead.MISSING
                : stored.readMaterial(sectionY, block, Block.BLOCK_STATE_REGISTRY.getId(state), sample);
        if (read == DormantChunkThermalState.MaterialRead.MISSING) {
            double natural = WorldTemperature.naturalBlock(level, position);
            double temperature = MinecraftThermalInput.gameplayPassiveEnvironment(level, position, natural);
            var edge = law.heating() != null && temperature >= law.heating().temperatureC() ? law.heating()
                    : law.cooling() != null && temperature <= law.cooling().temperatureC() ? law.cooling() : null;
            if (edge == null) return PhaseAttempt.GAMEPLAY;
            var definition = profiles.transitionData(profileId, edge.heating());
            if (!canTransition(level, position, state, edge, definition)) return PhaseAttempt.GAMEPLAY;
            boolean changed = setMaterialBlock(level, position, state, definition.target(), MaterialChanges.GAMEPLAY_TRANSITION);
            if (changed) playEffect(level, position, definition.effect());
            return changed ? PhaseAttempt.CHANGED : PhaseAttempt.GAMEPLAY;
        }
        if (read == DormantChunkThermalState.MaterialRead.MISMATCH) return PhaseAttempt.DEFERRED;
        stored.projectMaterial(level, position, law, sample, NEIGHBOR.get());
        double energy = sample.enthalpyJ();
        var edge = law.transition(sample.branch());
        if (law.heating() != null && MinecraftGameplayFields.guaranteedFloor(level, position) >= law.heating().temperatureC()) {
            edge = law.heating();
            energy = Math.max(energy, edge.targetEnthalpyJ());
        }
        if (edge == null || !edge.complete(energy)) return PhaseAttempt.DEFERRED;
        return applyDormantTransition(level, position, state, edge, energy, level.getGameTime())
                ? PhaseAttempt.CHANGED : PhaseAttempt.DEFERRED;
    }

    public static byte materialChangeCause(BlockState before, BlockState after, int x, int y, int z) {
        Mutation mutation = APPLYING.get();
        if (mutation != null && mutation.position == BlockPos.asLong(x, y, z)) {
            if (!mutation.superseded && mutation.before == before && mutation.target == after) return mutation.cause;
            mutation.superseded = true;
        }
        if (before.getBlock() != after.getBlock()) return MaterialChanges.REPLACE;
        return before.getFluidState() != after.getFluidState() ? MaterialChanges.MASS_CHANGE : 0;
    }

    private static boolean setMaterialBlock(ServerLevel level, BlockPos position, BlockState before, BlockState target, byte cause) {
        return setMaterialBlock(level, position, target, new Mutation(position.asLong(), cause, before, target, Double.NaN, 0));
    }

    private static boolean setMaterialBlock(ServerLevel level, BlockPos position, BlockState target, Mutation mutation) {
        Mutation previous = APPLYING.get();
        APPLYING.set(mutation);
        try {
            boolean changed = level.setBlockAndUpdate(position, target);
            if (previous != null && previous.position == mutation.position && (changed || mutation.superseded))
                previous.superseded = true;
            return changed;
        } finally {
            if (previous == null) APPLYING.remove();
            else APPLYING.set(previous);
        }
    }

    /** Valid only during the actual matching chunk mutation, before neighbor callbacks run. */
    public static Mutation dormantTransition(BlockPos position, BlockState before, BlockState target) {
        Mutation mutation = APPLYING.get();
        return mutation != null && !mutation.superseded && Double.isFinite(mutation.energyJ) && mutation.position == position.asLong()
                && mutation.before == before && mutation.target == target ? mutation : null;
    }

    public static boolean applyDormantTransition(ServerLevel level, BlockPos position, BlockState state,
            MaterialThermalLaw.Transition edge, double energyJ, long tick) {
        var profiles = MinecraftThermalProfiles.prepare();
        int profileId = profiles.signatures().materialProfileId(profiles.states().signatureId(state));
        if (!canTransition(level, position, state, edge, profiles.transitionData(profileId, edge.heating()))) return false;
        BlockState target = net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.byId(edge.targetStateId());
        return target != null && setMaterialBlock(level, position, target,
                new Mutation(position.asLong(), MaterialChanges.THERMAL_TRANSITION, state, target, energyJ, tick));
    }
    private enum Outcome {
        APPLIED,
        REJECTED,
        RETRY
    }

    private final ServerLevel level;
    private final MinecraftPageManager pages;
    private final MinecraftThermalProfiles.Snapshot profiles;
    private DimensionInputAccumulator accumulator;
    private final int maximumPerTick;
    private final ArrayDeque<PhaseTransitionRuntime.Request> pending =
            new ArrayDeque<>();
    private static final ThreadLocal<BlockPos.MutableBlockPos> NEIGHBOR = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    public MinecraftPhaseController(
            ServerLevel level,
            MinecraftPageManager pages,
            MinecraftThermalProfiles.Snapshot profiles,
            DimensionInputAccumulator accumulator,
            int maximumPerTick
    ) {
        if (maximumPerTick <= 0) {
            throw new IllegalArgumentException(
                    "phase mutation limit must be positive");
        }
        this.level = level;
        this.pages = pages;
        this.profiles = profiles;
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
                profiles.materials().profileOrNull(request.profileId());
        if (profile == null) {
            return Outcome.REJECTED;
        }
        BlockState state = chunk.getBlockState(position);
        int signatureId = profiles.states().signatureId(state);
        if (!profiles.signatures().valid(signatureId)
                || profiles.signatures().materialProfileId(signatureId) != profile.id()) {
            return Outcome.REJECTED;
        }
        var transition = profile.thermalLaw().transition(request.materialBranch());
        if (transition == null || transition.targetStateId() != request.targetStateId()) return Outcome.REJECTED;
        if (!canTransition(level, position, state, transition, profiles.transitionData(profile.id(), transition.heating()))) return Outcome.RETRY;
        BlockState target = net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.byId(transition.targetStateId());
        if (target == null) return Outcome.REJECTED;
        return setMaterialBlock(level, position, state, target, MaterialChanges.THERMAL_TRANSITION)
                ? Outcome.APPLIED : Outcome.REJECTED;
    }

    private static boolean canTransition(ServerLevel level, BlockPos position, BlockState state,
            MaterialThermalLaw.Transition transition, StateTransitionData.Edge definition) {
        if (definition == null || Block.BLOCK_STATE_REGISTRY.getId(definition.target()) != transition.targetStateId()) return false;
        if (level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) <= 0) return false;
        LevelChunk chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
        if (chunk == null || chunk.getBlockState(position) != state) return false;
        var conditions = definition.worldConditions();
        if (position.getY() <= conditions.minYExclusive()) return false;
        if (!transition.heating() && MinecraftGameplayFields.guaranteedFloor(level, position) >= transition.temperatureC()) return false;
        if (conditions.fluidBoundary() != null) {
            if (!level.isAreaLoaded(position, 1)) return false;
            var neighbor = NEIGHBOR.get();
            var fluid = conditions.fluidBoundary();
            boolean boundary = !level.getFluidState(neighbor.set(position).move(Direction.WEST)).is(fluid)
                    || !level.getFluidState(neighbor.set(position).move(Direction.EAST)).is(fluid)
                    || !level.getFluidState(neighbor.set(position).move(Direction.NORTH)).is(fluid)
                    || !level.getFluidState(neighbor.set(position).move(Direction.SOUTH)).is(fluid);
            if (!boundary) return false;
        }
        return conditions.excludedBiome() == null || !level.getBiome(position).is(conditions.excludedBiome());
    }

    private static void playEffect(ServerLevel level, BlockPos position, StateTransitionData.Effect effect) {
        double x = position.getX()+.5, y = position.getY()+.5, z = position.getZ()+.5;
        if (effect == StateTransitionData.Effect.NONE || !level.hasNearbyAlivePlayer(x, y, z, 12)) return;
        switch (effect) {
            case MELTING -> {
                level.sendParticles(ParticleTypes.DRIPPING_WATER, x, y, z, 8, .3, .3, .3, 0);
                level.playSound(null, position, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .5F,
                        2.6F + (level.random.nextFloat()-level.random.nextFloat())*.8F);
            }
            case EVAPORATION, SUBLIMATION -> {
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 12, .25, .25, .25, .05);
                level.playSound(null, position, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .4F, 2+level.random.nextFloat()*.4F);
            }
            case FREEZING -> level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 10, .3, .3, .3, 0);
            case CONDENSATION, DEPOSITION -> {
                level.sendParticles(ParticleTypes.DRIPPING_WATER, x, position.getY()+.8, z, 15, .4, .1, .4, 0);
                level.playSound(null, position, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.AMBIENT, .5F, 1);
            }
            default -> { }
        }
    }

    private static long sectionKey(BlockPos position) {
        return SectionPos.asLong(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getY()),
                SectionPos.blockToSectionCoord(position.getZ()));
    }
}
