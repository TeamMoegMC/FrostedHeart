package com.teammoeg.frostedheart.content.agriculture.tree;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.FertilizedDirt;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerGrade;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual placement checks for a legacy-equivalent four-unit fertilizer settlement per tree. */
@GameTestHolder("frostedheart_cultivated_oak")
@PrefixGameTestTemplate(false)
public class CultivatedFertilizerGameTests {
    private static BlockPos prepare(GameTestHelper helper) {
        BlockPos pos = CultivatedTestEnvironment.origin(helper);
        helper.getLevel().setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos.east(3).below(), Blocks.GLOWSTONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos, FHBlocks.CULTIVATED_SPRUCE.getDefaultState(), 3);
        return pos;
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void allFertilizerTypesAndGradesPayFourOnActualSuccess(GameTestHelper helper) {
        var level = helper.getLevel();BlockPos pos = prepare(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.getMaxLocalRawBrightness(pos.above()) >= 9,
                "fixture light ready")).thenExecute(() -> {
            for (FertilizerType type : FertilizerType.values()) for (FertilizerGrade grade : FertilizerGrade.values()) {
                for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) for (int y = 0; y < 34; y++)
                    level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                var soil = FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.FERTILIZER, type)
                        .setValue(FertilizedDirt.GRADE, grade).setValue(FertilizedDirt.STORAGE, 8);
                level.setBlock(pos.below(), soil, 3);
                level.setBlock(pos, FHBlocks.CULTIVATED_SPRUCE.getDefaultState(), 3);
                int ticks = type == FertilizerType.ACCELERATED ? (int)(8 * grade.growSpeed) : 16;
                for (int i = 0; i < ticks; i++) ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).advance(level, 20);
                helper.assertTrue(level.getBlockState(pos).getValue(CultivatedOakBlock.AGE) == 4
                        && level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 8,
                        "mature growth spends nothing before placement: " + type + "/" + grade);
                ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).advance(level, 20, RandomSource.create(7));
                helper.assertTrue(level.getBlockState(pos).is(Blocks.SPRUCE_LOG)
                        && level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 4,
                        "successful single tree pays exactly four independent of growth speed: " + type + "/" + grade);
            }
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void failuresAreFreeAndSmallStorageMatchesLegacyExhaustion(GameTestHelper helper) {
        var level = helper.getLevel();BlockPos pos = prepare(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.getMaxLocalRawBrightness(pos.above()) >= 9,
                "fixture light ready")).thenExecute(() -> {
            var soil = FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.STORAGE, 3);
            level.setBlock(pos.below(), soil, 3);
            level.setBlock(pos, FHBlocks.CULTIVATED_SPRUCE.getDefaultState().setValue(CultivatedOakBlock.AGE, 4), 3);
            CompoundTag tag = new CompoundTag();tag.putShort("p", (short)256);tag.putShort("v", (short)256);
            level.getBlockEntity(pos).load(tag);
            level.setBlock(pos.above(2), Blocks.STONE.defaultBlockState(), 3);
            for (int i = 0; i < 5; i++) ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).advance(level, 20);
            helper.assertTrue(level.getBlockState(pos.below()).equals(soil), "blocked attempts never spend fertilizer");
            level.setBlock(pos.above(2), Blocks.AIR.defaultBlockState(), 3);
            ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).advance(level, 20, RandomSource.create(7));
            helper.assertTrue(level.getBlockState(pos).is(Blocks.SPRUCE_LOG) && level.getBlockState(pos.below()).is(Blocks.DIRT),
                    "three remaining units are exhausted on success just as legacy cost-four logic did");
            helper.succeed();
        });
    }
}
