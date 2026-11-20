package com.teammoeg.frostedheart.content.agriculture.tree;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Native trunk sampling uses a live in-memory scene, retaining log collisions and axes. */
@GameTestHolder("frostedheart_cultivated_oak")
@PrefixGameTestTemplate(false)
public class CultivatedNativeShapeGameTests {
    static final class Scene implements LevelSimulatedReader {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockState state(BlockPos pos) { return blocks.getOrDefault(pos, pos.getY() < 0
                ? Blocks.DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState()); }
        void set(BlockPos pos, BlockState state) { blocks.put(pos.immutable(), state); }
        public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> test) { return test.test(state(pos)); }
        public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> test) { return test.test(state(pos).getFluidState()); }
        public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) { return Optional.empty(); }
        public BlockPos getHeightmapPos(Heightmap.Types type, BlockPos pos) { return new BlockPos(pos.getX(), 0, pos.getZ()); }
        LevelSimulatedReader reader() {
            return (LevelSimulatedReader)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{net.minecraft.world.level.LevelReader.class, LevelSimulatedReader.class}, (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "getBlockState" -> state((BlockPos)args[0]);
                            case "getFluidState" -> state((BlockPos)args[0]).getFluidState();
                            case "isStateAtPosition" -> isStateAtPosition((BlockPos)args[0], (Predicate<BlockState>)args[1]);
                            case "isFluidAtPosition" -> isFluidAtPosition((BlockPos)args[0], (Predicate<FluidState>)args[1]);
                            case "getBlockEntity" -> Optional.empty();
                            case "getHeightmapPos" -> getHeightmapPos((Heightmap.Types)args[0], (BlockPos)args[1]);
                            default -> throw new UnsupportedOperationException("Unexpected native scene read: " + method);
                        };
                    });
        }
        int logs() { return (int)blocks.values().stream().filter(s -> s.is(BlockTags.LOGS)).count(); }
    }
    static TreeConfiguration nativeTree(GameTestHelper helper, String name) {
        return (TreeConfiguration)helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("minecraft", name)).config();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void nativeYieldReferenceSamples(GameTestHelper helper) {
        StringBuilder csv = new StringBuilder("shape,height,mean,min,max\n");
        for (String name : List.of("oak", "fancy_oak", "spruce", "mega_spruce", "birch", "jungle_tree_no_vine",
                "mega_jungle_tree", "acacia", "dark_oak", "mangrove", "tall_mangrove", "cherry")) {
            var tree = nativeTree(helper, name);
            for (int height : java.util.stream.IntStream.concat(java.util.stream.IntStream.rangeClosed(1, 32),
                    java.util.stream.IntStream.of(48, 64, 96)).toArray()) {
                int sum = 0, min = Integer.MAX_VALUE, max = 0;
                for (int seed = 0; seed < 128; seed++) {
                    var scene = new Scene();
                    tree.trunkPlacer.placeTrunk(scene.reader(), scene::set, RandomSource.create(seed * 7919L + 17), height,
                            BlockPos.ZERO, tree);
                    int logs = scene.logs();sum += logs;min = Math.min(min, logs);max = Math.max(max, logs);
                }
                csv.append(name).append(',').append(height).append(',').append(sum / 128.0).append(',')
                        .append(min).append(',').append(max).append('\n');
            }
        }
        try { java.nio.file.Files.writeString(java.nio.file.Path.of("native-tree-yield-samples.csv"), csv); }
        catch (java.io.IOException ex) { throw new IllegalStateException("Cannot write native yield samples", ex); }
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void nativeAdapterPreservesEveryWriteAndAttachment(GameTestHelper helper) {
        for (String name : List.of("oak", "fancy_oak", "spruce", "mega_spruce", "mega_pine", "birch",
                "jungle_tree_no_vine", "mega_jungle_tree", "acacia", "dark_oak", "mangrove", "tall_mangrove", "cherry")) {
            var tree = nativeTree(helper, name);
            for (int height : new int[]{1, 4, 8, 12, 20, 32, 64}) for (int seed = 0; seed < 32; seed++) {
                var reference = new Scene();var actual = new Scene();
                var expected = tree.trunkPlacer.placeTrunk(reference.reader(), reference::set,
                        RandomSource.create(seed), height, BlockPos.ZERO, tree);
                var adapter = new CultivatedNativeTrunkPlacer(tree.trunkPlacer, height);
                var attachments = adapter.placeTrunk(actual.reader(), actual::set,
                        RandomSource.create(seed), height, BlockPos.ZERO, tree);
                helper.assertTrue(reference.blocks.equals(actual.blocks),
                        "native logs, forks, axes and soil writes must match exactly: " + name + "/" + height + "/" + seed);
                helper.assertTrue(expected.size() == attachments.size(), "no native crown may be discarded: " + name);
                for (int j = 0; j < expected.size(); j++) {
                    var a = expected.get(j);var b = attachments.get(j);
                    helper.assertTrue(a.pos().equals(b.pos()) && a.radiusOffset() == b.radiusOffset()
                            && a.doubleTrunk() == b.doubleTrunk(), "native crown origin/size retained: " + name);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void wholeNativeTreesTrackExpectedWoodAcrossSeeds(GameTestHelper helper) {
        var rows = new StringBuilder("species,giant,budget,expected,mean,min,max\n");
        var errors = new ArrayList<String>();
        for (String species : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry")) {
            var profile = CultivatedOakProfile.get(new ResourceLocation("frostedheart", species));
            for (boolean giant : new boolean[]{false, true}) {
                if (giant && profile.giantTrees().isEmpty() || !giant && profile.giantOnly()) continue;
                for (int budget : giant ? new int[]{3, 13, 18, 25, 64} : new int[]{1, 4, 6, 8, 12, 16, 24, 64, 256}) {
                    if (!giant && budget < profile.minimumBudget()) {
                        helper.assertTrue(profile.atHeight(helper.getLevel(), RandomSource.create(1), budget, false) == null,
                                "insufficient native fork budget waits rather than using another trunk type");
                        continue;
                    }
                    long sum = 0;int min = Integer.MAX_VALUE, max = 0;
                    for (int seed = 0; seed < 512; seed++) {
                        var random = RandomSource.create(1000003L * seed + 41);
                        var tree = profile.atHeight(helper.getLevel(), random, budget, giant);
                        int height = tree.trunkPlacer.getTreeHeight(random);
                        // Match TreeFeature's RNG order, including canopy and root-origin sampling.
                        int foliage = tree.foliagePlacer.foliageHeight(random, height, tree);
                        tree.foliagePlacer.foliageRadius(random, height - foliage);
                        BlockPos trunkOrigin = tree.rootPlacer.map(r -> r.getTrunkOrigin(BlockPos.ZERO, random))
                                .orElse(BlockPos.ZERO);
                        var scene = new Scene();
                        tree.trunkPlacer.placeTrunk(scene.reader(), scene::set, random, height, trunkOrigin, tree);
                        int logs = scene.logs();sum += logs;min = Math.min(min, logs);max = Math.max(max, logs);
                    }
                    double mean = sum / 512.0, target = (giant ? 4 : 1) * budget;
                    rows.append(species).append(',').append(giant).append(',').append(budget).append(',')
                            .append(target).append(',').append(mean).append(',').append(min).append(',').append(max).append('\n');
                    if (Math.abs(mean - target) > Math.max(.5, target * .08))
                        errors.add(species + "/" + giant + "/" + budget + ": expected " + target + " mean " + mean);
                }
            }
        }
        try { java.nio.file.Files.writeString(java.nio.file.Path.of("native-tree-yield-validation.csv"), rows); }
        catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        helper.assertTrue(errors.isEmpty(), "native wood expectation calibration: " + errors);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void nativeCanopiesRootsDecoratorsAndBeeVariantsSurvive(GameTestHelper helper) {
        var sources = new IdentityHashMap<net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer, TreeConfiguration>();
        for (String name : List.of("oak", "fancy_oak", "oak_bees_005", "fancy_oak_bees_005", "spruce",
                "mega_spruce", "mega_pine", "birch", "birch_bees_005", "jungle_tree_no_vine", "mega_jungle_tree",
                "acacia", "dark_oak", "mangrove", "tall_mangrove", "cherry", "cherry_bees_005")) {
            var source = nativeTree(helper, name);sources.put(source.trunkPlacer, source);
        }
        for (String species : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry")) {
            var profile = CultivatedOakProfile.get(new ResourceLocation("frostedheart", species));
            for (boolean giant : new boolean[]{false, true}) {
                if (giant && profile.giantTrees().isEmpty() || !giant && profile.giantOnly()) continue;
                for (int seed = 0; seed < 64; seed++) {
                    var tree = profile.atHeight(helper.getLevel(), RandomSource.create(seed), 24, giant);
                    var nativeTrunk = ((CultivatedNativeTrunkPlacer)tree.trunkPlacer).nativeTrunk();
                    var source = sources.get(nativeTrunk);
                    helper.assertTrue(source != null && tree.foliagePlacer == source.foliagePlacer
                            && tree.rootPlacer.equals(source.rootPlacer) && tree.decorators.equals(source.decorators)
                            && tree.minimumSize == source.minimumSize && tree.trunkProvider == source.trunkProvider,
                            "native canopy/root/decorator/configuration retained: " + species + "/" + giant);
                }
            }
        }
        BlockPos origin = CultivatedTestEnvironment.origin(helper);
        helper.getLevel().setBlock(origin.east().below(), Blocks.DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(origin.east(), Blocks.DANDELION.defaultBlockState(), 3);
        for (String species : List.of("oak", "birch", "cherry")) {
            var profile = CultivatedOakProfile.get(new ResourceLocation("frostedheart", species));
            var tree = profile.atHeight(helper.getLevel(), RandomSource.create(17), 20, false, origin);
            helper.assertTrue(tree.decorators.stream().anyMatch(d -> d instanceof
                    net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator),
                    "flowers enable the species' vanilla bee variant: " + species);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void actualTreeMayGrowBeyondVanillaMaximum(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = CultivatedTestEnvironment.origin(helper).below(96);
        level.setBlock(origin.below(), Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(origin, com.teammoeg.frostedheart.bootstrap.common.FHBlocks.CULTIVATED_SPRUCE.getDefaultState()
                .setValue(CultivatedOakBlock.AGE, 4), 3);
        var tag = new net.minecraft.nbt.CompoundTag();tag.putShort("p", (short)256);tag.putShort("v", (short)1072);
        level.getBlockEntity(origin).load(tag);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.getMaxLocalRawBrightness(origin.above()) >= 9,
                "tall-tree scene light ready")).thenExecute(() -> {
            ((CultivatedOakBlockEntity)level.getBlockEntity(origin)).advance(level, 20, RandomSource.create(7));
            int logs = 0;
            for (int y = 0; y < 96; y++) if (level.getBlockState(origin.above(y)).is(Blocks.SPRUCE_LOG)) logs++;
            helper.assertTrue(logs == 64, "actual native spruce trunk reaches 64 blocks without a vanilla height cap: " + logs);
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void cherryWaitsForCompleteNativeForkBudget(GameTestHelper helper) {
        var level = helper.getLevel();BlockPos origin = CultivatedTestEnvironment.origin(helper);
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
            level.setBlock(origin.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(origin, com.teammoeg.frostedheart.bootstrap.common.FHBlocks.CULTIVATED_CHERRY.getDefaultState()
                .setValue(CultivatedOakBlock.AGE, 4), 3);
        var tag = new net.minecraft.nbt.CompoundTag();tag.putShort("p", (short)256);tag.putShort("v", (short)128);
        level.getBlockEntity(origin).load(tag);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.getMaxLocalRawBrightness(origin.above()) >= 9,
                "cherry scene light ready")).thenExecute(() -> {
            for (int i = 0; i < 3; i++) {
                ((CultivatedOakBlockEntity)level.getBlockEntity(origin)).advance(level, 20, RandomSource.create(7));
                helper.assertTrue(level.getBlockEntity(origin) instanceof CultivatedOakBlockEntity
                        && level.getBlockEntity(origin).saveWithFullMetadata().getShort("v") == 144 + 16 * i,
                        "full-progress cherry retains stage and earns credit until a full native fork fits");
            }
            ((CultivatedOakBlockEntity)level.getBlockEntity(origin)).advance(level, 20, RandomSource.create(7));
            helper.assertTrue(level.getBlockState(origin).is(Blocks.CHERRY_LOG),
                    "eight-log target releases a full native cherry instead of a straight replacement");
            helper.succeed();
        });
    }

}
