/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Main-thread dimension lifecycle, fixed-cut transport, and gameplay query facade. */
public final class MinecraftThermalInput implements AutoCloseable {
    public static final int QUERY_STALE_GEOMETRY = 1;
    public static final int QUERY_DEGRADED_TOPOLOGY = 1 << 1;
    public static final int QUERY_PUBLICATION_MISS = 1 << 2;
    public static final int QUERY_PUBLICATION_STALE = 1 << 3;
    public static final int QUERY_NO_PAGE = 1 << 4;
    public static final int QUERY_NO_AIR_COMPONENT = 1 << 5;
    public static final int QUERY_RADIATION_UNAVAILABLE = 1 << 6;
    public static final int QUERY_RADIATION_BUDGET_LIMITED = 1 << 7;
    public static final int QUERY_RADIATION_UNRESOLVED = 1 << 8;
    public static final int QUERY_ANALYTIC_FIELD_APPLIED = 1 << 11;

    private static final int MAX_PUBLICATION_AGE_TICKS = 40;
    private static final float[] NO_INFRARED_FIELDS = new float[0];
    private static final double FAR_FIELD_CONDUCTANCE_W_PER_K =
            7_747.2298793470545D;
    private static final ThermalMemoryBudget MEMORY =
            new ThermalMemoryBudget(
                    128L * 1024L * 1024L,
                    4L * 1024L * 1024L);
    private static final RadiationService.Parameters RADIATION_PARAMETERS =
            new RadiationService.Parameters(
                    1_024, 128, 64, 8, 24, 8, 256,
                    16.0D, 0.1D, 0.5D, 0.1D, 0.9D, 1.62D);
    private static final IdentityHashMap<ServerLevel, MinecraftThermalInput>
            ACTIVE = new IdentityHashMap<>();
    private static final AtomicLong NEXT_GENERATION =
            new AtomicLong(1_000L);

    private final ServerLevel level;
    private final Thread mainThread;
    private final MinecraftThermalProfiles.Snapshot profiles;
    private final double referenceTemperatureC;
    private final ThermalAnalyticFieldIndex analyticFields =
            new ThermalAnalyticFieldIndex();
    private final MinecraftSignatureCapture signatureCapture;
    private final MinecraftEnvironmentCapture environment;
    private final MinecraftPageManager pages;
    private final PhysicalSourceSpatialIndex physicalSources;
    private final MinecraftPhaseController phase;
    private final MinecraftRadiationOcclusion radiationOcclusion;
    private final RadiationService radiation;
    private final QueryPublication.MutableSample querySample =
            new QueryPublication.MutableSample();
    private final RadiationService.MutableSample radiationSample =
            new RadiationService.MutableSample();
    private final ThermalEnvironmentSample passiveScratch =
            new ThermalEnvironmentSample();
    private final ThermalEnvironmentSample townScratch =
            new ThermalEnvironmentSample();
    private final BlockPos.MutableBlockPos townPosition =
            new BlockPos.MutableBlockPos();

    private long dimensionGeneration;
    private DimensionInputAccumulator accumulator;
    private QueryPublication queryPublication;
    private ThermalDimensionMailbox mailbox;
    private ThermalInputBatch inFlight;
    private ThermalInputBatch pendingSubmission;
    private long lastCompletedTargetTick;
    private boolean closed;

    private MinecraftThermalInput(
            ServerLevel level,
            double initialTemperatureC
    ) {
        this.level = level;
        referenceTemperatureC = initialTemperatureC;
        mainThread = Thread.currentThread();
        profiles = MinecraftThermalProfiles.prepare();
        long initialTick = alignedTick(level.getGameTime());
        dimensionGeneration = nextGeneration();
        accumulator = new DimensionInputAccumulator(
                dimensionGeneration, initialTick);
        lastCompletedTargetTick = initialTick;
        signatureCapture = new MinecraftSignatureCapture(
                level,
                profiles.dispatcher(),
                profiles.signatures(),
                profiles.signatureIdsByState());
        environment = new MinecraftEnvironmentCapture(level, accumulator);
        pages = new MinecraftPageManager(
                this, level, accumulator, signatureCapture, environment);
        physicalSources = new PhysicalSourceSpatialIndex(
                accumulator, pages, 64);
        createWorker(initialTick, initialTemperatureC);
        phase = new MinecraftPhaseController(
                level, pages, signatureCapture, profiles.signatures(),
                profiles.materials(), accumulator,
                MinecraftPhaseTransitionHandler.rejectCustomActions(), 8);
        radiationOcclusion = new MinecraftRadiationOcclusion(level, 1_024);
        pages.attachMutationConsumers(physicalSources, radiationOcclusion);
        radiation = RadiationService.tryCreate(
                RADIATION_PARAMETERS,
                physicalSources,
                radiationOcclusion,
                MEMORY.createDimensionBudget(
                        RadiationService.projectedMaximumBytes(
                                RADIATION_PARAMETERS),
                        0L));
    }

