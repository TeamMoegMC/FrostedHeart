package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.frostedheart.content.tips.client.TipHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class DebugItem extends Item {
    public DebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        if (worldIn.isRemote) {
            TipHandler.openDebugScreen();
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
    }
}
