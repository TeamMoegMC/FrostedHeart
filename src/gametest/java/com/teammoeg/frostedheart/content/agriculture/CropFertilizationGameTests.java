package com.teammoeg.frostedheart.content.agriculture;

import java.util.List;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_fertilizer")
@PrefixGameTestTemplate(false)
public class CropFertilizationGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void growthCreditAndLateApplication(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlock(pos.east(), Blocks.GLOWSTONE.defaultBlockState(), 18);
        helper.runAfterDelay(10, () -> {
        BlockState fertilizer = FHBlocks.FERTILIZED_FARMLAND.getDefaultState()
            .setValue(FertilizedDirt.FERTILIZER, Fertilizer.FertilizerType.INCREASING)
            .setValue(FertilizedDirt.STORAGE, 8).setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7);
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        level.setBlock(pos.below(), fertilizer, 18);
        level.setBlock(pos, wheat.getStateForAge(7), 18);
        helper.assertTrue(harvestCount(level, pos) == 7, "late fertilizer must not multiply mature crops");
        level.setBlock(pos, wheat.getStateForAge(0), 18);
        for (int age = 0; age < 7; age++) {
            CropFertilization.before(level, pos);
            BlockState old = level.getBlockState(pos);
            level.setBlock(pos, wheat.getStateForAge(age + 1), 18);
            CropFertilization.after(new BlockEvent.CropGrowEvent.Post(level, pos, old, level.getBlockState(pos)));
        }
        int count = harvestCount(level, pos);
        helper.assertTrue(count == 14, "full fertilized growth doubles yield; actual=" + count + ", crop=" + level.getBlockState(pos) + ", soil=" + level.getBlockState(pos.below()));
        helper.assertTrue(harvestCount(level, pos) == 14, "loot preview must not spend crop credit");
        var saved = level.getDataStorage().computeIfAbsent(CropFertilization.Records::load, CropFertilization.Records::new,
            "frostedheart_crop_fertilization").save(new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(Math.abs(CropFertilization.Records.load(saved).peek(pos, level.getBlockState(pos)) - 1) < 0.000001,
            "growth credit survives NBT reload");
        level.setBlock(pos, wheat.getStateForAge(0), 18);
        level.setBlock(pos, wheat.getStateForAge(7), 18);
        helper.assertTrue(harvestCount(level, pos) == 7, "reset crop cannot reuse previous cycle credit");
        int storage = level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE);
        CropFertilization.before(level, pos);
        CropFertilization.after(new BlockEvent.CropGrowEvent.Post(level, pos, level.getBlockState(pos), level.getBlockState(pos)));
        helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == storage, "no change consumes nothing");
        helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void verticalRootsAndCreateLoot(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlock(pos.below(), FHBlocks.FERTILIZED_DIRT.getDefaultState()
            .setValue(FertilizedDirt.FERTILIZER, Fertilizer.FertilizerType.INCREASING)
            .setValue(FertilizedDirt.STORAGE, 8), 18);
        level.setBlock(pos.below().east(), Blocks.WATER.defaultBlockState(), 18);
        level.setBlock(pos, Blocks.SUGAR_CANE.defaultBlockState(), 18);
        level.setBlock(pos.above(), Blocks.SUGAR_CANE.defaultBlockState(), 18);
        helper.assertTrue(CropFertilization.soil(level, pos.above()).equals(pos.below()), "cane top resolves soil");
        helper.assertTrue(level.getBlockState(pos).canSurvive(level, pos), "cane survives fertilized dirt");
        var hemp = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation("immersiveengineering", "hemp"));
        level.setBlock(pos.below(), FHBlocks.FERTILIZED_FARMLAND.getDefaultState(), 18);
        var hempCrop = (blusunrize.immersiveengineering.common.blocks.plant.HempBlock) hemp;
        level.setBlock(pos, hempCrop.getStateForAge(4), 18);
        level.setBlock(pos.above(), hempCrop.getStateForAge(4).setValue(blusunrize.immersiveengineering.common.blocks.plant.HempBlock.TOP, true), 18);
        helper.assertTrue(CropFertilization.soil(level, pos.above()).equals(pos.below()), "hemp upper resolves soil: " + level.getBlockState(pos) + " / " + level.getBlockState(pos.above()));
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(pos.below(), FHBlocks.FERTILIZED_FARMLAND.getDefaultState()
            .setValue(FertilizedDirt.FERTILIZER, Fertilizer.FertilizerType.INCREASING)
            .setValue(FertilizedDirt.STORAGE, 8), 18);
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        level.setBlock(pos, wheat.getStateForAge(0), 18);
        CropFertilization.before(level, pos);
        level.setBlock(pos, wheat.getStateForAge(7), 18);
        CropFertilization.after(new BlockEvent.CropGrowEvent.Post(level, pos, wheat.getStateForAge(0), wheat.getStateForAge(7)));
        // Wheat's mature loot always contains one wheat; the same helper is used by the harvester.
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        com.simibubi.create.foundation.utility.BlockHelper.destroyBlockAs(level, pos, null, ItemStack.EMPTY, 1, drops::add);
        helper.assertTrue(drops.stream().filter(item -> item.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum() == 2,
            "Create receives growth-earned double wheat through common loot mixin");
        helper.succeed();
    }
    /** Exercises Create's harvest, seed reservation and replanting for consecutive crops. */
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void createConsecutiveHarvests(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        level.setBlock(pos.east(), Blocks.GLOWSTONE.defaultBlockState(), 18);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(com.simibubi.create.infrastructure.config.AllConfigs.server().kinetics.harvesterReplants.get(),
                "scenario requires Create replanting enabled");
            level.setBlock(pos.below(), FHBlocks.FERTILIZED_FARMLAND.getDefaultState()
                .setValue(FertilizedDirt.FERTILIZER, Fertilizer.FertilizerType.INCREASING)
                .setValue(FertilizedDirt.STORAGE, 8)
                .setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), 18);
            level.setBlock(pos, ((CropBlock) Blocks.WHEAT).getStateForAge(0), 18);
            List<ItemStack> collected = new java.util.ArrayList<>();
            var harvester = new com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour() {
                @Override
                public void dropItem(com.simibubi.create.content.contraptions.behaviour.MovementContext context, ItemStack item) {
                    collected.add(item.copy());
                }
            };
            var context = new com.simibubi.create.content.contraptions.behaviour.MovementContext(level,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                    BlockPos.ZERO, com.simibubi.create.AllBlocks.MECHANICAL_HARVESTER.getDefaultState(), new net.minecraft.nbt.CompoundTag()), null);
            growToMaturity(level, pos);
            harvester.visitNewPosition(context, pos);
            checkCreateRound(helper, pos, collected, 2, "first fertilized harvest");
            level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState()
                .setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), 18);
            helper.runAfterDelay(2, () -> {
                collected.clear();
                growToMaturity(level, pos);
                harvester.visitNewPosition(context, pos);
                checkCreateRound(helper, pos, collected, 1, "second unfertilized harvest must not inherit first bonus");
                level.setBlock(pos.below(), FHBlocks.FERTILIZED_FARMLAND.getDefaultState()
                    .setValue(FertilizedDirt.FERTILIZER, Fertilizer.FertilizerType.INCREASING)
                    .setValue(FertilizedDirt.STORAGE, 8)
                    .setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), 18);
                helper.runAfterDelay(2, () -> {
                    collected.clear();
                    growToMaturity(level, pos);
                    harvester.visitNewPosition(context, pos);
                    checkCreateRound(helper, pos, collected, 2, "third fertilized harvest earns new bonus");
                    helper.succeed();
                });
            });
        });
    }

    private static void growToMaturity(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        for (int age = wheat.getAge(level.getBlockState(pos)); age < wheat.getMaxAge(); age++) {
            BlockState old = level.getBlockState(pos);
            CropFertilization.before(level, pos);
            level.setBlock(pos, wheat.getStateForAge(age + 1), 18);
            CropFertilization.after(new BlockEvent.CropGrowEvent.Post(level, pos, old, level.getBlockState(pos)));
        }
    }

    private static void checkCreateRound(GameTestHelper helper, BlockPos pos, List<ItemStack> drops, int expected, String message) {
        int actual = drops.stream().filter(item -> item.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(actual == expected, message + "; expected=" + expected + ", actual=" + actual);
        BlockState replanted = helper.getLevel().getBlockState(pos);
        helper.assertTrue(replanted.is(Blocks.WHEAT) && ((CropBlock) Blocks.WHEAT).getAge(replanted) == 0,
            message + ": Create replants age zero");
        var records = helper.getLevel().getDataStorage().computeIfAbsent(CropFertilization.Records::load,
            CropFertilization.Records::new, "frostedheart_crop_fertilization");
        helper.assertTrue(records.peek(pos, replanted) == 0, message + ": previous growth credit cleared");
    }

    private static int harvestCount(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        return CropFertilization.harvest(level, pos, level.getBlockState(pos), List.of(new ItemStack(Items.WHEAT, 7)))
            .stream().mapToInt(ItemStack::getCount).sum();
    }
}
