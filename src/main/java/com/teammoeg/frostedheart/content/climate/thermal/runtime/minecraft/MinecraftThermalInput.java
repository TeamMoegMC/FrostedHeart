/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticFieldIndex;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftSignatureCapture;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.BlockRadiationIndex;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.MinecraftRadiationOcclusion;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalDimensionMailbox;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionLimits;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.DimensionInputAccumulator;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftEnvironmentCapture;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftThermalSectionAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.PhysicalSourceSpatialIndex;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FarFieldSettings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.FHMain;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Minecraft 热系统的唯一公开运行时入口。
 *
 * <p>服务器主线程通过该类创建每维度 runtime、收集 20-tick 输入 cut、提交
 * mailbox、消费 completion，并从不可变 publication 回答玩法查询。它不编译
 * Brick，也不直接推进 solver。</p>
 *
 * <p>The sole public runtime facade for dimension lifecycle, fixed-cut
 * transport, and gameplay queries.</p>
 */
public final class MinecraftThermalInput implements AutoCloseable {
    private static final int MAX_PUBLICATION_AGE_TICKS = 40;
    private static final int INFRARED_ACTIVE_TICKS = 80;
    private static final int INFRARED_PAGE_CAPACITY = 9 * 9 * 9;
    private static final int INFRARED_PRESENCE_WORDS = 12;
    private static final long[] NO_INFRARED_PRESENCE = new long[0];
    private static final byte[] NO_INFRARED_RECORDS = new byte[0];
    private static final int MAXIMUM_PHYSICAL_SOURCES = 65_536;
    private static final int SOURCE_DISCOVERY_CHUNKS_PER_TICK = 8;
    private static final int MAXIMUM_SOURCE_NODES = 131_072;
    private static final int MAXIMUM_RADIATION_SECTIONS = 3_200;
    private static final ThermalMemoryBudget MEMORY =
            new ThermalMemoryBudget(128L * 1024L * 1024L);
    private static final RadiationService.Parameters RADIATION_PARAMETERS =
            new RadiationService.Parameters(
                    MAXIMUM_RADIATION_SECTIONS, 128, 64, 8, 24, 8, 256,
                    16.0D, 0.1D, 0.5D, 0.1D, 0.9D, 1.62D);
    private static final IdentityHashMap<ServerLevel, MinecraftThermalInput>
            ACTIVE = new IdentityHashMap<>();
    private static final AtomicLong NEXT_GENERATION =
            new AtomicLong(1_000L);
    private static final ThreadLocal<BlockPos.MutableBlockPos>
            DORMANT_QUERY_POSITION = ThreadLocal.withInitial(
                    BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<ThermalAnalyticFieldIndex.Sample> TOWN_FIELD_SAMPLE =
            ThreadLocal.withInitial(ThermalAnalyticFieldIndex.Sample::new);

    private final ServerLevel level;
    private final Thread mainThread;
    private final MinecraftThermalProfiles.Snapshot profiles;
    private final double referenceTemperatureC;
    private final ThermalAnalyticFieldIndex analyticFields;
    private final MinecraftEnvironmentCapture environment;
    private final MinecraftPageManager pages;
    private final PhysicalSourceSpatialIndex physicalSources;
    private final Long2ObjectLinkedOpenHashMap<LevelChunk> pendingSourceChunks =
            new Long2ObjectLinkedOpenHashMap<>();
    private final MinecraftPhaseController phase;
    private final MinecraftRadiationOcclusion radiationOcclusion;
    private final BlockRadiationIndex blockRadiation;
    private final RadiationService radiation;
    private final QueryPublication.MutableSample querySample =
            new QueryPublication.MutableSample();
    private final DormantChunkThermalState.CaptureScratch dormantCapture =
            new DormantChunkThermalState.CaptureScratch();
    private final BlockPos.MutableBlockPos dormantPosition =
            new BlockPos.MutableBlockPos();
    private final RadiationService.MutableSample radiationSample =
            new RadiationService.MutableSample();
    private final ThermalEnvironmentSample passiveScratch =
            new ThermalEnvironmentSample();
    private final ThermalEnvironmentSample townScratch =
            new ThermalEnvironmentSample();
    private final BlockPos.MutableBlockPos townPosition =
            new BlockPos.MutableBlockPos();
    private static InfraredCapture infraredCapture;
    private static final long[] EMPTY_REFRESH_PAGES = new long[INFRARED_PRESENCE_WORDS];

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
        analyticFields = MinecraftGameplayFields.indexFor(level);
        referenceTemperatureC = initialTemperatureC;
        mainThread = Thread.currentThread();
        profiles = MinecraftThermalProfiles.prepare();
        long initialTick = alignedTick(level.getGameTime());
        dimensionGeneration = nextGeneration();
        accumulator = new DimensionInputAccumulator(
                dimensionGeneration, initialTick);
        lastCompletedTargetTick = initialTick;
        MinecraftSignatureCapture signatureCapture =
                new MinecraftSignatureCapture(
                        level,
                        profiles.states(),
                        profiles.signatures());
        environment = new MinecraftEnvironmentCapture(level, accumulator);
        pages = new MinecraftPageManager(
                this, level, accumulator, signatureCapture, environment);
        physicalSources = new PhysicalSourceSpatialIndex(
                accumulator, pages, profiles.tuning().campfire(),
                64, MAXIMUM_PHYSICAL_SOURCES);
        createWorker(initialTick, initialTemperatureC);
        phase = new MinecraftPhaseController(
                level, pages, profiles.states(), profiles.signatures(),
                profiles.materials(), accumulator, 8);
        radiationOcclusion = new MinecraftRadiationOcclusion(
                level, pages, MAXIMUM_RADIATION_SECTIONS);
        blockRadiation = profiles.states().radiationEnabled()
                ? BlockRadiationIndex.tryCreate(
                        level,
                        pages,
                        profiles.states(),
                        MEMORY.createDimensionBudget(
                                BlockRadiationIndex.projectedMaximumBytes(
                                        MAXIMUM_RADIATION_SECTIONS)),
                        MAXIMUM_RADIATION_SECTIONS)
                : null;
        pages.attachMutationConsumers(
                physicalSources, radiationOcclusion);
        radiation = RadiationService.tryCreate(
                RADIATION_PARAMETERS,
                physicalSources,
                blockRadiation,
                radiationOcclusion,
                MEMORY.createDimensionBudget(
                        RadiationService.projectedMaximumBytes(
                                RADIATION_PARAMETERS)));
    }

