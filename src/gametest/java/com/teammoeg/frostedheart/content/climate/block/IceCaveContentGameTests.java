package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
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
        var crystal = FHBlocks.ICE_CRYSTAL.get().defaultBlockState();
        helper.assertTrue(Block.getDrops(crystal, level, pos, null, null, pickaxe).isEmpty(),
                "ordinary mining must not collect ice crystals");
        var iceDrops = Block.getDrops(crystal, level, pos, null, null, silk);
        helper.assertTrue(iceDrops.size() == 1 && iceDrops.get(0).is(FHBlocks.ICE_CRYSTAL.asItem())
                && iceDrops.get(0).getCount() == 1, "Silk Touch collects one ice crystal");
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
}
