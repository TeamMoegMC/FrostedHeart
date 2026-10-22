package com.teammoeg.frostedheart.content.world.features;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.LargeDripstoneConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real feature placement on controlled cave geometry; no separate copy of the shape algorithm. */
@GameTestHolder("frostedheart_ice_cave_body")
@PrefixGameTestTemplate(false)
public final class IceCaveIceBodyGameTests {
    private static final ResourceKey<Biome> ICE_CAVES = ResourceKey.create(Registries.BIOME,
            new ResourceLocation("the_winter_rescue", "ice_caves"));
    private static final LargeDripstoneConfiguration LARGE = new LargeDripstoneConfiguration(40,
            ConstantInt.of(4), ConstantFloat.of(1), 0.25F, ConstantFloat.of(0.7F),
            ConstantFloat.of(0.7F), ConstantFloat.of(0), 6, 0.5F);

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void pairedIceBodiesUseActualFloorAndCeilingAcrossChunkEdges(GameTestHelper helper) {
        Feature<LargeDripstoneConfiguration> feature = new IceCaveLargeIceFeature(LargeDripstoneConfiguration.CODEC);
        for (int x : new int[] {0, 15, -1, -16}) {
            Terrain terrain = new Terrain(helper, new BlockPos(x, -12, -1), 20, 12);
            helper.assertTrue(terrain.place(feature, LARGE, 12), "large ice fits a dry broad hall");
            boolean floorIce = false;
            boolean ceilingIce = false;
            for (var entry : terrain.writes.entrySet()) {
                floorIce |= entry.getKey().getY() <= -20;
                ceilingIce |= entry.getKey().getY() >= -1;
                helper.assertTrue(Math.abs(entry.getKey().getX() - x) <= 15
                        && Math.abs(entry.getKey().getZ() + 1) <= 15, "writes remain in the bounded vanilla neighborhood");
            }
            helper.assertTrue(floorIce && ceilingIce, "both ends attach to real terrain");
            helper.assertTrue(terrain.writes.keySet().stream().anyMatch(pos -> (pos.getX() >> 4) != (x >> 4)),
                    "edge candidates retain their location and can span the adjacent chunk");
        }
        Terrain narrow = new Terrain(helper, new BlockPos(0, -12, 0), 20, 1);
        helper.assertTrue(!narrow.place(feature, LARGE, 12) && narrow.writes.isEmpty(), "large bodies leave narrow passages intact");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void protectedCellsAndBiomeBoundariesRejectWholeBodies(GameTestHelper helper) {
        Feature<LargeDripstoneConfiguration> feature = new IceCaveLargeIceFeature(LargeDripstoneConfiguration.CODEC);
        for (BlockState protectedState : new BlockState[] {Blocks.WATER.defaultBlockState(),
                Blocks.OAK_PLANKS.defaultBlockState(), FHBlocks.ICE_CRYSTAL.get().defaultBlockState()}) {
            Terrain terrain = new Terrain(helper, new BlockPos(15, -12, 0), 20, 12);
            terrain.states.put(new BlockPos(15, -21, 0), protectedState);
            helper.assertTrue(!terrain.place(feature, LARGE, 12), "protected volume rejects the ice body");
            helper.assertTrue(terrain.writes.isEmpty(), "failed preflight leaves no partial ice");
        }
        Terrain mineral = new Terrain(helper, new BlockPos(15, -12, 0), 20, 12);
        BlockPos mineralPos = new BlockPos(15, -22, 0);
        mineral.states.put(mineralPos, FHBlocks.PYRITE_ORE.get().defaultBlockState());
        helper.assertTrue(mineral.place(feature, LARGE, 12) && mineral.get(mineralPos).is(FHBlocks.PYRITE_ORE.get()),
                "ore inclusions support embedded roots and remain intact");
        Terrain boundary = new Terrain(helper, new BlockPos(15, -12, 0), 20, 12);
        boundary.boundaryX = 16;
        helper.assertTrue(!boundary.place(feature, LARGE, 12) && boundary.writes.isEmpty(),
                "the actual body cannot cross into a different three-dimensional biome");
        Terrain spring = new Terrain(helper, new BlockPos(15, -12, 0), 20, 12);
        spring.states.put(new BlockPos(16, -22, 0), FHFluids.HOT_SPRING.get().getSource().defaultFluidState().createLegacyBlock());
        helper.assertTrue(!spring.place(feature, LARGE, 12) && spring.writes.isEmpty(), "spring and its dry shore survive");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void smallSpikesStaySmallAndBlobsRemainWallEmbedded(GameTestHelper helper) {
        Feature<NoneFeatureConfiguration> spike = new IceCaveSpikeFeature(NoneFeatureConfiguration.CODEC);
        Feature<NoneFeatureConfiguration> blob = new IceCaveIceBlobFeature(NoneFeatureConfiguration.CODEC);
        int successfulBlobs = 0;
        for (int seed = 0; seed < 20; seed++) {
            Terrain floor = new Terrain(helper, new BlockPos(15, -21, 0), 20, 12);
            helper.assertTrue(floor.place(spike, FeatureConfiguration.NONE, seed), "small spike fits a broad hall");
            int tip = floor.writes.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow();
            helper.assertTrue(tip >= -19 && tip <= -16, "spike body has three-to-six-block height");
            helper.assertTrue(floor.writes.values().stream().filter(state -> state.is(Blocks.SNOW_BLOCK)).count() <= 4,
                    "snow stays a small foot rather than a carpet");
            Terrain wall = new Terrain(helper, new BlockPos(11, -12, 0), 20, 12);
            for (int x = 13; x <= 25; x++) for (int y = -18; y <= -6; y++) for (int z = -6; z <= 6; z++) {
                wall.states.put(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
            }
            if (wall.place(blob, FeatureConfiguration.NONE, seed)) {
                successfulBlobs++;
                helper.assertTrue(wall.writes.keySet().stream().anyMatch(pos -> pos.getX() >= 13), "blob extends into the wall");
                helper.assertTrue(wall.writes.keySet().stream().anyMatch(pos -> pos.getX() <= 12), "blob has a visible cave-facing cap");
                helper.assertTrue(wall.writes.values().stream().filter(IceCaveIceBody::isIce).map(BlockState::getBlock).distinct().count() == 1,
                        "each blob has one coherent ice material");
            }
        }
        helper.assertTrue(successfulBlobs > 0, "wall-embedded blob candidates actually place");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 80)
    public static void spikesFindUnevenMineralFloorsWithoutGrowingLarger(GameTestHelper helper) {
        var spike = new IceCaveSpikeFeature(NoneFeatureConfiguration.CODEC);
        BlockPos floor = new BlockPos(15, -21, 0);
        BlockPos mineral = floor.below();
        for (int seed = 0; seed < 40; seed++) {
            Terrain room = new Terrain(helper, floor.above(16), 24, 12);
            room.states.put(mineral, FHBlocks.PYRITE_ORE.get().defaultBlockState());
            room.states.put(floor.north(), Blocks.STONE.defaultBlockState());
            room.states.put(floor.east().below(), Blocks.AIR.defaultBlockState());
            helper.assertTrue(room.place(spike, FeatureConfiguration.NONE, seed),
                    "a candidate above an uneven mineral floor can find and root one small spike");
            helper.assertTrue(room.get(mineral).is(FHBlocks.PYRITE_ORE.get()), "root mineral remains intact");
            int top = room.writes.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow();
            helper.assertTrue(top >= floor.getY() + 2 && top <= floor.getY() + 5, "spike stays three-to-six blocks tall");
            Terrain water = new Terrain(helper, floor, 24, 12);
            water.states.put(mineral, Blocks.WATER.defaultBlockState());
            helper.assertTrue(!IceCaveSpikeFeature.tryPlace(water.level, floor, floor, RandomSource.create(seed))
                            && water.writes.isEmpty(), "a flooded central root rejects the entire candidate");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 80)
    public static void blobsReceiveTheirOwnSupportedCrystalPockets(GameTestHelper helper) {
        var blob = new IceCaveIceBlobFeature(NoneFeatureConfiguration.CODEC);
        int glowing = 0;
        for (int seed = 0; seed < 60; seed++) {
            Terrain wall = new Terrain(helper, new BlockPos(11, -12, 0), 20, 12);
            for (int x = 13; x <= 25; x++) for (int y = -18; y <= -6; y++) for (int z = -6; z <= 6; z++) {
                wall.states.put(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
            }
            wall.place(blob, FeatureConfiguration.NONE, seed);
            for (var entry : wall.writes.entrySet()) {
                if (entry.getValue().is(FHBlocks.GLOWSTONE_CRYSTAL.get())) glowing++;
                if (entry.getValue().is(FHBlocks.ICE_CRYSTAL.get())) {
                    helper.assertTrue(entry.getValue().canSurvive(wall.level, entry.getKey()), "blob crystal has valid support");
                }
            }
        }
        helper.assertTrue(glowing > 0, "blob pass creates crystals before generic decoration, without a persistent marker");
        helper.succeed();
    }

    private static final class Terrain {
        private final GameTestHelper helper;
        private final BlockPos origin;
        private final int height;
        private final int width;
        private final Map<BlockPos, BlockState> states = new HashMap<>();
        private final Map<BlockPos, BlockState> writes = new HashMap<>();
        private final WorldGenLevel level;
        private final Holder.Reference<Biome> cave;
        private int boundaryX = Integer.MAX_VALUE;

        Terrain(GameTestHelper helper, BlockPos origin, int height, int width) {
            this.helper = helper;
            this.origin = origin;
            this.height = height;
            this.width = width;
            cave = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ICE_CAVES);
            level = (WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(), new Class<?>[] {WorldGenLevel.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getBlockState" -> get((BlockPos)args[0]);
                        case "getFluidState" -> get((BlockPos)args[0]).getFluidState();
                        case "getBiome" -> ((BlockPos)args[0]).getX() < boundaryX ? cave
                                : helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
                        case "hasChunk", "ensureCanWrite" -> true;
                        case "isOutsideBuildHeight" -> (args[0] instanceof BlockPos pos ? pos.getY() : (int)args[0]) < -64
                                || (args[0] instanceof BlockPos pos ? pos.getY() : (int)args[0]) >= 320;
                        case "setBlock" -> {
                            BlockPos pos = ((BlockPos)args[0]).immutable();
                            BlockState state = (BlockState)args[1];
                            writes.put(pos, state);
                            states.put(pos, state);
                            yield true;
                        }
                        default -> method.invoke(helper.getLevel(), args);
                    });
        }

        private BlockState get(BlockPos pos) {
            BlockState explicit = states.get(pos);
            if (explicit != null) return explicit;
            // Fixed actual cave elevations, independent of the feature's origin height and chunk.
            return pos.getY() >= -21 && pos.getY() < -21 + height
                    && Math.abs(pos.getX() - origin.getX()) <= width
                    && Math.abs(pos.getZ() - origin.getZ()) <= width
                    ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }

        private <C extends FeatureConfiguration> boolean place(Feature<C> feature, C config, long seed) {
            return feature.place(new FeaturePlaceContext<>(Optional.empty(), level,
                    helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed), origin, config));
        }
    }
}
