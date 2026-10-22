package com.teammoeg.frostedheart.content.world.features;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import com.teammoeg.frostedheart.util.mixin.SeedSetable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Opt-in real noise/carver/feature samples conditional on two fixed biomes, not preset occurrence. */
@GameTestHolder("frostedheart_ice_cave_sample")
@PrefixGameTestTemplate(false)
public final class IceCaveNaturalSampleGameTests {
    private static final long[] SEEDS = {111L, 222L, 333L};
    private static final String[] DIMENSIONS = {"ice", "hydrothermal"};
    private static final int SIZE = Integer.getInteger("frostedheart.iceCaveSampleChunks", 4);

    @GameTest(template = "phase0a_empty", batch = "ice_cave_sample", timeoutTicks = 2400)
    public static void sampleConditionalNoiseTerrain(GameTestHelper helper) {
        new Sampling(helper).next();
    }

    private static final class Sampling {
        private final GameTestHelper helper;
        private final List<Map<String, Object>> results = new ArrayList<>();
        private int sample;
        private int generated;
        private int carved;
        private int probed;
        private int counted;
        private ServerLevel level;
        private int start;
        private long startedNanos;
        private long carveNanos, baselineNanos, featureNanos, countNanos;
        private Map<String, Long> counts;
        private IceCaveLandscapeMetrics landscape;
        private final Set<BlockPos> sources = new HashSet<>();
        private final Set<BlockPos> viableBasins = new HashSet<>();

        private Sampling(GameTestHelper helper) { this.helper = helper; }

