/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftThermalSectionAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Passive queries in Chunks without a Page answer from the dormant record or natural value
 * without attaching mutation owners. Also logs query, mutation-hook and drain cost.
 */
@GameTestHolder("frostedheart_passive_query_fast_path")
@PrefixGameTestTemplate(false)
public final class ThermalPassiveQueryFastPathGameTests {
    private static final String TEMPLATE = "phase0a_empty";
    private static final double NATURAL_C = 7.25D;
    private static final int WARMUP_QUERIES = 50_000;
    private static final int MEASURED_QUERIES = 400_000;
    private static final int MUTATIONS = 4_096;
    private static final int NATURAL_WINDOW_TICKS = 400;
    private static final int DRAIN_TICKS = 3;

    private ThermalPassiveQueryFastPathGameTests() {
    }

    /** Main-thread inclusive wall time and call count per diagnostic stage. */
    private static final class StageTotals implements ThermalDiagnostics.Observer {
        private final Thread main;
        private final Map<String, long[]> totals = new HashMap<>();
        private final String[] names = new String[32];
        private final long[] starts = new long[32];
        private int depth;

        StageTotals(Thread main) {
            this.main = main;
        }

        @Override
        public void stage(String stage, boolean entering) {
            if (Thread.currentThread() != main) return;
            if (entering) {
                if (depth < names.length) {
                    names[depth] = stage;
                    starts[depth] = System.nanoTime();
                }
                depth++;
                return;
            }
            if (depth == 0) return;
            depth--;
            if (depth < names.length && stage.equals(names[depth])) {
                long[] total = totals.computeIfAbsent(stage, ignored -> new long[2]);
                total[0]++;
                total[1] += System.nanoTime() - starts[depth];
            }
        }

        void reset() {
            totals.clear();
        }

        long count(String stage) {
            long[] total = totals.get(stage);
            return total == null ? 0 : total[0];
        }

        long nanos(String stage) {
            long[] total = totals.get(stage);
            return total == null ? 0 : total[1];
        }
    }

