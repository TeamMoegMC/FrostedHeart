package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Exercises the generated loot tables after datapack loading. */
@GameTestHolder("frostedheart_ice_caves")
@PrefixGameTestTemplate(false)
public final class IceCaveContentGameTests {
    @GameTest(template = "phase0a_empty", batch = "ice_cave_loot", timeoutTicks = 40)
    public static void crystalAndOreLootRemainDistinct(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 4, 1));
        var pickaxe = new ItemStack(Items.IRON_PICKAXE);
        var silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(Enchantments.SILK_TOUCH, 1);
        for (Block block : new Block[] {FHBlocks.ICE_CRYSTAL.get(), FHBlocks.LARGE_ICE_CRYSTAL.get()}) {
            var crystal = block.defaultBlockState();
            helper.assertTrue(Block.getDrops(crystal, level, pos, null, null, pickaxe).isEmpty(),
                    "ordinary mining must not collect either size of ice crystal");
            var iceDrops = Block.getDrops(crystal, level, pos, null, null, silk);
            helper.assertTrue(iceDrops.size() == 1 && iceDrops.get(0).is(block.asItem())
                    && iceDrops.get(0).getCount() == 1, "Silk Touch preserves the corresponding crystal size");
        }
        var glow = FHBlocks.GLOWSTONE_CRYSTAL.get().defaultBlockState();
        for (ItemStack tool : new ItemStack[] {pickaxe, silk}) {
            var glowDrops = Block.getDrops(glow, level, pos, null, null, tool);
            helper.assertTrue(glowDrops.size() == 1 && glowDrops.get(0).is(FHItems.GLOWSTONE_SHARD.get())
                    && glowDrops.get(0).getCount() == 2, "glowstone crystals drop provisional two shards");
        }
        for (Block ore : new Block[] {FHBlocks.PYROLUSITE_ORE.get(), FHBlocks.DEEPSLATE_PYROLUSITE_ORE.get()}) {
            var drops = Block.getDrops(ore.defaultBlockState(), level, pos, null, null, pickaxe);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(FHItems.RAW_PYROLUSITE.get()),
                    "both host rocks drop raw pyrolusite");
            var preserved = Block.getDrops(ore.defaultBlockState(), level, pos, null, null, silk);
            helper.assertTrue(preserved.size() == 1 && preserved.get(0).is(ore.asItem()),
                    "Silk Touch preserves the corresponding ore host");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave_crystal", timeoutTicks = 40)
    public static void registeredCrystalsKeepLightAndSixWayAttachment(GameTestHelper helper) {
        var level = helper.getLevel();
        Block[] blocks = {FHBlocks.ICE_CRYSTAL.get(), FHBlocks.LARGE_ICE_CRYSTAL.get()};
        int[] light = {1, 3};
        for (int size = 0; size < blocks.length; size++) {
            for (Direction facing : Direction.values()) {
                var pos = helper.absolutePos(new BlockPos(3 + facing.ordinal() * 2, 5, 3 + size * 4));
                var support = pos.relative(facing.getOpposite());
                level.setBlock(support, Blocks.PACKED_ICE.defaultBlockState(), 2);
                var player = FakePlayerFactory.getMinecraft(level);
                var previous = player.getMainHandItem();
                try {
                    var stack = new ItemStack(blocks[size]);
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    player.setPos(pos.getX() + 4, pos.getY(), pos.getZ() + 4);
                    var hit = new BlockHitResult(Vec3.atCenterOf(support)
                            .add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5)), facing, support, false);
                    helper.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                            "right-clicking all six support faces must place each registered crystal item");
                } finally {
                    player.setItemInHand(InteractionHand.MAIN_HAND, previous);
                }
                var placed = level.getBlockState(pos);
                helper.assertTrue(placed.is(blocks[size]) && placed.getValue(AmethystClusterBlock.FACING) == facing,
                        "item placement must point away from the clicked support face");
                helper.assertTrue(placed.getLightEmission(level, pos) == light[size],
                        "registered crystal light must match its size in every facing");
                helper.assertTrue(placed.canSurvive(level, pos), "all six ice support faces must hold crystals");
                var wet = placed.setValue(AmethystClusterBlock.WATERLOGGED, true);
                helper.assertTrue(wet.canSurvive(level, pos) && wet.getFluidState().is(Fluids.WATER),
                        "waterlogged crystals retain their support and water");
                level.setBlock(support, Blocks.AIR.defaultBlockState(), 2);
                helper.assertTrue(!placed.canSurvive(level, pos), "crystals require their actual attachment face");
                var unsupported = placed.updateShape(facing.getOpposite(), Blocks.AIR.defaultBlockState(),
                        level, pos, support);
                helper.assertTrue(unsupported.isAir(), "support loss must remove the crystal");
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        helper.succeed();
    }
}