    private void createWorker(
            long initialTick,
            double referenceTemperatureC
    ) {
        MinecraftThermalProfiles.Tuning tuning = profiles.tuning();
        ThermalDimensionLimits limits = new ThermalDimensionLimits(
                3_200, MAXIMUM_PHYSICAL_SOURCES, MAXIMUM_SOURCE_NODES,
                131_072, 65_536,
                262_144, 65_536, 65_536,
                20, 1.0e-6D);
        QueryPublication publication = QueryPublication.tryCreate(
                MEMORY.createDimensionBudget(
                        16L * 1024L * 1024L),
                256,
                limits.maximumPages());
        if (publication == null) {
            throw new IllegalStateException(
                    "thermal query publication memory was refused");
        }
        ThermalDimensionEngine engine = null;
        try {
            ThermalTopologyParameters topology = new ThermalTopologyParameters(
                    tuning.airHeatCapacityJPerBlockK(),
                    referenceTemperatureC,
                    tuning.airMixingWPerBlockK(),
                    new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D),
                    1_024, 8);
            engine = new ThermalDimensionEngine(
                    dimensionGeneration, initialTick,
                    new ThermalCellArena(256),
                    profiles.signatures(), profiles.materials(), topology,
                    new FarFieldSettings(
                            tuning.farFieldConductanceWPerK(),
                            32.0D, 16.0D),
                    tuning.campfire(),
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
        if (blockRadiation != null) {
            blockRadiation.tick(gameTick);
        }
        boolean cut = gameTick % ThermalInputBatch.CUT_INTERVAL_TICKS == 0L;
        if (cut) {
            physicalSources.flush(gameTick);
        }
        boolean discovered = drainPendingSources();
        if (cut) {
            if (discovered) physicalSources.flush(gameTick);
            submitCut(gameTick);
        }
    }

    private void attachLoadedChunk(LevelChunk chunk) {
        pages.onChunkLoad(chunk);
        enqueueSourceDiscovery(chunk);
    }

    public void enqueueSourceDiscovery(LevelChunk chunk) {
        requireMainThread();
        if (!closed) pendingSourceChunks.put(chunk.getPos().toLong(), chunk);
    }

    private boolean drainPendingSources() {
        int remaining = Math.min(
                pendingSourceChunks.size(), SOURCE_DISCOVERY_CHUNKS_PER_TICK);
        boolean processed = false;
        while (remaining-- > 0 && physicalSources.hasAvailableCapacity()) {
            LevelChunk chunk = pendingSourceChunks.removeFirst();
            if (!physicalSources.discoverChunk(chunk)) {
                enqueueSourceDiscovery(chunk);
            }
            processed = true;
        }
        return processed;
    }

    private void attachLoadedWorld(BlockPos center) {
        for (var holder : level.getChunkSource().chunkMap.getChunks()) {
            ChunkPos position = holder.getPos();
            LevelChunk chunk = level.getChunkSource().getChunkNow(position.x, position.z);
            if (chunk != null) attachLoadedChunk(chunk);
        }
        int centerX = SectionPos.blockToSectionCoord(center.getX());
        int centerZ = SectionPos.blockToSectionCoord(center.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long key = ChunkPos.asLong(centerX + dx, centerZ + dz);
                LevelChunk chunk = pendingSourceChunks.get(key);
                if (chunk != null) pendingSourceChunks.putAndMoveToFirst(key, chunk);
            }
        }
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
        if (completion.status() == ThermalCompletion.Status.ENGINE_FAILED) {
            FHMain.LOGGER.error(
                    "Thermal dimension worker failed for {}",
                    level.dimension().location(),
                    completion.failure());
            pages.checkpointAll(true, false);
            try {
                mailbox.acknowledgeCompletion(completion.batchSequence());
            } catch (RuntimeException | Error closeFailure) {
                FHMain.LOGGER.error(
                        "Failed to close terminal thermal worker for {}",
                        level.dimension().location(),
                        closeFailure);
            }
            inFlight = null;
            restartWorker(level.getGameTime());
            return;
        }
        mailbox.acknowledgeCompletion(completion.batchSequence());
        inFlight = null;
        lastCompletedTargetTick = completedBatch.targetTick();
        pages.acknowledgeResync(completion.committedResyncTokens());
        for (ThermalCompletion.BrickResidency residency
                : completion.residencyUpdates()) {
            pages.applyResidency(residency);
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
        MinecraftPageManager.SectionOwner owner =
                pages.loadedSectionOrAttach(sectionKey);
        ThermalPageHandle page = owner == null ? null : owner.page();
        LevelChunk loadedChunk = owner == null ? null : owner.chunk();
        if (page == null) {
            sampleDormant(
                    loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            return;
        }
        int localX = SectionPos.sectionRelative(blockX);
        int localY = SectionPos.sectionRelative(blockY);
        int localZ = SectionPos.sectionRelative(blockZ);
        PagePublication publication = page.currentPublication();
        if (publication == null) {
            if (!resolveLastPublication(page, localX, localY, localZ,
                sampleTick, maximumAgeTicks, out)) {
                sampleDormant(
                        loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            }
            return;
        }
        PagePublication.Brick coverage = publication.brickAt(
                localX, localY, localZ);
        int slot = publication.resolveAirPoint(
                localX, localY, localZ);
        if (slot == PagePublication.NO_AIR_POINT) {
            if (coverage.signaturePayload() == null) {
                sampleDormant(
                        loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            }
            return;
        }
        if (!queryPublication.tryRead(
                slot,
                coverage.arenaGeneration(),
                publication.topologyGeneration(),
                querySample)) {
            if (!resolveLastPublication(page, localX, localY, localZ,
                sampleTick, maximumAgeTicks, out)) {
                sampleDormant(
                        loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            }
            return;
        }
        if (page.currentPublication() != publication) {
            if (!resolveLastPublication(page, localX, localY, localZ,
                sampleTick, maximumAgeTicks, out)) {
                sampleDormant(
                        loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            }
            return;
        }
        if (sampleTick - querySample.sampleTick() > maximumAgeTicks) {
            sampleDormant(
                    loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            return;
        }
        out.setAir(querySample.temperatureC());
    }

    /** Returns true when the last cut answers the point, including definite no-Air. */
    private boolean resolveLastPublication(
            ThermalPageHandle page,
            int localX,
            int localY,
            int localZ,
            long sampleTick,
            int maximumAgeTicks,
            ThermalEnvironmentSample out
    ) {
        PagePublication publication = page.lastPublication();
        if (publication == null) {
            return false;
        }
        PagePublication.Brick brick = publication.brickAt(
                localX, localY, localZ);
        if (brick.coverageSlot() < 0) {
            return brick.signaturePayload() != null;
        }
        int components = brick.transportNodeCount();
        double warmest = -Double.MAX_VALUE;
        long commonTick = -1L;
        for (int component = 0; component < components; component++) {
            if (!queryPublication.tryRead(
                    brick.coverageSlot() + component,
                    brick.arenaGeneration(),
                    publication.topologyGeneration(),
                    querySample)) {
                return false;
            }
            if (commonTick < 0L) {
                commonTick = querySample.sampleTick();
            } else if (commonTick != querySample.sampleTick()) {
                return false;
            }
            warmest = Math.max(warmest, querySample.temperatureC());
        }
        if (page.lastPublication() != publication
                || sampleTick - commonTick > maximumAgeTicks) {
            return false;
        }
        out.setAir(warmest);
        return true;
    }

    private void sampleDormant(
            LevelChunk loadedChunk,
            int blockX,
            int blockY,
            int blockZ,
            long gameTick,
            ThermalEnvironmentSample out
    ) {
        double temperature = loadedChunk == null
                ? dormantTemperature(
                        level, blockX, blockY, blockZ,
                        gameTick, dormantPosition)
                : dormantTemperature(
                        level, loadedChunk, blockX, blockY, blockZ,
                        gameTick, dormantPosition);
        if (Double.isFinite(temperature)) {
            out.setAir(temperature);
        }
    }

    private void sampleRadiation(
            ServerPlayer player,
            ThermalEnvironmentSample out
    ) {
        if (radiation == null) {
            return;
        }
        radiation.samplePlayer(
                receiverKey(player),
                player.getId() & Integer.MAX_VALUE,
                player.getX(), player.getY(), player.getEyeY(), player.getZ(),
                radiationSample);
        out.setRadiation(radiationSample.radiantFluxWPerM2());
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
                    player.serverLevel(), naturalTemperatureC, player.blockPosition());
        }
        if (input == null) {
            ThermalAnalyticFieldIndex fields = MinecraftGameplayFields.existing(player.serverLevel());
            double composed = fields == null ? naturalTemperatureC : fields.compose(
                    player.getX(), player.getEyeY(), player.getZ(), naturalTemperatureC, naturalTemperatureC);
            if (Double.compare(composed, naturalTemperatureC) != 0) out.setComposedAir(composed);
            return composed;
        }
        input.sampleAir(
                player.getX(), player.getEyeY(), player.getZ(),
                player.serverLevel().getGameTime(),
                MAX_PUBLICATION_AGE_TICKS, out);
        input.sampleRadiation(player, out);
        double base = out.airAvailable()
                ? out.airTemperatureC() : naturalTemperatureC;
        double composed = input.analyticFields.compose(
                player.getX(), player.getEyeY(), player.getZ(), naturalTemperatureC, base);
        if (Double.compare(composed, base) != 0) {
            out.setComposedAir(composed);
        }
        return composed;
    }

    public static double gameplayPassiveEnvironment(
            LevelReader level,
            BlockPos position,
            double naturalTemperatureC
    ) {
        return gameplayPassiveEnvironment(level, position, naturalTemperatureC, null);
    }

    public static double gameplayPassiveEnvironment(
            LevelReader level, BlockPos position, double naturalTemperatureC,
            ThermalAnalyticFieldIndex.Sample fieldsOut
    ) {
        if (!(level instanceof ServerLevel server)
                || !Double.isFinite(naturalTemperatureC)
                || !server.getServer().isSameThread()) {
            if (fieldsOut != null) fieldsOut.clear();
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(server);
        double base;
        if (input == null) {
            double dormant = dormantTemperature(
                    server,
                    position.getX(), position.getY(), position.getZ(),
                    server.getGameTime(), DORMANT_QUERY_POSITION.get());
            base = Double.isFinite(dormant) ? dormant : naturalTemperatureC;
        } else {
            ThermalEnvironmentSample out = input.passiveScratch;
            out.clear();
            input.sampleAir(
                    position.getX() + 0.5D,
                    position.getY() + 0.5D,
                    position.getZ() + 0.5D,
                    server.getGameTime(), MAX_PUBLICATION_AGE_TICKS, out);
            base = out.airAvailable() ? out.airTemperatureC() : naturalTemperatureC;
        }
        ThermalAnalyticFieldIndex fields = input == null
                ? MinecraftGameplayFields.existing(server) : input.analyticFields;
        if (fields == null || fields.isEmpty()) {
            if (fieldsOut != null) fieldsOut.clear();
            return base;
        }
        double x = position.getX() + 0.5D;
        double y = position.getY() + 0.5D;
        double z = position.getZ() + 0.5D;
        if (fieldsOut == null) return fields.compose(x, y, z, naturalTemperatureC, base);
        fields.sample(x, y, z, fieldsOut);
        return fieldsOut.compose(naturalTemperatureC, base);
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
                || !Double.isFinite(naturalTemperatureC)
                || !server.getServer().isSameThread()) {
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(server);
        ThermalAnalyticFieldIndex fields = input == null
                ? MinecraftGameplayFields.existing(server) : input.analyticFields;
        ThermalAnalyticFieldIndex.Sample fieldSample = fields == null || fields.isEmpty()
                ? null : TOWN_FIELD_SAMPLE.get();
        int totalWeight = 0;
        double total = 0.0D;
        for (long key : projection.groupKeys()) {
            int weight = projection.weight(key);
            if (weight <= 0) continue;
            int x = projection.representativeX(key);
            int y = projection.representativeY(key);
            int z = projection.representativeZ(key);
            if (fieldSample != null) fields.sample(x + 0.5D, y + 0.5D, z + 0.5D, fieldSample);
            double natural = Double.NaN;
            double base;
            if (input != null) {
                ThermalEnvironmentSample out = input.townScratch;
                out.clear();
                input.sampleAir(
                        x + 0.5D, y + 0.5D, z + 0.5D,
                        server.getGameTime(), MAX_PUBLICATION_AGE_TICKS, out);
                if (out.airAvailable()) {
                    base = out.airTemperatureC();
                } else {
                    input.townPosition.set(x, y, z);
                    base = natural = WorldTemperature.naturalBlock(
                            server, input.townPosition);
                }
            } else {
                double dormant = dormantTemperature(
                        server, x, y, z, server.getGameTime(),
                        DORMANT_QUERY_POSITION.get());
                base = Double.isFinite(dormant)
                        ? dormant : (natural = WorldTemperature.naturalBlock(
                                server, DORMANT_QUERY_POSITION.get().set(x, y, z)));
            }
            if (fieldSample != null) {
                if (fieldSample.requiresNatural() && !Double.isFinite(natural)) {
                    natural = WorldTemperature.naturalBlock(
                            server, DORMANT_QUERY_POSITION.get().set(x, y, z));
                }
                base = fieldSample.compose(natural, base);
            }
            total += base * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? naturalTemperatureC : total / totalWeight;
    }

    public static boolean upsertGameplayAnalyticField(
            ServerLevel level, ThermalAnalyticField field
    ) {
        return MinecraftGameplayFields.upsert(level, field);
    }

    public static boolean removeGameplayAnalyticField(
            ServerLevel level, ThermalFieldKey key
    ) {
        return MinecraftGameplayFields.remove(level, key);
    }

    public static List<ThermalAnalyticField> gameplayAnalyticFieldsAt(
            ServerLevel level, BlockPos position
    ) {
        if (!level.getServer().isSameThread()) return List.of();
        ThermalAnalyticFieldIndex fields = MinecraftGameplayFields.existing(level);
        return fields == null ? List.of() : fields.fieldsAt(
                        position.getX() + 0.5D,
                        position.getY() + 0.5D,
                        position.getZ() + 0.5D);
    }

    public static boolean hasGameplayAnalyticFieldAt(
            ServerLevel level, BlockPos position
    ) {
        if (!level.getServer().isSameThread()) return false;
        ThermalAnalyticFieldIndex fields = MinecraftGameplayFields.existing(level);
        return fields != null && fields.appliesAt(
                        position.getX() + 0.5D,
                        position.getY() + 0.5D,
                        position.getZ() + 0.5D);
    }

    public static InfraredSnapshot gameplayInfraredSnapshot(
            ServerPlayer player, boolean forceFull, int lastInfraredEpoch,
            long[] knownPresence, long lastDormantRevision, long[] knownDormantPresence,
            long[] knownRefreshPages
    ) {
        Objects.requireNonNull(player, "player");
        if (!player.server.isSameThread()) return null;
        if (knownPresence.length != INFRARED_PRESENCE_WORDS
                || knownDormantPresence.length != INFRARED_PRESENCE_WORDS
                || knownRefreshPages.length != INFRARED_PRESENCE_WORDS) {
            throw new IllegalArgumentException("infrared presence requires 12 words");
        }
        if (infraredCapture == null) infraredCapture = new InfraredCapture();
        return infraredCapture.capture(active(player.serverLevel()), player.serverLevel(),
                SectionPos.blockToSectionCoord(Mth.floor(player.getX())),
                SectionPos.blockToSectionCoord(Mth.floor(player.getZ())),
                SectionPos.blockToSectionCoord(Mth.floor(player.getEyeY())),
                forceFull, lastInfraredEpoch, knownPresence, lastDormantRevision,
                knownDormantPresence, forceFull ? EMPTY_REFRESH_PAGES : knownRefreshPages);
    }

    /** One lazily allocated server-thread scratch for the existing display path.
     * A display request does not require a physical dimension engine. */
    private static final class InfraredCapture implements AutoCloseable {
        private final QueryPublication.InfraredReadCursor infraredCursor =
                new QueryPublication.InfraredReadCursor();
        private final ThermalPageHandle[] infraredHandles =
                new ThermalPageHandle[INFRARED_PAGE_CAPACITY];
        private final PagePublication[] infraredPublications =
                new PagePublication[INFRARED_PAGE_CAPACITY];
        private final short[] infraredLocalIndexes =
                new short[INFRARED_PAGE_CAPACITY];
        private final long[] infraredPresence =
                new long[INFRARED_PRESENCE_WORDS];
        private final long[] infraredChangedPages = new long[INFRARED_PRESENCE_WORDS];
        private final long[] infraredDormantPresence = new long[INFRARED_PRESENCE_WORDS];
        private final short[] infraredBlockTemperatures =
                new short[InfraredBrickCodec.BLOCKS_PER_BRICK];
        private final int[] infraredUniqueSlots =
                new int[InfraredBrickCodec.BLOCKS_PER_BRICK];
        private final short[] infraredUniqueTemperatures =
                new short[InfraredBrickCodec.BLOCKS_PER_BRICK];
        private InfraredBrickCodec.Builder infraredPayload;

        private MinecraftThermalInput input;
        private ServerLevel level;
        private long[] previousRefreshPages;
        private long[] previousPresence;
        private boolean rawReadFailed;
        private final long[] refreshPages = new long[INFRARED_PRESENCE_WORDS];
        private final java.util.ArrayList<ThermalAnalyticField> fields = new java.util.ArrayList<>();
        private final java.util.ArrayList<ThermalAnalyticField> brickFields = new java.util.ArrayList<>();
        private final ThermalAnalyticFieldIndex.Sample fieldSample = new ThermalAnalyticFieldIndex.Sample();
        private final QueryPublication.MutableSample querySample = new QueryPublication.MutableSample();
        private final BlockPos.MutableBlockPos dormantPosition = new BlockPos.MutableBlockPos();
        private final double[] nodeTemperatures = new double[64];
        private final double[] rawTemperatures = new double[64];
        private final boolean[] loadedNeighbors = new boolean[9];

        private boolean refreshPage(int page) {
            return presenceBit(refreshPages, page) || presenceBit(previousRefreshPages, page);
        }

        private void collectRefreshPages(int centerX, int centerY, int centerZ) {
            Arrays.fill(refreshPages, 0);
            fields.clear();
            var index = MinecraftGameplayFields.existing(level);
            if (index == null) return;
            int x = (centerX - 4) * 16, y = (centerY - 4) * 16, z = (centerZ - 4) * 16;
            index.collectIntersecting(x + .5, y + .5, z + .5, x + 143.5, y + 143.5, z + 143.5, fields);
            for (int i = 0; i < fields.size(); i++) {
                var field = fields.get(i);
                int minX = clippedPage(field.min(0), x), maxX = clippedPage(field.max(0), x);
                int minY = clippedPage(field.min(1), y), maxY = clippedPage(field.max(1), y);
                int minZ = clippedPage(field.min(2), z), maxZ = clippedPage(field.max(2), z);
                for (int py = minY; py <= maxY; py++) for (int pz = minZ; pz <= maxZ; pz++) {
                    for (int px = minX; px <= maxX; px++) {
                        if (!field.intersects(x + px * 16 + .5, y + py * 16 + .5, z + pz * 16 + .5,
                                x + px * 16 + 15.5, y + py * 16 + 15.5, z + pz * 16 + 15.5)) continue;
                        int page = (py * 9 + pz) * 9 + px;
                        refreshPages[page >>> 6] |= 1L << (page & 63);
                    }
                }
            }
        }

        private static int clippedPage(double coordinate, int origin) {
            return (int) Math.max(0, Math.min(8, Math.floor((coordinate - origin) / 16)));
        }

        private void writeRefreshPages(int centerX, int centerY, int centerZ) {
            for (int word = 0; word < refreshPages.length; word++) {
                long remaining = refreshPages[word] | previousRefreshPages[word];
                while (remaining != 0) {
                    int page = word * 64 + Long.numberOfTrailingZeros(remaining);
                    remaining &= remaining - 1;
                    if (page >= INFRARED_PAGE_CAPACITY) continue;
                    int sx = centerX - 4 + page % 9;
                    int sz = centerZ - 4 + page / 9 % 9;
                    int sy = centerY - 4 + page / 81;
                    int x = sx * 16, y = sy * 16, z = sz * 16;
                    int pageStart = infraredPayload.size();
                    rawReadFailed = false;
                    var chunk = level.getChunkSource().getChunkNow(sx, sz);
                    PagePublication publication = infraredPublications[page];
                    if (chunk != null && publication == null && input != null) {
                        ThermalPageHandle handle = input.pages.handle(SectionPos.asLong(sx, sy, sz));
                        if (handle != null) publication = currentOrLast(handle);
                    }
                    var dormant = chunk == null ? null : dormantState(chunk);
                    long storedDormantMask = dormant == null ? 0 : dormant.storedBrickMask(sy);
                    var dormantSection = dormant == null ? null : dormant.infraredSection(
                            sy, level.getGameTime(), dormantHalfLifeSeconds(), level, sx, sz, dormantPosition);
                    for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                        loadedNeighbors[(dz + 1) * 3 + dx + 1] =
                                level.getChunkSource().getChunkNow(sx + dx, sz + dz) != null;
                    }
                    long dormantMask = 0;
                    for (int brick = 0; brick < 64; brick++) {
                        int bx = x + (brick & 3) * 4, by = y + (brick >>> 4) * 4, bz = z + (brick >>> 2 & 3) * 4;
                        Arrays.fill(rawTemperatures, Double.NaN);
                        boolean live = chunk != null && copyRawBrick(publication, brick);
                        if (rawReadFailed) break;
                        short dormantValue = dormantSection == null || (storedDormantMask & 1L << brick) == 0
                                ? InfraredBrickCodec.INVALID_TEMPERATURE
                                : dormantSection.temperatures()[brick];
                        boolean usesDormant = !live && dormantValue != InfraredBrickCodec.INVALID_TEMPERATURE;
                        if (usesDormant) Arrays.fill(rawTemperatures, dormantValue * .25);
                        brickFields.clear();
                        if (chunk != null && !level.isOutsideBuildHeight(by)) {
                            for (int f = 0; f < fields.size(); f++) {
                                var field = fields.get(f);
                                if (field.intersects(bx + .5, by + .5, bz + .5, bx + 3.5, by + 3.5, bz + 3.5)) brickFields.add(field);
                            }
                        }
                        boolean composed = false;
                        if (brickFields.isEmpty()) {
                            if (usesDormant) dormantMask |= 1L << brick;
                            else {
                                for (int block = 0; block < 64; block++) {
                                    double temperature = rawTemperatures[block];
                                    infraredBlockTemperatures[block] = Double.isFinite(temperature)
                                            ? quantizeInfrared(temperature) : InfraredBrickCodec.INVALID_TEMPERATURE;
                                }
                                infraredPayload.writeBrick(page * 64 + brick, infraredBlockTemperatures, false);
                            }
                            continue;
                        }
                        for (int block = 0; block < 64; block++) {
                            int px = bx + (block & 3), py = by + (block >>> 4), pz = bz + (block >>> 2 & 3);
                            fieldSample.clear();
                            for (int f = 0; f < brickFields.size(); f++) {
                                var field = brickFields.get(f);
                                if (field.contains(px + .5, py + .5, pz + .5)) fieldSample.include(field);
                            }
                            double temperature = rawTemperatures[block];
                            if (fieldSample.present()) {
                                composed = true;
                                boolean needsNatural = fieldSample.requiresNatural()
                                        || !Double.isFinite(temperature) && fieldSample.requiresBase();
                                if (!needsNatural || hasBiomeNeighbors(px, pz)) {
                                    double natural = needsNatural
                                            ? WorldTemperature.naturalAir(level, dormantPosition.set(px, py, pz)) : 0;
                                    temperature = fieldSample.compose(natural,
                                            Double.isFinite(temperature) ? temperature : natural);
                                }
                            }
                            infraredBlockTemperatures[block] = Double.isFinite(temperature)
                                    ? quantizeInfrared(temperature) : InfraredBrickCodec.INVALID_TEMPERATURE;
                        }
                        if (usesDormant && !composed) dormantMask |= 1L << brick;
                        else infraredPayload.writeBrick(page * 64 + brick, infraredBlockTemperatures, false);
                    }
                    if (rawReadFailed) {
                        infraredPayload.rewind(pageStart);
                        long bit = 1L << (page & 63);
                        refreshPages[page >>> 6] |= bit;
                        infraredPresence[page >>> 6] = (infraredPresence[page >>> 6] & ~bit)
                                | (previousPresence[page >>> 6] & bit);
                        continue;
                    }
                    // Only genuine dormant final Bricks retain dormant ownership.
                    // Ordinary records above already replaced physical/analytic/invalid Bricks.
                    infraredPayload.writeDormantSection(page, dormantMask,
                            dormantSection == null ? null : dormantSection.temperatures(), true);
                }
            }
        }

        private boolean copyRawBrick(PagePublication publication, int brickIndex) {
            if (input == null || publication == null) return false;
            var brick = publication.brick(brickIndex);
            if (brick.coverageSlot() < 0) return brick.signaturePayload() != null;
            if (!input.queryPublication.beginInfraredRead(infraredCursor) || !infraredCursor.valid()
                    || level.getGameTime() - infraredCursor.sampleTick() > MAX_PUBLICATION_AGE_TICKS) {
                rawReadFailed = true;
                return false;
            }
            for (int node = 0; node < brick.transportNodeCount(); node++) {
                if (!infraredCursor.tryRead(brick.coverageSlot() + node, brick.arenaGeneration(),
                        publication.topologyGeneration(), querySample)) {
                    rawReadFailed = true;
                    return false;
                }
                nodeTemperatures[node] = querySample.temperatureC();
            }
            if (!infraredCursor.isCurrent()) {
                rawReadFailed = true;
                return false;
            }
            if (brick.blockLayout() == null) Arrays.fill(rawTemperatures, nodeTemperatures[0]);
            else for (int block = 0; block < 64; block++) {
                int node = brick.blockLayout().transportAt(block);
                if (node >= 0) rawTemperatures[block] = nodeTemperatures[node];
            }
            return true;
        }

        private boolean hasBiomeNeighbors(int x, int z) {
            // Biome zoom may read an adjacent Chunk within two blocks of its edge.
            int dx = (x & 15) < 2 ? -1 : (x & 15) >= 14 ? 1 : 0;
            int dz = (z & 15) < 2 ? -1 : (z & 15) >= 14 ? 1 : 0;
            return loadedNeighbors[4 + dx] && loadedNeighbors[4 + dz * 3] && loadedNeighbors[4 + dz * 3 + dx];
        }

        @Override
        public void close() {
            if (infraredPayload != null) infraredPayload.close();
        }
        InfraredSnapshot capture(
                MinecraftThermalInput input, ServerLevel level,
                int centerChunkX,
                int centerChunkZ,
                int centerSectionY,
                boolean forceFull,
                int lastInfraredEpoch,
                long[] knownPresence,
                long lastDormantRevision,
                long[] knownDormantPresence, long[] knownRefreshPages
        ) {
            this.input = input;
            this.level = level;
            this.previousRefreshPages = knownRefreshPages;
            this.previousPresence = knownPresence;
            collectRefreshPages(centerChunkX, centerSectionY, centerChunkZ);
            long gameTick = level.getGameTime();
            boolean reactivated = input != null && input.queryPublication.noteInfraredRequest(
                    gameTick, INFRARED_ACTIVE_TICKS);
            boolean liveReadable = input != null && input.queryPublication.beginInfraredRead(infraredCursor)
                    && infraredCursor.valid()
                    && gameTick - infraredCursor.sampleTick() <= MAX_PUBLICATION_AGE_TICKS;
            boolean full = forceFull || liveReadable && (reactivated
                    || lastInfraredEpoch == 0
                    || lastInfraredEpoch > infraredCursor.infraredEpoch());
            if (infraredPayload == null) {
                infraredPayload = new InfraredBrickCodec.Builder();
            }
            infraredPayload.reset();
            Arrays.fill(infraredChangedPages, 0L);
            try {
                if (liveReadable) {
                    liveReadable = writeLiveInfrared(
                            centerChunkX, centerSectionY, centerChunkZ,
                            full, lastInfraredEpoch, knownPresence);
                }
                if (!liveReadable) {
                    // A missing live cut must not delete the client's last live coverage.
                    full = forceFull;
                    infraredPayload.reset();
                    Arrays.fill(infraredPublications, null);
                    Arrays.fill(infraredChangedPages, 0L);
                    if (full) {
                        Arrays.fill(infraredPresence, 0L);
                    } else {
                        System.arraycopy(knownPresence, 0, infraredPresence, 0,
                                INFRARED_PRESENCE_WORDS);
                    }
                }
                int currentEpoch = liveReadable ? infraredCursor.infraredEpoch() : 0;
                long dormantRevision = writeDormantInfrared(
                        centerChunkX, centerChunkZ, centerSectionY, gameTick,
                        full, lastDormantRevision, knownDormantPresence, knownPresence);
                writeRefreshPages(centerChunkX, centerSectionY, centerChunkZ);
                boolean presenceChanged = !Arrays.equals(knownPresence, infraredPresence);
                if (!full && !presenceChanged && infraredPayload.size() == 0
                        && Arrays.equals(refreshPages, knownRefreshPages)) {
                    return null;
                }
                byte[] records = infraredPayload.size() == 0
                        ? NO_INFRARED_RECORDS
                        : infraredPayload.toByteArray();
                long[] presence = full || presenceChanged
                        ? infraredPresence.clone()
                        : NO_INFRARED_PRESENCE;
                return new InfraredSnapshot(
                        centerChunkX, centerChunkZ, centerSectionY,
                        currentEpoch, full, presence, records, dormantRevision, refreshPages.clone());
            } finally {
                Arrays.fill(infraredPublications, null);
                fields.clear();
                brickFields.clear();
                this.previousRefreshPages = null;
                this.previousPresence = null;
                this.input = null;
                this.level = null;
            }
        }

        private boolean writeLiveInfrared(
                int centerX, int centerY, int centerZ,
                boolean full, int lastEpoch, long[] knownPresence
        ) {
            int count = input.pages.collectInfraredPages(
                    centerX, centerY, centerZ, infraredHandles, infraredLocalIndexes,
                    infraredPresence);
            for (int index = 0; index < count; index++) {
                PagePublication publication = currentOrLast(infraredHandles[index]);
                if (publication == null) {
                    return false;
                }
                int localPage = Short.toUnsignedInt(infraredLocalIndexes[index]);
                infraredPublications[localPage] = publication;
                if (refreshPage(localPage)) continue;
                boolean added = full || !presenceBit(knownPresence, localPage);
                long changed = added ? -1L : changedBrickMask(publication.workerPageSlot(), lastEpoch);
                if (changed != 0L) {
                    infraredChangedPages[localPage >>> 6] |= 1L << (localPage & 63);
                }
                while (changed != 0L) {
                    int brick = Long.numberOfTrailingZeros(changed);
                    if (!writeInfraredBrick(publication, localPage, brick, added)) {
                        return false;
                    }
                    changed &= changed - 1L;
                }
            }
            // All live values are now copied into the payload; later worker writes cannot alter them.
            return infraredCursor.isCurrent();
        }

        private long writeDormantInfrared(
                int centerChunkX,
                int centerChunkZ,
                int centerSectionY,
                long gameTick,
                boolean full,
                long lastRevision,
                long[] knownDormantPresence,
                long[] knownLivePresence
        ) {
            Arrays.fill(infraredDormantPresence, 0L);
            long currentRevision = full ? 0L : lastRevision;
            double halfLifeSeconds = dormantHalfLifeSeconds();
            for (int dz = -4; dz <= 4; dz++) {
                for (int dx = -4; dx <= 4; dx++) {
                    int sectionX = centerChunkX + dx;
                    int sectionZ = centerChunkZ + dz;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(
                            sectionX, sectionZ);
                    DormantChunkThermalState state = chunk == null
                            ? null : dormantState(chunk);
                    if (state == null) {
                        continue;
                    }
                    for (int dy = -4; dy <= 4; dy++) {
                        int localPageIndex = ((dy + 4) * 9 + (dz + 4)) * 9
                                + dx + 4;
                        int sectionY = centerSectionY + dy;
                        if (refreshPage(localPageIndex)) continue;
                        long stored = state.storedBrickMask(sectionY);
                        PagePublication publication = infraredPublications[localPageIndex];
                        long resolved = resolvedInfraredBricks(publication, stored);
                        long brickMask = stored & ~resolved;
                        if (brickMask == 0L) {
                            continue;
                        }
                        DormantChunkThermalState.InfraredSection snapshot = state.infraredSection(
                                sectionY, gameTick, halfLifeSeconds, level,
                                sectionX, sectionZ, dormantPosition);
                        resolved |= resolvedInfraredBricks(
                                publication, snapshot.changedBrickMask() & ~stored);
                        infraredDormantPresence[localPageIndex >>> 6] |= 1L << (localPageIndex & 63);
                        currentRevision = Math.max(currentRevision, snapshot.revision());
                        boolean replace = full || !presenceBit(knownDormantPresence, localPageIndex)
                                || presenceBit(infraredChangedPages, localPageIndex)
                                || presenceBit(knownLivePresence, localPageIndex)
                                != presenceBit(infraredPresence, localPageIndex);
                        if (replace || snapshot.revision() > lastRevision) {
                            replace |= snapshot.previousRevision() == 0L
                                    || lastRevision < snapshot.previousRevision();
                            long written = replace ? brickMask : snapshot.changedBrickMask() & ~resolved;
                            if (replace || written != 0L) {
                                infraredPayload.writeDormantSection(localPageIndex, written,
                                        snapshot.temperatures(), replace);
                            }
                        }
                    }
                }
            }
            if (!full) {
                for (int word = 0; word < INFRARED_PRESENCE_WORDS; word++) {
                    long removed = knownDormantPresence[word] & ~infraredDormantPresence[word];
                    while (removed != 0L) {
                        int section = word * 64 + Long.numberOfTrailingZeros(removed);
                        if (!refreshPage(section)) infraredPayload.writeDormantSection(section, 0L, null, true);
                        removed &= removed - 1L;
                    }
                }
            }
            return currentRevision;
        }

        private static long resolvedInfraredBricks(PagePublication publication, long candidates) {
            long result = 0L;
            if (publication != null) {
                while (candidates != 0L) {
                    int brick = Long.numberOfTrailingZeros(candidates);
                    if (publication.brick(brick).resolved()) result |= 1L << brick;
                    candidates &= candidates - 1L;
                }
            }
            return result;
        }

        private long changedBrickMask(int pageSlot, int lastInfraredEpoch) {
            if (infraredCursor.pageChangeEpoch(pageSlot) <= lastInfraredEpoch) {
                return 0L;
            }
            long result = 0L;
            for (int brick = 0; brick < 64; brick++) {
                if (infraredCursor.brickChangeEpoch(pageSlot, brick)
                        > lastInfraredEpoch) {
                    result |= 1L << brick;
                }
            }
            return result;
        }

        private boolean writeInfraredBrick(
                PagePublication publication,
                int localPageIndex,
                int brickIndex,
                boolean omitInvalid
        ) {
            PagePublication.Brick brick = publication.brick(brickIndex);
            int localBrickIndex = localPageIndex * 64 + brickIndex;
            int slot = brick.coverageSlot();
            if (slot == PagePublication.NO_AIR_POINT) {
                infraredPayload.writeInvalid(localBrickIndex, omitInvalid);
                return true;
            }
            if (brick.blockLayout() == null) {
                if (!infraredCursor.tryRead(
                        slot,
                        brick.arenaGeneration(),
                        publication.topologyGeneration(),
                        querySample)) {
                    return false;
                }
                infraredPayload.writeUniform(
                        localBrickIndex,
                        quantizeInfrared(querySample.temperatureC()));
                return true;
            }

            Arrays.fill(
                    infraredBlockTemperatures,
                    InfraredBrickCodec.INVALID_TEMPERATURE);
            int brickX = (brickIndex & 3) << 2;
            int brickZ = (brickIndex >>> 2 & 3) << 2;
            int brickY = (brickIndex >>> 4) << 2;
            int uniqueCount = 0;
            for (int block = 0; block < 64; block++) {
                int resolvedSlot = publication.resolveAirPoint(
                        brickX + (block & 3),
                        brickY + (block >>> 4),
                        brickZ + (block >>> 2 & 3));
                if (resolvedSlot == PagePublication.NO_AIR_POINT) {
                    continue;
                }
                int unique = 0;
                while (unique < uniqueCount
                        && infraredUniqueSlots[unique] != resolvedSlot) {
                    unique++;
                }
                if (unique == uniqueCount) {
                    if (!infraredCursor.tryRead(
                            resolvedSlot,
                            brick.arenaGeneration(),
                            publication.topologyGeneration(),
                            querySample)) {
                        return false;
                    }
                    infraredUniqueSlots[uniqueCount] = resolvedSlot;
                    infraredUniqueTemperatures[uniqueCount] =
                            quantizeInfrared(querySample.temperatureC());
                    uniqueCount++;
                }
                infraredBlockTemperatures[block] =
                        infraredUniqueTemperatures[unique];
            }
            infraredPayload.writeBrick(
                    localBrickIndex, infraredBlockTemperatures, omitInvalid);
            return true;
        }

    }

    private static boolean presenceBit(long[] presence, int localPageIndex) {
        return (presence[localPageIndex >>> 6]
                & 1L << (localPageIndex & 63)) != 0L;
    }

    private static PagePublication currentOrLast(ThermalPageHandle handle) {
        PagePublication current = handle.currentPublication();
        return current == null ? handle.lastPublication() : current;
    }

    private static short quantizeInfrared(double temperatureC) {
        long value = Math.round(temperatureC * 4.0D);
        return (short) Math.max(-32767L, Math.min(32767L, value));
    }

    public record InfraredSnapshot(
            int centerChunkX,
            int centerChunkZ,
            int centerSectionY,
            int infraredEpoch,
            boolean full,
            long[] presence,
            byte[] brickRecords,
            long dormantRevision,
            long[] refreshPages
    ) {
        public InfraredSnapshot {
            if (infraredEpoch < 0 || presence == null || brickRecords == null
                    || presence.length != 0
                    && presence.length != INFRARED_PRESENCE_WORDS
                    || full && presence.length != INFRARED_PRESENCE_WORDS
                    || brickRecords.length
                            > InfraredBrickCodec.MAX_PAYLOAD_BYTES) {
                throw new IllegalArgumentException("invalid infrared snapshot");
            }
        }
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
        if (!data.willTransit() || data.heatCapacity() <= 0) {
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
                int pageFlags = flags & (MinecraftThermalProfiles.TOPOLOGY_MUTATION
                        | MinecraftThermalProfiles.SOURCE_MUTATION);
                if (pageFlags != 0) {
                    input.pages.onBlockMutation(
                            owner, localX, localY, localZ,
                            (flags & MinecraftThermalProfiles.TOPOLOGY_MUTATION) != 0,
                            (flags & MinecraftThermalProfiles.SOURCE_MUTATION) != 0);
                }
                if ((flags & MinecraftThermalProfiles.RADIATION_MUTATION) != 0
                        && input.blockRadiation != null) {
                    input.blockRadiation.markBlock(
                            owner, localX, localY, localZ);
                }
                if ((flags & MinecraftThermalProfiles.OCCLUSION_MUTATION) != 0) {
                    long sectionKey = owner.sectionKey();
                    input.radiationOcclusion.onSectionMutation(
                            SectionPos.x(sectionKey),
                            SectionPos.y(sectionKey),
                            SectionPos.z(sectionKey));
                }
            }
        }
    }

    public static void onRadiantLiquidNeighborChanged(
            ServerLevel level,
            BlockPos position
    ) {
        MinecraftThermalInput input = active(level);
        if (input != null && input.blockRadiation != null) {
            input.blockRadiation.markBlock(
                    position.getX(), position.getY(), position.getZ());
        }
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        DormantChunkThermalState state = dormantState(chunk);
        if (state != null && state.activateLoaded(
                level.getGameTime(), dormantHalfLifeSeconds())) {
            if (state.isEmpty()) {
                setDormantState(chunk, null);
            }
            chunk.setUnsaved(true);
        }
        MinecraftThermalInput input = active(level);
        if (input != null) {
            input.attachLoadedChunk(chunk);
            if (input.blockRadiation != null) {
                input.blockRadiation.onChunkLoad(chunk);
            }
            input.radiationOcclusion.onChunkLoad(chunk);
        }
    }

    public static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            input.pendingSourceChunks.remove(chunk.getPos().toLong(), chunk);
            LevelChunk current = level.getChunkSource().getChunkNow(
                    chunk.getPos().x, chunk.getPos().z);
            if (current != null && current != chunk) return;
            if (input.blockRadiation != null) {
                input.blockRadiation.onChunkUnload(chunk);
            }
            input.pages.onChunkUnload(chunk);
            input.finishDormantCheckpoint(chunk, true);
            input.physicalSources.beforeChunkUnload(
                    chunk, level.getGameTime());
            input.radiationOcclusion.onChunkUnload(chunk);
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
        if (input != null) {
            if (input.radiation != null) {
                input.radiation.removeReceiver(receiverKey(player));
            }
        }
    }

    public static void onPlayerChangedDimension(
            ServerPlayer player,
            ServerLevel previousLevel
    ) {
        MinecraftThermalInput input = active(previousLevel);
        if (input != null && input.radiation != null) {
            input.radiation.removeReceiver(receiverKey(player));
        }
    }

    public static void closeAll() {
        MinecraftThermalInput[] inputs;
        synchronized (ACTIVE) {
            inputs = ACTIVE.values().toArray(MinecraftThermalInput[]::new);
        }
        for (MinecraftThermalInput input : inputs) input.close();
        ThermalWorkerPool.closeShared();
        if (infraredCapture != null) {
            infraredCapture.close();
            infraredCapture = null;
        }
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
            if (input.blockRadiation != null) {
                input.blockRadiation.onSectionIdentityReplaced(
                        SectionPos.asLong(
                                chunk.getPos().x,
                                chunk.getSectionYFromSectionIndex(sectionIndex),
                                chunk.getPos().z));
            }
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
        pages.checkpointAll(true, true);
        closed = true;
        pendingSourceChunks.clear();
        physicalSources.close();
        if (blockRadiation != null) blockRadiation.close();
        pages.close();
        environment.close();
        if (radiation != null) radiation.close();
        mailbox.close();
        synchronized (ACTIVE) {
            ACTIVE.remove(level, this);
        }
    }

    public ThermalInputBatch.DormantAirCut dormantAdmissionCut(
            long sectionKey,
            double naturalTemperatureC,
            long gameTick
    ) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        DormantChunkThermalState state = chunk == null ? null : dormantState(chunk);
        return state == null ? null : state.admissionCut(
                SectionPos.y(sectionKey), gameTick,
                profiles.tuning().dormantTemperatureHalfLifeSeconds(),
                naturalTemperatureC);
    }

    public void captureDormantPage(ThermalPageHandle page, boolean markDirty) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.x(page.sectionKey()), SectionPos.z(page.sectionKey()));
        if (chunk != null) {
            captureDormantPage(page, chunk, markDirty);
        }
    }

    public void captureDormantPage(
            ThermalPageHandle page,
            LevelChunk chunk,
            boolean markDirty
    ) {
        PagePublication publication;
        DormantChunkThermalState.CaptureResult captured = null;
        int sectionX = SectionPos.x(page.sectionKey());
        int sectionY = SectionPos.y(page.sectionKey());
        int sectionZ = SectionPos.z(page.sectionKey());
        dormantPosition.set(
                SectionPos.sectionToBlockCoord(sectionX) + 8,
                SectionPos.sectionToBlockCoord(sectionY) + 8,
                SectionPos.sectionToBlockCoord(sectionZ) + 8);
        double natural = WorldTemperature.naturalAir(level, dormantPosition);
        for (int attempt = 0; attempt < 2; attempt++) {
            publication = page.lastPublication();
            if (publication == null) {
                return;
            }
            captured = DormantChunkThermalState.capture(
                    publication,
                    queryPublication,
                    querySample,
                    natural,
                    dormantCapture);
            if (captured.valid() && page.lastPublication() == publication) {
                break;
            }
            captured = null;
        }
        if (captured == null || !captured.valid()) {
            return;
        }
        DormantChunkThermalState state = dormantState(chunk);
        if (state == null && captured.entry() != null) {
            state = new DormantChunkThermalState(
                    chunk.getSectionYFromSectionIndex(0),
                    chunk.getSections().length);
            setDormantState(chunk, state);
        }
        if (state == null) {
            return;
        }
        boolean changed = state.replace(sectionY, captured.entry());
        if (captured.entry() != null) {
            changed |= state.updateSourceSupport(
                    sectionY,
                    physicalSources.supportsDormantSection(
                            page.sectionKey()));
        }
        if (state.isEmpty()) {
            setDormantState(chunk, null);
        }
        if (changed && markDirty) {
            chunk.setUnsaved(true);
        }
    }

    public void finishDormantCheckpoint(LevelChunk chunk, boolean markDirty) {
        DormantChunkThermalState state = dormantState(chunk);
        if (state == null) {
            return;
        }
        boolean changed = state.rebaseForSave(
                level.getGameTime(),
                profiles.tuning().dormantTemperatureHalfLifeSeconds());
        changed |= state.refreshSourceSupport(
                chunk.getPos().x,
                chunk.getPos().z,
                physicalSources::supportsDormantSection);
        if (state.isEmpty()) {
            setDormantState(chunk, null);
        }
        if (changed && markDirty) {
            chunk.setUnsaved(true);
        }
    }

    public void updateDormantSourceSupport(
            long sectionKey,
            boolean supported
    ) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        DormantChunkThermalState state = chunk == null
                ? null : dormantState(chunk);
        if (state != null && state.updateSourceSupport(
                SectionPos.y(sectionKey), supported)) {
            chunk.setUnsaved(true);
        }
    }

