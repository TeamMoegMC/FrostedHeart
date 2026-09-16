/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticFieldIndex;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftSignatureCapture;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.BlockRadiationIndex;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.MinecraftRadiationOcclusion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalDimensionMailbox;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionLimits;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.DimensionInputAccumulator;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftEnvironmentCapture;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftThermalSectionAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.PhysicalSourceSpatialIndex;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FarFieldSettings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

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
    static final int MAX_PUBLICATION_AGE_TICKS = 40;
    private static final int MAXIMUM_PHYSICAL_SOURCES = 65_536;
    private static final int SOURCE_DISCOVERY_CHUNKS_PER_TICK = 8;
    private static final int MAXIMUM_SOURCE_NODES = 131_072;
    private static final int MAXIMUM_RADIATION_SECTIONS = 3_200;
    static final int GAMEPLAY_ITEM_ENVIRONMENT_SAMPLES_PER_TICK = 64;
    private static final ThermalMemoryBudget MEMORY = new ThermalMemoryBudget(128L * 1024L * 1024L);
    private static final RadiationService.Parameters RADIATION_PARAMETERS =
            new RadiationService.Parameters(
                    MAXIMUM_RADIATION_SECTIONS,
                    128,
                    64,
                    8,
                    24,
                    8,
                    256,
                    16.0D,
                    0.1D,
                    0.5D,
                    0.1D,
                    0.9D,
                    1.62D);
    private static final IdentityHashMap<ServerLevel, MinecraftThermalInput> ACTIVE =
            new IdentityHashMap<>();
    private static final AtomicLong NEXT_GENERATION = new AtomicLong(1_000L);
    private static final ThreadLocal<BlockPos.MutableBlockPos> DORMANT_QUERY_POSITION =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
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
    private final QueryPublication.MutableSample querySample = new QueryPublication.MutableSample();
    private final DormantChunkThermalState.CaptureScratch dormantCapture =
            new DormantChunkThermalState.CaptureScratch();
    private final MaterialSectionState.CaptureScratch materialCapture =
            new MaterialSectionState.CaptureScratch();
    private final QueryPublication.ReadCursor checkpointCursor = new QueryPublication.ReadCursor();
    private static final ThreadLocal<MaterialSample> MATERIAL_SAMPLE =
            ThreadLocal.withInitial(MaterialSample::new);
    private final BlockPos.MutableBlockPos dormantPosition = new BlockPos.MutableBlockPos();
    private final RadiationService.MutableSample radiationSample =
            new RadiationService.MutableSample();
    private final ThermalEnvironmentSample passiveScratch = new ThermalEnvironmentSample();
    private final ThermalEnvironmentSample townScratch = new ThermalEnvironmentSample();
    private final ItemEnvironmentSampleCache itemEnvironmentCache =
            new ItemEnvironmentSampleCache(GAMEPLAY_ITEM_ENVIRONMENT_SAMPLES_PER_TICK);
    private final BlockPos.MutableBlockPos townPosition = new BlockPos.MutableBlockPos();
    private static InfraredCapture infraredCapture;

    private long dimensionGeneration;
    private DimensionInputAccumulator accumulator;
    private QueryPublication queryPublication;
    private ThermalDimensionMailbox mailbox;
    private ThermalInputBatch inFlight;
    private ThermalInputBatch pendingSubmission;
    private long lastCompletedTargetTick;
    private boolean closed;

    private MinecraftThermalInput(ServerLevel level) {
        this.level = level;
        analyticFields = MinecraftGameplayFields.indexFor(level);
        referenceTemperatureC = 0;
        mainThread = Thread.currentThread();
        profiles = MinecraftThermalProfiles.prepare();
        long initialTick = alignedTick(level.getGameTime());
        dimensionGeneration = nextGeneration();
        accumulator = new DimensionInputAccumulator(dimensionGeneration, initialTick);
        lastCompletedTargetTick = initialTick;
        MinecraftSignatureCapture signatureCapture =
                new MinecraftSignatureCapture(level, profiles.states(), profiles.signatures());
        environment = new MinecraftEnvironmentCapture(level, accumulator);
        pages = new MinecraftPageManager(this, level, accumulator, signatureCapture, environment);
        physicalSources =
                new PhysicalSourceSpatialIndex(
                        accumulator,
                        pages,
                        profiles.tuning().campfire(),
                        64,
                        MAXIMUM_PHYSICAL_SOURCES);
        createWorker(initialTick, referenceTemperatureC);
        phase = new MinecraftPhaseController(level, pages, profiles, accumulator, 8);
        radiationOcclusion =
                new MinecraftRadiationOcclusion(level, pages, MAXIMUM_RADIATION_SECTIONS);
        blockRadiation =
                profiles.states().radiationEnabled()
                        ? BlockRadiationIndex.tryCreate(
                                level,
                                pages,
                                profiles.states(),
                                MEMORY.createDimensionBudget(
                                        BlockRadiationIndex.projectedMaximumBytes(
                                                MAXIMUM_RADIATION_SECTIONS)),
                                MAXIMUM_RADIATION_SECTIONS)
                        : null;
        pages.attachMutationConsumers(physicalSources, radiationOcclusion);
        radiation =
                RadiationService.tryCreate(
                        RADIATION_PARAMETERS,
                        physicalSources,
                        blockRadiation,
                        radiationOcclusion,
                        MEMORY.createDimensionBudget(
                                RadiationService.projectedMaximumBytes(RADIATION_PARAMETERS)));
    }

    private void createWorker(long initialTick, double referenceTemperatureC) {
        MinecraftThermalProfiles.Tuning tuning = profiles.tuning();
        ThermalDimensionLimits limits =
                new ThermalDimensionLimits(
                        3_200,
                        MAXIMUM_PHYSICAL_SOURCES,
                        MAXIMUM_SOURCE_NODES,
                        131_072,
                        65_536,
                        262_144,
                        65_536,
                        20,
                        1.0e-6D);
        QueryPublication publication =
                QueryPublication.tryCreate(
                        MEMORY.createDimensionBudget(16L * 1024L * 1024L),
                        256,
                        limits.maximumPages());
        if (publication == null) {
            throw new IllegalStateException("thermal query publication memory was refused");
        }
        ThermalDimensionEngine engine = null;
        try {
            ThermalTopologyParameters topology =
                    new ThermalTopologyParameters(
                            tuning.airHeatCapacityJPerBlockK(),
                            referenceTemperatureC,
                            tuning.airMixingWPerBlockK(),
                            new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D),
                            1_024,
                            8);
            engine =
                    new ThermalDimensionEngine(
                            dimensionGeneration,
                            initialTick,
                            new ThermalCellArena(256),
                            profiles.signatures(),
                            profiles.materials(),
                            topology,
                            new FarFieldSettings(tuning.farFieldConductanceWPerK(), 32.0D, 16.0D),
                            tuning.campfire(),
                            limits,
                            publication);
            mailbox = new ThermalDimensionMailbox(ThermalWorkerPool.shared(), engine);
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
        phase.tick(queryPublication);
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
        int remaining = Math.min(pendingSourceChunks.size(), SOURCE_DISCOVERY_CHUNKS_PER_TICK);
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
            throw new IllegalStateException("thermal completion does not own the in-flight batch");
        }
        ThermalInputBatch completedBatch = inFlight;
        if (completion.status() == ThermalCompletion.Status.ENGINE_FAILED) {
            FHMain.LOGGER.error(
                    "Thermal dimension worker failed for {}",
                    level.dimension().location(),
                    completion.failure());
            pages.checkpointAll(true);
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
        for (ThermalCompletion.BrickResidency residency : completion.residencyUpdates()) {
            pages.applyResidency(residency);
        }
        phase.accept(completion.phaseRequests());
        if (completion.status() == ThermalCompletion.Status.WORK_LIMITED) {
            pages.retryWorkLimited(completedBatch, level.getGameTime());
        }
    }

    private void restartWorker(long gameTick) {
        // These main-thread owners survive the worker. Replace generation-local inputs before
        // reseeding; queries always use this instance's current publication.
        dimensionGeneration = nextGeneration();
        long initialTick = alignedTick(gameTick);
        accumulator = new DimensionInputAccumulator(dimensionGeneration, initialTick);
        lastCompletedTargetTick = initialTick;
        environment.replaceAccumulator(accumulator);
        physicalSources.replaceAccumulator(accumulator);
        phase.replaceAccumulator(accumulator);
        pendingSubmission = null;
        createWorker(initialTick, referenceTemperatureC);
        pages.reseedAll(accumulator);
        physicalSources.reseedAll(gameTick);
    }

    private void sampleAir(
            double x,
            double y,
            double z,
            long sampleTick,
            int maximumAgeTicks,
            ThermalEnvironmentSample out) {
        int blockX = floor(x);
        int blockY = floor(y);
        int blockZ = floor(z);
        long sectionKey =
                SectionPos.asLong(
                        SectionPos.blockToSectionCoord(blockX),
                        SectionPos.blockToSectionCoord(blockY),
                        SectionPos.blockToSectionCoord(blockZ));
        MinecraftPageManager.SectionOwner owner = pages.loadedSectionOrAttach(sectionKey);
        ThermalPageHandle page = owner == null ? null : owner.page();
        LevelChunk loadedChunk = owner == null ? null : owner.chunk();
        if (page == null) {
            sampleDormant(loadedChunk, blockX, blockY, blockZ, sampleTick, out);
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
        PagePublication.Brick coverage = publication.brickAt(localX, localY, localZ);
        int slot = publication.resolveAirPoint(localX, localY, localZ);
        if (slot == PagePublication.NO_AIR_POINT) {
            if (sampleRoutedAir(
                    page,
                    publication,
                    coverage,
                    (localX & 3) | (localZ & 3) << 2 | (localY & 3) << 4,
                    sampleTick,
                    maximumAgeTicks,
                    out)) return;
            if (coverage.signaturePayload() == null) {
                sampleDormant(loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            }
            return;
        }
        if (!queryPublication.tryRead(
                slot, coverage.arenaGeneration(), publication.topologyGeneration(), querySample)) {
            sampleDormant(loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            return;
        }
        if (page.currentPublication() != publication) {
            sampleDormant(loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            return;
        }
        if (sampleTick - querySample.sampleTick() > maximumAgeTicks) {
            sampleDormant(loadedChunk, blockX, blockY, blockZ, sampleTick, out);
            return;
        }
        out.setAir(querySample.temperatureC());
    }

    private boolean sampleRoutedAir(
            ThermalPageHandle owner,
            PagePublication ownerPublication,
            PagePublication.Brick brick,
            int block,
            long sampleTick,
            int maximumAgeTicks,
            ThermalEnvironmentSample out) {
        var layout = brick.blockLayout();
        if (layout == null || !layout.hasAirRoute(block)) return false;
        long target = layout.routedAirBlock(block);
        int x = BlockPos.getX(target), y = BlockPos.getY(target), z = BlockPos.getZ(target);
        ThermalPageHandle page = pages.handle(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        PagePublication publication = page == null ? null : page.currentPublication();
        if (publication == null) return false;
        var coverage = publication.brickAt(x & 15, y & 15, z & 15);
        int slot = publication.resolveAirPoint(x & 15, y & 15, z & 15);
        if (slot < 0
                || !queryPublication.tryRead(
                        slot,
                        coverage.arenaGeneration(),
                        publication.topologyGeneration(),
                        querySample)
                || sampleTick - querySample.sampleTick() > maximumAgeTicks
                || page.currentPublication() != publication
                || owner.currentPublication() != ownerPublication
                || !layout.hasAirRoute(block)) return false;
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
            ThermalEnvironmentSample out) {
        double temperature =
                loadedChunk == null
                        ? dormantTemperature(
                                level, blockX, blockY, blockZ, gameTick, dormantPosition)
                        : dormantTemperature(
                                level,
                                loadedChunk,
                                blockX,
                                blockY,
                                blockZ,
                                gameTick,
                                dormantPosition);
        if (Double.isFinite(temperature)) {
            out.setAir(temperature);
        }
    }

    private void sampleRadiation(ServerPlayer player, ThermalEnvironmentSample out) {
        if (radiation == null) {
            return;
        }
        radiation.samplePlayer(
                receiverKey(player),
                player.getId() & Integer.MAX_VALUE,
                player.getX(),
                player.getY(),
                player.getEyeY(),
                player.getZ(),
                radiationSample);
        out.setRadiation(radiationSample.radiantFluxWPerM2());
    }

    public static double gameplayPlayerEnvironment(
            ServerPlayer player, double naturalTemperatureC, ThermalEnvironmentSample out) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(out, "out").clear();
        if (!Double.isFinite(naturalTemperatureC)) {
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(player.serverLevel());
        if (input == null) {
            input = start(player.serverLevel(), player.blockPosition());
        }
        if (input == null) {
            ThermalAnalyticFieldIndex fields =
                    MinecraftGameplayFields.existing(player.serverLevel());
            double composed =
                    fields == null
                            ? naturalTemperatureC
                            : fields.compose(
                                    player.getX(),
                                    player.getEyeY(),
                                    player.getZ(),
                                    naturalTemperatureC,
                                    naturalTemperatureC);
            if (Double.compare(composed, naturalTemperatureC) != 0) out.setComposedAir(composed);
            return composed;
        }
        input.sampleAir(
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                player.serverLevel().getGameTime(),
                MAX_PUBLICATION_AGE_TICKS,
                out);
        input.sampleRadiation(player, out);
        double base = out.airAvailable() ? out.airTemperatureC() : naturalTemperatureC;
        double composed =
                input.analyticFields.compose(
                        player.getX(), player.getEyeY(), player.getZ(), naturalTemperatureC, base);
        if (Double.compare(composed, base) != 0) {
            out.setComposedAir(composed);
        }
        return composed;
    }

    public static double gameplayPassiveEnvironment(
            LevelReader level, BlockPos position, double naturalTemperatureC) {
        return gameplayPassiveEnvironment(level, position, naturalTemperatureC, null);
    }

    public static double gameplayPassiveEnvironment(
            LevelReader level,
            BlockPos position,
            double naturalTemperatureC,
            ThermalAnalyticFieldIndex.Sample fieldsOut) {
        if (!(level instanceof ServerLevel server)
                || !Double.isFinite(naturalTemperatureC)
                || !server.getServer().isSameThread()) {
            if (fieldsOut != null) fieldsOut.clear();
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(server);
        double base;
        if (input == null) {
            double dormant =
                    dormantTemperature(
                            server,
                            position.getX(),
                            position.getY(),
                            position.getZ(),
                            server.getGameTime(),
                            DORMANT_QUERY_POSITION.get());
            base = Double.isFinite(dormant) ? dormant : naturalTemperatureC;
        } else {
            ThermalEnvironmentSample out = input.passiveScratch;
            out.clear();
            input.sampleAir(
                    position.getX() + 0.5D,
                    position.getY() + 0.5D,
                    position.getZ() + 0.5D,
                    server.getGameTime(),
                    MAX_PUBLICATION_AGE_TICKS,
                    out);
            base = out.airAvailable() ? out.airTemperatureC() : naturalTemperatureC;
        }
        ThermalAnalyticFieldIndex fields =
                input == null ? MinecraftGameplayFields.existing(server) : input.analyticFields;
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
            ItemEntity entity, double naturalTemperatureC, ThermalEnvironmentSample out) {
        Objects.requireNonNull(entity, "entity");
        if (!(entity.level() instanceof ServerLevel server)) {
            out.clear();
            return;
        }
        gameplayExposedReservoirEnvironment(
                server,
                receiverKey(entity),
                entity.getId() & Integer.MAX_VALUE,
                entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.5D,
                entity.getZ(),
                naturalTemperatureC,
                out);
    }

    /** Samples just above a placed reservoir using the same budget and boundary as dropped items. */
    public static void gameplayPlacedReservoirEnvironment(
            ServerLevel server,
            BlockPos position,
            double naturalTemperatureC,
            ThermalEnvironmentSample out) {
        gameplayExposedReservoirEnvironment(
                server,
                position.asLong(),
                0,
                position.getX() + 0.5D,
                position.getY() + 0.3125D,
                position.getZ() + 0.5D,
                naturalTemperatureC,
                out);
    }

    private static void gameplayExposedReservoirEnvironment(
            ServerLevel server,
            long receiverIdentity,
            int receiverGeneration,
            double x,
            double y,
            double z,
            double naturalTemperatureC,
            ThermalEnvironmentSample out) {
        Objects.requireNonNull(out, "out").clear();
        if (!server.getServer().isSameThread() || !Double.isFinite(naturalTemperatureC)) return;
        MinecraftThermalInput input = active(server);
        long tick = server.getGameTime();
        if (input == null) {
            double dormant =
                    dormantTemperature(
                            server,
                            floor(x),
                            floor(y),
                            floor(z),
                            tick,
                            DORMANT_QUERY_POSITION.get());
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
                    input.radiation.sampleItem(
                            receiverIdentity, receiverGeneration, x, y, z, input.radiationSample);
                    out.setRadiation(input.radiationSample.radiantFluxWPerM2());
                }
                input.itemEnvironmentCache.store(quarterX, quarterY, quarterZ, out);
            }
        }
        double base = out.airAvailable() ? out.airTemperatureC() : naturalTemperatureC;
        ThermalAnalyticFieldIndex fields =
                input == null ? MinecraftGameplayFields.existing(server) : input.analyticFields;
        out.setComposedAir(
                fields == null ? base : fields.compose(x, y, z, naturalTemperatureC, base));
    }

    public static double gameplayCropEnvironment(
            LevelAccessor level, BlockPos position, double naturalTemperatureC) {
        return gameplayPassiveEnvironment(level, position, naturalTemperatureC);
    }

    public static double gameplayTownEnvironment(
            LevelAccessor level, TownThermalProjection projection, double naturalTemperatureC) {
        if (!(level instanceof ServerLevel server)
                || projection.voxelCount() == 0
                || !Double.isFinite(naturalTemperatureC)
                || !server.getServer().isSameThread()) {
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(server);
        ThermalAnalyticFieldIndex fields =
                input == null ? MinecraftGameplayFields.existing(server) : input.analyticFields;
        ThermalAnalyticFieldIndex.Sample fieldSample =
                fields == null || fields.isEmpty() ? null : TOWN_FIELD_SAMPLE.get();
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
                        x + 0.5D,
                        y + 0.5D,
                        z + 0.5D,
                        server.getGameTime(),
                        MAX_PUBLICATION_AGE_TICKS,
                        out);
                if (out.airAvailable()) {
                    base = out.airTemperatureC();
                } else {
                    input.townPosition.set(x, y, z);
                    base = natural = WorldTemperature.naturalBlock(server, input.townPosition);
                }
            } else {
                double dormant =
                        dormantTemperature(
                                server,
                                x,
                                y,
                                z,
                                server.getGameTime(),
                                DORMANT_QUERY_POSITION.get());
                base =
                        Double.isFinite(dormant)
                                ? dormant
                                : (natural =
                                        WorldTemperature.naturalBlock(
                                                server, DORMANT_QUERY_POSITION.get().set(x, y, z)));
            }
            if (fieldSample != null) {
                if (fieldSample.requiresNatural() && !Double.isFinite(natural)) {
                    natural =
                            WorldTemperature.naturalBlock(
                                    server, DORMANT_QUERY_POSITION.get().set(x, y, z));
                }
                base = fieldSample.compose(natural, base);
            }
            total += base * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? naturalTemperatureC : total / totalWeight;
    }

    public static List<ThermalAnalyticField> gameplayAnalyticFieldsAt(
            ServerLevel level, BlockPos position) {
        if (!level.getServer().isSameThread()) return List.of();
        ThermalAnalyticFieldIndex fields = MinecraftGameplayFields.existing(level);
        return fields == null
                ? List.of()
                : fields.fieldsAt(
                        position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
    }

    public static boolean hasGameplayAnalyticFieldAt(ServerLevel level, BlockPos position) {
        if (!level.getServer().isSameThread()) return false;
        ThermalAnalyticFieldIndex fields = MinecraftGameplayFields.existing(level);
        return fields != null
                && fields.appliesAt(
                        position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
    }

    public static InfraredSnapshot gameplayInfraredSnapshot(
            ServerPlayer player,
            boolean forceFull,
            long knownGeneration,
            long knownCenter,
            int lastEpoch,
            long[] knownPresence,
            boolean knownReadable,
            long[] knownFieldPages,
            long knownStoredEpoch,
            long knownStoredSampleTick) {
        if (knownPresence.length != InfraredCapture.INFRARED_PRESENCE_WORDS
                || knownFieldPages.length != 0
                        && knownFieldPages.length != InfraredCapture.INFRARED_PRESENCE_WORDS)
            throw new IllegalArgumentException("infrared presence requires 12 words");
        if (infraredCapture == null) infraredCapture = new InfraredCapture();
        MinecraftThermalInput input = active(player.serverLevel());
        return infraredCapture.capture(
                input == null ? null : input.pages,
                input == null ? null : input.queryPublication,
                input == null ? 0 : input.dimensionGeneration,
                player,
                forceFull,
                knownGeneration,
                knownCenter,
                lastEpoch,
                knownPresence,
                knownReadable,
                knownFieldPages,
                knownStoredEpoch,
                knownStoredSampleTick);
    }

    public static double materialTemperature(ServerLevel level, BlockPos position) {
        var sample = MATERIAL_SAMPLE.get();
        return sampleMaterial(level, position, sample) ? sample.temperatureC() : Double.NaN;
    }

    /** Reads a body without creating physical residency or substituting environmental temperature. */
    public static boolean sampleMaterial(ServerLevel level, BlockPos position, MaterialSample out) {
        out.clear();
        MinecraftThermalInput input = active(level);
        int x = position.getX(), y = position.getY(), z = position.getZ();
        long section =
                SectionPos.asLong(
                        SectionPos.blockToSectionCoord(x),
                        SectionPos.blockToSectionCoord(y),
                        SectionPos.blockToSectionCoord(z));
        if (input != null) {
            ThermalPageHandle handle = input.pages.handle(section);
            if (handle != null) {
                PagePublication publication = handle.lastPublication();
                if (publication == null
                        || publication.geometryRevision() != handle.liveGeometryRevision()
                                && input.pages.materialChangedSince(
                                        section,
                                        (x & 15) | (z & 15) << 4 | (y & 15) << 8,
                                        publication.geometryRevision())) return false;
                var brick = publication.brickAt(x & 15, y & 15, z & 15);
                if (brick.resolved() && brick.firstSlot() >= 0 && brick.blockLayout() != null) {
                    int node = brick.blockLayout().nodeAt((x & 3) | (z & 3) << 2 | (y & 3) << 4);
                    if (node >= 0)
                        return input.queryPublication.tryReadMaterial(
                                        brick.firstSlot() + node,
                                        brick.arenaGeneration(),
                                        publication.topologyGeneration(),
                                        out)
                                && handle.lastPublication() == publication;
                }
            }
        }
        LevelChunk chunk =
                level.getChunkSource().getChunkNow(SectionPos.x(section), SectionPos.z(section));
        DormantChunkThermalState stored = chunk == null ? null : dormantState(chunk);
        if (stored == null || !stored.hasMaterials(SectionPos.y(section))) return false;
        BlockState state = chunk.getBlockState(position);
        var law = MinecraftThermalProfiles.materialLaw(state);
        if (law == null) return false;
        if (stored.readMaterial(
                        SectionPos.y(section),
                        (x & 15) | (z & 15) << 4 | (y & 15) << 8,
                        Block.BLOCK_STATE_REGISTRY.getId(state),
                        out)
                != DormantChunkThermalState.MaterialRead.MATCH) return false;
        stored.projectMaterial(level, position, law, out, DORMANT_QUERY_POSITION.get());
        return true;
    }

    /** Existing random updates select the location. This method never creates residency. */
    public static MinecraftPhaseController.PhaseAttempt tryMaterialPhaseAtRandomTick(
            ServerLevel level, LevelChunk chunk, BlockPos position, BlockState state) {
        return MinecraftPhaseController.tryAtRandomTick(level, chunk, position, state);
    }

    /** Final display transaction; readable describes the live worker baseline, independent of stored bodies and fields. */
    public record InfraredSnapshot(
            int centerChunkX,
            int centerChunkZ,
            int centerSectionY,
            long generation,
            int infraredEpoch,
            boolean readable,
            boolean full,
            long[] presence,
            long[] fieldPages,
            byte[][] brickRecords,
            long storedEpoch,
            long storedSampleTick) {}

    public static BlockPos nearestGameplayGenerator(
            Level level, BlockPos position, double maximumDistanceBlocks) {
        if (!(level instanceof ServerLevel server)
                || !server.getServer().isSameThread()
                || !Double.isFinite(maximumDistanceBlocks)
                || maximumDistanceBlocks <= 0.0D) {
            return null;
        }
        MinecraftThermalInput input = active(server);
        return input == null
                ? null
                : input.physicalSources.nearestEnabledGenerator(
                        position, maximumDistanceBlocks * maximumDistanceBlocks);
    }

    public static boolean requestGameplayPhase(
            ServerLevel level, BlockPos position, BlockState state, byte branch) {
        MinecraftThermalInput input = active(level);
        if (input == null) return false;
        int signature = input.profiles.states().signatureId(state);
        var profile =
                input.profiles
                        .materials()
                        .profileOrNull(input.profiles.signatures().materialProfileId(signature));
        if (profile == null
                || profile.thermalLaw() == null
                || profile.thermalLaw().transition(branch) == null) return false;
        var page = input.pages.handle(SectionPos.asLong(position));
        if (page == null) return false;
        int block =
                (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8;
        input.accumulator.requestPhase(
                position.asLong(), page, block, Block.BLOCK_STATE_REGISTRY.getId(state), branch);
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
                if (MinecraftThermalProfiles.prepare().tuning().campfire().ratedPowerW() <= 0)
                    return null;
                return start(level, pos);
            }
        }
        return null;
    }

    /** State changes cover ignition before section owners exist. */
    public static void onCampfireIgnited(ServerLevel level, BlockPos pos) {
        if (level.getServer().isSameThread()
                && active(level) == null
                && MinecraftThermalProfiles.prepare().tuning().campfire().ratedPowerW() > 0) {
            start(level, pos);
        }
    }

    public static void onSectionSetBlockState(
            LevelChunkSection section,
            int localX,
            int localY,
            int localZ,
            BlockState oldState,
            BlockState newState) {
        if (oldState == newState) return;
        MinecraftPageManager.SectionOwner owner =
                ((MinecraftThermalSectionAttachment) (Object) section)
                        .frostedheart$getThermalInputOwner();
        if (owner != null) {
            MinecraftThermalInput input = owner.input();
            if (input != null) {
                int flags = MinecraftThermalProfiles.mutationFlags(oldState, newState);
                int oldSignature = input.profiles.states().signatureId(oldState);
                int newSignature = input.profiles.states().signatureId(newState);
                if (input.profiles.signatures().materialProfileId(oldSignature) != 0
                        || input.profiles.signatures().materialProfileId(newSignature) != 0) {
                    int worldX =
                            SectionPos.sectionToBlockCoord(SectionPos.x(owner.sectionKey()))
                                    + localX;
                    int worldY =
                            SectionPos.sectionToBlockCoord(SectionPos.y(owner.sectionKey()))
                                    + localY;
                    int worldZ =
                            SectionPos.sectionToBlockCoord(SectionPos.z(owner.sectionKey()))
                                    + localZ;
                    byte cause =
                            MinecraftPhaseController.materialChangeCause(
                                    oldState, newState, worldX, worldY, worldZ);
                    if (cause != 0) {
                        owner.recordMaterialChange(
                                localX | localZ << 4 | localY << 8,
                                oldSignature,
                                newSignature,
                                cause);
                        flags |= MinecraftThermalProfiles.TOPOLOGY_MUTATION;
                    }
                }
                int pageFlags =
                        flags
                                & (MinecraftThermalProfiles.TOPOLOGY_MUTATION
                                        | MinecraftThermalProfiles.SOURCE_MUTATION);
                if (pageFlags != 0) {
                    input.pages.onBlockMutation(
                            owner,
                            localX,
                            localY,
                            localZ,
                            (flags & MinecraftThermalProfiles.TOPOLOGY_MUTATION) != 0,
                            (flags & MinecraftThermalProfiles.SOURCE_MUTATION) != 0);
                }
                if ((flags & MinecraftThermalProfiles.RADIATION_MUTATION) != 0
                        && input.blockRadiation != null) {
                    input.blockRadiation.markBlock(owner, localX, localY, localZ);
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

    public static void onRadiantLiquidNeighborChanged(ServerLevel level, BlockPos position) {
        MinecraftThermalInput input = active(level);
        if (input != null && input.blockRadiation != null) {
            input.blockRadiation.markBlock(position.getX(), position.getY(), position.getZ());
        }
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        DormantChunkThermalState state = dormantState(chunk);
        // A newly loaded chunk can replace a client's previous stored display,
        // including when the new chunk has no material records at all.
        setDormantState(chunk, state);
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
            LevelChunk current =
                    level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z);
            if (current != null && current != chunk) return;
            if (input.blockRadiation != null) {
                input.blockRadiation.onChunkUnload(chunk);
            }
            input.pages.onChunkUnload(chunk);
            input.physicalSources.beforeChunkUnload(chunk, level.getGameTime());
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

    public static void onPlayerChangedDimension(ServerPlayer player, ServerLevel previousLevel) {
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
        if (owner != null)
            owner.recordFullResync(ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED);
    }

    public static void onSectionIdentityReplaced(
            ServerLevel level, LevelChunk chunk, int sectionIndex, LevelChunkSection previous) {
        DormantChunkThermalState stored = dormantState(chunk);
        if (stored != null && sectionIndex >= 0 && sectionIndex < chunk.getSections().length) {
            stored.replaceMaterials(chunk.getSectionYFromSectionIndex(sectionIndex), null);
            chunk.setUnsaved(true);
        }
        MinecraftThermalInput input = active(level);
        if (input != null && sectionIndex >= 0 && sectionIndex < chunk.getSections().length) {
            input.pages.onSectionIdentityReplaced(
                    chunk, sectionIndex, previous, chunk.getSections()[sectionIndex]);
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
            ServerLevel level,
            BlockPos source,
            BlockPos target,
            double thermalLevel,
            boolean active) {
        observeMachine(
                level,
                source,
                target,
                MinecraftPhysicalSourceProfile.GENERATOR,
                thermalLevel,
                active);
    }

    public static void onFountainTick(
            ServerLevel level,
            BlockPos source,
            BlockPos target,
            double thermalLevel,
            boolean active) {
        observeMachine(
                level,
                source,
                target,
                MinecraftPhysicalSourceProfile.FOUNTAIN,
                thermalLevel,
                active);
    }

    public static void onRadiatorTick(
            ServerLevel level,
            BlockPos source,
            BlockPos target,
            double thermalLevel,
            boolean active) {
        observeMachine(
                level,
                source,
                target,
                MinecraftPhysicalSourceProfile.RADIATOR,
                thermalLevel,
                active);
    }

    private static void observeMachine(
            ServerLevel level,
            BlockPos source,
            BlockPos target,
            MinecraftPhysicalSourceProfile profile,
            double thermalLevel,
            boolean enabled) {
        MinecraftThermalInput input = active(level);
        if (input == null
                && enabled
                && profile.powerForLevel(thermalLevel) > 0
                && level.getServer().isSameThread()) {
            input = start(level, source);
        }
        if (input != null)
            input.physicalSources.observeMachine(source, target, profile, thermalLevel, enabled);
    }

    public static void onPhysicalSourceRemoved(ServerLevel level, BlockPos source) {
        MinecraftThermalInput input = active(level);
        if (input != null)
            input.physicalSources.remove(source.getX(), source.getY(), source.getZ());
    }

    @Override
    public void close() {
        requireMainThread();
        if (closed) return;
        awaitLatestCompletion();
        pages.checkpointAll(true);
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
            long sectionKey, double naturalTemperatureC, long gameTick) {
        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        DormantChunkThermalState state = chunk == null ? null : dormantState(chunk);
        return state == null
                ? null
                : state.admissionCut(
                        SectionPos.y(sectionKey),
                        gameTick,
                        profiles.tuning().dormantTemperatureHalfLifeSeconds(),
                        naturalTemperatureC);
    }

    public void captureDormantPage(ThermalPageHandle page, boolean markDirty) {
        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(
                                SectionPos.x(page.sectionKey()), SectionPos.z(page.sectionKey()));
        if (chunk != null) {
            captureDormantPage(page, chunk, markDirty);
        }
    }

    public ThermalInputBatch.DormantMaterialCut dormantMaterialAdmissionCut(long sectionKey) {
        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        DormantChunkThermalState stored = chunk == null ? null : dormantState(chunk);
        MaterialSectionState state =
                stored == null ? null : stored.materials(SectionPos.y(sectionKey));
        if (state == null) return null;
        long tick = level.getGameTime();
        double natural =
                stored.naturalTemperature(
                        level,
                        SectionPos.x(sectionKey),
                        SectionPos.y(sectionKey),
                        SectionPos.z(sectionKey),
                        tick,
                        dormantPosition);
        return new ThermalInputBatch.DormantMaterialCut(
                state, tick, natural, DormantThermalCooling.rate(dormantHalfLifeSeconds()));
    }

    public long materialPublicationRevision(ThermalPageHandle page) {
        PagePublication publication = page.lastPublication();
        return publication != null
                        && queryPublication.beginRead(checkpointCursor)
                        && checkpointCursor.valid()
                        && checkpointCursor.topologyGeneration() >= publication.topologyGeneration()
                        && checkpointCursor.isCurrent()
                ? publication.geometryRevision()
                : -1;
    }

    /** Also runs without an active dimension worker; saved matter must follow world replacement. */
    public static void onMaterialBlockChanged(
            LevelChunk chunk, BlockPos position, BlockState previous, BlockState next) {
        if (previous == null
                || previous == next
                || !(chunk.getLevel() instanceof ServerLevel level)) return;
        if (!level.getServer().isSameThread()) {
            BlockPos capturedPosition = position.immutable();
            level.getServer()
                    .execute(() -> onMaterialBlockChanged(chunk, capturedPosition, previous, next));
            return;
        }
        DormantChunkThermalState stored = dormantState(chunk);
        int sectionY = SectionPos.blockToSectionCoord(position.getY());
        int sectionIndex = chunk.getSectionIndex(position.getY());
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) return;
        var owner =
                ((MinecraftThermalSectionAttachment) (Object) chunk.getSections()[sectionIndex])
                        .frostedheart$getThermalInputOwner();
        int block =
                (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8;
        boolean saved = stored != null && stored.hasMaterial(sectionY, block);
        if (!saved && (owner == null || owner.page() == null)) return;
        // onPlace may replace this position before the outer chunk setter returns.
        if (chunk.getBlockState(position) != next) return;
        byte cause =
                MinecraftPhaseController.materialChangeCause(
                        previous, next, position.getX(), position.getY(), position.getZ());
        int nextId = Block.BLOCK_STATE_REGISTRY.getId(next);
        if (owner != null) owner.recordMaterialCheckpointChange(block, nextId, cause);
        if (saved) {
            var nextLaw = MinecraftThermalProfiles.materialLaw(next);
            var transition = MinecraftPhaseController.dormantTransition(position, previous, next);
            if (transition != null) {
                if (stored.updateMaterial(
                        sectionY,
                        block,
                        nextId,
                        nextLaw,
                        transition.energyJ(),
                        (byte) 0,
                        transition.tick())) chunk.setUnsaved(true);
                return;
            }
            boolean live = owner != null && owner.ownsMaterialPosition(block);
            double rate = live ? 0 : DormantThermalCooling.rate(dormantHalfLifeSeconds());
            double natural = 0;
            double coolingNatural = 0;
            if (nextLaw != null) {
                if (cause == ResolvedGeometryBatch.MaterialChanges.REPLACE
                        || cause == ResolvedGeometryBatch.MaterialChanges.MASS_CHANGE)
                    natural = WorldTemperature.naturalAir(level, position);
                if (!live && cause != ResolvedGeometryBatch.MaterialChanges.REPLACE)
                    coolingNatural =
                            stored.naturalTemperature(
                                    level,
                                    chunk.getPos().x,
                                    sectionY,
                                    chunk.getPos().z,
                                    level.getGameTime(),
                                    DORMANT_QUERY_POSITION.get());
            }
            if (stored.applyMaterialChange(
                    sectionY,
                    block,
                    nextId,
                    nextLaw,
                    cause,
                    natural,
                    level.getGameTime(),
                    rate,
                    coolingNatural)) chunk.setUnsaved(true);
        }
    }

    public void captureDormantPage(ThermalPageHandle page, LevelChunk chunk, boolean markDirty) {
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
        double airNatural = natural;
        DormantChunkThermalState state = dormantState(chunk);
        for (int attempt = 0; attempt < 2; attempt++) {
            publication = page.lastPublication();
            if (publication == null) {
                return;
            }
            if (!queryPublication.beginRead(checkpointCursor) || !checkpointCursor.valid())
                continue;
            airNatural =
                    state == null
                            ? natural
                            : state.captureNatural(
                                    sectionY, checkpointCursor.sampleTick(), natural);
            captured =
                    DormantChunkThermalState.capture(
                            publication, queryPublication, querySample, airNatural, dormantCapture);
            capturedMaterials =
                    MaterialSectionState.capture(
                            publication, queryPublication, profiles.signatures(), materialCapture);
            if (captured.valid()
                    && capturedMaterials.valid()
                    && checkpointCursor.isCurrent()
                    && page.lastPublication() == publication) {
                materialSnapshotRevision = publication.geometryRevision();
                break;
            }
            captured = null;
        }
        if (captured == null
                || !captured.valid()
                || capturedMaterials == null
                || !capturedMaterials.valid()) {
            return;
        }
        if (state == null && (captured.entry() != null || capturedMaterials.state() != null)) {
            state =
                    new DormantChunkThermalState(
                            chunk.getSectionYFromSectionIndex(0), chunk.getSections().length);
            setDormantState(chunk, state);
        }
        if (state == null) {
            return;
        }
        boolean changed =
                state.mergeAir(
                        sectionY,
                        captured.entry(),
                        capturedMaterials.sampledBricks(),
                        checkpointCursor.sampleTick(),
                        airNatural,
                        DormantThermalCooling.rate(dormantHalfLifeSeconds()),
                        dormantCapture);
        MaterialSectionState materialState = capturedMaterials.state();
        int sectionIndex = chunk.getSectionIndex(SectionPos.sectionToBlockCoord(sectionY));
        var owner =
                ((MinecraftThermalSectionAttachment) (Object) chunk.getSections()[sectionIndex])
                        .frostedheart$getThermalInputOwner();
        if (owner != null)
            materialState =
                    owner.projectMaterialCheckpoint(
                            materialState, materialSnapshotRevision, natural);
        changed |= state.mergeMaterials(sectionY, materialState, capturedMaterials.sampledBricks());
        if (state.isEmpty()) {
            setDormantState(chunk, null);
        }
        if (changed && markDirty) {
            chunk.setUnsaved(true);
        }
    }

    static void checkpointForSave(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            input.awaitLatestCompletion();
            input.pages.checkpointChunk(chunk, false);
        }
    }

    static void checkpointAllForStop() {
        MinecraftThermalInput[] inputs;
        synchronized (ACTIVE) {
            inputs = ACTIVE.values().toArray(MinecraftThermalInput[]::new);
        }
        for (MinecraftThermalInput input : inputs) {
            input.awaitLatestCompletion();
            input.pages.checkpointAll(true);
        }
    }

    private static double dormantTemperature(
            ServerLevel level,
            int blockX,
            int blockY,
            int blockZ,
            long gameTick,
            BlockPos.MutableBlockPos naturalPosition) {
        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(
                                SectionPos.blockToSectionCoord(blockX),
                                SectionPos.blockToSectionCoord(blockZ));
        return dormantTemperature(level, chunk, blockX, blockY, blockZ, gameTick, naturalPosition);
    }

    private static double dormantTemperature(
            ServerLevel level,
            LevelChunk chunk,
            int blockX,
            int blockY,
            int blockZ,
            long gameTick,
            BlockPos.MutableBlockPos naturalPosition) {
        DormantChunkThermalState state = chunk == null ? null : dormantState(chunk);
        if (state == null) {
            return Double.NaN;
        }
        int sectionX = SectionPos.blockToSectionCoord(blockX);
        int sectionY = SectionPos.blockToSectionCoord(blockY);
        int sectionZ = SectionPos.blockToSectionCoord(blockZ);
        int brick =
                SectionPos.sectionRelative(blockX) >>> 2
                        | (SectionPos.sectionRelative(blockZ) >>> 2) << 2
                        | (SectionPos.sectionRelative(blockY) >>> 2) << 4;
        return state.sample(
                sectionY,
                brick,
                gameTick,
                dormantHalfLifeSeconds(),
                level,
                sectionX,
                sectionZ,
                naturalPosition);
    }

    static DormantChunkThermalState dormantState(LevelChunk chunk) {
        return ((MinecraftThermalChunkAttachment) (Object) chunk)
                .frostedheart$getDormantThermalState();
    }

    static void setDormantState(LevelChunk chunk, DormantChunkThermalState state) {
        ((MinecraftThermalChunkAttachment) (Object) chunk)
                .frostedheart$setDormantThermalState(state);
    }

    static double dormantHalfLifeSeconds() {
        return MinecraftThermalProfiles.dormantTemperatureHalfLifeSeconds();
    }

    private static MinecraftThermalInput start(ServerLevel level, BlockPos center) {
        synchronized (ACTIVE) {
            MinecraftThermalInput existing = ACTIVE.get(level);
            if (existing != null) return existing;
            MinecraftThermalInput created = null;
            try {
                created = new MinecraftThermalInput(level);
                created.attachLoadedWorld(center);
                ACTIVE.put(level, created);
                return created;
            } catch (RuntimeException failure) {
                if (created != null) created.close();
                FHMain.LOGGER.error(
                        "Could not start thermal runtime for {}",
                        level.dimension().location(),
                        failure);
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
            throw new IllegalStateException("Minecraft thermal input requires the level thread");
        }
    }

    private static long alignedTick(long tick) {
        return Math.floorDiv(tick, ThermalInputBatch.CUT_INTERVAL_TICKS)
                * ThermalInputBatch.CUT_INTERVAL_TICKS;
    }

    private static long receiverKey(net.minecraft.world.entity.Entity entity) {
        return entity.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(entity.getUUID().getLeastSignificantBits(), 17);
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
                if (quarterX[index] == x && quarterY[index] == y && quarterZ[index] == z)
                    return index;
            }
            return -1;
        }

        boolean canAdmit() {
            return size < samples.length;
        }

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

        int size() {
            return size;
        }

        int capacity() {
            return samples.length;
        }

        long generationTick() {
            return generationTick;
        }

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
