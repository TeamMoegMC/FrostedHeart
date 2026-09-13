/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticFieldIndex;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
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
    private static final int MAXIMUM_PHYSICAL_SOURCES = 65_536;
    private static final int SOURCE_DISCOVERY_CHUNKS_PER_TICK = 8;
    private static final int MAXIMUM_SOURCE_NODES = 131_072;
    private static final int MAXIMUM_RADIATION_SECTIONS = 3_200;
    static final int GAMEPLAY_ITEM_ENVIRONMENT_SAMPLES_PER_TICK = 64;
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
    private final MaterialSectionState.CaptureScratch materialCapture = new MaterialSectionState.CaptureScratch();
    private final QueryPublication.InfraredReadCursor checkpointCursor = new QueryPublication.InfraredReadCursor();
    private static final ThreadLocal<QueryPublication.MutableMaterialSample> MATERIAL_SAMPLE =
            ThreadLocal.withInitial(QueryPublication.MutableMaterialSample::new);
    private final BlockPos.MutableBlockPos dormantPosition =
            new BlockPos.MutableBlockPos();
    private final RadiationService.MutableSample radiationSample =
            new RadiationService.MutableSample();
    private final ThermalEnvironmentSample passiveScratch =
            new ThermalEnvironmentSample();
    private final ThermalEnvironmentSample townScratch =
            new ThermalEnvironmentSample();
    private final ItemEnvironmentSampleCache itemEnvironmentCache =
            new ItemEnvironmentSampleCache(GAMEPLAY_ITEM_ENVIRONMENT_SAMPLES_PER_TICK);
    private final BlockPos.MutableBlockPos townPosition =
            new BlockPos.MutableBlockPos();
    private static InfraredCapture infraredCapture;

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
        referenceTemperatureC = 0;
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
        createWorker(initialTick, referenceTemperatureC);
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
                262_144, 65_536,
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
            sampleDormant(loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            return;
        }
        PagePublication.Brick coverage = publication.brickAt(
                localX, localY, localZ);
        int slot = publication.resolveAirPoint(
                localX, localY, localZ);
        if (slot == PagePublication.NO_AIR_POINT) {
            if (sampleRoutedAir(page, publication, coverage, (localX & 3) | (localZ & 3) << 2 | (localY & 3) << 4,
                    sampleTick, maximumAgeTicks, out)) return;
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
            sampleDormant(loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            return;
        }
        if (page.currentPublication() != publication) {
            sampleDormant(loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            return;
        }
        if (sampleTick - querySample.sampleTick() > maximumAgeTicks) {
            sampleDormant(
                    loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            return;
        }
        out.setAir(querySample.temperatureC());
    }

    private boolean sampleRoutedAir(ThermalPageHandle owner, PagePublication ownerPublication,
            PagePublication.Brick brick, int block, long sampleTick, int maximumAgeTicks, ThermalEnvironmentSample out) {
        var layout = brick.blockLayout();
        if (layout == null || !layout.hasAirRoute(block)) return false;
        long target = layout.routedAirBlock(block);
        int x = BlockPos.getX(target), y = BlockPos.getY(target), z = BlockPos.getZ(target);
        ThermalPageHandle page = pages.handle(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        PagePublication publication = page == null ? null : page.currentPublication();
        if (publication == null) return false;
        var coverage = publication.brickAt(x & 15, y & 15, z & 15);
        int slot = publication.resolveAirPoint(x & 15, y & 15, z & 15);
        if (slot < 0 || !queryPublication.tryRead(slot, coverage.arenaGeneration(), publication.topologyGeneration(), querySample)
                || sampleTick - querySample.sampleTick() > maximumAgeTicks || page.currentPublication() != publication
                || owner.currentPublication() != ownerPublication || !layout.hasAirRoute(block)) return false;
        out.setAirFromRegion(querySample.temperatureC(), target);
        return true;
    }

    private void awaitLatestCompletion() {
        if (inFlight != null) {
            mailbox.awaitCompletion();
            drainCompletion();
        }
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

    /**
     * Samples a dropped reservoir without starting a physical runtime or admitting
     * Pages. Cache only physical inputs; world fields are composed at the exact
     * receiver position on every query, including same-tick field updates.
     */
    public static void gameplayItemEnvironment(
            ItemEntity entity, double naturalTemperatureC, ThermalEnvironmentSample out
    ) {
        Objects.requireNonNull(entity, "entity");
        if (!(entity.level() instanceof ServerLevel server)) {
            out.clear();
            return;
        }
        gameplayExposedReservoirEnvironment(server, receiverKey(entity), entity.getId() & Integer.MAX_VALUE,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                naturalTemperatureC, out);
    }

    /** Samples just above a placed reservoir using the same budget and boundary as dropped items. */
    public static void gameplayPlacedReservoirEnvironment(
            ServerLevel server, BlockPos position, double naturalTemperatureC, ThermalEnvironmentSample out
    ) {
        gameplayExposedReservoirEnvironment(server, position.asLong(), 0,
                position.getX() + 0.5D, position.getY() + 0.3125D, position.getZ() + 0.5D,
                naturalTemperatureC, out);
    }

    private static void gameplayExposedReservoirEnvironment(
            ServerLevel server, long receiverIdentity, int receiverGeneration,
            double x, double y, double z, double naturalTemperatureC, ThermalEnvironmentSample out
    ) {
        Objects.requireNonNull(out, "out").clear();
        if (!server.getServer().isSameThread() || !Double.isFinite(naturalTemperatureC)) return;
        MinecraftThermalInput input = active(server);
        long tick = server.getGameTime();
        if (input == null) {
            double dormant = dormantTemperature(server, floor(x), floor(y), floor(z),
                    tick, DORMANT_QUERY_POSITION.get());
            if (Double.isFinite(dormant)) out.setAir(dormant);
        } else {
            int quarterX = floorQuarter(x);
            int quarterY = floorQuarter(y);
            int quarterZ = floorQuarter(z);
            int cached = input.itemEnvironmentCache.find(tick, quarterX, quarterY, quarterZ);
            if (cached >= 0) {
                input.itemEnvironmentCache.copyTo(cached, out);
            } else {
                input.sampleAir(x, y, z, tick, MAX_PUBLICATION_AGE_TICKS, out);
                if (input.itemEnvironmentCache.canAdmit() && input.radiation != null) {
                    input.radiation.sampleItem(receiverIdentity, receiverGeneration,
                            x, y, z, input.radiationSample);
                    out.setRadiation(input.radiationSample.radiantFluxWPerM2());
                }
                input.itemEnvironmentCache.store(quarterX, quarterY, quarterZ, out);
            }
        }
        double base = out.airAvailable() ? out.airTemperatureC() : naturalTemperatureC;
        ThermalAnalyticFieldIndex fields = input == null
                ? MinecraftGameplayFields.existing(server) : input.analyticFields;
        out.setComposedAir(fields == null ? base
                : fields.compose(x, y, z, naturalTemperatureC, base));
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
            ServerPlayer player, boolean forceFull, long knownGeneration, long knownCenter,
            int lastEpoch, long[] knownPresence, boolean knownReadable, long[] knownFieldPages, long knownStoredEpoch) {
        if (knownPresence.length != INFRARED_PRESENCE_WORDS
                || knownFieldPages.length != 0 && knownFieldPages.length != INFRARED_PRESENCE_WORDS)
            throw new IllegalArgumentException("infrared presence requires 12 words");
        if (infraredCapture == null) infraredCapture = new InfraredCapture();
        return infraredCapture.capture(active(player.serverLevel()), player, forceFull,
                knownGeneration, knownCenter, lastEpoch, knownPresence, knownReadable, knownFieldPages, knownStoredEpoch);
    }

    public static double materialTemperature(ServerLevel level, BlockPos position) {
        var sample = MATERIAL_SAMPLE.get();
        return sampleMaterial(level, position, sample) ? sample.temperatureC() : Double.NaN;
    }

    public boolean matchesMaterialRequest(com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime.Request request) {
        int x = request.blockX(), y = request.blockY(), z = request.blockZ();
        long section = SectionPos.asLong(x >> 4, y >> 4, z >> 4);
        ThermalPageHandle handle = pages.handle(section);
        PagePublication publication = handle == null ? null : handle.lastPublication();
        if (publication == null || pages.materialChangedSince(section, (x & 15) | (z & 15) << 4 | (y & 15) << 8,
                publication.geometryRevision())) return false;
        var brick = publication.brickAt(x & 15, y & 15, z & 15);
        if (brick.firstSlot() < 0 || brick.blockLayout() == null) return false;
        int node = brick.blockLayout().nodeAt((x & 3) | (z & 3) << 2 | (y & 3) << 4);
        var sample = MATERIAL_SAMPLE.get();
        if (node < 0 || !queryPublication.tryReadMaterial(brick.firstSlot() + node, brick.arenaGeneration(),
                publication.topologyGeneration(), sample) || sample.requestSequence() != request.requestSequence()
                || sample.branch() != request.materialBranch()) return false;
        var edge = sample.law().transition(sample.branch());
        return edge != null && edge.targetStateId() == request.targetStateId() && edge.complete(sample.enthalpyJ());
    }

    /** Reads a body without creating physical residency or substituting environmental temperature. */
    public static boolean sampleMaterial(ServerLevel level, BlockPos position,
            QueryPublication.MutableMaterialSample out) {
        out.clear();
        MinecraftThermalInput input = active(level);
        int x = position.getX(), y = position.getY(), z = position.getZ();
        long section = SectionPos.asLong(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(y),
                SectionPos.blockToSectionCoord(z));
        if (input != null) {
            ThermalPageHandle handle = input.pages.handle(section);
            if (handle != null) {
                PagePublication publication = handle.lastPublication();
                if (publication == null || publication.geometryRevision() != handle.liveGeometryRevision()
                        && input.pages.materialChangedSince(section, (x & 15) | (z & 15) << 4 | (y & 15) << 8,
                                publication.geometryRevision())) return false;
                var brick = publication.brickAt(x & 15, y & 15, z & 15);
                if (brick.resolved() && brick.firstSlot() >= 0 && brick.blockLayout() != null) {
                    int node = brick.blockLayout().nodeAt((x & 3) | (z & 3) << 2 | (y & 3) << 4);
                    if (node >= 0) return input.queryPublication.tryReadMaterial(brick.firstSlot() + node,
                            brick.arenaGeneration(), publication.topologyGeneration(), out)
                            && handle.lastPublication() == publication;
                }
            }
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.x(section), SectionPos.z(section));
        DormantChunkThermalState stored = chunk == null ? null : dormantState(chunk);
        if (stored == null || !stored.hasMaterials(SectionPos.y(section))) return false;
        BlockState state = chunk.getBlockState(position);
        var law = MinecraftThermalProfiles.materialLaw(state);
        if (law == null) return false;
        return stored.readMaterial(SectionPos.y(section), (x & 15) | (z & 15) << 4 | (y & 15) << 8,
                net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.getId(state), law, out);
    }

    /** Shared main-thread scratch; display reads never start a runtime or load chunks. */
    private static final class InfraredCapture implements AutoCloseable {
        private final QueryPublication.InfraredReadCursor cursor = new QueryPublication.InfraredReadCursor();
        private final ThermalPageHandle[] handles = new ThermalPageHandle[INFRARED_PAGE_CAPACITY];
        private final ThermalPageHandle[] localHandles = new ThermalPageHandle[INFRARED_PAGE_CAPACITY];
        private final short[] localIndexes = new short[INFRARED_PAGE_CAPACITY];
        private final long[] presence = new long[INFRARED_PRESENCE_WORDS];
        private final long[] fieldPages = new long[INFRARED_PRESENCE_WORDS];
        private final long[] storedPresence = new long[INFRARED_PRESENCE_WORDS];
        private final LevelChunk[] loadedChunks = new LevelChunk[81];
        private final double[] storedTemperatures = new double[4096];
        private final long[] changedMaterialBlocks = new long[64];
        private long changedMaterialBricks;
        private final QueryPublication.MutableMaterialSample storedSample = new QueryPublication.MutableMaterialSample();
        private int profileEpoch = -1;
        private long profileRevision;
        private final short[] nodes = new short[64], blocks = new short[64];
        private final double[] rawNodes = new double[64], rawBlocks = new double[64];
        private final java.util.ArrayList<ThermalAnalyticField> fields = new java.util.ArrayList<>();
        private final java.util.ArrayList<ThermalAnalyticField> brickFields = new java.util.ArrayList<>();
        private final ThermalAnalyticFieldIndex.Sample fieldSample = new ThermalAnalyticFieldIndex.Sample();
        private final QueryPublication.MutableSample sample = new QueryPublication.MutableSample();
        private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        private final InfraredBrickCodec.Builder payload = new InfraredBrickCodec.Builder();
        private final boolean[] loadedNeighbors = new boolean[9];
        private boolean neighborsReady;
        private ServerLevel level;
        private int originX, originY, originZ;

        InfraredSnapshot capture(MinecraftThermalInput input, ServerPlayer player, boolean forceFull,
                long knownGeneration, long knownCenter, int lastEpoch, long[] knownPresence,
                boolean knownReadable, long[] knownFieldPages, long knownStoredEpoch) {
            level = player.serverLevel();
            int cx = Mth.floor(player.getX()) >> 4, cy = Mth.floor(player.getEyeY()) >> 4;
            int cz = Mth.floor(player.getZ()) >> 4;
            originX = (cx - 4) * 16; originY = (cy - 4) * 16; originZ = (cz - 4) * 16;
            long center = SectionPos.asLong(cx, cy, cz), tick = level.getGameTime();
            long generation = input == null ? 0 : input.dimensionGeneration;
            boolean reactivated = input != null && input.queryPublication.noteInfraredRequest(tick, INFRARED_ACTIVE_TICKS);
            try {
                if (profileEpoch != MinecraftThermalProfiles.profileEpoch()) {
                    profileEpoch = MinecraftThermalProfiles.profileEpoch();
                    profileRevision = DormantChunkThermalState.nextMaterialRevision();
                }
                collectFields();
                collectStoredMaterials(cx, cy, cz);
                long storedEpoch = DormantChunkThermalState.currentMaterialRevision();
                // A concurrent publication exchange is not a material deletion. Retry, then retain the client baseline.
                captureAttempt: for (int attempt = 0; attempt < 2; attempt++) {
                    payload.reset(); cursor.clear();
                    Arrays.fill(presence, 0L); Arrays.fill(localHandles, null);
                    if (input != null && !input.queryPublication.beginInfraredRead(cursor)) continue;
                    boolean readable = input != null && cursor.valid()
                            && tick - cursor.sampleTick() <= MAX_PUBLICATION_AGE_TICKS;
                    int epoch = readable ? cursor.infraredEpoch() : 0;
                    boolean full = forceFull || knownGeneration != generation || knownCenter != center || knownStoredEpoch > storedEpoch
                            || knownReadable != readable || readable && (reactivated || lastEpoch == 0 || lastEpoch > epoch);
                    if (input != null) collectMaterials(input, cx, cy, cz, readable);
                    for (int word = 0; word < presence.length; word++) {
                        long previousFields = full || knownFieldPages.length == 0 ? 0 : knownFieldPages[word];
                        long work = presence[word] | storedPresence[word] | fieldPages[word]
                                | (full ? 0 : knownPresence[word] | previousFields);
                        while (work != 0) {
                            int local = word * 64 + Long.numberOfTrailingZeros(work);
                            work &= work - 1;
                            if (local >= INFRARED_PAGE_CAPACITY) continue;
                            LevelChunk chunk = loadedChunks[local % 81];
                            int sectionY = cy - 4 + local / 81;
                            var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
                            var state = attachment == null ? null : attachment.frostedheart$getDormantThermalState();
                            MaterialSectionState stored = state == null ? null : state.materials(sectionY);
                            long storedRevision = attachment == null ? 0 : Math.max(attachment.frostedheart$getMaterialRevision(),
                                    state == null ? 0 : state.materialRevision(sectionY));
                            ThermalPageHandle handle = localHandles[local];
                            if (handle == null && input != null && stored != null) {
                                handle = input.pages.handle(SectionPos.asLong(cx - 4 + local % 9,
                                        cy - 4 + local / 81, cz - 4 + local / 9 % 9));
                            }
                            PagePublication page = !readable || handle == null ? null : handle.lastPublication();
                            if (page != null && page.topologyGeneration() > cursor.topologyGeneration()) continue captureAttempt;
                            if (page == null) {
                                presence[word] &= ~(1L << (local & 63));
                            }
                            if (handle != null && page == null) stored = null;
                            changedMaterialBricks = page != null && page.geometryRevision() != handle.liveGeometryRevision()
                                    ? input.pages.collectMaterialChangesSince(handle.sectionKey(), page.geometryRevision(), changedMaterialBlocks) : 0L;
                            if (changedMaterialBricks != 0L) {
                                presence[word] &= ~(1L << (local & 63));
                                for (int b = 0; b < 64; b++) if (readableSurfaceMask(page.brick(b), b) != 0) {
                                    presence[word] |= 1L << (local & 63);
                                    break;
                                }
                            }
                            long storedMask = storedBrickMask(page, stored);
                            if (storedMask != 0) presence[word] |= 1L << (local & 63);
                            boolean refresh = presenceBit(fieldPages, local) || (previousFields & 1L << (local & 63)) != 0;
                            boolean replace = full || refresh || presenceBit(presence, local) != presenceBit(knownPresence, local);
                            long changed = replace ? -1L : page == null ? 0 : changedBricks(page.workerPageSlot(), lastEpoch);
                            changed |= changedMaterialBricks;
                            if (storedRevision > knownStoredEpoch
                                    || storedMask != 0 && profileRevision > knownStoredEpoch) changed = -1L;
                            if (changed == 0) continue;
                            payload.beginPage();
                            if (!writePage(page, stored, storedMask, local, changed, full)
                                    || page != null && handle.lastPublication() != page) continue captureAttempt;
                        }
                    }
                    if (input != null && !cursor.isCurrent()) continue;
                    boolean changedPresence = !Arrays.equals(presence, knownPresence);
                    if (!full && !changedPresence && payload.size() == 0 && sameFieldPages(knownFieldPages)) return null;
                    return new InfraredSnapshot(cx, cz, cy, generation, epoch, readable, full,
                            full || changedPresence ? presence.clone() : NO_INFRARED_PRESENCE,
                            hasFieldPages() ? fieldPages.clone() : NO_INFRARED_PRESENCE, payload.finishParts(), storedEpoch);
                }
                return null;
            } finally {
                cursor.clear(); Arrays.fill(handles, null); Arrays.fill(localHandles, null);
                Arrays.fill(loadedChunks, null);
                fields.clear(); brickFields.clear(); level = null; payload.reset();
            }
        }

        private void collectMaterials(MinecraftThermalInput input, int cx, int cy, int cz, boolean readable) {
            int count = input.pages.collectInfraredPages(cx, cy, cz, handles, localIndexes, presence);
            Arrays.fill(presence, 0L);
            for (int i = 0; i < count; i++) {
                ThermalPageHandle handle = handles[i];
                int local = Short.toUnsignedInt(localIndexes[i]);
                localHandles[local] = handle;
                PagePublication page = handle.lastPublication();
                if (!readable || page == null || page.topologyGeneration() > cursor.topologyGeneration()) continue;
                for (int b = 0; b < 64; b++) {
                    if (surfaceMask(page.brick(b)) == 0) continue;
                    presence[local >>> 6] |= 1L << (local & 63);
                    break;
                }
            }
        }

        private void collectStoredMaterials(int cx, int cy, int cz) {
            Arrays.fill(storedPresence, 0);
            for (int z = 0; z < 9; z++) for (int x = 0; x < 9; x++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx - 4 + x, cz - 4 + z);
                loadedChunks[x + 9 * z] = chunk;
                if (chunk == null) continue;
                var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
                var state = attachment.frostedheart$getDormantThermalState();
                if (state == null) continue;
                for (int y = 0; y < 9; y++) {
                    int local = x + 9 * (z + 9 * y);
                    if (state.hasMaterials(cy - 4 + y)) storedPresence[local >>> 6] |= 1L << (local & 63);
                }
            }
        }

        private static long storedBrickMask(PagePublication page, MaterialSectionState stored) {
            if (stored == null) return 0;
            long mask = stored.brickMask(), remaining = mask;
            if (page != null) while (remaining != 0) {
                int index = Long.numberOfTrailingZeros(remaining); remaining &= remaining - 1;
                var brick = page.brick(index);
                if (brick.resolved() && brick.firstSlot() >= 0) mask &= ~(1L << index);
            }
            return mask;
        }

        private void collectFields() {
            Arrays.fill(fieldPages, 0L); fields.clear();
            var index = MinecraftGameplayFields.existing(level);
            if (index == null) return;
            index.collectIntersecting(originX + .5, originY + .5, originZ + .5,
                    originX + 143.5, originY + 143.5, originZ + 143.5, fields);
            for (var field : fields) {
                int minX = clippedPage(field.min(0), originX), maxX = clippedPage(field.max(0), originX);
                int minY = clippedPage(field.min(1), originY), maxY = clippedPage(field.max(1), originY);
                int minZ = clippedPage(field.min(2), originZ), maxZ = clippedPage(field.max(2), originZ);
                for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) {
                    int bx = originX + x * 16, by = originY + y * 16, bz = originZ + z * 16;
                    if (!field.intersects(bx + .5, by + .5, bz + .5, bx + 15.5, by + 15.5, bz + 15.5)) continue;
                    int local = x + 9 * (z + 9 * y);
                    fieldPages[local >>> 6] |= 1L << (local & 63);
                }
            }
        }

        private static int clippedPage(double coordinate, int origin) {
            return (int)Math.max(0, Math.min(8, Math.floor((coordinate - origin) / 16)));
        }

        private boolean hasFieldPages() {
            for (long word : fieldPages) if (word != 0) return true;
            return false;
        }

        private boolean sameFieldPages(long[] known) {
            return known.length == 0 ? !hasFieldPages() : Arrays.equals(fieldPages, known);
        }

        private boolean writePage(PagePublication page, MaterialSectionState stored, long storedMask,
                int local, long changed, boolean full) {
            int x = originX + local % 9 * 16, z = originZ + local / 9 % 9 * 16, y = originY + local / 81 * 16;
            boolean withFields = presenceBit(fieldPages, local) && !level.isOutsideBuildHeight(y)
                    && loadedChunks[local % 81] != null;
            neighborsReady = false;
            if ((storedMask & changed) != 0) readStoredPage(stored, storedMask & changed, local, x, y, z);
            while (changed != 0) {
                int brick = Long.numberOfTrailingZeros(changed); changed &= changed - 1;
                int bx = x + (brick & 3) * 4, by = y + (brick >>> 4) * 4, bz = z + (brick >>> 2 & 3) * 4;
                brickFields.clear();
                if (withFields) for (int i = 0; i < fields.size(); i++) {
                    var field = fields.get(i);
                    if (field.intersects(bx + .5, by + .5, bz + .5, bx + 3.5, by + 3.5, bz + 3.5)) brickFields.add(field);
                }
                if (!writeBrick(page, (storedMask & 1L << brick) != 0, local, brick, full, bx, by, bz)) return false;
            }
            return true;
        }

        private void readStoredPage(MaterialSectionState stored, long mask, int local, int x, int y, int z) {
            Arrays.fill(storedTemperatures, Double.NaN);
            LevelChunk chunk = loadedChunks[local % 81];
            for (int index = 0; index < stored.size(); index++) {
                int block = stored.position(index);
                int brick = (block & 15) >>> 2 | ((block >>> 4 & 15) >>> 2) << 2 | ((block >>> 8) >>> 2) << 4;
                if ((mask & 1L << brick) == 0) continue;
                BlockState state = chunk.getBlockState(position.set(x + (block & 15), y + (block >>> 8), z + (block >>> 4 & 15)));
                if (net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.getId(state) != stored.stateId(index)) continue;
                var law = MinecraftThermalProfiles.materialLaw(state);
                if (law == null) continue;
                stored.read(index, law, storedSample);
                storedTemperatures[block] = storedSample.temperatureC();
            }
        }

        private static long surfaceMask(PagePublication.Brick brick) {
            return brick.resolved() && brick.firstSlot() >= 0 && brick.blockLayout() != null
                    ? brick.blockLayout().surfaceNodeMask() : 0L;
        }

        private long readableSurfaceMask(PagePublication.Brick brick, int index) {
            long mask = surfaceMask(brick);
            if ((changedMaterialBricks & 1L << index) == 0L) return mask;
            long nodes = mask;
            while (nodes != 0L) {
                int node = Long.numberOfTrailingZeros(nodes); nodes &= nodes - 1;
                if ((brick.blockLayout().nodeBlockMask(node) & changedMaterialBlocks[index]) != 0L)
                    mask &= ~(1L << node);
            }
            return mask;
        }

        private long changedBricks(int page, int epoch) {
            if (cursor.pageChangeEpoch(page) <= epoch) return 0;
            long result = 0;
            for (int brick = 0; brick < 64; brick++)
                if (cursor.brickChangeEpoch(page, brick) > epoch) result |= 1L << brick;
            return result;
        }

        private boolean writeBrick(PagePublication page, boolean stored, int localPage, int index, boolean full, int x, int y, int z) {
            var brick = page == null ? null : page.brick(index);
            long mask = brick == null ? 0 : readableSurfaceMask(brick, index);
            int address = localPage * 64 + index;
            boolean compose = !brickFields.isEmpty();
            if (mask == 0 && !stored && !compose) { payload.writeInvalid(address, full); return true; }
            long remaining = mask;
            while (remaining != 0) {
                int node = Long.numberOfTrailingZeros(remaining); remaining &= remaining - 1;
                if (!cursor.tryRead(brick.firstSlot() + node, brick.arenaGeneration(), page.topologyGeneration(), sample)) return false;
                if (compose) rawNodes[node] = sample.temperatureC();
                else nodes[node] = InfraredBrickCodec.quantize(sample.temperatureC());
            }
            for (int block = 0; block < 64; block++) {
                int node = brick == null || brick.blockLayout() == null ? -1 : brick.blockLayout().nodeAt(block);
                boolean material = node >= 0 && (mask & 1L << node) != 0;
                double saved = stored ? storedTemperatures[BlockBrickLayout.pageBlock(index, block)] : Double.NaN;
                if (compose) rawBlocks[block] = material ? rawNodes[node] : saved;
                else blocks[block] = material ? nodes[node] : InfraredBrickCodec.quantize(saved);
            }
            if (compose) {
                int naturalMode = 0, naturalLayers = 0;
                // Material values have been expanded; rawNodes can now hold four exact natural Y layers.
                for (int block = 0; block < 64; block++) {
                    int px = x + (block & 3), py = y + (block >>> 4), pz = z + (block >>> 2 & 3);
                    fieldSample.clear();
                    for (int i = 0; i < brickFields.size(); i++) {
                        var field = brickFields.get(i);
                        if (field.contains(px + .5, py + .5, pz + .5)) fieldSample.include(field);
                    }
                    double temperature = rawBlocks[block];
                    if (fieldSample.present()) {
                        boolean needsNatural = fieldSample.requiresNatural() || !Double.isFinite(temperature) && fieldSample.requiresBase();
                        if (needsNatural && !neighborsReady) prepareNeighbors(x >> 4, z >> 4);
                        if (!needsNatural || hasBiomeNeighbors(px, pz)) {
                            double natural = 0;
                            if (needsNatural) {
                                if (naturalMode == 0) naturalMode = uniformBiomeBrick(x, y, z) ? 1 : -1;
                                int layer = block >>> 4;
                                if (naturalMode > 0) {
                                    if ((naturalLayers & 1 << layer) == 0) {
                                        rawNodes[layer] = WorldTemperature.naturalAir(level, position.set(px, py, pz));
                                        naturalLayers |= 1 << layer;
                                    }
                                    natural = rawNodes[layer];
                                } else natural = WorldTemperature.naturalAir(level, position.set(px, py, pz));
                            }
                            temperature = fieldSample.compose(natural, Double.isFinite(temperature) ? temperature : natural);
                        }
                    }
                    blocks[block] = InfraredBrickCodec.quantize(temperature);
                }
            }
            payload.writeBrick(address, blocks, full);
            return true;
        }

        private void prepareNeighbors(int cx, int cz) {
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++)
                loadedNeighbors[(dz + 1) * 3 + dx + 1] = level.getChunkSource().getChunkNow(cx + dx, cz + dz) != null;
            neighborsReady = true;
        }

        private boolean hasBiomeNeighbors(int x, int z) {
            int dx = (x & 15) < 2 ? -1 : (x & 15) >= 14 ? 1 : 0;
            int dz = (z & 15) < 2 ? -1 : (z & 15) >= 14 ? 1 : 0;
            return loadedNeighbors[4 + dx] && loadedNeighbors[4 + dz * 3] && loadedNeighbors[4 + dz * 3 + dx];
        }

        private boolean uniformBiomeBrick(int x, int y, int z) {
            int minX = (x & 15) == 0 ? -1 : 0, maxX = (x & 15) == 12 ? 1 : 0;
            int minZ = (z & 15) == 0 ? -1 : 0, maxZ = (z & 15) == 12 ? 1 : 0;
            for (int dz = minZ; dz <= maxZ; dz++) for (int dx = minX; dx <= maxX; dx++)
                if (!loadedNeighbors[(dz + 1) * 3 + dx + 1]) return false;
            int qx = x >> 2, qy = y >> 2, qz = z >> 2;
            var biome = level.getNoiseBiome(qx, qy, qz);
            for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++)
                if ((dx != 0 || dy != 0 || dz != 0) && level.getNoiseBiome(qx + dx, qy + dy, qz + dz) != biome) return false;
            return true;
        }

        @Override public void close() { payload.close(); }
    }

    private static boolean presenceBit(long[] presence, int page) {
        return presence.length != 0 && (presence[page >>> 6] & 1L << (page & 63)) != 0;
    }

    /** Final display transaction; readable describes the live worker baseline, independent of stored bodies and fields. */
    public record InfraredSnapshot(int centerChunkX, int centerChunkZ, int centerSectionY,
            long generation, int infraredEpoch, boolean readable, boolean full,
            long[] presence, long[] fieldPages, byte[][] brickRecords, long storedEpoch) {
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
        if (data == null || !data.willTransit() || data.heatCapacity() <= 0) {
            return false;
        }
        return ownsMaterialTransitions(level, position, state);
    }

    public static boolean ownsMaterialTransitions(ServerLevel level, BlockPos position, BlockState state) {
        Integer profileId = MinecraftThermalProfiles.phaseProfileId(state);
        MinecraftThermalInput input = active(level);
        return profileId != null && input != null
                && input.phase.ownsHeatingTransition(position, profileId);
    }

    public static boolean requestGameplayPhase(ServerLevel level, BlockPos position, BlockState state, byte branch) {
        MinecraftThermalInput input = active(level);
        if (input == null) return false;
        int signature = input.profiles.states().signatureId(state);
        var profile = input.profiles.materials().profileOrNull(input.profiles.signatures().materialProfileId(signature));
        if (profile == null || profile.thermalLaw() == null || profile.thermalLaw().transition(branch) == null) return false;
        var page = input.pages.handle(SectionPos.asLong(position));
        if (page == null) return false;
        int block = (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8;
        input.accumulator.requestPhase(position.asLong(), page, block,
                net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.getId(state), branch);
        return true;
    }

    public static void prepareGameplayProfiles() {
        MinecraftThermalProfiles.prepare();
    }

    public static void invalidateGameplayProfilesForRecipeReload() {
        closeAll();
        MinecraftThermalProfiles.invalidate();
    }

    /** One lifecycle scan of loaded block-entity positions; never loads chunks. */
    public static void bootstrapLoadedSources(MinecraftServer server) {
        if (!server.isSameThread()) return;
        for (ServerLevel level : server.getAllLevels()) {
            if (active(level) != null) continue;
            for (var holder : level.getChunkSource().chunkMap.getChunks()) {
                ChunkPos pos = holder.getPos();
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk != null && startFromCampfires(level, chunk) != null) break;
            }
        }
    }

    private static MinecraftThermalInput startFromCampfires(ServerLevel level, LevelChunk chunk) {
        if (!level.getServer().isSameThread()) return null;
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (CampfireBlock.isLitCampfire(chunk.getBlockState(pos))) {
                if (MinecraftThermalProfiles.prepare().tuning().campfire().ratedPowerW() <= 0) return null;
                return start(level, WorldTemperature.dimension(level), pos);
            }
        }
        return null;
    }

    /** State changes cover ignition before section owners exist. */
    public static void onCampfireIgnited(ServerLevel level, BlockPos pos) {
        if (level.getServer().isSameThread() && active(level) == null
                && MinecraftThermalProfiles.prepare().tuning().campfire().ratedPowerW() > 0) {
            start(level, WorldTemperature.dimension(level), pos);
        }
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
                int oldSignature = input.profiles.states().signatureId(oldState);
                int newSignature = input.profiles.states().signatureId(newState);
                if (input.profiles.signatures().materialProfileId(oldSignature) != 0
                        || input.profiles.signatures().materialProfileId(newSignature) != 0) {
                    int worldX = SectionPos.sectionToBlockCoord(SectionPos.x(owner.sectionKey())) + localX;
                    int worldY = SectionPos.sectionToBlockCoord(SectionPos.y(owner.sectionKey())) + localY;
                    int worldZ = SectionPos.sectionToBlockCoord(SectionPos.z(owner.sectionKey())) + localZ;
                    byte cause = MinecraftPhaseController.materialChangeCause(oldState, newState, worldX, worldY, worldZ);
                    if (cause != 0) {
                        owner.recordMaterialChange(localX | localZ << 4 | localY << 8, oldSignature, newSignature, cause);
                        flags |= MinecraftThermalProfiles.TOPOLOGY_MUTATION;
                    }
                }
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
        // A newly loaded chunk can replace a client's previous stored display,
        // including when the new chunk has no material records at all.
        setDormantState(chunk, state);
        if (state != null && state.activateLoaded(
                level.getGameTime(), dormantHalfLifeSeconds())) {
            if (state.isEmpty()) {
                setDormantState(chunk, null);
            }
            chunk.setUnsaved(true);
        }
        MinecraftThermalInput input = active(level);
        if (input == null) input = startFromCampfires(level, chunk);
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
            input.awaitLatestCompletion();
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
                ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED);
    }

    public static void onSectionIdentityReplaced(
            ServerLevel level,
            LevelChunk chunk,
            int sectionIndex,
            LevelChunkSection previous
    ) {
        DormantChunkThermalState stored = dormantState(chunk);
        if (stored != null && sectionIndex >= 0 && sectionIndex < chunk.getSections().length) {
            stored.replaceMaterials(chunk.getSectionYFromSectionIndex(sectionIndex), null);
            chunk.setUnsaved(true);
        }
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
        observeMachine(level, source, target, MinecraftPhysicalSourceProfile.GENERATOR, thermalLevel, active);
    }

    public static void onFountainTick(
            ServerLevel level, BlockPos source, BlockPos target,
            double thermalLevel, boolean active
    ) {
        observeMachine(level, source, target, MinecraftPhysicalSourceProfile.FOUNTAIN, thermalLevel, active);
    }

    public static void onRadiatorTick(
            ServerLevel level, BlockPos source, BlockPos target,
            double thermalLevel, boolean active
    ) {
        observeMachine(level, source, target, MinecraftPhysicalSourceProfile.RADIATOR, thermalLevel, active);
    }

    private static void observeMachine(ServerLevel level, BlockPos source, BlockPos target,
            MinecraftPhysicalSourceProfile profile, double thermalLevel, boolean enabled) {
        MinecraftThermalInput input = active(level);
        if (input == null && enabled && profile.powerForLevel(thermalLevel) > 0
                && level.getServer().isSameThread()) {
            input = start(level, WorldTemperature.dimension(level), source);
        }
        if (input != null) input.physicalSources.observeMachine(source, target, profile, thermalLevel, enabled);
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
        awaitLatestCompletion();
        pages.checkpointAll(true, true);
        closed = true;
        pendingSourceChunks.clear();
        physicalSources.close();
        if (blockRadiation != null) blockRadiation.close();
        pages.close();
        environment.close();
        if (radiation != null) radiation.close();
        itemEnvironmentCache.clear();
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

    public MaterialSectionState dormantMaterialAdmissionCut(long sectionKey) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        DormantChunkThermalState stored = chunk == null ? null : dormantState(chunk);
        return stored == null ? null : stored.materials(SectionPos.y(sectionKey));
    }

    public long materialPublicationRevision(ThermalPageHandle page) {
        PagePublication publication = page.lastPublication();
        return publication != null && queryPublication.beginInfraredRead(checkpointCursor)
                && checkpointCursor.valid() && checkpointCursor.topologyGeneration() >= publication.topologyGeneration()
                && checkpointCursor.isCurrent() ? publication.geometryRevision() : -1;
    }

    /** Also runs without an active dimension worker; saved matter must follow world replacement. */
    public static void onMaterialBlockChanged(LevelChunk chunk, BlockPos position, BlockState previous, BlockState next) {
        if (previous == null || previous == next || !(chunk.getLevel() instanceof ServerLevel level)) return;
        if (!level.getServer().isSameThread()) {
            BlockPos capturedPosition = position.immutable();
            level.getServer().execute(() -> onMaterialBlockChanged(chunk, capturedPosition, previous, next));
            return;
        }
        DormantChunkThermalState stored = dormantState(chunk);
        int sectionY = SectionPos.blockToSectionCoord(position.getY());
        int sectionIndex = chunk.getSectionIndex(position.getY());
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) return;
        var owner = ((MinecraftThermalSectionAttachment) (Object) chunk.getSections()[sectionIndex]).frostedheart$getThermalInputOwner();
        int block = (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8;
        boolean saved = stored != null && stored.hasMaterial(sectionY, block);
        if (!saved && (owner == null || owner.page() == null)) return;
        byte cause = MinecraftPhaseController.materialChangeCause(previous, next, position.getX(), position.getY(), position.getZ());
        int nextId = net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.getId(next);
        if (owner != null) owner.recordMaterialCheckpointChange(block, nextId, cause);
        if (saved) {
            var nextLaw = MinecraftThermalProfiles.materialLaw(next);
            double natural = nextLaw != null && (cause == ResolvedGeometryBatch.MaterialChanges.REPLACE
                    || cause == ResolvedGeometryBatch.MaterialChanges.MASS_CHANGE)
                    ? WorldTemperature.naturalAir(level, position) : 0;
            if (stored.applyMaterialChange(sectionY, block, nextId, nextLaw, cause, natural)) chunk.setUnsaved(true);
        }
    }

    public void captureDormantPage(
            ThermalPageHandle page,
            LevelChunk chunk,
            boolean markDirty
    ) {
        PagePublication publication;
        DormantChunkThermalState.CaptureResult captured = null;
        MaterialSectionState.Capture capturedMaterials = null;
        long materialSnapshotRevision = -1;
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
            if (!queryPublication.beginInfraredRead(checkpointCursor) || !checkpointCursor.valid()) continue;
            captured = DormantChunkThermalState.capture(
                    publication,
                    queryPublication,
                    querySample,
                    natural,
                    dormantCapture);
            capturedMaterials = MaterialSectionState.capture(publication, queryPublication, profiles.signatures(), materialCapture);
            if (captured.valid() && capturedMaterials.valid() && checkpointCursor.isCurrent()
                    && page.lastPublication() == publication) {
                materialSnapshotRevision = publication.geometryRevision();
                break;
            }
            captured = null;
        }
        if (captured == null || !captured.valid() || capturedMaterials == null || !capturedMaterials.valid()) {
            return;
        }
        DormantChunkThermalState state = dormantState(chunk);
        if (state == null && (captured.entry() != null || capturedMaterials.state() != null)) {
            state = new DormantChunkThermalState(
                    chunk.getSectionYFromSectionIndex(0),
                    chunk.getSections().length);
            setDormantState(chunk, state);
        }
        if (state == null) {
            return;
        }
        boolean changed = state.replace(sectionY, captured.entry());
        MaterialSectionState materialState = capturedMaterials.state();
        int sectionIndex = chunk.getSectionIndex(SectionPos.sectionToBlockCoord(sectionY));
        var owner = ((MinecraftThermalSectionAttachment) (Object) chunk.getSections()[sectionIndex]).frostedheart$getThermalInputOwner();
        if (owner != null) materialState = owner.projectMaterialCheckpoint(materialState, materialSnapshotRevision, natural);
        changed |= state.mergeMaterials(sectionY, materialState, capturedMaterials.sampledBricks());
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
            input.awaitLatestCompletion();
            input.pages.checkpointChunk(chunk, false, true);
        }
    }

    static void checkpointAllForStop() {
        MinecraftThermalInput[] inputs;
        synchronized (ACTIVE) {
            inputs = ACTIVE.values().toArray(MinecraftThermalInput[]::new);
        }
        for (MinecraftThermalInput input : inputs) {
            input.awaitLatestCompletion();
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

    private static long receiverKey(net.minecraft.world.entity.Entity entity) {
        return entity.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(
                        entity.getUUID().getLeastSignificantBits(), 17);
    }

    private static long nextGeneration() {
        return NEXT_GENERATION.getAndUpdate(Math::incrementExact);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int floorQuarter(double value) {
        return (int) Math.floor(value * 4.0D);
    }

    static final class ItemEnvironmentSampleCache {
        private final int[] quarterX;
        private final int[] quarterY;
        private final int[] quarterZ;
        private final ThermalEnvironmentSample[] samples;
        private long generationTick = Long.MIN_VALUE;
        private int size;

        ItemEnvironmentSampleCache(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be positive");
            }
            quarterX = new int[capacity];
            quarterY = new int[capacity];
            quarterZ = new int[capacity];
            samples = new ThermalEnvironmentSample[capacity];
            for (int index = 0; index < capacity; index++) {
                samples[index] = new ThermalEnvironmentSample();
            }
        }

        int find(long tick, int x, int y, int z) {
            beginGeneration(tick);
            for (int index = 0; index < size; index++) {
                if (quarterX[index] == x && quarterY[index] == y && quarterZ[index] == z) return index;
            }
            return -1;
        }

        boolean canAdmit() { return size < samples.length; }

        boolean store(int x, int y, int z, ThermalEnvironmentSample sample) {
            if (!canAdmit()) return false;
            quarterX[size] = x;
            quarterY[size] = y;
            quarterZ[size] = z;
            samples[size].copyFrom(sample);
            size++;
            return true;
        }

        void copyTo(int index, ThermalEnvironmentSample out) {
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
            out.copyFrom(samples[index]);
        }

        int size() { return size; }
        int capacity() { return samples.length; }
        long generationTick() { return generationTick; }

        void clear() {
            generationTick = Long.MIN_VALUE;
            size = 0;
        }

        private void beginGeneration(long tick) {
            if (tick < 0L) throw new IllegalArgumentException("tick must be non-negative");
            if (generationTick != tick) {
                generationTick = tick;
                size = 0;
            }
        }
    }



}