    static void checkpointForSave(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            input.pages.checkpointChunk(chunk, false, true);
        }
    }

    static void checkpointAllForStop() {
        MinecraftThermalInput[] inputs;
        synchronized (ACTIVE) {
            inputs = ACTIVE.values().toArray(MinecraftThermalInput[]::new);
        }
        for (MinecraftThermalInput input : inputs) {
            input.pages.checkpointAll(true, true);
        }
    }

    private static double dormantTemperature(
            ServerLevel level,
            int blockX,
            int blockY,
            int blockZ,
            long gameTick,
            BlockPos.MutableBlockPos naturalPosition
    ) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockZ));
        return dormantTemperature(
                level, chunk, blockX, blockY, blockZ,
                gameTick, naturalPosition);
    }

    private static double dormantTemperature(
            ServerLevel level,
            LevelChunk chunk,
            int blockX,
            int blockY,
            int blockZ,
            long gameTick,
            BlockPos.MutableBlockPos naturalPosition
    ) {
        DormantChunkThermalState state = chunk == null ? null : dormantState(chunk);
        if (state == null) {
            return Double.NaN;
        }
        int sectionX = SectionPos.blockToSectionCoord(blockX);
        int sectionY = SectionPos.blockToSectionCoord(blockY);
        int sectionZ = SectionPos.blockToSectionCoord(blockZ);
        int brick = SectionPos.sectionRelative(blockX) >>> 2
                | (SectionPos.sectionRelative(blockZ) >>> 2) << 2
                | (SectionPos.sectionRelative(blockY) >>> 2) << 4;
        return state.sample(
                sectionY, brick, gameTick,
                dormantHalfLifeSeconds(),
                level, sectionX, sectionZ, naturalPosition);
    }

    static DormantChunkThermalState dormantState(LevelChunk chunk) {
        return ((MinecraftThermalChunkAttachment) (Object) chunk)
                .frostedheart$getDormantThermalState();
    }

    static void setDormantState(
            LevelChunk chunk,
            DormantChunkThermalState state
    ) {
        ((MinecraftThermalChunkAttachment) (Object) chunk)
                .frostedheart$setDormantThermalState(state);
    }

    static double dormantHalfLifeSeconds() {
        return MinecraftThermalProfiles.dormantTemperatureHalfLifeSeconds();
    }

    private static MinecraftThermalInput start(
            ServerLevel level,
            double initialTemperatureC,
            BlockPos center
    ) {
        synchronized (ACTIVE) {
            MinecraftThermalInput existing = ACTIVE.get(level);
            if (existing != null) return existing;
            MinecraftThermalInput created = null;
            try {
                created = new MinecraftThermalInput(level, initialTemperatureC);
                created.attachLoadedWorld(center);
                ACTIVE.put(level, created);
                return created;
            } catch (RuntimeException failure) {
                if (created != null) created.close();
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



}
