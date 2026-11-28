package com.teammoeg.frostedheart.content.agriculture.tree;

import net.minecraft.world.level.levelgen.feature.foliageplacers.MegaPineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;

import java.util.List;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.FertilizedDirt;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real-world regressions for native spruce shapes, giant selection, rollback and yield. */
@GameTestHolder("frostedheart_cultivated_oak")
@PrefixGameTestTemplate(false)
public class CultivatedSpruceGameTests {
    private static CultivatedOakProfile.Profile profile() {
        return CultivatedOakProfile.get(new ResourceLocation("frostedheart:spruce"));
    }
    private static void plant(ServerLevel level, BlockPos pos, int progress, int credit) {
        level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(pos, FHBlocks.CULTIVATED_SPRUCE.getDefaultState()
                .setValue(CultivatedOakBlock.AGE, progress >= 256 ? 4 : 0), 3);
        CompoundTag tag = new CompoundTag();
        tag.putShort("p", (short)progress); tag.putShort("v", (short)credit);
        ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).load(tag);
    }
    private static List<BlockPos> square(BlockPos origin) {
        return List.of(origin, origin.east(), origin.south(), origin.east().south());
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void profilesAndVanillaShapes(GameTestHelper helper) {
        var p = profile();
        helper.assertTrue(p != null && !p.branches() && p.giantTrees().size() == 2,
                "spruce exposes its own two vanilla giant variants, not oak branches");
        helper.assertTrue(CultivatedOakProfile.get().giantTrees().isEmpty(), "oak has no 2x2 giant variant");
        helper.assertTrue(p.atHeight(helper.getLevel(), RandomSource.create(1), 16, false).trunkPlacer
                instanceof StraightTrunkPlacer, "single spruce trunk");
        var giant = p.atHeight(helper.getLevel(), RandomSource.create(1), 16, true);
        helper.assertTrue(((CultivatedNativeTrunkPlacer)giant.trunkPlacer).nativeTrunk() instanceof GiantTrunkPlacer
                && giant.foliagePlacer instanceof MegaPineFoliagePlacer,
                "giant preserves native conifer canopy and 2x2 trunk");
        helper.assertTrue(CultivatedGiantTree.height(new CultivatedGiantTree.Group(BlockPos.ZERO,
                square(BlockPos.ZERO), 0), p) == 0, "giant bonus never creates yield from zero credit");
        helper.assertTrue(p.earnedHeight(1) == 13 && p.earnedHeight(304.0 / 256) == 16
                && p.earnedHeight(352.0 / 256) == 19 && p.earnedHeight(416.0 / 256) == 23,
                "spruce height references and proportional fertilizer credit");
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void bestSquareUsesImmatureCreditAndRejectsMixedSpecies(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos center = CultivatedTestEnvironment.origin(helper);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            plant(level, center.offset(x, 0, z), 16, 16);
        plant(level, center, 256, 256);
        plant(level, center.east().south(), 16, 512);
        var group = CultivatedGiantTree.find(level, center, FHBlocks.CULTIVATED_SPRUCE.get());
        helper.assertTrue(group != null && group.origin().equals(center) && group.credit() == 800,
                "choose highest total already-earned yield despite immature members");
        helper.assertTrue(CultivatedGiantTree.height(group, profile()) == profile().earnedHeight(800.0 / 1024) + CultivatedGiantTree.PER_SAPLING_LOG_BONUS,
                "average credit normalized to full lifecycle, not immature progress");
        level.setBlock(center.east().south(), FHBlocks.CULTIVATED_OAK.getDefaultState(), 3);
        var other = CultivatedGiantTree.find(level, center, FHBlocks.CULTIVATED_SPRUCE.get());
        helper.assertTrue(other != null && !other.origin().equals(center), "mixed oak/spruce square excluded");
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void everyTriggerCornerAndUnloadedCandidate(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos origin = CultivatedTestEnvironment.origin(helper);
        for (BlockPos pos : square(origin)) plant(level, pos, 16, 64);
        for (BlockPos corner : square(origin)) {
            var group = CultivatedGiantTree.find(level, corner, FHBlocks.CULTIVATED_SPRUCE.get());
            helper.assertTrue(group != null && group.origin().equals(origin), "all four trigger corners find the same square");
        }
        BlockPos unloaded = origin.offset(100000, 0, 100000);
        helper.assertTrue(!level.hasChunkAt(unloaded), "remote fixture starts unloaded");
        helper.assertTrue(CultivatedGiantTree.find(level, unloaded, FHBlocks.CULTIVATED_SPRUCE.get()) == null
                && !level.hasChunkAt(unloaded), "scanning unloaded candidates does not create chunks");
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void giantPlacementRestoresFourLedgersOnFailure(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos origin = CultivatedTestEnvironment.origin(helper);
        for (BlockPos pos : square(origin)) plant(level, pos, 256, 256);
        var soil = FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.FERTILIZER, FertilizerType.INCREASING)
                .setValue(FertilizedDirt.STORAGE, 8);
        for (BlockPos pos : square(origin)) level.setBlock(pos.below(), soil, 3);
        var group = CultivatedGiantTree.find(level, origin, FHBlocks.CULTIVATED_SPRUCE.get());
        level.setBlock(origin.above(1), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue("space".equals(CultivatedGiantTree.generate(level, group, profile(), RandomSource.create(7))),
                "blocked giant must wait");
        for (BlockPos pos : square(origin)) {
            helper.assertTrue(level.getBlockState(pos).is(FHBlocks.CULTIVATED_SPRUCE.get())
                    && level.getBlockState(pos.below()).equals(soil), "restore all four saplings and paid roots");
            CompoundTag tag = level.getBlockEntity(pos).saveWithFullMetadata();
            helper.assertTrue(tag.getShort("p") == 256 && tag.getShort("v") == 256, "restore all four ledgers");
        }
        level.setBlock(origin.above(1), Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(CultivatedGiantTree.generate(level, group, profile(), RandomSource.create(7)) == null,
                "clear giant space generates native spruce");
        for (BlockPos pos : square(origin)) helper.assertTrue(level.getBlockState(pos).is(Blocks.SPRUCE_LOG)
                && level.getBlockState(pos.below()).equals(pos.equals(origin) ? soil.setValue(FertilizedDirt.STORAGE, 4) : soil),
                "giant charges four units once at its origin, not four units per member");
        int logs = 0;
        for (BlockPos root : square(origin)) for (int y = 0; y < 24; y++)
            if (level.getBlockState(root.above(y)).is(Blocks.SPRUCE_LOG)) logs++;
        helper.assertTrue(logs >= 53 && logs <= 65 && logs % 4 == 1,
                "native giant spruce retains its shortened three columns near 60 expected logs: " + logs);
        int highest = 0;
        for (int y = 0; y < 24; y++) if (level.getBlockState(origin.above(y)).is(Blocks.SPRUCE_LOG)) highest = y;
        helper.assertTrue(!level.getBlockState(origin.east().above(highest)).is(Blocks.SPRUCE_LOG)
                && !level.getBlockState(origin.south().above(highest)).is(Blocks.SPRUCE_LOG),
                "giant native taper is not filled to a flat 2x2 top");
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void isolatedMatureSpruceFallsBackToSingle(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = CultivatedTestEnvironment.origin(helper);
        plant(level, pos, 256, 256);
        level.setBlock(pos.east().above(), Blocks.GLOWSTONE.defaultBlockState(), 3);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.getMaxLocalRawBrightness(pos.above()) >= 9,
                "fixture light ready")).thenExecute(() -> {
            helper.assertTrue(CultivatedGiantTree.find(level, pos, FHBlocks.CULTIVATED_SPRUCE.get()) == null, "no 2x2");
            // Remove the fixture obstacle from the actual vanilla clearance volume after lighting propagated.
            level.setBlock(pos.east().above(), Blocks.AIR.defaultBlockState(), 2);
            ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).advance(level, 20, RandomSource.create(7));
            helper.assertTrue(level.getBlockState(pos).is(Blocks.SPRUCE_LOG), "eligible height still permits single fallback");
            for (int y = 0; y < 13; y++) helper.assertTrue(level.getBlockState(pos.above(y)).is(Blocks.SPRUCE_LOG),
                    "single uses spruce log and unbranched native conifer shape");
            helper.succeed();
        });
    }
}
