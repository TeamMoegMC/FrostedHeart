package com.teammoeg.frostedheart.content.world.features;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import com.teammoeg.frostedheart.content.world.FHFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
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
        int wideCrystals = 0;
        for (int seed = 0; seed < 32; seed++) {
            Terrain wide = new Terrain(helper, CENTER, false);
            helper.assertTrue(wide.place(FHFeatures.ICE_CAVE_SURFACE.get(), seed), "wide cave gains ice");
            wideCrystals += wide.count(FHBlocks.ICE_CRYSTAL.get().defaultBlockState());
            wide.checkCrystals();

            Terrain narrow = new Terrain(helper, CENTER, true);
            helper.assertTrue(narrow.place(FHFeatures.ICE_CAVE_SURFACE.get(), seed), "narrow cave gains ice");
            helper.assertTrue(narrow.count(FHBlocks.ICE_CRYSTAL.get().defaultBlockState()) == 0,
                    "ordinary narrow passages do not gain ice crystals");
            helper.assertTrue(narrow.count(FHBlocks.GLOWSTONE_CRYSTAL.get().defaultBlockState()) == 0,
                    "ordinary narrow passages do not gain glowstone crystals");
        }
        helper.assertTrue(wideCrystals > 0, "wide halls actually generate supported crystal clusters");
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
            helper.assertTrue(terrain.count(FHBlocks.ICE_CRYSTAL.get().defaultBlockState()) > 0,
                    "spring approach actually gains a marker");
            terrain.checkCrystals();
            if (seed == 0) {
                Map<BlockPos, BlockState> shore = new HashMap<>();
                for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = CENTER.offset(dx, -1, dz);
                    if (dx * dx + dz * dz > 4 && dx * dx + dz * dz <= 10
                            && IceCaveSpace.isRock(terrain.get(pos))) shore.put(pos, terrain.get(pos));
                }
                for (int patch = 0; patch < 64; patch++) terrain.place(FHFeatures.ICE_CAVE_SURFACE.get(), patch);
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

    private static final class Terrain {
        private final GameTestHelper helper;
        private final BlockPos origin;
        private final boolean narrow;
        private final Map<BlockPos, BlockState> states = new HashMap<>();
        private final List<BlockPos> writes = new ArrayList<>();
        private final WorldGenLevel level;

        private Terrain(GameTestHelper helper, BlockPos origin, boolean narrow) {
            this.helper = helper;
            this.origin = origin;
            this.narrow = narrow;
            level = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                    new Class<?>[] {WorldGenLevel.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getBlockState")) return get((BlockPos) args[0]);
                        if (method.getName().equals("getFluidState")) return get((BlockPos) args[0]).getFluidState();
                        if (method.getName().equals("setBlock")) {
                            BlockPos pos = ((BlockPos) args[0]).immutable();
                            BlockState next = (BlockState) args[1];
                            helper.assertTrue((pos.getX() >> 4) == (origin.getX() >> 4)
                                            && (pos.getZ() >> 4) == (origin.getZ() >> 4),
                                    "feature write remains in its generating chunk");
                            if (next.is(FHBlocks.GLOWSTONE_CRYSTAL.get())) {
                                helper.assertTrue(get(pos).is(Blocks.ICE), "glowstone replaces ice only");
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
                if (!entry.getValue().is(FHBlocks.ICE_CRYSTAL.get())) continue;
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
