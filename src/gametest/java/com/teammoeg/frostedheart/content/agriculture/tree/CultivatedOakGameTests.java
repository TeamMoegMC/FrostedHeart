package com.teammoeg.frostedheart.content.agriculture.tree;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.FertilizedDirt;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerGrade;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real block placement, growth ledgers, data-pack decoding, vanilla tree placement and obstacle retry. */
@GameTestHolder("frostedheart_cultivated_oak")
@PrefixGameTestTemplate(false)
public class CultivatedOakGameTests {
    private static BlockPos plant(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(9, 3, 9));
        helper.getLevel().setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos.east(5), Blocks.GLOWSTONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos, FHBlocks.CULTIVATED_OAK.getDefaultState(), 3);
        return pos;
    }
    private static CultivatedOakBlockEntity sapling(ServerLevel level, BlockPos pos) {
        return (CultivatedOakBlockEntity)level.getBlockEntity(pos);
    }
    private static BlockState fertilizer(FertilizerType type) {
        return FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.FERTILIZER, type)
                .setValue(FertilizedDirt.GRADE, FertilizerGrade.BASIC).setValue(FertilizedDirt.STORAGE, 8);
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void stagesBlockedRetryAndSave(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = plant(helper);
        helper.runAfterDelay(10, () -> {
            var profile = CultivatedOakProfile.get();
            helper.assertTrue(profile != null, "built-in data-pack profile must load");
            helper.assertTrue(profile.height(8.1) == 9 && profile.height(100) == 12, "data-driven ceil and height clamp");
            for (int stage = 1; stage <= 4; stage++) {
                for (int i = 0; i < profile.progressPerStage(); i++) sapling(level, pos).advance(level, 20);
                helper.assertTrue(level.getBlockState(pos).getValue(CultivatedOakBlock.AGE) == stage, "visible stage " + stage);
            }
            var saved = sapling(level, pos).saveWithFullMetadata();
            helper.assertTrue(saved.getInt("TargetHeight") == 6, "ideal unamended target is six logs");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(pos, FHBlocks.CULTIVATED_OAK.getDefaultState().setValue(CultivatedOakBlock.AGE, 4), 3);
            sapling(level, pos).load(saved);
            level.setBlock(pos.below(), fertilizer(FertilizerType.INCREASING), 3);
            level.setBlock(pos.above(5), Blocks.STONE.defaultBlockState(), 3);
            for (int i = 0; i < 5; i++) sapling(level, pos).advance(level, 20);
            helper.assertTrue(level.getBlockState(pos).is(FHBlocks.CULTIVATED_OAK.get()), "obstacle retains sapling");
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 8, "retry spends no fertilizer");
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getInt("TargetHeight") == 6, "late fertilizer cannot change target");
            level.setBlock(pos.above(5), Blocks.AIR.defaultBlockState(), 3);
            sapling(level, pos).advance(level, 20);
            int count = 0;
            for (int i = 0; i < 16; i++) if (level.getBlockState(pos.above(i)).is(Blocks.OAK_LOG)) count++;
            helper.assertTrue(count == 6, "vanilla generator must place six trunk logs, actual=" + count);
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 8, "generation preserves paid soil");
            helper.assertTrue(Blocks.OAK_SAPLING.defaultBlockState().hasProperty(net.minecraft.world.level.block.SaplingBlock.STAGE),
                    "vanilla sapling retains original stage property");
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void temperatureAndProtection(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.runAfterDelay(10, () -> {
            level.setBlock(pos.below(), fertilizer(FertilizerType.INCREASING), 3);
            sapling(level, pos).advance(level, 0);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getDouble("Progress") == 0, "cold stops growth");
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 8, "cold spends nothing");
            level.setBlock(pos.below(), fertilizer(FertilizerType.PRESERVED), 3);
            sapling(level, pos).advance(level, 0);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getDouble("Progress") > 0, "preservation extends cold range");
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 7, "earned growth pays one nutrient");
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void increasingYieldIsEarned(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.runAfterDelay(10, () -> {
            for (int i = 0; i < 16; i++) {
                level.setBlock(pos.below(), fertilizer(FertilizerType.INCREASING), 3);
                sapling(level, pos).advance(level, 20);
            }
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getInt("TargetHeight") == 12, "full basic yield fertilizer doubles trunk height");
            sapling(level, pos).advance(level, 20);
            int count = 0;
            for (int i = 0; i < 16; i++) if (level.getBlockState(pos.above(i)).is(Blocks.OAK_LOG)) count++;
            helper.assertTrue(count == 12, "fertilized tree places twelve logs, actual=" + count);
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void acceleratedAndMarginalClimate(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.runAfterDelay(10, () -> {
            level.setBlock(pos.below(), fertilizer(FertilizerType.ACCELERATED), 3);
            for (int i = 0; i < 8; i++) sapling(level, pos).advance(level, 20);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getInt("TargetHeight") == 6, "acceleration halves growth opportunities without multiplying yield");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(pos, FHBlocks.CULTIVATED_OAK.getDefaultState(), 3);
            level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
            for (int i = 0; i < 64; i++) sapling(level, pos).advance(level, 5);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getInt("TargetHeight") == 4, "marginal temperature reduces yield to lower height bound");
            helper.succeed();
        });
    }
}
