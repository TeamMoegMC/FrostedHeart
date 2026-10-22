package com.teammoeg.frostedheart.content.world.features;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import com.teammoeg.frostedheart.content.world.FHFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Synthetic terrain exercises the real features; it is not a natural-world sampling harness. */
@GameTestHolder("frostedheart_ice_cave")
@PrefixGameTestTemplate(false)
public final class IceCaveFeatureGameTests {
    private static final BlockPos CENTER = new BlockPos(8, -20, 8);

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void hallClassificationSurvivesOreAndIceFloors(GameTestHelper helper) {
        for (BlockState floor : List.of(FHBlocks.PYRITE_ORE.get().defaultBlockState(), Blocks.ICE.defaultBlockState())) {
            Terrain terrain = new Terrain(helper, CENTER, false);
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                terrain.states.put(CENTER.offset(dx, -1, dz), floor);
            }
            helper.assertTrue(IceCaveSpace.nearWideAir(terrain.level, CENTER.below(3)),
                    "ore or ice under the same open hall does not downgrade its spatial classification");
            helper.assertTrue(IceCaveSpace.findFloorAir(terrain.level, CENTER, 5) == null,
                    "landscape replacement search still accepts only stone/deepslate/tuff floors");
        }
        Terrain flooded = new Terrain(helper, CENTER, false);
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            flooded.states.put(CENTER.offset(dx, -1, dz), Blocks.WATER.defaultBlockState());
        }
        helper.assertTrue(!IceCaveSpace.nearWideAir(flooded.level, CENTER.below(3)),
                "water provides no sturdy floor for the cave-space probe");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void surfaceRespectsRoomAndIceSubstrate(GameTestHelper helper) {
        for (int seed = 0; seed < 8; seed++) {
            Terrain wide = new Terrain(helper, CENTER, false);
            wide.worldSeed = seed;
            helper.assertTrue(wide.place(FHFeatures.ICE_CAVE_SURFACE.get(), seed), "wide cave gains ice");
            helper.assertTrue(wide.count(FHBlocks.LARGE_ICE_CRYSTAL.get().defaultBlockState()) == 0
                            && wide.count(FHBlocks.GLOWSTONE_CRYSTAL.get().defaultBlockState()) == 0,
                    "surface pass leaves crystal placement to the later decoration pass");

            Terrain narrow = new Terrain(helper, CENTER, true);
            helper.assertTrue(narrow.place(FHFeatures.ICE_CAVE_SURFACE.get(), seed), "narrow cave gains ice");
            helper.assertTrue(narrow.count(FHBlocks.LARGE_ICE_CRYSTAL.get().defaultBlockState()) == 0,
                    "ordinary narrow passages do not gain ice crystals");
            helper.assertTrue(narrow.count(FHBlocks.GLOWSTONE_CRYSTAL.get().defaultBlockState()) == 0,
                    "ordinary narrow passages do not gain glowstone crystals");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void springHasDryRimAndShortSupportedMarkers(GameTestHelper helper) {
        for (int seed = 0; seed < 16; seed++) {
            Terrain terrain = new Terrain(helper, CENTER, false);
            helper.assertTrue(terrain.place(FHFeatures.ICE_CAVE_SPRING.get(), seed), "valid basin places a spring");
            int sources = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos ground = CENTER.offset(dx, -1, dz);
                    if (dx * dx + dz * dz <= 4) {
                        helper.assertTrue(terrain.get(ground).getFluidState().isSource()
                                        && terrain.get(ground).getFluidState().getType().isSame(FHFluids.HOT_SPRING.get()),
                                "pool is recessed one block and consists of spring sources");
                        helper.assertTrue(terrain.get(ground.below()).is(FHBlocks.HYDROTHERMAL_ROCK.get())
                                        || terrain.get(ground.below()).is(Blocks.CALCITE), "pool has a cosmetic solid bed");
                        sources++;
                    } else if (dx * dx + dz * dz <= 10) {
                        helper.assertTrue(terrain.get(ground).getFluidState().isEmpty()
                                        && terrain.get(ground).isSolid(), "rim stays dry and solid");
                    }
                }
            }
            helper.assertTrue(sources == 13, "basin keeps its configured radius-two footprint");
            helper.assertTrue(terrain.count(FHBlocks.LARGE_ICE_CRYSTAL.get().defaultBlockState()) > 0,
                    "spring approach actually gains a marker");
            terrain.checkCrystals();
            if (seed == 0) {
                Map<BlockPos, BlockState> shore = new HashMap<>();
                for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = CENTER.offset(dx, -1, dz);
                    if (dx * dx + dz * dz > 4 && dx * dx + dz * dz <= 10
                            && IceCaveSpace.isRock(terrain.get(pos))) shore.put(pos, terrain.get(pos));
                }
                for (int patch = 0; patch < 3; patch++) terrain.place(FHFeatures.ICE_CAVE_SURFACE.get(), patch);
                for (Map.Entry<BlockPos, BlockState> entry : shore.entrySet()) {
                    helper.assertTrue(terrain.get(entry.getKey()) == entry.getValue(),
                            "later repeated surface patches preserve the existing stone spring shore");
                }
                helper.assertTrue(terrain.count(FHFluids.HOT_SPRING.get().getSource().defaultFluidState().createLegacyBlock()) == 13,
                        "later surface decoration preserves all pool sources");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void invalidBasinDoesNotLeavePartialPool(GameTestHelper helper) {
        Terrain terrain = new Terrain(helper, CENTER, false);
        // Remove the entire supporting bed so the fixed candidate grid cannot move around the leak.
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            terrain.states.put(CENTER.offset(dx, -2, dz), Blocks.AIR.defaultBlockState());
        }
        helper.assertTrue(!terrain.place(FHFeatures.ICE_CAVE_SPRING.get(), 17), "broken rim rejects basin");
        helper.assertTrue(terrain.writes.isEmpty(), "rejected basin makes no partial changes");
        Terrain narrow = new Terrain(helper, CENTER, true);
        helper.assertTrue(!narrow.place(FHFeatures.ICE_CAVE_SPRING.get(), 17), "ordinary tunnel rejects basin");
        helper.assertTrue(narrow.writes.isEmpty(), "narrow tunnel remains untouched by spring feature");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void springLevelsOneBlockFloorReliefWithoutDeepFilling(GameTestHelper helper) {
        Terrain terrain = new Terrain(helper, CENTER, false);
        terrain.states.put(CENTER.east().south(), Blocks.STONE.defaultBlockState());
        terrain.states.put(CENTER.west(3).below(), Blocks.AIR.defaultBlockState());
        terrain.states.put(CENTER.north().below(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(terrain.place(FHFeatures.ICE_CAVE_SPRING.get(), 17), "one-block floor relief accepts basin");
        helper.assertTrue(terrain.get(CENTER.east().south()).isAir(), "one-block bump is cleared for passage");
        helper.assertTrue(terrain.get(CENTER.west(3).below()).is(Blocks.STONE), "one-block rim dip has solid stone footing");
        helper.assertTrue(terrain.get(CENTER.north().below()).getFluidState().isSource(), "pool occupies shallow dip above solid bed");
        helper.assertTrue(terrain.get(CENTER.west(3).below(2)).is(Blocks.STONE), "existing supporting bed remains intact");
        terrain.checkCrystals();
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void edgeAttemptsKeepWritesInTheirSourceChunk(GameTestHelper helper) {
        for (BlockPos origin : List.of(new BlockPos(0, -20, 15), new BlockPos(15, -20, 0),
                new BlockPos(-1, -20, -16), new BlockPos(-16, -20, -1))) {
            Terrain surface = new Terrain(helper, origin, false);
            helper.assertTrue(surface.place(FHFeatures.ICE_CAVE_SURFACE.get(), 23), "edge surface succeeds");
            Terrain spring = new Terrain(helper, origin, false);
            helper.assertTrue(spring.place(FHFeatures.ICE_CAVE_SPRING.get(), 23), "edge basin succeeds");
            surface.checkCrystals();
            spring.checkCrystals();
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 80)
    public static void surfaceCoversRealFloorHighWallsAndCeilingAndPreservesOre(GameTestHelper helper) {
        int[] total = new int[3];
        int[] covered = new int[3];
        for (int seed = 0; seed < 8; seed++) {
            Terrain room = new Terrain(helper, CENTER, false);
            room.worldSeed = seed * 7919L + 17;
            room.shape = pos -> pos.getX() >= 2 && pos.getX() <= 13 && pos.getZ() >= 2 && pos.getZ() <= 13
                    && pos.getY() >= -32 && pos.getY() <= 15 ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
            helper.assertTrue(room.place(FHFeatures.ICE_CAVE_SURFACE.get(), seed), "tall room gains surface ice");
            // Denominators are the original visible faces, counted separately from buried shell volume.
            for (int x = 2; x <= 13; x++) for (int z = 2; z <= 13; z++) {
                total[0]++;
                total[2]++;
                if (IceCaveTerrain.ice(room.get(new BlockPos(x, -33, z)))) covered[0]++;
                if (IceCaveTerrain.ice(room.get(new BlockPos(x, 16, z)))) covered[2]++;
            }
            for (int y = -30; y <= 13; y++) for (int along = 2; along <= 13; along++) {
                for (BlockPos wall : List.of(new BlockPos(1, y, along), new BlockPos(14, y, along),
                        new BlockPos(along, y, 1), new BlockPos(along, y, 14))) {
                    total[1]++;
                    if (IceCaveTerrain.ice(room.get(wall))) covered[1]++;
                }
            }
        }
        for (int face = 0; face < 3; face++) {
            double ratio = (double)covered[face] / total[face];
            helper.assertTrue(ratio > .60 && ratio < .98, "floor/wall/ceiling " + face + " coverage=" + ratio);
        }
        Terrain ores = new Terrain(helper, CENTER, false);
        ores.worldSeed = 4321;
        for (int x = 3; x <= 12; x++) for (int z = 3; z <= 12; z++) {
            ores.states.put(new BlockPos(x, -21, z), FHBlocks.PYRITE_ORE.get().defaultBlockState());
        }
        ores.place(FHFeatures.ICE_CAVE_SURFACE.get(), 0);
        int wrapped = 0;
        for (int x = 3; x <= 12; x++) for (int z = 3; z <= 12; z++) {
            BlockPos ore = new BlockPos(x, -21, z);
            helper.assertTrue(ores.get(ore).is(FHBlocks.PYRITE_ORE.get()), "the original ore stays in place");
            if (IceCaveTerrain.ice(ores.get(ore.above()))) wrapped++;
            helper.assertTrue(ores.get(ore.above(2)).isAir() && ores.get(ore.above(3)).isAir(),
                    "wrapping leaves two clear passage blocks");
        }
        helper.assertTrue(wrapped > 0, "exposed ore actually gains ice on its air side");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 80)
    public static void undergroundLakeIsFullySealedWithMostlyThinIce(GameTestHelper helper) {
        int thin = 0;
        int stable = 0;
        for (int seed = 0; seed < 12; seed++) {
            Terrain lake = new Terrain(helper, CENTER, false);
            lake.worldSeed = seed * 104729L + 7;
            lake.shape = pos -> pos.getY() >= -24 && pos.getY() <= -21 ? Blocks.WATER.defaultBlockState()
                    : pos.getY() >= -20 && pos.getY() <= -1 ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
            helper.assertTrue(lake.place(FHFeatures.ICE_CAVE_SURFACE.get(), seed), "underground lake freezes");
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                BlockPos surface = new BlockPos(x, -21, z);
                BlockState state = lake.get(surface);
                if (state.is(FHBlocks.THIN_ICE_BLOCK.get())) thin++;
                else if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)) stable++;
                else helper.fail("unsealed ordinary lake at " + surface);
                helper.assertTrue(lake.get(surface.below()).is(Blocks.WATER), "lake freezes only its upper surface");
            }
        }
        double ratio = (double)thin / (thin + stable);
        helper.assertTrue(ratio > .66 && ratio < .91, "many-seed lake thin-ice share=" + ratio);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 80)
    public static void trapsHaveSealedThreeToTwelveDeepWholeWetOrDryCavities(GameTestHelper helper) {
        Terrain mineralGround = new Terrain(helper, CENTER, false);
        mineralGround.states.put(CENTER.below(2), FHBlocks.PYRITE_ORE.get().defaultBlockState());
        BlockPos retainedOre = CENTER.offset(3, -2, 0);
        mineralGround.states.put(retainedOre, FHBlocks.PYRITE_ORE.get().defaultBlockState());
        helper.assertTrue(IceCaveTrapFeature.tryPlace(mineralGround.level, CENTER.below(), RandomSource.create(5)),
                "an ore inclusion inside the excavated volume does not reject a sealed cavity");
        helper.assertTrue(mineralGround.get(CENTER.below(2)).isAir() || mineralGround.get(CENTER.below(2)).is(Blocks.WATER),
                "excavation leaves an open cavity instead of a floating ore obstruction");
        helper.assertTrue(mineralGround.get(retainedOre).is(FHBlocks.PYRITE_ORE.get()),
                "ore outside the excavation remains in the retaining terrain");
        int wet = 0;
        int shortest = Integer.MAX_VALUE;
        int deepest = 0;
        BlockPos lid = CENTER.below();
        for (int seed = 0; seed < 200; seed++) {
            Terrain pit = new Terrain(helper, CENTER, false);
            helper.assertTrue(IceCaveTrapFeature.tryPlace(pit.level, lid, RandomSource.create(seed)), "solid hall floor accepts trap");
            Set<BlockPos> cavity = new HashSet<>();
            List<BlockPos> lids = new ArrayList<>();
            for (BlockPos pos : pit.writes) {
                if (pit.get(pos).is(FHBlocks.THIN_ICE_BLOCK.get())) lids.add(pos);
                else cavity.add(pos);
            }
            int depth = lid.getY() - cavity.stream().mapToInt(BlockPos::getY).min().orElseThrow();
            shortest = Math.min(shortest, depth);
            deepest = Math.max(deepest, depth);
            helper.assertTrue(depth >= 3 && depth <= 12, "trap depth counts from the first cell below its lid");
            boolean water = pit.get(cavity.iterator().next()).is(Blocks.WATER);
            if (water) wet++;
            for (BlockPos top : lids) for (int down = 1; down <= depth; down++) {
                BlockState fill = pit.get(top.below(down));
                helper.assertTrue(water ? fill.is(Blocks.WATER) && fill.getFluidState().isSource() : fill.isAir(),
                        "the complete pit shares one wet or dry outcome, including its top cells");
            }
            for (BlockPos pos : cavity) for (Direction side : Direction.values()) {
                BlockPos neighbor = pos.relative(side);
                if (cavity.contains(neighbor) || lids.contains(neighbor)) continue;
                helper.assertTrue(pit.get(neighbor).isFaceSturdy(pit.level, neighbor, side.getOpposite()),
                        "every external side and bottom cell retains solid support");
            }
        }
        helper.assertTrue(shortest == 3 && deepest == 12, "both configured depth endpoints occur");
        helper.assertTrue(wet >= 24 && wet <= 56, "whole-pit wet frequency over 200 seeds=" + wet);
        for (BlockState obstruction : List.of(Blocks.AIR.defaultBlockState(), Blocks.WATER.defaultBlockState(),
                Blocks.OAK_PLANKS.defaultBlockState())) {
            Terrain broken = new Terrain(helper, CENTER, false);
            broken.states.put(lid.below(2), obstruction);
            helper.assertTrue(!IceCaveTrapFeature.tryPlace(broken.level, lid, RandomSource.create(4))
                            && broken.writes.isEmpty(), "unsupported, flooded or constructed cavities leave no partial trap");
        }
        Terrain sideLeak = new Terrain(helper, CENTER, false);
        sideLeak.states.put(lid.east(2).below(2), Blocks.CAVE_AIR.defaultBlockState());
        helper.assertTrue(!IceCaveTrapFeature.tryPlace(sideLeak.level, lid, RandomSource.create(4))
                        && sideLeak.writes.isEmpty(), "side leak rejects the whole cavity before carving");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 80)
    public static void lateDecorationUsesThickIceWithoutPuncturingLakeLids(GameTestHelper helper) {
        Feature<NoneFeatureConfiguration> decoration = new IceCaveDecorationFeature(NoneFeatureConfiguration.CODEC);
        Terrain room = new Terrain(helper, CENTER, false);
        room.shape = pos -> {
            if (pos.getY() == -21) return FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState();
            if (pos.getY() >= -24 && pos.getY() <= -22) return Blocks.WATER.defaultBlockState();
            if (pos.getY() >= -20 && pos.getY() <= -1) return Blocks.AIR.defaultBlockState();
            if (pos.getY() >= 0 && pos.getY() <= 5) return Blocks.ICE.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        };
        helper.assertTrue(room.place(decoration, 41), "thick overhead ice gains later decoration");
        helper.assertTrue(room.count(FHBlocks.LARGE_ICE_CRYSTAL.get().defaultBlockState()) > 0,
                "real decoration places internal crystal pockets");
        room.checkCrystals();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            BlockPos lid = new BlockPos(x, -21, z);
            helper.assertTrue(room.get(lid).is(FHBlocks.THIN_ICE_BLOCK.get()) && room.get(lid.below()).is(Blocks.WATER),
                    "later crystals and icicles preserve the complete thin-ice lake");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 100)
    public static void surfaceBandsHaveNoTinyFragmentsAndCanSpanHighWalls(GameTestHelper helper) {
        int largest = 0;
        int groups = 0;
        Set<net.minecraft.world.level.block.Block> materials = new HashSet<>();
        for (int seed = 0; seed < 48; seed++) {
            Terrain room = new Terrain(helper, CENTER, false);
            room.worldSeed = seed * 7919L + 17;
            room.shape = pos -> pos.getX() >= 2 && pos.getX() <= 13 && pos.getZ() >= 2 && pos.getZ() <= 13
                    && pos.getY() >= -32 && pos.getY() <= 15 ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
            room.place(FHFeatures.ICE_CAVE_SURFACE.get(), seed);
            java.util.Set<BlockPos> remaining = new java.util.HashSet<>();
            room.states.forEach((pos, state) -> {
                if (state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                        || state.is(FHBlocks.THIN_ICE_BLOCK.get())) remaining.add(pos);
            });
            while (!remaining.isEmpty()) {
                BlockPos start = remaining.iterator().next();
                remaining.remove(start);
                List<BlockPos> group = new ArrayList<>();
                group.add(start);
                for (int i = 0; i < group.size(); i++) for (Direction side : Direction.values()) {
                    BlockPos neighbor = group.get(i).relative(side);
                    if (remaining.contains(neighbor) && room.get(neighbor).is(room.get(start).getBlock())) {
                        remaining.remove(neighbor);
                        group.add(neighbor);
                    }
                }
                helper.assertTrue(group.size() >= 4, "every material ribbon contains at least four connected exposed-shell cells");
                materials.add(room.get(start).getBlock());
                largest = Math.max(largest, group.size());
                groups++;
            }
        }
        helper.assertTrue(groups > 0 && largest >= 100, "coarse sparse bands can form hundred-block continuous regions, largest=" + largest);
        helper.assertTrue(materials.containsAll(List.of(Blocks.PACKED_ICE, Blocks.BLUE_ICE, FHBlocks.THIN_ICE_BLOCK.get())),
                "thin-ice ribbons coexist with both retained ribbon materials");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void smallCrystalsUseEveryNaturalFaceWithoutThickIceOrWideHall(GameTestHelper helper) {
        for (Direction side : Direction.values()) {
            for (BlockState base : List.of(Blocks.STONE.defaultBlockState(), Blocks.ICE.defaultBlockState(),
                    FHBlocks.PYRITE_ORE.get().defaultBlockState())) {
                Terrain room = new Terrain(helper, CENTER, true);
                room.shape = pos -> Blocks.AIR.defaultBlockState();
                BlockPos support = CENTER.relative(side);
                room.states.put(support, base);
                room.states.put(CENTER.offset(1, 1, 1),
                        FHFluids.HOT_SPRING.get().getSource().defaultFluidState().createLegacyBlock());
                helper.assertTrue(IceCaveSmallCrystalFeature.tryPlace(room.level, CENTER, RandomSource.create(12)),
                        "a natural face holds a small crystal even beside a spring");
                var crystal = room.get(CENTER);
                helper.assertTrue(crystal.is(FHBlocks.ICE_CRYSTAL.get()) && crystal.canSurvive(room.level, CENTER),
                        "the small crystal has actual support on each of six faces");
                helper.assertTrue(room.get(support) == base && room.writes.size() == 1,
                        "surface crystals retain their stone, ice or mineral support without glowstone conversion");
            }
        }
        Terrain construction = new Terrain(helper, CENTER, false);
        construction.shape = pos -> pos.equals(CENTER.below()) ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState();
        helper.assertTrue(!IceCaveSmallCrystalFeature.tryPlace(construction.level, CENTER, RandomSource.create(1)),
                "the natural decoration predicate does not attach to constructed wood");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void smallCrystalCandidatesSearchLocallyAndPlaceAtMostOne(GameTestHelper helper) {
        var small = FHFeatures.ICE_CAVE_SMALL_CRYSTAL.get();
        Terrain room = new Terrain(helper, CENTER, false);
        room.restrictToChunk = false;
        room.shape = pos -> pos.getX() == CENTER.getX() + 16 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        helper.assertTrue(room.place(small, 21), "a ray finds a real wall across the chunk edge");
        helper.assertTrue(room.writes.size() == 1 && room.get(room.writes.get(0)).is(FHBlocks.ICE_CRYSTAL.get()),
                "one glow-lichen-style candidate produces at most one small crystal");
        Terrain far = new Terrain(helper, CENTER, false);
        far.restrictToChunk = false;
        far.shape = pos -> pos.getX() >= CENTER.getX() + 22 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        helper.assertTrue(!far.place(small, 21) && far.writes.isEmpty(), "twenty-block searches do not reach distant terrain");
        helper.succeed();
    }

    private static final class Terrain {
        private final GameTestHelper helper;
        private final BlockPos origin;
        private final boolean narrow;
        private final Map<BlockPos, BlockState> states = new HashMap<>();
        private final List<BlockPos> writes = new ArrayList<>();
        private final WorldGenLevel level;
        private long worldSeed;
        private Function<BlockPos, BlockState> shape;
        private boolean restrictToChunk = true;

        private Terrain(GameTestHelper helper, BlockPos origin, boolean narrow) {
            this.helper = helper;
            this.origin = origin;
            this.narrow = narrow;
            var caveBiome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation("the_winter_rescue", "ice_caves")));
            level = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                    new Class<?>[] {WorldGenLevel.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getBlockState")) return get((BlockPos) args[0]);
                        if (method.getName().equals("getFluidState")) return get((BlockPos) args[0]).getFluidState();
                        if (method.getName().equals("getBiome")) return caveBiome;
                        if (method.getName().equals("getHeight")) return 200;
                        if (method.getName().equals("getSeed")) return worldSeed;
                        if (method.getName().equals("getMinBuildHeight")) return -64;
                        if (method.getName().equals("hasChunk") || method.getName().equals("ensureCanWrite")) return true;
                        if (method.getName().equals("setBlock")) {
                            BlockPos pos = ((BlockPos) args[0]).immutable();
                            BlockState next = (BlockState) args[1];
                            helper.assertTrue(!restrictToChunk || (pos.getX() >> 4) == (origin.getX() >> 4)
                                            && (pos.getZ() >> 4) == (origin.getZ() >> 4),
                                    "feature write remains in its generating chunk");
                            if (next.is(FHBlocks.GLOWSTONE_CRYSTAL.get())) {
                                helper.assertTrue(IceCaveTerrain.ice(get(pos)), "glowstone replaces ice only");
                            }
                            states.put(pos, next);
                            writes.add(pos);
                            return true;
                        }
                        if (method.getName().equals("isOutsideBuildHeight")) {
                            int y = args[0] instanceof BlockPos pos ? pos.getY() : (int) args[0];
                            return y < -64 || y >= 320;
                        }
                        return method.invoke(helper.getLevel(), args);
                    });
        }

        private BlockState get(BlockPos pos) {
            BlockState state = states.get(pos);
            if (state != null) return state;
            if (shape != null) return shape.apply(pos);
            if (pos.getY() >= origin.getY() && pos.getY() < origin.getY() + 5
                    && (!narrow || Math.abs(pos.getX() - origin.getX()) <= 1)) return Blocks.AIR.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        }

        private boolean place(Feature<NoneFeatureConfiguration> feature, long seed) {
            return feature.place(new FeaturePlaceContext<>(Optional.empty(), level,
                    helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed), origin,
                    FeatureConfiguration.NONE));
        }

        private int count(BlockState state) {
            return (int) states.values().stream().filter(value -> value.is(state.getBlock())).count();
        }

        private void checkCrystals() {
            for (Map.Entry<BlockPos, BlockState> entry : states.entrySet()) {
                if (!entry.getValue().is(FHBlocks.LARGE_ICE_CRYSTAL.get())) continue;
                helper.assertTrue(entry.getValue().canSurvive(level, entry.getKey()), "ice crystal has valid support");
                boolean glowstone = false;
                for (Direction side : Direction.values()) {
                    glowstone |= get(entry.getKey().relative(side)).is(FHBlocks.GLOWSTONE_CRYSTAL.get());
                }
                helper.assertTrue(glowstone, "ice crystal touches glowstone crystal");
            }
        }
    }
}