        private void next() {
            if (sample == SEEDS.length * DIMENSIONS.length) {
                Path output = Path.of(System.getProperty("frostedheart.validationRoot", ".."), "build", "ice-cave-natural-samples.json");
                try {
                    Files.createDirectories(output.getParent());
                    Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(results));
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot write natural sampling result", exception);
                }
                helper.succeed();
                return;
            }
            int seedIndex = sample / DIMENSIONS.length;
            String dimension = DIMENSIONS[sample % DIMENSIONS.length];
            level = helper.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation("frostedheart_ice_cave_sample", dimension)));
            helper.assertTrue(level != null, "prepare sampler assets after processGameTestResources before launch");
            ((SeedSetable) level).setSeed(SEEDS[seedIndex]);
            helper.assertTrue(level.getSeed() == SEEDS[seedIndex], "dimension uses the requested sample seed");
            start = 1024 + seedIndex * 128;
            generated = 0;
            carved = 0;
            probed = 0;
            counted = 0;
            startedNanos = System.nanoTime();
            carveNanos = baselineNanos = featureNanos = countNanos = 0;
            counts = new LinkedHashMap<>();
            landscape = new IceCaveLandscapeMetrics();
            for (String key : List.of("sampled_volume", "air", "air_at_or_below_40", "stone_deepslate_tuff",
                    "vanilla_ice", "pyrite", "pyrolusite", "zinc", "lead", "silver", "ice_crystal",
                    "glowstone_crystal", "hydrothermal_rock", "spring_source", "spring_flowing",
                    "deep_floor_with_two_air_after_decoration", "wide_deep_floor_after_decoration",
                    "valid_basin_origins_before_features")) counts.put(key, 0L);
            sources.clear();
            viableBasins.clear();
            System.out.println("ICE_CAVE_SAMPLE start seed=" + SEEDS[seedIndex] + " biome=" + dimension);
            step();
        }

        private void step() {
            int width = SIZE + 2;
            long operationStart = System.nanoTime();
            if (carved < width * width) {
                level.getChunkSource().getChunk(start - 1 + carved % width, start - 1 + carved / width,
                        ChunkStatus.CARVERS, true);
                carved++;
                carveNanos += System.nanoTime() - operationStart;
            } else if (probed < SIZE * SIZE) {
                probeBasins(start + probed % SIZE, start + probed / SIZE);
                landscape.beforeChunk(level, start + probed % SIZE, start + probed / SIZE, pos -> true);
                probed++;
                baselineNanos += System.nanoTime() - operationStart;
            } else if (generated < width * width) {
                // Complete a one-chunk border before counting interior blocks, including neighbors' features.
                level.getChunk(start - 1 + generated % width, start - 1 + generated / width);
                generated++;
                featureNanos += System.nanoTime() - operationStart;
            } else if (counted < SIZE * SIZE) {
                countChunk(level.getChunk(start + counted % SIZE, start + counted / SIZE));
                landscape.afterChunk(level, start + counted % SIZE, start + counted / SIZE, pos -> true);
                counted++;
                countNanos += System.nanoTime() - operationStart;
            } else {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("seed", SEEDS[sample / DIMENSIONS.length]);
                result.put("fixed_biome", DIMENSIONS[sample % DIMENSIONS.length]);
                result.put("scope", "Actual TWR noise/carver/feature terrain with fixed biome; not normal preset biome occurrence");
                result.put("world_directory", System.getProperty("frostedheart.iceCaveSampleWorld", "legacy default; use sample init script for a new world each run"));
                result.put("runtime_directory", Path.of("").toAbsolutePath().toString());
                result.put("interior_chunk_start_xz", start);
                result.put("interior_chunks", SIZE * SIZE);
                result.put("generated_border_chunks", width * width - SIZE * SIZE);
                result.put("min_y_inclusive", level.getMinBuildHeight());
                result.put("max_y_exclusive", level.getMaxBuildHeight());
                result.put("spring_source_components_clipped_to_interior", sourceComponents());
                result.put("distinct_viable_basins_before_features", viableBasins.size());
                result.put("viable_basin_positions_before_features", viableBasins.stream()
                        .sorted(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ)
                                .thenComparingInt(BlockPos::getY))
                        .map(pos -> List.of(pos.getX(), pos.getY(), pos.getZ())).toList());
                result.put("counts", counts);
                result.put("landscape", landscape.finish(level));
                result.put("timing_ms", Map.of("elapsed_wall", (System.nanoTime() - startedNanos) / 1_000_000L,
                        "carvers_requests", carveNanos / 1_000_000L, "baseline_and_basin_scan", baselineNanos / 1_000_000L,
                        "features_to_full_requests", featureNanos / 1_000_000L, "final_count_scan", countNanos / 1_000_000L));
                results.add(result);
                System.out.println("ICE_CAVE_SAMPLE result " + new GsonBuilder().create().toJson(result));
                sample++;
                helper.runAfterDelay(1, this::next);
                return;
            }
            helper.runAfterDelay(1, this::step);
        }

        private void probeBasins(int chunkX, int chunkZ) {
            var chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.CARVERS, true);
            helper.assertTrue(!chunk.getStatus().isOrAfter(ChunkStatus.FEATURES),
                    "basin telemetry inspects carved terrain before feature decoration");
            var region = new WorldGenRegion(level, List.of(chunk), ChunkStatus.CARVERS, 0);
            // Every original x/z clamps to one of these four anchor pairs. Enumerating all
            // source heights gives actual pre-feature opportunities without placing a pool.
            for (int localX : new int[] {7, 8}) for (int localZ : new int[] {7, 8}) {
                for (int y = -40; y <= 0; y++) {
                    BlockPos basin = IceCaveSpringFeature.findBasin(region,
                            new BlockPos(chunkX * 16 + localX, y, chunkZ * 16 + localZ));
                    if (basin != null) {
                        add("valid_basin_origins_before_features");
                        viableBasins.add(basin.immutable());
                    }
                }
            }
        }

        private void countChunk(LevelChunk chunk) {
            int minX = chunk.getPos().getMinBlockX();
            int minZ = chunk.getPos().getMinBlockZ();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    pos.set(minX + x, y, minZ + z);
                    var state = chunk.getBlockState(pos);
                    add("sampled_volume");
                    if (state.isAir()) {
                        add("air");
                        if (y <= 40) add("air_at_or_below_40");
                        if (y <= 0 && y >= -48 && chunk.getBlockState(pos.above()).isAir()) {
                            var floor = chunk.getBlockState(pos.below());
                            if (floor.isSolid() && floor.getFluidState().isEmpty()) {
                                add("deep_floor_with_two_air_after_decoration");
                                if (IceCaveSpace.isWide(level, pos)) add("wide_deep_floor_after_decoration");
                            }
                        }
                        continue;
                    }
                    if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF)) add("stone_deepslate_tuff");
                    if (state.is(Blocks.ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.PACKED_ICE)) add("vanilla_ice");
                    String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    if (id.endsWith("pyrite_ore")) add("pyrite");
                    if (id.endsWith("pyrolusite_ore")) add("pyrolusite");
                    if (id.endsWith("zinc_ore")) add("zinc");
                    if (id.endsWith("ore_lead")) add("lead");
                    if (id.endsWith("ore_silver")) add("silver");
                    if (id.equals("frostedheart:ice_crystal")) add("ice_crystal");
                    if (id.equals("frostedheart:glowstone_crystal")) add("glowstone_crystal");
                    if (id.equals("frostedheart:hydrothermal_rock")) add("hydrothermal_rock");
                    if (state.getFluidState().getType().isSame(FHFluids.HOT_SPRING.get())) {
                        if (state.getFluidState().isSource()) {
                            add("spring_source");
                            sources.add(pos.immutable());
                        } else add("spring_flowing");
                    }
                }
            }
        }

        private void add(String key) { counts.merge(key, 1L, Long::sum); }

        private int sourceComponents() {
            Set<BlockPos> remaining = new HashSet<>(sources);
            int components = 0;
            while (!remaining.isEmpty()) {
                List<BlockPos> pending = new ArrayList<>();
                pending.add(remaining.iterator().next());
                remaining.remove(pending.get(0));
                for (int index = 0; index < pending.size(); index++) {
                    for (Direction direction : Direction.values()) {
                        BlockPos neighbor = pending.get(index).relative(direction);
                        if (remaining.remove(neighbor)) pending.add(neighbor);
                    }
                }
                components++;
            }
            return components;
        }
    }
}
