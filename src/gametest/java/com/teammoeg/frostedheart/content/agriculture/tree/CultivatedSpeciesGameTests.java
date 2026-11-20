package com.teammoeg.frostedheart.content.agriculture.tree;

import java.util.List;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Native species and dark-oak-only giant growth regressions. */
@GameTestHolder("frostedheart_cultivated_oak")
@PrefixGameTestTemplate(false)
public class CultivatedSpeciesGameTests {
    private static CultivatedOakProfile.Profile profile(String species) {
        return CultivatedOakProfile.get(new ResourceLocation("frostedheart", species));
    }
    private static void dark(GameTestHelper helper, BlockPos pos, int progress, int credit) {
        var level = helper.getLevel();
        level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(pos, FHBlocks.CULTIVATED_DARK_OAK.getDefaultState()
                .setValue(CultivatedOakBlock.AGE, progress >= 256 ? 4 : 0), 3);
        CompoundTag tag = new CompoundTag();tag.putShort("p", (short)progress);tag.putShort("v", (short)credit);
        level.getBlockEntity(pos).load(tag);
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void allProfilesResolveNativeShapes(GameTestHelper helper) {
        for (String name : List.of("birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry")) {
            var p = profile(name);
            helper.assertTrue(p != null, "profile loaded: " + name);
            boolean giant = name.equals("dark_oak");
            var tree = p.atHeight(helper.getLevel(), RandomSource.create(7), p.maxHeight() + (giant ? 2 : 0), giant);
            helper.assertTrue(tree != null && tree.trunkPlacer instanceof CultivatedNativeTrunkPlacer,
                    "complete native placement uses a sampled size: " + name);
            helper.assertTrue(p.giantOnly() == giant, "only dark oak is 2x2-only: " + name);
            helper.assertTrue(p.giantTrees().isEmpty() == !(name.equals("jungle") || giant),
                    "only species with a real vanilla giant variant can combine");
        }
        helper.assertTrue(profile("dark_oak").atHeight(helper.getLevel(), RandomSource.create(7), 13, false) == null,
                "dark oak never returns a single-tree configuration");
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void darkOakKeepsEarningAfterMaturity(GameTestHelper helper) {
        var level = helper.getLevel();BlockPos pos = CultivatedTestEnvironment.origin(helper);
        dark(helper, pos, 0, 0);
        level.setBlock(pos.east(), Blocks.GLOWSTONE.defaultBlockState(), 3);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.getMaxLocalRawBrightness(pos.above()) >= 9,
                "fixture light ready")).thenExecute(() -> {
            for (int i = 0; i < 16; i++) ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).advance(level, 5);
            CompoundTag first = level.getBlockEntity(pos).saveWithFullMetadata();
            helper.assertTrue(first.getShort("p") == 256 && first.getShort("v") == 128,
                    "maturity below giant yield threshold retains the sapling");
            for (int i = 0; i < 16; i++) ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).advance(level, 5);
            CompoundTag second = level.getBlockEntity(pos).saveWithFullMetadata();
            helper.assertTrue(second.getShort("p") == 256 && second.getShort("v") == 256,
                    "full progress still earns credit up to giant threshold");
            ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).advance(level, 20);
            helper.assertTrue(level.getBlockState(pos).is(FHBlocks.CULTIVATED_DARK_OAK.get()),
                    "ready isolated dark oak must not fall back to a single trunk");
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void darkOakRequiresEnoughGroupYieldAndRestoresOnObstruction(GameTestHelper helper) {
        var level = helper.getLevel();BlockPos origin = CultivatedTestEnvironment.origin(helper);
        List<BlockPos> roots = List.of(origin, origin.east(), origin.south(), origin.east().south());
        for (BlockPos pos : roots) dark(helper, pos, 16, pos.equals(origin) ? 256 : 64);
        var low = CultivatedGiantTree.find(level, origin, FHBlocks.CULTIVATED_DARK_OAK.get());
        helper.assertTrue(CultivatedGiantTree.height(low, profile("dark_oak")) == 0,
                "a complete square with insufficient average credit still waits");
        helper.assertTrue("yield".equals(CultivatedGiantTree.generate(level, low, profile("dark_oak"), RandomSource.create(7))),
                "low-yield dark oak does not generate a tiny 2x2");
        for (BlockPos pos : roots) dark(helper, pos, 16, 256);
        var ready = CultivatedGiantTree.find(level, origin, FHBlocks.CULTIVATED_DARK_OAK.get());
        level.setBlock(origin.above(), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue("space".equals(CultivatedGiantTree.generate(level, ready, profile("dark_oak"), RandomSource.create(7))),
                "native obstruction must reject giant placement");
        for (BlockPos pos : roots) helper.assertTrue(level.getBlockState(pos).is(FHBlocks.CULTIVATED_DARK_OAK.get())
                && level.getBlockEntity(pos).saveWithFullMetadata().getShort("v") == 256,
                "failed generation restores all four immature members and their credit");
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void darkOakSuccessfulGiantPreservesNativeBranches(GameTestHelper helper) {
        var level = helper.getLevel();BlockPos origin = CultivatedTestEnvironment.origin(helper);
        List<BlockPos> roots = List.of(origin, origin.east(), origin.south(), origin.east().south());
        for (BlockPos pos : roots) dark(helper, pos, 16, 256);
        var group = CultivatedGiantTree.find(level, origin, FHBlocks.CULTIVATED_DARK_OAK.get());
        helper.assertTrue(CultivatedGiantTree.generate(level, group, profile("dark_oak"), RandomSource.create(7)) == null,
                "members need no individual maturity check once average credit is sufficient");
        int logs = 0;
        for (int y = 0; y < 20; y++) for (int x = -8; x <= 9; x++) for (int z = -8; z <= 9; z++)
            if (level.getBlockState(origin.offset(x, y, z)).is(Blocks.DARK_OAK_LOG)) logs++;
        helper.assertTrue(logs >= 40 && logs <= 80, "native dark oak wood varies around the 60-log expectation: " + logs);
        helper.succeed();
    }
}
