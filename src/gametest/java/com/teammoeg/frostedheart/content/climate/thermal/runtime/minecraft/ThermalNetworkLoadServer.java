/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.google.gson.Gson;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in workload on the ordinary dedicated server; it never advances the thermal engine itself. */
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class ThermalNetworkLoadServer {
    private static final boolean ENABLED = Boolean.getBoolean("frostedheart.thermalLoad");
    private static final int PLAYERS = Integer.getInteger("frostedheart.thermalLoad.players", 100);
    private static final int BASES = Integer.getInteger("frostedheart.thermalLoad.bases", 25);
    private static final int SOURCE_SIDE = Integer.getInteger("frostedheart.thermalLoad.sourceSide", 8);
    private static final int SECONDS = Integer.getInteger("frostedheart.thermalLoad.seconds", 1200);
    private static final int RECOVERY_SECONDS = Integer.getInteger("frostedheart.thermalLoad.recoverySeconds", 0);
    private static final boolean RECOVERY_RETURN_HOME = Boolean.getBoolean("frostedheart.thermalLoad.recoveryReturnHome");
    private static final int WIDTH = SOURCE_SIDE * 4, HEIGHT = 7;
    private static final boolean LIFECYCLE_TRACE = Boolean.getBoolean("frostedheart.thermalLoad.lifecycleTrace");
    private static final java.util.concurrent.ArrayBlockingQueue<String> lifecycleEvents = LIFECYCLE_TRACE ? new java.util.concurrent.ArrayBlockingQueue<>(4096) : null;
    private static final TraceSourceState[] tracedSources = LIFECYCLE_TRACE ? new TraceSourceState[BASES * SOURCE_SIDE * SOURCE_SIDE] : null;
    private static final LongAdder droppedLifecycleEvents = new LongAdder();
    private record TraceSourceState(int generation, boolean enabled, boolean unloaded, double powerW) { }
    private static final Gson JSON = new Gson();
    private static final Path ROOT = Path.of(System.getProperty("frostedheart.thermalLoadRoot", "build/thermal-network-load"));
    private static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final com.sun.management.OperatingSystemMXBean OS = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final LongAdder chunkLoads = new LongAdder(), chunkUnloads = new LongAdder(), chunkSaves = new LongAdder();
    private static final LongAdder geometryCommits = new LongAdder(), geometryBoundarySections = new LongAdder(), geometryRepeatedCuts = new LongAdder();
    private static final LongAdder coefficientOnlyGeometryCommits = new LongAdder();
    private static final LongAdder mixingUpdatePages = new LongAdder(), mixingUpdateBricks = new LongAdder();
    private static final LongAdder coordinatorRounds = new LongAdder();
    private static long geometryLastSequence = Long.MIN_VALUE, geometryLastTick = Long.MIN_VALUE;
    private static final Distribution ticks = new Distribution(100_000), fixture = new Distribution(10_000), thermal = new Distribution(10_000);
    private static final Distribution legacyThermal = new Distribution(10_000);
    private static final Distribution ages = new Distribution(1);
    private static final Distribution gameplayAges = new Distribution(1);
    private static final List<BlockPos> fires = new ArrayList<>();
    private static final List<UUID> workloadItems = new ArrayList<>();
    private static final QueryPublication.MutableSample probe = new QueryPublication.MutableSample();
    private static final long[] arrived = new long[BASES];
    private static final int[] residents = new int[BASES];
    private static final long[] availableByBase = new long[BASES], unavailableByBase = new long[BASES], oldByBase = new long[BASES], oldestByBase = new long[BASES];
    private static final long[] oldFrozenByBase = new long[BASES], oldGeometryHeldByBase = new long[BASES],
            oldGlobalClockByBase = new long[BASES], oldSectionClockByBase = new long[BASES], oldReadRacedByBase = new long[BASES];
    private static final long[] gameplayProbeTick = new long[PLAYERS], gameplayCapturedByBase = new long[BASES], gameplayDormantByBase = new long[BASES], gameplayMissingByBase = new long[BASES];
    private static final Map<UUID, Integer> playerIndices = new LinkedHashMap<>();
    private static final Map<Long, Long> workerCpu = new LinkedHashMap<>(), workerAllocated = new LinkedHashMap<>();
    private static MinecraftServer server;
    private static volatile Phase phase = Phase.BUILDING;
    private static long tickStarted, mainThread, fixtureNanos, thermalNanos, stageStarted;
    private static String rootStage;
    private static long rootObservationStarted;
    private static final Map<String, long[]> mainStageTotals = new java.util.LinkedHashMap<>();
    private static long queryObservationNanos, gameplayCalls, gameplayCurrent, gameplayCaptured, gameplayDormant, gameplayMissing, gameplayMinimum;
    private static long legacyThermalNanos, legacyStarted, legacyObservationNanos;
    private static int legacyDepth;
    private static int stageDepth, buildBase, buildCell, placedSources, ignited, phaseTicks, awayCycle = -1;
    private static long readyTick, ignitionStartedTick, ignitionCompletedTick, measurementStartedTick;
    private static String runtimeKind = "not_started";
    private static long measuredTicks, startedNanos, peakHeap, peakAccounted, mutations, itemSpawns;
    private static volatile Map<String, Object> workerSnapshot = Map.of();
    private static long unavailable, ageViolations, playerTicks, deaths, disconnects, teleports;
    private static long mainCpuStart, processCpuStart, workersCpuTotal, workersAllocatedTotal;
    private static long coordinatorRoundsStart;
    private static long loadsStart, unloadsStart, savesStart;
    private static long fuelItems;
    private static int forcedBases;
    private static boolean constructionTicketsReleased;
    private static boolean previousMeasured;
    private static boolean previousRecovery;
    private static Map<String, Object> measurement;
    private static long recoveryStartedTick, recoveryStartedNanos, recoveryAllocatedStart;
    private static long recoveryTailStartedNanos, recoveryTailAllocatedStart;
    private static long recoveryRoundsStart, recoveryTailRoundsStart;
    private static long recoverySamples, recoveryMissing, recoveryOld, recoveryTailSamples, recoveryTailMissing, recoveryTailOld;
    private static long recoveryFirstFreshTick = -1;
    private static final Distribution recoveryAges = new Distribution(1), recoveryTailAges = new Distribution(1);
    private static final Distribution recoveryMain = new Distribution(10_000), recoveryTailMain = new Distribution(10_000);
    private static long recoveryTailFrames, recoveryTailMinimumPlayers = Long.MAX_VALUE;
    private enum Phase { BUILDING, WAITING_FOR_PLAYERS, WARMUP, MEASURING, RECOVERY, DIAGNOSTIC_HOLD, FINISHING }

    public static int workerTimingPhase() { return phase == Phase.MEASURING ? 1 : phase == Phase.RECOVERY ? 2 : 0; }

    /** Diagnostic mode only: scalar state copied while the original coordinator owns its turn. */
    private static Map<String, Object> workerSnapshot(com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine engine) {
        com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField field = readWorker(engine, "fixedField");
        if (field == null) return Map.of("progress", engine.regionalProgressDescription());
        com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress ingress = readWorker(engine, "sourceIngress");
        com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore pages = readWorker(engine, "pages");
        it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap cuts = readWorker(engine, "fixedPhysicalCuts");
        com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox inbox = readWorker(engine, "topologyInputs");
        var domains = new java.util.ArrayList<Map<String, Object>>();
        for (var domain : field.domains()) {
            long physical = Long.MAX_VALUE;
            for (long section : domain.sections()) physical = Math.min(physical, cuts.getOrDefault(section, Long.MAX_VALUE));
            domains.add(Map.of("sections", domain.sections().toLongArray(), "minimumTick", field.minimumTick(domain.sections()),
                    "maximumTick", field.maximumTick(domain.sections()), "physicalCut", physical,
                    "sourcePrefix", ingress.firstPendingTick(domain.sections(), section -> pages.capturedBrickMask(section) != 0)));
        }
        var inputs = new java.util.ArrayList<Map<String, Object>>();
        inbox.forEachPendingInput(input -> {
            long section = input.page().sectionKey(); var batch = input.batch();
            inputs.add(Map.of("section", section, "tick", batch.targetTick(), "sampleTick", field.sectionSampleTick(section),
                    "activeSince", field.activeSince(section), "frozen", field.frozen(section), "mixingBricks", input.mixing().size(),
                    "admissions", batch.admissions().length, "retirements", batch.retirements().length,
                    "residency", batch.residencyUpdates().length, "geometry", batch.geometry().size()));
        });
        return Map.of("progress", engine.regionalProgressDescription(), "domains", domains, "inputs", inputs,
                "transportThrough", (Long) readWorker(engine, "transportThrough"), "sourceReady", ingress.hasReadyWork(),
                "fieldIdle", field.idle());
    }
    @SuppressWarnings("unchecked")
    private static <T> T readWorker(Object owner, String name) {
        try { var member = owner.getClass().getDeclaredField(name); member.setAccessible(true); return (T) member.get(owner); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Observe the current test worker field " + name, failure); }
    }

    private static final class Distribution {
        final long quantum;
        final long[] buckets = new long[10001];
        long count, sum, maximum;
        Distribution(long quantum) { this.quantum = quantum; }
        void add(long value) {
            value = Math.max(0, value); count++; sum += value; maximum = Math.max(maximum, value);
            buckets[(int) Math.min(buckets.length - 1, value / quantum)]++;
        }
        long percentile(double fraction) {
            long target = Math.max(1, (long) Math.ceil(count * fraction)), seen = 0;
            for (int i = 0; i < buckets.length; i++) if ((seen += buckets[i]) >= target) return i * quantum;
            return 0;
        }
        Map<String, Object> milliseconds() {
            return Map.of("count", count, "mean", count == 0 ? 0 : sum / (double) count / 1e6,
                    "p50", percentile(.5) / 1e6, "p95", percentile(.95) / 1e6,
                    "p99", percentile(.99) / 1e6, "max", maximum / 1e6);
        }
    }

    @SubscribeEvent public static void started(ServerStartedEvent event) throws java.io.IOException {
        if (!ENABLED) return;
        server = event.getServer(); Files.createDirectories(ROOT);
        if (THREADS.isThreadCpuTimeSupported() && !THREADS.isThreadCpuTimeEnabled()) THREADS.setThreadCpuTimeEnabled(true);
        if (THREADS.isThreadAllocatedMemorySupported() && !THREADS.isThreadAllocatedMemoryEnabled()) THREADS.setThreadAllocatedMemoryEnabled(true);
        if (Boolean.getBoolean("frostedheart.thermalLoad.workerTiming")) ThermalNetworkWorkerTiming.start();
        var level = server.overworld();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);
        level.setWeatherParameters(1000000, 0, false, false);
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
            level.setBlockAndUpdate(new BlockPos(x, 64, z), Blocks.STONE.defaultBlockState());
        level.setDefaultSpawnPos(new BlockPos(0, 65, 0), 0);
        ThermalDiagnostics.observer = new ThermalDiagnostics.Observer() {
            @Override public void fixedRoundFinished(com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine engine) {
                coordinatorRounds.increment();
                if (Boolean.getBoolean("frostedheart.thermalLoad.workerTiming") && phase == Phase.MEASURING
                        && coordinatorRounds.sum() % 128 == 0) workerSnapshot = workerSnapshot(engine);
            }
            @Override public void stage(String name, boolean entering) {
                if (Thread.currentThread().getId() != mainThread) return;
                if (entering) {
                    if (stageDepth++ == 0) {
                        rootStage = name; rootObservationStarted = queryObservationNanos; stageStarted = System.nanoTime();
                    }
                } else if (stageDepth > 0 && --stageDepth == 0) {
                    long elapsed = System.nanoTime() - stageStarted;
                    thermalNanos += elapsed;
                    if (phase == Phase.MEASURING) {
                        long value = Math.max(0, elapsed - (queryObservationNanos - rootObservationStarted));
                        var total = mainStageTotals.computeIfAbsent(rootStage, ignored -> new long[3]);
                        total[0]++; total[1] += value; total[2] = Math.max(total[2], value);
                    }
                }
                boolean legacy = switch (name) {
                    case "thermal_input_tick", "thermal_completion", "thermal_data_load", "thermal_data_save", "thermal_chunk_load", "thermal_chunk_unload" -> true;
                    default -> false;
                };
                if (legacy) {
                    if (entering) { if (legacyDepth++ == 0) legacyStarted = System.nanoTime(); }
                    else if (legacyDepth > 0 && --legacyDepth == 0) legacyThermalNanos += System.nanoTime() - legacyStarted;
                }
            }
            @Override public void playerAirSampled(ServerPlayer player, ThermalEnvironmentSample sample, String source) {
                observeGameplayAir(player, sample, source);
            }
            @Override public void inputAccepted(com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope input, boolean unfinished) {
                traceSourceControls(input);
            }
            @Override public void geometry(Object task, com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch input,
                    com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary boundary, String stage, boolean completed) {
                if (phase != Phase.MEASURING || !stage.equals("PUBLISHED")) return;
                geometryCommits.increment(); geometryBoundarySections.add(boundary.sectionCount());
                if (!com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary.hasStructuralInput(input)
                        && input.environmentUpdates().length == 0) coefficientOnlyGeometryCommits.increment();
                if (input.sequence() == geometryLastSequence && input.targetTick() == geometryLastTick) geometryRepeatedCuts.increment();
                geometryLastSequence = input.sequence(); geometryLastTick = input.targetTick();
            }
            @Override public void nativeUnload(ServerLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk) {
                if (LIFECYCLE_TRACE && level == server.overworld()) traceChunk("native_request", chunk);
            }
            @Override public void mixingUpdated(com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch input, int bricks) {
                if (phase == Phase.MEASURING) { mixingUpdatePages.increment(); mixingUpdateBricks.add(bricks); }
            }
        };
        write("state.json", Map.of("phase", phase.name(), "pid", ProcessHandle.current().pid(), "players", PLAYERS,
                "bases", BASES, "sources", BASES * SOURCE_SIDE * SOURCE_SIDE, "seconds", SECONDS));
        FHMain.LOGGER.info("THERMAL_NETWORK_START,pid={},bases={},sources={},players={}", ProcessHandle.current().pid(), BASES, BASES * SOURCE_SIDE * SOURCE_SIDE, PLAYERS);
    }

    private static BlockPos origin(int base) { return new BlockPos(4096 + base % 10 * 512, 64, 4096 + base / 10 * 512); }
    private static void forceBase(int base, boolean forced) {
        BlockPos point = origin(base);
        for (int x = (point.getX() - 16) >> 4; x <= (point.getX() + WIDTH + 16) >> 4; x++)
            for (int z = (point.getZ() - 16) >> 4; z <= (point.getZ() + WIDTH + 16) >> 4; z++)
                server.overworld().setChunkForced(x, z, forced);
    }
    private static void refuel(BlockPos fire) {
        var entity = server.overworld().getBlockEntity(fire);
        var fuel = entity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(() -> new IllegalStateException("The actual campfire must expose its fuel inventory"));
        // This capability stores burn time, not stacks; surplus coal is returned normally.
        var remainder = fuel.insertItem(0, new ItemStack(Items.COAL, 4), false);
        fuelItems += 4 - remainder.getCount();
        if (((ICampfireExtra) entity).getLifeTime() <= 0)
            throw new IllegalStateException("Campfire has no fuel after insertion: " + fire);
    }
    private static int index(ServerPlayer player) {
        Integer cached = playerIndices.get(player.getUUID());
        if (cached != null) return cached;
        String name = player.getGameProfile().getName();
        if (!name.startsWith("FHLoad")) return -1;
        try {
            int value = Integer.parseInt(name.substring(6)) - 1;
            if (value < 0 || value >= PLAYERS) return -1;
            playerIndices.put(player.getUUID(), value); return value;
        }
        catch (NumberFormatException ignored) { return -1; }
    }
    private static int base(int player) { return player * BASES / PLAYERS; }
    private static BlockPos home(int player) {
        int local = player % Math.max(1, PLAYERS / BASES);
        int margin = Math.max(4, WIDTH / 4);
        return origin(base(player)).offset(local % 2 == 0 ? margin : WIDTH - margin, 1, local / 2 % 2 == 0 ? margin : WIDTH - margin);
    }
    private static void configure(ServerPlayer player) {
        int index = index(player); if (index < 0) return;
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = false;
        player.getAbilities().mayfly = true; player.getAbilities().flying = true; player.onUpdateAbilities();
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
        player.setHealth(player.getMaxHealth());
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1728000, 4, false, false));
        player.getFoodData().setFoodLevel(20); player.getFoodData().setSaturation(20);
        teleport(player, home(index));
    }
    private static void teleport(ServerPlayer player, BlockPos point) {
        player.teleportTo(server.overworld(), point.getX() + .5, point.getY() + .01, point.getZ() + .5, 0, 0); teleports++;
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (ENABLED && event.getEntity() instanceof ServerPlayer player) configure(player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (ENABLED && event.getEntity() instanceof ServerPlayer player) configure(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (ENABLED && phase == Phase.MEASURING && event.getEntity() instanceof ServerPlayer player && index(player) >= 0) disconnects++;
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (ENABLED && event.getEntity() instanceof ServerPlayer player && index(player) >= 0) deaths++;
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (ENABLED && phase == Phase.MEASURING && event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player && index(player) >= 0) playerTicks++;
    }
    @SubscribeEvent public static void chunkLoad(ChunkEvent.Load event) {
        if (ENABLED) chunkLoads.increment();
        if (ENABLED && LIFECYCLE_TRACE && server != null && event.getLevel() == server.overworld()) traceChunk("chunk_load", event.getChunk());
    }
    @SubscribeEvent public static void chunkUnload(ChunkEvent.Unload event) {
        if (ENABLED) chunkUnloads.increment();
        if (ENABLED && LIFECYCLE_TRACE && server != null && event.getLevel() == server.overworld()) traceChunk("chunk_unload", event.getChunk());
    }
    @SubscribeEvent public static void chunkSave(ChunkDataEvent.Save event) { if (ENABLED) chunkSaves.increment(); }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void tickStart(TickEvent.ServerTickEvent event) {
        if (!ENABLED || server == null || event.phase != TickEvent.Phase.START) return;
        mainThread = Thread.currentThread().getId(); tickStarted = System.nanoTime();
        previousMeasured = phase == Phase.MEASURING;
        previousRecovery = phase == Phase.RECOVERY;
        legacyThermalNanos = legacyObservationNanos = 0; legacyDepth = 0;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void tickEnd(TickEvent.ServerTickEvent event) {
        if (!ENABLED || server == null || event.phase != TickEvent.Phase.END) return;
        long actionStarted = System.nanoTime();
        try {
            step();
            if (previousMeasured) {
                measuredTicks++;
                if (measuredTicks % 20 == 0) sample();
                if (measuredTicks % 200 == 0) appendMetrics();
            }
            fixtureNanos += System.nanoTime() - actionStarted;
            if (previousMeasured) {
                ticks.add(System.nanoTime() - tickStarted); fixture.add(fixtureNanos); thermal.add(Math.max(0, thermalNanos - queryObservationNanos));
                legacyThermal.add(Math.max(0, legacyThermalNanos - legacyObservationNanos));
                if (measuredTicks >= SECONDS * 20L) {
                    phase = Phase.DIAGNOSTIC_HOLD;
                    measurement = metrics(); write("measurement.json", measurement);
                    FHMain.LOGGER.info("THERMAL_NETWORK_MEASUREMENT_COMPLETE,ticks={},players={}", measuredTicks, server.getPlayerList().getPlayerCount());
                    if (RECOVERY_SECONDS > 0) {
                        phase = Phase.RECOVERY; phaseTicks = 0;
                        recoveryStartedTick = server.overworld().getGameTime(); recoveryStartedNanos = System.nanoTime();
                        sampleWorkers(); recoveryAllocatedStart = workersAllocatedTotal;
                        recoveryRoundsStart = recoveryTailRoundsStart = coordinatorRounds.sum();
                        recoveryTailStartedNanos = recoveryStartedNanos; recoveryTailAllocatedStart = recoveryAllocatedStart;
                        if (RECOVERY_RETURN_HOME && BASES > 5) moveScouts(Math.max(0, awayCycle), true);
                    }
                }
            }
            if (previousRecovery) {
                long nanos = Math.max(0, thermalNanos - queryObservationNanos);
                recoveryMain.add(nanos);
                if (phaseTicks > Math.max(0, RECOVERY_SECONDS - 10) * 20L) recoveryTailMain.add(nanos);
                if (phaseTicks >= RECOVERY_SECONDS * 20L) finishRecovery();
            }
            if (LIFECYCLE_TRACE && server.overworld().getGameTime() % 20 == 0) flushLifecycleTrace();
        } catch (Exception failure) {
            FHMain.LOGGER.error("Thermal network fixture failed", failure);
            try { write("failure.json", Map.of("error", failure.toString(), "phase", phase.name())); } catch (Exception ignored) { }
            server.halt(false);
        } finally {
            // Keep thermal work between START events: packets and native callbacks
            // can run after the previous tick ended. Each END closes one interval.
            fixtureNanos = thermalNanos = queryObservationNanos = 0; stageDepth = 0;
        }
    }

    private static void step() throws java.io.IOException {
        phaseTicks++;
        var level = server.overworld();
        if (Files.exists(ROOT.resolve("stop.json"))) { write("aborted.json", Map.of("phase", phase.name(), "ticks", measuredTicks)); server.halt(false); return; }
        if (phase == Phase.BUILDING) {
            for (int budget = 0; budget < 512 && buildBase < BASES; budget++) {
                if (buildCell == 0) { forceBase(buildBase, true); forcedBases++; }
                int side = WIDTH + 1, x = buildCell % side, z = buildCell / side % side, y = buildCell / (side * side);
                var block = x == 0 || x == WIDTH || z == 0 || z == WIDTH || y == 0 || y == HEIGHT ? Blocks.STONE : Blocks.AIR;
                level.setBlockAndUpdate(origin(buildBase).offset(x, y, z), block.defaultBlockState());
                if (++buildCell == side * side * (HEIGHT + 1)) { buildBase++; buildCell = 0; }
            }
            if (buildBase == BASES) {
                for (int budget = 0; budget < 25 && placedSources < BASES * SOURCE_SIDE * SOURCE_SIDE; budget++, placedSources++) {
                    int base = placedSources / (SOURCE_SIDE * SOURCE_SIDE), source = placedSources % (SOURCE_SIDE * SOURCE_SIDE);
                    BlockPos fire = origin(base).offset(2 + source % SOURCE_SIDE * 4, 1, 2 + source / SOURCE_SIDE * 4);
                    level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                    refuel(fire);
                    fires.add(fire);
                }
                if (placedSources == BASES * SOURCE_SIDE * SOURCE_SIDE) {
                    for (int b = 0; b < BASES; b++) for (int item = 0; item < 4; item++) {
                        BlockPos point = origin(b).offset(WIDTH / 2 + item, 1, WIDTH / 2);
                        var drop = new ItemEntity(level, point.getX() + .5, point.getY() + .2, point.getZ() + .5,
                                new ItemStack(item % 2 == 0 ? Items.BEEF : Items.POTATO, 16));
                        drop.setNeverPickUp(); drop.setUnlimitedLifetime();
                        if (!level.addFreshEntity(drop)) throw new IllegalStateException("Workload item was not spawned at " + point);
                        workloadItems.add(drop.getUUID()); itemSpawns++;
                    }
                    phase = Phase.WAITING_FOR_PLAYERS; phaseTicks = 0;
                    readyTick = level.getGameTime();
                    write("ready.json", Map.of("ready", true, "pid", ProcessHandle.current().pid(), "sources", fires.size(), "bases", BASES, "sourcesLit", 0, "gameTick", readyTick));
                    FHMain.LOGGER.info("THERMAL_NETWORK_READY,bases={},sources={},items={}", BASES, fires.size(), itemSpawns);
                }
            }
            return;
        }
        for (var player : server.getPlayerList().getPlayers()) if (index(player) >= 0 && phaseTicks % 20 == 0) {
            if (player.getHealth() < player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20); player.getFoodData().setSaturation(20);
        }
        if (phase == Phase.WAITING_FOR_PLAYERS && server.getPlayerList().getPlayerCount() == PLAYERS) {
            phase = Phase.WARMUP; phaseTicks = 0;
        } else if (phase == Phase.WARMUP && ignited < fires.size()) {
            if (ignited == 0 && level.getGameTime() % 20 != 0) return;
            if (ignited == 0) ignitionStartedTick = level.getGameTime();
            for (int budget = 0; budget < 25 && ignited < fires.size(); budget++, ignited++) {
                var fire = fires.get(ignited);
                level.setBlockAndUpdate(fire, level.getBlockState(fire).setValue(CampfireBlock.LIT, true));
            }
            if (ignited == fires.size()) { ignitionCompletedTick = level.getGameTime(); phaseTicks = 0; }
        } else if (phase == Phase.WARMUP && phaseTicks >= 200) {
            var publication = MinecraftThermalInput.queryPublicationForObservation(level);
            if (publication == null) throw new IllegalStateException("The real fueled workload has not created its runtime");
            runtimeKind = "fixed_cell_enthalpy";
            measurementStartedTick = level.getGameTime();
            for (int b = 0; b < forcedBases; b++) forceBase(b, false);
            constructionTicketsReleased = true;
            mainStageTotals.clear();
            phase = Phase.MEASURING; phaseTicks = 0; startedNanos = System.nanoTime();
            mainCpuStart = THREADS.getThreadCpuTime(mainThread); processCpuStart = OS.getProcessCpuTime();
            coordinatorRoundsStart = coordinatorRounds.sum();
            loadsStart = chunkLoads.sum(); unloadsStart = chunkUnloads.sum(); savesStart = chunkSaves.sum();
            sampleWorkers();
            write("measuring.json", Map.of("pid", ProcessHandle.current().pid(), "targetTicks", SECONDS * 20L,
                    "runtime", runtimeKind, "gameTick", measurementStartedTick, "ignitionStartedTick", ignitionStartedTick,
                    "ignitionCompletedTick", ignitionCompletedTick, "warmupTicksAfterIgnition", measurementStartedTick - ignitionCompletedTick));
        } else if (phase == Phase.MEASURING) {
            if (measuredTicks % 1200 == 0) for (var fire : fires) if (level.hasChunkAt(fire)) {
                refuel(fire);
                if (level.getBlockState(fire).is(Blocks.CAMPFIRE) && !level.getBlockState(fire).getValue(CampfireBlock.LIT))
                    level.setBlockAndUpdate(fire, level.getBlockState(fire).setValue(CampfireBlock.LIT, true));
            }
            if (measuredTicks % 20 == 0) {
                updateResidents();
                for (int b = 0; b < BASES; b++) if (residents[b] > 0) {
                    BlockPos point = origin(b).offset(0, 2, WIDTH / 2);
                    if (level.hasChunkAt(point)) {
                        boolean air = measuredTicks % 40 == 0;
                        boolean changed = level.setBlockAndUpdate(point, (air ? Blocks.AIR : Blocks.STONE).defaultBlockState()); mutations++;
                        if (LIFECYCLE_TRACE) trace("wall_edit", b, Map.of("position", point.asLong(), "air", air, "changed", changed));
                    }
                }
            }
            int cycle = (int) (measuredTicks / 600);
            if (cycle != awayCycle && BASES > 5) { awayCycle = cycle; moveScouts(cycle, false); }
            if (measuredTicks % 600 == 300 && BASES > 5) moveScouts(cycle, true);
        } else if (phase == Phase.RECOVERY) {
            if (phaseTicks % 20 == 0) sampleRecovery();
        } else if (phase == Phase.DIAGNOSTIC_HOLD && Files.exists(ROOT.resolve("finish.json"))) {
            write("complete.json", measurement); phase = Phase.FINISHING; phaseTicks = 0;
        } else if (phase == Phase.FINISHING && phaseTicks >= 60) server.halt(false);
    }

    private static void moveScouts(int cycle, boolean home) {
        var level = server.overworld();
        for (int group = 0; group < 5; group++) {
            int target = 1 + (cycle * 5 + group) % (BASES - 1);
            BlockPos remote = origin(target).offset(8192 + cycle * 256, 0, 8192 + group * 256);
            if (!home) for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
                level.setBlockAndUpdate(remote.offset(x, 0, z), Blocks.STONE.defaultBlockState());
            for (var player : server.getPlayerList().getPlayers()) {
                int index = index(player); if (index < 0 || base(index) != target) continue;
                teleport(player, home ? home(index) : remote.offset(index % 2 * 2, 1, index / 2 % 2 * 2));
            }
            if (home) arrived[target] = level.getGameTime();
        }
    }

    private static void updateResidents() {
        java.util.Arrays.fill(residents, 0);
        for (var player : server.getPlayerList().getPlayers()) {
            int index = index(player); if (index < 0) continue;
            BlockPos origin = origin(base(index));
            if (Math.abs(player.getX() - origin.getX() - WIDTH / 2.0) <= WIDTH && Math.abs(player.getZ() - origin.getZ() - WIDTH / 2.0) <= WIDTH) residents[base(index)]++;
        }
    }
    private static void observeGameplayAir(ServerPlayer player, ThermalEnvironmentSample sample, String source) {
        if (phase != Phase.MEASURING || Thread.currentThread().getId() != mainThread) return;
        int index = index(player); if (index < 0) return;
        long started = System.nanoTime();
        try {
            gameplayCalls++;
            int base = base(index); long now = player.serverLevel().getGameTime();
            BlockPos home = origin(base);
            if (now - arrived[base] <= 100 || now - gameplayProbeTick[index] < 20
                    || Math.abs(player.getX() - home.getX() - WIDTH / 2.0) > WIDTH
                    || Math.abs(player.getZ() - home.getZ() - WIDTH / 2.0) > WIDTH) return;
            gameplayProbeTick[index] = now;
            if (sample.airMinimumQuality()) gameplayMinimum++;
            switch (source) {
                case "CURRENT" -> gameplayCurrent++;
                case "CAPTURED" -> { gameplayCaptured++; gameplayCapturedByBase[base]++; }
                case "DORMANT" -> { gameplayDormant++; gameplayDormantByBase[base]++; }
                default -> { gameplayMissing++; gameplayMissingByBase[base]++; }
            }
            if (sample.airSampleTick() >= 0) gameplayAges.add(now - sample.airSampleTick());
        } finally {
            long elapsed = System.nanoTime() - started;
            fixtureNanos += elapsed; queryObservationNanos += elapsed;
            if (legacyDepth > 0) legacyObservationNanos += elapsed;
        }
    }
    private static void sample() throws java.io.IOException {
        var level = server.overworld(); updateResidents();
        var publication = MinecraftThermalInput.queryPublicationForObservation(level);
        for (var player : server.getPlayerList().getPlayers()) {
            int index = index(player); if (index < 0 || level.getGameTime() - arrived[base(index)] <= 100) continue;
            BlockPos origin = origin(base(index));
            if (Math.abs(player.getX() - origin.getX() - WIDTH / 2.0) > WIDTH || Math.abs(player.getZ() - origin.getZ() - WIDTH / 2.0) > WIDTH) continue;
            int base = base(index);
            if (publication == null || !publication.tryReadAir(player.getX(), player.getEyeY(), player.getZ(), probe)) { unavailable++; unavailableByBase[base]++; continue; }
            availableByBase[base]++;
            long age = Math.max(0, level.getGameTime() - probe.sampleTick()); ages.add(age);
            oldestByBase[base] = Math.max(oldestByBase[base], age);
            if (age > 40) {
                ageViolations++; oldByBase[base]++;
                var field = publication.fixedState();
                if (field == null) oldSectionClockByBase[base]++;
                else synchronized (field) {
                    long section = net.minecraft.core.SectionPos.asLong((int) Math.floor(player.getX()) >> 4,
                            (int) Math.floor(player.getEyeY()) >> 4, (int) Math.floor(player.getZ()) >> 4);
                    if (field.sectionSampleTick(section) != probe.sampleTick()) oldReadRacedByBase[base]++;
                    else if (field.frozen(section)) oldFrozenByBase[base]++;
                    else if (!field.canAdvance(section)) oldGeometryHeldByBase[base]++;
                    else oldSectionClockByBase[base]++;
                }
            }
        }
        peakHeap = Math.max(peakHeap, ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        peakAccounted = Math.max(peakAccounted, MinecraftThermalInput.thermalBudgetUsedBytes()); sampleWorkers();
        sampleComparisonPoints();
    }
    private static void sampleComparisonPoints() throws java.io.IOException {
        var level = server.overworld();
        var publication = MinecraftThermalInput.queryPublicationForObservation(level);
        var points = new ArrayList<Map<String, Object>>(BASES * 4);
        for (int base = 0; base < BASES; base++) if (residents[base] > 0) {
            BlockPos home = origin(base);
            for (int point = 0; point < 4; point++) {
                int x = point == 0 ? 2 : point == 1 ? WIDTH / 4 : point == 2 ? WIDTH / 2 : -1;
                int z = point == 0 ? 2 : point == 1 ? WIDTH / 4 : WIDTH / 2;
                double px = home.getX() + x + .5, py = home.getY() + (point == 2 ? 1.25 : 2.625), pz = home.getZ() + z + .5;
                var row = new LinkedHashMap<String, Object>();
                row.put("base", base); row.put("point", point);
                row.put("naturalC", WorldTemperature.naturalAir(level, BlockPos.containing(px, py, pz)));
                boolean available = publication != null && publication.tryReadAir(px, py, pz, probe);
                row.put("available", available);
                if (available) {
                    row.put("temperatureC", probe.temperatureC()); row.put("sampleTick", probe.sampleTick());
                    row.put("sampleSinceIgnition", probe.sampleTick() - ignitionStartedTick);
                    row.put("minimumQuality", probe.minimumQuality());
                }
                points.add(row);
            }
        }
        var frame = Map.of("measuredTicks", measuredTicks, "gameTick", level.getGameTime(),
                "sinceIgnition", level.getGameTime() - ignitionStartedTick, "runtime", runtimeKind, "points", points);
        Files.writeString(ROOT.resolve("comparison-points.jsonl"), JSON.toJson(frame) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void sampleRecovery() throws java.io.IOException {
        var level = server.overworld();
        var publication = MinecraftThermalInput.queryPublicationForObservation(level);
        long available = 0, missing = 0, old = 0, maximumAge = 0;
        boolean tail = phaseTicks > Math.max(0, RECOVERY_SECONDS - 10) * 20L;
        for (var player : server.getPlayerList().getPlayers()) {
            int index = index(player); if (index < 0 || level.getGameTime() - arrived[base(index)] <= 100) continue;
            var home = origin(base(index));
            if (Math.abs(player.getX() - home.getX() - WIDTH / 2.0) > WIDTH
                    || Math.abs(player.getZ() - home.getZ() - WIDTH / 2.0) > WIDTH) continue;
            if (publication == null || !publication.tryReadAir(player.getX(), player.getEyeY(), player.getZ(), probe)) { missing++; continue; }
            long age = Math.max(0, level.getGameTime() - probe.sampleTick());
            available++; maximumAge = Math.max(maximumAge, age); if (age > 40) old++;
            recoveryAges.add(age); if (tail) recoveryTailAges.add(age);
        }
        recoverySamples += available; recoveryMissing += missing; recoveryOld += old;
        if (tail) {
            recoveryTailSamples += available; recoveryTailMissing += missing; recoveryTailOld += old;
            recoveryTailFrames++; recoveryTailMinimumPlayers = Math.min(recoveryTailMinimumPlayers, available + missing);
        }
        if (available > 0 && (!RECOVERY_RETURN_HOME || available == PLAYERS)
                && missing == 0 && old == 0 && recoveryFirstFreshTick < 0) recoveryFirstFreshTick = phaseTicks;
        sampleWorkers();
        if (RECOVERY_SECONDS > 10 && phaseTicks == (RECOVERY_SECONDS - 10) * 20L) {
            recoveryTailStartedNanos = System.nanoTime(); recoveryTailAllocatedStart = workersAllocatedTotal;
            recoveryTailRoundsStart = coordinatorRounds.sum();
        }
        var frame = Map.of("ticks", phaseTicks, "gameTick", level.getGameTime(), "startedTick", recoveryStartedTick,
                "available", available, "missing", missing, "old", old, "maximumAgeTicks", maximumAge);
        Files.writeString(ROOT.resolve("recovery.jsonl"), JSON.toJson(frame) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void finishRecovery() throws java.io.IOException {
        double seconds = (System.nanoTime() - recoveryStartedNanos) / 1e9;
        double tailSeconds = (System.nanoTime() - recoveryTailStartedNanos) / 1e9;
        var result = new LinkedHashMap<String, Object>();
        result.put("ticks", phaseTicks); result.put("elapsedSeconds", seconds);
        result.put("players", server.getPlayerList().getPlayerCount());
        result.put("enabledPhysicalSources", MinecraftThermalInput.enabledPhysicalSourceCount(server.overworld()));
        result.put("returnScoutsHome", RECOVERY_RETURN_HOME);
        result.put("samples", recoverySamples); result.put("missing", recoveryMissing); result.put("old", recoveryOld);
        result.put("ageP99Ticks", recoveryAges.percentile(.99)); result.put("maximumAgeTicks", recoveryAges.maximum);
        result.put("firstAllFreshAfterTicks", recoveryFirstFreshTick);
        result.put("tailSamples", recoveryTailSamples); result.put("tailMissing", recoveryTailMissing); result.put("tailOld", recoveryTailOld);
        result.put("tailAgeP99Ticks", recoveryTailAges.percentile(.99)); result.put("tailMaximumAgeTicks", recoveryTailAges.maximum);
        result.put("tailFrames", recoveryTailFrames); result.put("tailMinimumObservedPlayers", recoveryTailMinimumPlayers);
        result.put("mainMs", recoveryMain.milliseconds()); result.put("tailMainMs", recoveryTailMain.milliseconds());
        result.put("workerAllocatedBytes", workersAllocatedTotal - recoveryAllocatedStart);
        result.put("workerAllocatedMiBPerSecond", (workersAllocatedTotal - recoveryAllocatedStart) / seconds / 1048576);
        result.put("tailElapsedSeconds", tailSeconds); result.put("tailWorkerAllocatedBytes", workersAllocatedTotal - recoveryTailAllocatedStart);
        result.put("tailWorkerAllocatedMiBPerSecond", (workersAllocatedTotal - recoveryTailAllocatedStart) / tailSeconds / 1048576);
        result.put("coordinatorRounds", coordinatorRounds.sum() - recoveryRoundsStart);
        result.put("tailCoordinatorRounds", coordinatorRounds.sum() - recoveryTailRoundsStart);
        if (Boolean.getBoolean("frostedheart.thermalLoad.workerTiming")) result.put("workerTaskTiming", ThermalNetworkWorkerTiming.recovery());
        result.put("scope", "Wall edits and scout trips stop; optional one-time return brings scouts home before recovery. Raw sample times, no forced catch-up or timestamp writes. Tail is the final ten seconds; main scope matches the measured workload.");
        write("recovery.json", result); phase = Phase.DIAGNOSTIC_HOLD;
        FHMain.LOGGER.info("THERMAL_NETWORK_RECOVERY_COMPLETE,firstFreshTicks={},tailMissing={},tailOld={},tailMaxAge={}",
                recoveryFirstFreshTick, recoveryTailMissing, recoveryTailOld, recoveryTailAges.maximum);
    }
    private static void sampleWorkers() {
        for (long id : THREADS.getAllThreadIds()) {
            var info = THREADS.getThreadInfo(id); if (info == null || !info.getThreadName().startsWith("frosted-heart-thermal-")) continue;
            long cpu = THREADS.getThreadCpuTime(id), allocated = THREADS.getThreadAllocatedBytes(id);
            Long before = workerCpu.put(id, cpu), bytes = workerAllocated.put(id, allocated);
            if (before != null && cpu >= before && before >= 0) workersCpuTotal += cpu - before;
            if (bytes != null && allocated >= bytes && bytes >= 0) workersAllocatedTotal += allocated - bytes;
        }
    }

    private static void traceSourceControls(com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope input) {
        if (!LIFECYCLE_TRACE || server == null) return;
        for (var window : input.sourceWindows()) {
            if (!window.controlsChanged) continue;
            var descriptor = window.descriptor;
            for (int base = 0; base < BASES; base++) {
                var home = origin(base); int dx = descriptor.x() - home.getX(), dz = descriptor.z() - home.getZ();
                if (descriptor.y() != home.getY() + 1 || dx < 0 || dx >= WIDTH || dz < 0 || dz >= WIDTH || (dx & 3) != 2 || (dz & 3) != 2) continue;
                int index = base * SOURCE_SIDE * SOURCE_SIDE + (dz >> 2) * SOURCE_SIDE + (dx >> 2);
                var state = new TraceSourceState(descriptor.generation(), window.enabled, window.unload, window.finalPowerW);
                if (state.equals(tracedSources[index])) break;
                tracedSources[index] = state;
                var details = new LinkedHashMap<String, Object>();
                details.put("sourceIndex", index); details.put("sourceId", descriptor.sourceId()); details.put("generation", descriptor.generation());
                details.put("enabled", window.enabled); details.put("unloaded", window.unload); details.put("powerW", window.finalPowerW);
                details.put("registration", window.registration); details.put("fromTick", window.fromTick); details.put("toTick", window.toTick);
                details.put("boundaryTick", window.boundaryTick); details.put("envelopeTick", input.targetTick());
                trace("source_control", base, details); break;
            }
        }
    }

    private static void traceChunk(String kind, net.minecraft.world.level.chunk.ChunkAccess chunk) {
        for (int base = 0; base < BASES; base++) {
            var home = origin(base); int dx = chunk.getPos().x - (home.getX() >> 4), dz = chunk.getPos().z - (home.getZ() >> 4);
            if (dx < 0 || dx > (WIDTH - 1) >> 4 || dz < 0 || dz > (WIDTH - 1) >> 4) continue;
            long savedTick = -1, bricks = 0;
            var details = new LinkedHashMap<String, Object>();
            if (chunk instanceof com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment attachment) {
                var stored = attachment.frostedheart$getDormantThermalState();
                var air = stored == null ? null : stored.air(home.getY() >> 4);
                if (air != null) {
                    savedTick = air.savedGameTick(); bricks = air.brickMask();
                    int brick = ((home.getY() + 2 & 15) >> 2) << 4;
                    if (air.hasBrick(brick)) {
                        var temperatures = new double[64];
                        // These fresh fixed-run checkpoints store absolute canonical cells.
                        air.fillBlockTemperatures(brick, 0, 1, temperatures);
                        details.put("storedProbeC", temperatures[2 | 2 << 2 | ((home.getY() + 2) & 3) << 4]);
                        details.put("storedProbePosition", BlockPos.asLong((chunk.getPos().x << 4) + 2, home.getY() + 2, (chunk.getPos().z << 4) + 2));
                    }
                }
            }
            details.put("chunkX", chunk.getPos().x); details.put("chunkZ", chunk.getPos().z); details.put("savedAirTick", savedTick); details.put("savedBricks", bricks);
            trace(kind, base, details);
            break;
        }
    }

    private static void trace(String kind, int base, Map<String, Object> details) {
        var row = new LinkedHashMap<String, Object>();
        row.put("kind", kind); row.put("base", base); row.put("phase", phase.name()); row.put("gameTick", server.overworld().getGameTime());
        row.put("sinceIgnition", server.overworld().getGameTime() - ignitionStartedTick); row.put("measuredTicks", measuredTicks); row.putAll(details);
        if (!lifecycleEvents.offer(JSON.toJson(row))) droppedLifecycleEvents.increment();
    }

    private static void flushLifecycleTrace() throws java.io.IOException {
        if (!LIFECYCLE_TRACE || lifecycleEvents.isEmpty()) return;
        var batch = new StringBuilder(); String line;
        while ((line = lifecycleEvents.poll()) != null) batch.append(line).append('\n');
        Files.writeString(ROOT.resolve("lifecycle-events.jsonl"), batch, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    private static Map<String, Object> metrics() {
        var result = new LinkedHashMap<String, Object>();
        double seconds = startedNanos == 0 ? 0 : (System.nanoTime() - startedNanos) / 1e9;
        result.put("phase", phase.name()); result.put("pid", ProcessHandle.current().pid()); result.put("elapsedSeconds", seconds);
        result.put("runtime", runtimeKind); result.put("readyTick", readyTick); result.put("ignitionStartedTick", ignitionStartedTick);
        result.put("workloadProfile", "join_then_ignite_with_comparison_points");
        result.put("ignitionCompletedTick", ignitionCompletedTick); result.put("measurementStartedTick", measurementStartedTick);
        result.put("warmupTicksAfterIgnition", measurementStartedTick - ignitionCompletedTick); result.put("sourcesIgnited", ignited);
        result.put("measuredTicks", measuredTicks); result.put("achievedTps", seconds == 0 ? 0 : measuredTicks / seconds);
        result.put("players", server.getPlayerList().getPlayerCount()); result.put("playerTickEvents", playerTicks);
        result.put("survivalPlayers", server.getPlayerList().getPlayers().stream().filter(p -> !p.isCreative() && !p.isSpectator()).count());
        result.put("invulnerablePlayers", server.getPlayerList().getPlayers().stream().filter(p -> p.isInvulnerable() || p.getAbilities().invulnerable).count());
        result.put("deaths", deaths); result.put("disconnects", disconnects); result.put("teleports", teleports);
        result.put("mutations", mutations); result.put("itemsSpawned", itemSpawns);
        result.put("loadedWorkloadItems", workloadItems.stream().filter(id -> server.overworld().getEntity(id) instanceof ItemEntity).count());
        result.put("coalAccepted", fuelItems); result.put("constructionTicketsReleased", constructionTicketsReleased);
        result.put("residentPlayers", java.util.Arrays.stream(residents).sum()); result.put("residentSamples", ages.count);
        result.put("availableByBase", availableByBase.clone()); result.put("unavailableByBase", unavailableByBase.clone());
        result.put("oldByBase", oldByBase.clone()); result.put("oldestByBase", oldestByBase.clone());
        result.put("oldFrozenByBase", oldFrozenByBase.clone()); result.put("oldGeometryHeldByBase", oldGeometryHeldByBase.clone());
        result.put("oldGlobalClockByBase", oldGlobalClockByBase.clone()); result.put("oldSectionClockByBase", oldSectionClockByBase.clone());
        result.put("oldReadRacedByBase", oldReadRacedByBase.clone());
        result.put("oldSampleScope", "Committed Tile sampleTick is the fixed runtime's numerical time. Over-age samples are classified as frozen, section/legacy, or raced publication; old global-clock/geometry-held counters remain zero for output compatibility and are not causal categories for this implementation.");
        result.put("sourceBlocks", fires.size()); result.put("enabledPhysicalSources", MinecraftThermalInput.enabledPhysicalSourceCount(server.overworld()));
        result.put("tickEventSpanMs", ticks.milliseconds()); result.put("fixtureMainMs", fixture.milliseconds()); result.put("instrumentedThermalMainMs", thermal.milliseconds());
        result.put("legacyThermalMainMs", legacyThermal.milliseconds());
        var stages = new java.util.LinkedHashMap<String, Object>();
        mainStageTotals.forEach((name, value) -> stages.put(name,
                Map.of("count", value[0], "totalMs", value[1] / 1e6, "maxMs", value[2] / 1e6)));
        result.put("thermalMainStageTotals", stages);
        result.put("thermalMainStageScope", "Disjoint outermost thermal observer stages; same gameplay-observer exclusion as the aggregate, added for attribution. Cumulative totals do not decompose per-tick P99.");
        result.put("geometryTransactions", geometryCommits.sum());
        result.put("coefficientOnlyGeometryTransactions", coefficientOnlyGeometryCommits.sum());
        result.put("mixingUpdatePages", mixingUpdatePages.sum()); result.put("mixingUpdateBricks", mixingUpdateBricks.sum());
        result.put("geometryBoundarySections", geometryBoundarySections.sum());
        result.put("geometryConsecutiveRepeatedCuts", geometryRepeatedCuts.sum());
        result.put("geometryCutScope", "Original geometry PUBLISHED callbacks during measurement; repeated means consecutive commits with the same producer sequence and physical tick. No task references retained.");
        result.put("thermalMainScopeRevision", 2);
        result.put("thermalMainWindow", "Previous ServerTick END to current END; legacyThermalMainMs retains the former START-to-END window");
        result.put("thermalMainScope", "Input/save/load stages plus campfire input, world edits and player/passive/item/town/material queries; nested stages counted once, gameplay observer time excluded; natural climate computation and vanilla chunk serialization outside these scopes remain in whole-server CPU/JFR");
        result.put("gameplayPlayerAirCalls", gameplayCalls);
        result.put("gameplayResidentAir", Map.of("current", gameplayCurrent, "captured", gameplayCaptured, "dormant", gameplayDormant, "missing", gameplayMissing,
                "sampledLiveCount", gameplayAges.count, "liveAgeP99Ticks", gameplayAges.percentile(.99), "liveAgeMaxTicks", gameplayAges.maximum,
                "minimumQuality", gameplayMinimum));
        result.put("gameplayCapturedByBase", gameplayCapturedByBase.clone());
        result.put("gameplayDormantByBase", gameplayDormantByBase.clone()); result.put("gameplayMissingByBase", gameplayMissingByBase.clone());
        result.put("workerCpuMs", workersCpuTotal / 1e6); result.put("workerAllocatedBytes", workersAllocatedTotal);
        result.put("workerCoordinatorRounds", coordinatorRounds.sum() - coordinatorRoundsStart);
        if (Boolean.getBoolean("frostedheart.thermalLoad.workerTiming")) {
            result.put("workerTaskTiming", ThermalNetworkWorkerTiming.mixed()); result.put("workerSnapshot", workerSnapshot);
        }
        result.put("mainCpuMs", (THREADS.getThreadCpuTime(mainThread) - mainCpuStart) / 1e6); result.put("processCpuMs", (OS.getProcessCpuTime() - processCpuStart) / 1e6);
        result.put("heapUsedBytes", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()); result.put("peakSampledHeapBytes", peakHeap);
        result.put("heapMaxBytes", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax());
        result.put("thermalAccountedBytes", MinecraftThermalInput.thermalBudgetUsedBytes()); result.put("peakSampledAccountedBytes", peakAccounted);
        result.put("thermalAccountedPeakBytes", MinecraftThermalInput.thermalBudgetPeakBytes()); result.put("thermalTargetBytes", MinecraftThermalInput.thermalBudgetLimitBytes());
        result.put("admissionRefusals", MinecraftThermalInput.thermalBudgetRefusals()); result.put("unavailableResidentSamples", unavailable);
        result.put("residentAgeViolations", ageViolations); result.put("residentAgeP99Ticks", ages.percentile(.99)); result.put("residentAgeMaxTicks", ages.maximum);
        result.put("chunkLoads", chunkLoads.sum() - loadsStart); result.put("chunkUnloads", chunkUnloads.sum() - unloadsStart); result.put("chunkSaves", chunkSaves.sum() - savesStart);
        result.put("thermalSaveAndUnload", MinecraftThermalInput.observedMainWaits(server.overworld()));
        result.put("lifecycleTrace", LIFECYCLE_TRACE); result.put("droppedLifecycleEvents", droppedLifecycleEvents.sum());
        result.put("scope", "Actual dedicated server and headless sockets; controlled survival players have 1024 health, resistance and flight, with invulnerable=false; no rendering or client-mod logic");
        return result;
    }
    private static void appendMetrics() throws java.io.IOException {
        String value = JSON.toJson(metrics()); Files.writeString(ROOT.resolve("metrics.jsonl"), value + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        write("state.json", metrics());
        FHMain.LOGGER.info("THERMAL_NETWORK_PROGRESS,ticks={},players={},accounted={},heap={},unavailable={},oldSamples={}", measuredTicks,
                server.getPlayerList().getPlayerCount(), MinecraftThermalInput.thermalBudgetUsedBytes(), peakHeap, unavailable, ageViolations);
    }
    private static void write(String name, Object value) throws java.io.IOException { Files.writeString(ROOT.resolve(name), JSON.toJson(value)); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) throws java.io.IOException {
        if (ENABLED) { flushLifecycleTrace(); write("stopped.json", Map.of("thermalAccountedBytes", MinecraftThermalInput.thermalBudgetUsedBytes(), "pid", ProcessHandle.current().pid())); ThermalDiagnostics.observer = null; }
    }
}