    private void createWorker(
            long initialTick,
            double referenceTemperatureC
    ) {
        QueryPublication publication = QueryPublication.tryCreate(
                MEMORY.createDimensionBudget(
                        16L * 1024L * 1024L,
                        1L * 1024L * 1024L),
                256);
        if (publication == null) {
            throw new IllegalStateException(
                    "thermal query publication memory was refused");
        }
        ThermalDimensionEngine engine = null;
        try {
            ThermalTopologyParameters topology = new ThermalTopologyParameters(
                    0, 64, 1_200.0D,
                    referenceTemperatureC, referenceTemperatureC,
                    1.0D, 0.25D, true,
                    new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D),
                    1_024, 8);
            ThermalDimensionLimits limits = new ThermalDimensionLimits(
                    3_200, 131_072, 65_536,
                    262_144, 65_536, 65_536,
                    20, 1.0e-6D);
            engine = new ThermalDimensionEngine(
                    dimensionGeneration, initialTick,
                    new ThermalCellArena(256),
                    profiles.signatures(), profiles.materials(), topology,
                    new FarFieldSettings(
                            true, FAR_FIELD_CONDUCTANCE_W_PER_K,
                            32.0D, 16.0D),
                    limits, publication);
            mailbox = new ThermalDimensionMailbox(
                    ThermalWorkerPool.shared(), engine);
            queryPublication = publication;
        } catch (RuntimeException | Error failure) {
            if (engine != null) {
                try {
                    engine.close();
                } catch (Throwable cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            } else {
                publication.close();
            }
            throw failure;
        }
    }

    private void tick() {
        requireMainThread();
        if (closed) {
            return;
        }
        drainCompletion();
        long gameTick = level.getGameTime();
        long alignedTick = alignedTick(gameTick);
        if (pendingSubmission != null
                || gameTick % ThermalInputBatch.CUT_INTERVAL_TICKS != 0L
                && alignedTick > lastCompletedTargetTick) {
            submitCut(alignedTick);
        }
        pages.tick(gameTick);
        phase.tick();
        if (gameTick % ThermalInputBatch.CUT_INTERVAL_TICKS != 0L) {
            return;
        }
        physicalSources.flush(gameTick);
        submitCut(gameTick);
    }

    private void submitCut(long targetTick) {
        if (inFlight != null) {
            return;
        }
        ThermalInputBatch batch = pendingSubmission;
        if (batch == null) {
            pages.flushCapturedGeometry();
            batch = accumulator.seal(targetTick);
        }
        if (mailbox.submit(batch)) {
            inFlight = batch;
            pendingSubmission = null;
        } else {
            pendingSubmission = batch;
        }
    }

    private void drainCompletion() {
        ThermalCompletion completion = mailbox.peekCompletion();
        if (completion == null) {
            return;
        }
        if (inFlight == null
                || completion.dimensionGeneration() != dimensionGeneration
                || completion.batchSequence() != inFlight.sequence()) {
            throw new IllegalStateException(
                    "thermal completion does not own the in-flight batch");
        }
        ThermalInputBatch completedBatch = inFlight;
        mailbox.acknowledgeCompletion(completion.batchSequence());
        inFlight = null;
        if (completion.status() == ThermalCompletion.Status.ENGINE_FAILED) {
            FHMain.LOGGER.error(
                    "Thermal dimension worker failed for {}",
                    level.dimension().location(),
                    completion.failure());
            restartWorker(level.getGameTime());
            return;
        }
        lastCompletedTargetTick = completedBatch.targetTick();
        pages.acknowledgeResync(completion.committedResyncTokens());
        for (ThermalCompletion.PageContinuation continuation
                : completion.continuations()) {
            pages.applyContinuation(continuation);
        }
        phase.accept(completion.phaseRequests());
        if (completion.status() == ThermalCompletion.Status.WORK_LIMITED) {
            pages.retryWorkLimited(completedBatch, level.getGameTime());
        }
    }

