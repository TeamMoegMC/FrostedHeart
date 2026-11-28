package com.teammoeg.frostedheart.content.agriculture.tree;

import java.util.List;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.FertilizedDirt;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerGrade;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Complete growth-to-placement regressions for native shapes and accelerated nonideal yield credit. */
@GameTestHolder("frostedheart_cultivated_oak")
@PrefixGameTestTemplate(false)
public class CultivatedGrowthGameTests {
    private static BlockPos ground(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = CultivatedTestEnvironment.origin(helper);
        for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++)
            level.setBlock(pos.offset(x - 9, -1, z - 9), Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(pos.east(4).below(), Blocks.GLOWSTONE.defaultBlockState(), 3);
        return pos;
    }
    private static void grow(GameTestHelper helper, CultivatedOakBlock block, Block logs, boolean giant) {
        var level = helper.getLevel();BlockPos pos = ground(helper);
        var profile = CultivatedOakProfile.get(block.profileId());
        if (giant) {
            for (BlockPos member : List.of(pos.east(), pos.south(), pos.east().south())) {
                level.setBlock(member, block.defaultBlockState(), 3);
                CompoundTag tag = new CompoundTag();tag.putShort("p", (short)16);tag.putShort("v", (short)256);
                level.getBlockEntity(member).load(tag);
            }
        }
        level.setBlock(pos, block.defaultBlockState(), 3);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.getMaxLocalRawBrightness(pos.above()) >= 9,
                "fixture light ready")).thenExecute(() -> {
            for (int i = 0; i < 16; i++) ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).advance(level, 20);
            helper.assertTrue(level.getBlockState(pos).getValue(CultivatedOakBlock.AGE) == 4,
                    "sixteen ideal random ticks reach visible maturity");
            // Rooted native shapes may legitimately reject one seed; subsequent random ticks retry.
            for (int attempt = 0; attempt < 12 && level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity sapling; attempt++)
                sapling.advance(level, 20, RandomSource.create(7 + attempt * 31));
            helper.assertTrue(!(level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity),
                    "native growth must actually place a tree: " + block.profileId());
            int count = 0;
            for (int y = 0; y < 36; y++) for (int x = -8; x <= 9; x++) for (int z = -8; z <= 9; z++)
                if (level.getBlockState(pos.offset(x, y, z)).is(logs)) count++;
            int expected = giant ? 4 * profile.maxHeight() + 8 : profile.maxHeight();
            helper.assertTrue(count >= expected * .5 && count <= expected * 1.6, "complete native shape wood near expectation for " + block.profileId() + ": expected " + expected + " actual " + count);
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void birchGrowth(GameTestHelper h) { grow(h, FHBlocks.CULTIVATED_BIRCH.get(), Blocks.BIRCH_LOG, false); }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void jungleGrowth(GameTestHelper h) { grow(h, FHBlocks.CULTIVATED_JUNGLE.get(), Blocks.JUNGLE_LOG, false); }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void acaciaGrowth(GameTestHelper h) { grow(h, FHBlocks.CULTIVATED_ACACIA.get(), Blocks.ACACIA_LOG, false); }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void mangroveGrowth(GameTestHelper h) { grow(h, FHBlocks.CULTIVATED_MANGROVE.get(), Blocks.MANGROVE_LOG, false); }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void cherryGrowth(GameTestHelper h) { grow(h, FHBlocks.CULTIVATED_CHERRY.get(), Blocks.CHERRY_LOG, false); }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void jungleGiantGrowth(GameTestHelper h) { grow(h, FHBlocks.CULTIVATED_JUNGLE.get(), Blocks.JUNGLE_LOG, true); }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void accelerationCreditsTheEntireAcceleratedStep(GameTestHelper helper) {
        var level = helper.getLevel();BlockPos pos = ground(helper);
        level.setBlock(pos, FHBlocks.CULTIVATED_OAK.getDefaultState(), 3);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.getMaxLocalRawBrightness(pos.above()) >= 9,
                "fixture light ready")).thenExecute(() -> {
            int[] steps = {32, 64, 128};int index = 0;
            for (FertilizerGrade grade : FertilizerGrade.values()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos, FHBlocks.CULTIVATED_OAK.getDefaultState(), 3);
                level.setBlock(pos.below(), FHBlocks.FERTILIZED_DIRT.getDefaultState()
                        .setValue(FertilizedDirt.FERTILIZER, FertilizerType.ACCELERATED)
                        .setValue(FertilizedDirt.GRADE, grade).setValue(FertilizedDirt.STORAGE, 8), 3);
                ((CultivatedOakBlockEntity)level.getBlockEntity(pos)).advance(level, 10);
                var tag = level.getBlockEntity(pos).saveWithFullMetadata();int step = steps[index++];
                helper.assertTrue(tag.getShort("p") == step && tag.getShort("v") == step * 3 / 4,
                        "every accelerated unit earns current-temperature yield: " + grade);
                helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 8,
                        "accelerated progress never charges fertilizer before a successful tree");
            }
            helper.succeed();
        });
    }
}
