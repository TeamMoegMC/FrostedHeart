package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.frostedheart.content.tips.client.util.TipDisplayUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import net.minecraft.item.Item.Properties;

public class DebugItem extends Item {
    public DebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        if (worldIn.isClientSide) {
            TipDisplayUtil.openDebugScreen();
        }
        return super.use(worldIn, playerIn, handIn);
    }
}