    private void restartWorker(long gameTick) {
        dimensionGeneration = nextGeneration();
        long initialTick = alignedTick(gameTick);
        accumulator = new DimensionInputAccumulator(
                dimensionGeneration, initialTick);
        lastCompletedTargetTick = initialTick;
        environment.replaceAccumulator(accumulator);
        physicalSources.replaceAccumulator(accumulator);
        phase.replaceAccumulator(accumulator);
        pendingSubmission = null;
        createWorker(
                initialTick,
                referenceTemperatureC);
        pages.reseedAll(accumulator);
        physicalSources.reseedAll(gameTick);
    }

    private void sampleAir(
            double x,
            double y,
            double z,
            long sampleTick,
            int maximumAgeTicks,
            ThermalEnvironmentSample out
    ) {
        int blockX = floor(x);
        int blockY = floor(y);
        int blockZ = floor(z);
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockY),
                SectionPos.blockToSectionCoord(blockZ));
        ThermalPageHandle page = pages.handle(sectionKey);
        if (page == null) {
            out.addFlag(QUERY_NO_PAGE);
            return;
        }
        int localX = SectionPos.sectionRelative(blockX);
        int localY = SectionPos.sectionRelative(blockY);
        int localZ = SectionPos.sectionRelative(blockZ);
        PagePublication publication = page.currentPublication();
        if (publication == null) {
            out.addFlag(QUERY_STALE_GEOMETRY);
            return;
        }
        PagePublication.Brick coverage = publication.brickAt(
                localX, localY, localZ);
        int microcell = microcell(x, y, z);
        int slot = publication.resolveAirPoint(
                localX, localY, localZ, microcell, profiles.signatures());
        if (slot == PagePublication.NO_AIR_POINT) {
            out.addFlag(QUERY_NO_AIR_COMPONENT);
            return;
        }
        if (!queryPublication.tryRead(
                slot,
                coverage.arenaGeneration(),
                publication.topologyGeneration(),
                querySample)) {
            out.addFlag(QUERY_PUBLICATION_MISS);
            return;
        }
        if (page.currentPublication() != publication) {
            out.addFlag(QUERY_STALE_GEOMETRY);
            return;
        }
        if (sampleTick - querySample.sampleTick() > maximumAgeTicks) {
            out.addFlag(QUERY_PUBLICATION_STALE);
            return;
        }
        out.setAir(
                querySample.temperatureC(),
                querySample.mediumId(),
                querySample.flags(),
                querySample.sampleTick());
        if (!querySample.topologyResolved()) {
            out.addFlag(QUERY_DEGRADED_TOPOLOGY);
        }
    }

    private void sampleRadiation(
            ServerPlayer player,
            ThermalEnvironmentSample out
    ) {
        if (radiation == null) {
            out.addFlag(QUERY_RADIATION_UNAVAILABLE);
            return;
        }
        radiation.samplePlayer(
                receiverKey(player),
                player.getId() & Integer.MAX_VALUE,
                player.getX(), player.getY(), player.getZ(),
                radiationSample);
        out.setRadiation(
                radiationSample.radiantFluxWPerM2(),
                radiationSample.confidence());
        if ((radiationSample.flags()
                & RadiationService.RADIATION_BUDGET_LIMITED) != 0) {
            out.addFlag(QUERY_RADIATION_BUDGET_LIMITED);
        }
        if ((radiationSample.flags()
                & RadiationService.RADIATION_UNRESOLVED) != 0) {
            out.addFlag(QUERY_RADIATION_UNRESOLVED);
        }
    }

    public static double gameplayPlayerEnvironment(
            ServerPlayer player,
            double naturalTemperatureC,
            ThermalEnvironmentSample out
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(out, "out").clear();
        if (!Double.isFinite(naturalTemperatureC)) {
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(player.serverLevel());
        if (input == null) {
            input = start(
                    player.serverLevel(), naturalTemperatureC);
        }
        if (input == null) {
            return naturalTemperatureC;
        }
        input.pages.requestPlayerPage(
                player.getUUID(),
                BlockPos.containing(
                        player.getX(), player.getEyeY(), player.getZ()),
                player.serverLevel().getGameTime());
        input.sampleAir(
                player.getX(), player.getEyeY(), player.getZ(),
                player.serverLevel().getGameTime(),
                MAX_PUBLICATION_AGE_TICKS, out);
        input.sampleRadiation(player, out);
        double base = out.airAvailable()
                ? out.airTemperatureC() : naturalTemperatureC;
        double composed = input.analyticFields.compose(
                player.getX(), player.getEyeY(), player.getZ(), base);
        if (Double.compare(composed, base) != 0) {
            out.setComposedAir(composed);
            out.addFlag(QUERY_ANALYTIC_FIELD_APPLIED);
        }
        return composed;
    }

    public static double gameplayPassiveEnvironment(
            LevelReader level,
            BlockPos position,
            double naturalTemperatureC
    ) {
        if (!(level instanceof ServerLevel server)
                || !Double.isFinite(naturalTemperatureC)
                || !server.getServer().isSameThread()) {
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(server);
        if (input == null) {
            return naturalTemperatureC;
        }
        ThermalEnvironmentSample out = input.passiveScratch;
        out.clear();
        input.sampleAir(
                position.getX() + 0.5D,
                position.getY() + 0.5D,
                position.getZ() + 0.5D,
                server.getGameTime(), MAX_PUBLICATION_AGE_TICKS, out);
        double base = out.airAvailable()
                ? out.airTemperatureC() : naturalTemperatureC;
        return input.analyticFields.compose(
                position.getX() + 0.5D,
                position.getY() + 0.5D,
                position.getZ() + 0.5D,
                base);
    }

    public static double gameplayCropEnvironment(
            LevelAccessor level,
            BlockPos position,
            double naturalTemperatureC
    ) {
        return gameplayPassiveEnvironment(level, position, naturalTemperatureC);
    }

    public static double gameplayTownEnvironment(
            LevelAccessor level,
            TownThermalProjection projection,
            double naturalTemperatureC
    ) {
        if (!(level instanceof ServerLevel server)
                || projection.voxelCount() == 0
                || !Double.isFinite(naturalTemperatureC)) {
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(server);
        if (input == null) {
            return naturalTemperatureC;
        }
        int totalWeight = 0;
        double total = 0.0D;
        for (long key : projection.groupKeys()) {
            int weight = projection.weight(key);
            if (weight <= 0) continue;
            ThermalEnvironmentSample out = input.townScratch;
            out.clear();
            int x = projection.representativeX(key);
            int y = projection.representativeY(key);
            int z = projection.representativeZ(key);
            input.sampleAir(
                    x + 0.5D, y + 0.5D, z + 0.5D,
                    server.getGameTime(), MAX_PUBLICATION_AGE_TICKS, out);
            double base;
            if (out.airAvailable()) {
                base = out.airTemperatureC();
            } else {
                input.townPosition.set(x, y, z);
                base = WorldTemperature.naturalBlock(
                        server, input.townPosition);
            }
            total += input.analyticFields.compose(
                    x + 0.5D, y + 0.5D, z + 0.5D, base) * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? naturalTemperatureC : total / totalWeight;
    }

    public static boolean upsertGameplayAnalyticField(
            ServerLevel level, ThermalAnalyticField field
    ) {
        MinecraftThermalInput input = active(level);
        if (input == null && level.getServer().isSameThread()) {
            input = start(
                    level,
                    WorldTemperature.naturalAir(
                            level,
                            BlockPos.containing(
                                    field.centerX(),
                                    field.centerY(),
                                    field.centerZ())));
        }
        if (input == null) return false;
        input.analyticFields.upsert(field);
        return true;
    }

    public static boolean removeGameplayAnalyticField(
            ServerLevel level, long fieldId
    ) {
        MinecraftThermalInput input = active(level);
        return input != null && level.getServer().isSameThread()
                && input.analyticFields.remove(fieldId);
    }

    public static List<ThermalAnalyticField> gameplayAnalyticFieldsAt(
            ServerLevel level, BlockPos position
    ) {
        MinecraftThermalInput input = active(level);
        return input == null || !level.getServer().isSameThread()
                ? List.of()
                : input.analyticFields.fieldsAt(
                        position.getX() + 0.5D,
                        position.getY() + 0.5D,
                        position.getZ() + 0.5D);
    }

    public static boolean hasGameplayAnalyticFieldAt(
            ServerLevel level, BlockPos position
    ) {
        MinecraftThermalInput input = active(level);
        return input != null && level.getServer().isSameThread()
                && input.analyticFields.appliesAt(
                        position.getX() + 0.5D,
                        position.getY() + 0.5D,
                        position.getZ() + 0.5D);
    }

    public static float[] gameplayInfraredFields(
            ServerLevel level,
            ChunkPos center,
            int chunkRadius,
            int maximumFields
    ) {
        MinecraftThermalInput input = active(level);
        if (input == null || chunkRadius < 0 || maximumFields <= 0
                || !level.getServer().isSameThread()) {
            return NO_INFRARED_FIELDS;
        }
        int minX = SectionPos.sectionToBlockCoord(center.x - chunkRadius);
        int maxX = SectionPos.sectionToBlockCoord(center.x + chunkRadius) + 15;
        int minZ = SectionPos.sectionToBlockCoord(center.z - chunkRadius);
        int maxZ = SectionPos.sectionToBlockCoord(center.z + chunkRadius) + 15;
        float[] fields = new float[maximumFields * 8];
        int count = input.analyticFields.appendInfrared(
                fields, 0, maximumFields, minX, maxX, minZ, maxZ);
        count = input.physicalSources.appendInfraredFields(
                fields, count, maximumFields, minX, maxX, minZ, maxZ);
        return count == maximumFields
                ? fields : Arrays.copyOf(fields, count * 8);
    }

    public static BlockPos nearestGameplayGenerator(
            Level level,
            BlockPos position,
            double maximumDistanceBlocks
    ) {
        if (!(level instanceof ServerLevel server)
                || !server.getServer().isSameThread()
                || !Double.isFinite(maximumDistanceBlocks)
                || maximumDistanceBlocks <= 0.0D) {
            return null;
        }
        MinecraftThermalInput input = active(server);
        return input == null ? null
                : input.physicalSources.nearestEnabledGenerator(
                        position,
                        maximumDistanceBlocks * maximumDistanceBlocks);
    }

    public static boolean ownsGameplayHeatingTransition(
            ServerLevel level,
            BlockPos position,
            BlockState state,
            StateTransitionData data
    ) {
        if (!data.willTransit() || data.heatCapacity() <= 0
                || data.heatingTransition(state) == null) {
            return false;
        }
        Integer profileId = MinecraftThermalProfiles.phaseProfileId(state);
        MinecraftThermalInput input = active(level);
        return profileId != null && input != null
                && input.phase.ownsHeatingTransition(position, profileId);
    }

    public static void prepareGameplayProfiles() {
        MinecraftThermalProfiles.prepare();
    }

    public static void invalidateGameplayProfilesForRecipeReload() {
        closeAll();
        MinecraftThermalProfiles.invalidate();
    }

    public static void onSectionSetBlockState(
            LevelChunkSection section,
            int localX,
            int localY,
            int localZ,
            BlockState oldState,
            BlockState newState
    ) {
        if (oldState == newState) return;
        MinecraftPageManager.SectionOwner owner =
                ((MinecraftThermalSectionAttachment) (Object) section)
                        .frostedheart$getThermalInputOwner();
        if (owner != null) {
            MinecraftThermalInput input = owner.input();
            if (input != null) {
                int flags = MinecraftThermalProfiles.mutationFlags(
                        oldState, newState);
                if (flags != 0) input.pages.onBlockMutation(
                        owner, localX, localY, localZ,
                        (flags & MinecraftThermalProfiles.TOPOLOGY_MUTATION) != 0);
            }
        }
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            input.pages.onChunkLoad(chunk);
            input.radiationOcclusion.onChunkLoad(chunk);
        }
    }

    public static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            input.physicalSources.beforeChunkUnload(
                    chunk, level.getGameTime());
            input.radiationOcclusion.onChunkUnload(chunk);
            input.pages.onChunkUnload(chunk);
        }
    }

    public static void sealActiveLevel(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        if (input != null) input.tick();
    }

    public static void closeActiveLevel(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        if (input != null) input.close();
    }

    public static void onPlayerLogout(ServerPlayer player) {
        MinecraftThermalInput input = active(player.serverLevel());
        if (input != null) input.pages.releasePlayer(player.getUUID());
    }

    public static void closeAll() {
        MinecraftThermalInput[] inputs;
        synchronized (ACTIVE) {
            inputs = ACTIVE.values().toArray(MinecraftThermalInput[]::new);
        }
        for (MinecraftThermalInput input : inputs) input.close();
        ThermalWorkerPool.closeShared();
    }

    public static void onRawBlockContainerReplaced(LevelChunkSection section) {
        MinecraftPageManager.SectionOwner owner =
                ((MinecraftThermalSectionAttachment) (Object) section)
                        .frostedheart$getThermalInputOwner();
        if (owner != null) owner.recordFullResync(
                ThermalPageHandle.GeometryResyncReason.EXPLICIT_INVALIDATION);
    }

    public static void onSectionIdentityReplaced(
            ServerLevel level,
            LevelChunk chunk,
            int sectionIndex,
            LevelChunkSection previous
    ) {
        MinecraftThermalInput input = active(level);
        if (input != null && sectionIndex >= 0
                && sectionIndex < chunk.getSections().length) {
            input.pages.onSectionIdentityReplaced(
                    chunk, sectionIndex, previous,
                    chunk.getSections()[sectionIndex]);
            input.radiationOcclusion.onSectionIdentityReplaced(
                    chunk.getPos().x,
                    chunk.getSectionYFromSectionIndex(sectionIndex),
                    chunk.getPos().z);
        }
    }

    public static void onGeneratorTick(
            ServerLevel level, BlockPos source, BlockPos target,
            double thermalLevel, boolean active
    ) {
        MinecraftThermalInput input = active(level);
        if (input != null) input.physicalSources.observeMachine(
                source, target, MinecraftPhysicalSourceProfile.GENERATOR,
                thermalLevel, active);
    }

    public static void onFountainTick(
            ServerLevel level, BlockPos source, BlockPos target,
            double thermalLevel, boolean active
    ) {
        MinecraftThermalInput input = active(level);
        if (input != null) input.physicalSources.observeMachine(
                source, target, MinecraftPhysicalSourceProfile.FOUNTAIN,
                thermalLevel, active);
    }

    public static void onRadiatorTick(
            ServerLevel level, BlockPos source, BlockPos target,
            double thermalLevel, boolean active
    ) {
        MinecraftThermalInput input = active(level);
        if (input != null) input.physicalSources.observeMachine(
                source, target, MinecraftPhysicalSourceProfile.RADIATOR,
                thermalLevel, active);
    }

    public static void onPhysicalSourceRemoved(
            ServerLevel level, BlockPos source
    ) {
        MinecraftThermalInput input = active(level);
        if (input != null) input.physicalSources.remove(
                source.getX(), source.getY(), source.getZ());
    }

    @Override
    public void close() {
        requireMainThread();
        if (closed) return;
        closed = true;
        physicalSources.close();
        pages.close();
        environment.close();
        if (radiation != null) radiation.close();
        mailbox.close();
        synchronized (ACTIVE) {
            ACTIVE.remove(level, this);
        }
    }

    private static MinecraftThermalInput start(
            ServerLevel level,
            double initialTemperatureC
    ) {
        synchronized (ACTIVE) {
            MinecraftThermalInput existing = ACTIVE.get(level);
            if (existing != null) return existing;
            try {
                MinecraftThermalInput created =
                        new MinecraftThermalInput(
                                level, initialTemperatureC);
                ACTIVE.put(level, created);
                return created;
            } catch (RuntimeException failure) {
                FHMain.LOGGER.error(
                        "Could not start thermal runtime for {}",
                        level.dimension().location(), failure);
                return null;
            }
        }
    }

    private static MinecraftThermalInput active(ServerLevel level) {
        synchronized (ACTIVE) {
            return ACTIVE.get(level);
        }
    }

    private void requireMainThread() {
        if (Thread.currentThread() != mainThread) {
            throw new IllegalStateException(
                    "Minecraft thermal input requires the level thread");
        }
    }

    private static long alignedTick(long tick) {
        return Math.floorDiv(tick, ThermalInputBatch.CUT_INTERVAL_TICKS)
                * ThermalInputBatch.CUT_INTERVAL_TICKS;
    }

    private static long receiverKey(ServerPlayer player) {
        return player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(
                        player.getUUID().getLeastSignificantBits(), 17);
    }

    private static long nextGeneration() {
        return NEXT_GENERATION.getAndUpdate(Math::incrementExact);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int microcell(double x, double y, double z) {
        int microX = Math.min(3, (int) Math.floor(
                (x - Math.floor(x)) * 4.0D));
        int microY = Math.min(3, (int) Math.floor(
                (y - Math.floor(y)) * 4.0D));
        int microZ = Math.min(3, (int) Math.floor(
                (z - Math.floor(z)) * 4.0D));
        return microX | microZ << 2 | microY << 4;
    }

}