    @GameTest(template = TEMPLATE, batch = "frostedheart_passive_query_fast_path", timeoutTicks = 3_000)
    public static void pagelessChunksAnswerWithoutAttachingOwners(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 4; y++)
                    level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        }
        for (int x = 1; x <= 2; x++) {
            for (int z = 1; z <= 2; z++) {
                BlockPos campfire = base.offset(x, 0, z);
                level.setBlockAndUpdate(campfire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
                BlockEntity entity = level.getBlockEntity(campfire);
                if (entity instanceof ICampfireExtra extra) extra.setLifeTime(20_000);
            }
        }
        BlockPos sourceAir = base.offset(1, 2, 1);
        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(sourceAir.getX() + 0.5D, sourceAir.getY() + 0.5D - player.getEyeHeight(), sourceAir.getZ() + 0.5D);
        MinecraftThermalInput.samplePlayerAirTemperature(
                player, WorldTemperature.naturalAir(level, sourceAir), new ThermalEnvironmentSample());

        ChunkPos home = new ChunkPos(sourceAir);
        List<LevelChunk> far = new ArrayList<>();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) < 4) continue;
                level.setChunkForced(home.x + dx, home.z + dz, true);
                far.add(level.getChunk(home.x + dx, home.z + dz));
            }
        }
        int minSection = level.getMinSection(), maxSection = level.getMaxSection();
        // A Section in the Page's own Chunk, far above any Page: owners are still attached there.
        BlockPos pagedChunkProbe = new BlockPos(sourceAir.getX(),
                SectionPos.sectionToBlockCoord(Math.min(maxSection - 1, SectionPos.blockToSectionCoord(sourceAir.getY()) + 4)) + 8,
                sourceAir.getZ());
        BlockPos[] queries = queryPositions(far, minSection, maxSection, new BlockPos[8_192]);
        // Random ticks query Chunk by Chunk; this order keeps consecutive queries in one Chunk.
        BlockPos[] localQueries = localQueryPositions(far, minSection, maxSection, 64);
        Thread main = Thread.currentThread();
        StageTotals totals = new StageTotals(main);
        ThermalDiagnostics.Observer previous = ThermalDiagnostics.observer;
        long[] farOwners = new long[3];
        long[] naturalWindow = new long[7];
        long[] explicit = new long[3];
        long[] mutation = new long[4];
        BlockPos[] mutated = new BlockPos[MUTATIONS];
        BlockState[] originals = new BlockState[MUTATIONS];

        helper.startSequence()
                .thenWaitUntil(() -> {
                    var publication = MinecraftThermalInput.queryPublicationForObservation(level);
                    helper.assertTrue(publication != null && publication.regionAt(
                                    sourceAir.getX() + 0.5D, sourceAir.getY() + 0.5D, sourceAir.getZ() + 0.5D) >= 0,
                            "campfire Page is published: " + MinecraftThermalInput.describeWorkerProgress(level));
                })
                .thenExecute(() -> {
                    farOwners[0] = owners(far);
                    naturalWindow[0] = level.getGameTime();
                    totals.reset();
                    ThermalDiagnostics.observer = totals;
                })
                .thenIdle(NATURAL_WINDOW_TICKS)
                .thenExecute(() -> {
                    ThermalDiagnostics.observer = previous;
                    naturalWindow[1] = level.getGameTime() - naturalWindow[0];
                    naturalWindow[2] = totals.count("thermal_passive_query");
                    naturalWindow[3] = totals.nanos("thermal_passive_query");
                    naturalWindow[4] = totals.count("thermal_world_mutation");
                    naturalWindow[5] = totals.nanos("thermal_world_mutation");
                    naturalWindow[6] = totals.nanos("thermal_input_tick");
                    farOwners[1] = owners(far);

                    long[] mismatches = new long[1];
                    query(level, queries, WARMUP_QUERIES, mismatches);
                    query(level, localQueries, WARMUP_QUERIES, mismatches);
                    explicit[0] = medianRound(level, queries, mismatches);
                    explicit[2] = medianRound(level, localQueries, mismatches);
                    explicit[1] = mismatches[0];
                    farOwners[2] = owners(far);
                    long started;

                    RandomSource random = RandomSource.create(0x5EED_F00DL);
                    for (int index = 0; index < MUTATIONS; index++) {
                        LevelChunk chunk = far.get(index % far.size());
                        BlockPos position;
                        BlockState original;
                        do {
                            position = new BlockPos(chunk.getPos().getMinBlockX() + random.nextInt(16),
                                    SectionPos.sectionToBlockCoord(minSection + random.nextInt(maxSection - minSection)) + random.nextInt(16),
                                    chunk.getPos().getMinBlockZ() + random.nextInt(16));
                            original = chunk.getBlockState(position);
                        } while (original.hasBlockEntity());
                        mutated[index] = position;
                        originals[index] = original;
                    }
                    totals.reset();
                    ThermalDiagnostics.observer = totals;
                    started = System.nanoTime();
                    for (int index = 0; index < MUTATIONS; index++) {
                        BlockState next = originals[index].is(Blocks.GLASS)
                                ? Blocks.STONE.defaultBlockState() : Blocks.GLASS.defaultBlockState();
                        level.setBlock(mutated[index], next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                    mutation[0] = System.nanoTime() - started;
                    mutation[1] = totals.count("thermal_world_mutation");
                    mutation[2] = totals.nanos("thermal_world_mutation");
                    totals.reset();
                })
                .thenIdle(DRAIN_TICKS)
                .thenExecute(() -> {
                    ThermalDiagnostics.observer = previous;
                    mutation[3] = totals.nanos("thermal_input_tick");
                    LevelChunk pagedChunk = level.getChunk(pagedChunkProbe.getX() >> 4, pagedChunkProbe.getZ() >> 4);
                    int probeSection = pagedChunk.getSectionIndex(pagedChunkProbe.getY());
                    boolean pagedChunkOwnerBefore = owner(pagedChunk, probeSection);
                    MinecraftThermalInput.sampleAirTemperature(level, pagedChunkProbe, NATURAL_C);
                    boolean pagedChunkOwner = owner(pagedChunk, probeSection);
                    long sections = (long) far.size() * (maxSection - minSection);
                    FHMain.LOGGER.info("THERMAL_PASSIVE_FAST_PATH,farChunks={},farSections={},farOwnersBeforeWindow={},"
                                    + "farOwnersAfterWindow={},farOwnersAfterQueries={},windowTicks={},windowPassiveQueries={},"
                                    + "windowPassiveUsPerTick={},windowMutationHooks={},windowMutationUsPerTick={},"
                                    + "windowInputTickUsPerTick={},measuredQueries={},nsPerQuery={},nsPerLocalQuery={},mismatches={},"
                                    + "mutations={},mutationLoopUs={},mutationHooks={},mutationHookUs={},"
                                    + "drainInputTickUsPerTick={},pagedChunkOwnerBefore={},pagedChunkOwner={}",
                            far.size(), sections, farOwners[0], farOwners[1], farOwners[2], naturalWindow[1],
                            naturalWindow[2], perTickUs(naturalWindow[3], naturalWindow[1]), naturalWindow[4],
                            perTickUs(naturalWindow[5], naturalWindow[1]), perTickUs(naturalWindow[6], naturalWindow[1]),
                            MEASURED_QUERIES, (double) explicit[0] / MEASURED_QUERIES,
                            (double) explicit[2] / MEASURED_QUERIES, explicit[1], MUTATIONS,
                            mutation[0] / 1_000L, mutation[1], mutation[2] / 1_000L, perTickUs(mutation[3], DRAIN_TICKS),
                            pagedChunkOwnerBefore, pagedChunkOwner);
                    for (var chunk : far) level.setChunkForced(chunk.getPos().x, chunk.getPos().z, false);
                    MinecraftThermalInput.closeActiveLevel(level);
                    helper.assertTrue(explicit[1] == 0, "page-less passive queries return the natural input unchanged");
                    helper.assertTrue(pagedChunkOwner, "a query in a Chunk with a Page still attaches its Section owner");
                    helper.assertTrue(farOwners[1] == farOwners[0],
                            "random ticks attach no owners in page-less Chunks: before=" + farOwners[0] + ", after=" + farOwners[1]);
                    helper.assertTrue(farOwners[2] == farOwners[1],
                            "page-less passive queries attach no owners: before=" + farOwners[1] + ", after=" + farOwners[2]);
                })
                .thenSucceed();
    }

    private static BlockPos[] queryPositions(List<LevelChunk> chunks, int minSection, int maxSection, BlockPos[] out) {
        RandomSource random = RandomSource.create(0x0C0FFEEL);
        for (int index = 0; index < out.length; index++) {
            LevelChunk chunk = chunks.get(random.nextInt(chunks.size()));
            out[index] = new BlockPos(chunk.getPos().getMinBlockX() + random.nextInt(16),
                    SectionPos.sectionToBlockCoord(minSection + random.nextInt(maxSection - minSection)) + random.nextInt(16),
                    chunk.getPos().getMinBlockZ() + random.nextInt(16));
        }
        return out;
    }

    private static BlockPos[] localQueryPositions(List<LevelChunk> chunks, int minSection, int maxSection, int perChunk) {
        RandomSource random = RandomSource.create(0x10CA1L);
        BlockPos[] out = new BlockPos[chunks.size() * perChunk];
        for (int index = 0; index < out.length; index++) {
            LevelChunk chunk = chunks.get(index / perChunk);
            out[index] = new BlockPos(chunk.getPos().getMinBlockX() + random.nextInt(16),
                    SectionPos.sectionToBlockCoord(minSection + random.nextInt(maxSection - minSection)) + random.nextInt(16),
                    chunk.getPos().getMinBlockZ() + random.nextInt(16));
        }
        return out;
    }

    private static void query(ServerLevel level, BlockPos[] positions, int count, long[] mismatches) {
        for (int index = 0; index < count; index++) {
            double value = MinecraftThermalInput.sampleAirTemperature(level, positions[index % positions.length], NATURAL_C);
            if (Double.compare(value, NATURAL_C) != 0) mismatches[0]++;
        }
    }

    /** Median wall time of five rounds of MEASURED_QUERIES queries. */
    private static long medianRound(ServerLevel level, BlockPos[] positions, long[] mismatches) {
        long[] rounds = new long[5];
        for (int round = 0; round < rounds.length; round++) {
            long started = System.nanoTime();
            query(level, positions, MEASURED_QUERIES, mismatches);
            rounds[round] = System.nanoTime() - started;
        }
        java.util.Arrays.sort(rounds);
        return rounds[rounds.length / 2];
    }

    private static long owners(List<LevelChunk> chunks) {
        long count = 0;
        for (var chunk : chunks)
            for (int index = 0; index < chunk.getSections().length; index++) if (owner(chunk, index)) count++;
        return count;
    }

    private static boolean owner(LevelChunk chunk, int sectionIndex) {
        var owner = ((MinecraftThermalSectionAttachment) (Object) chunk.getSections()[sectionIndex])
                .frostedheart$getThermalInputOwner();
        return owner != null && owner.isValid();
    }

    private static double perTickUs(long nanos, long ticks) {
        return ticks <= 0 ? 0 : nanos / 1_000.0D / ticks;
    }
}
