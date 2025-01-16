package com.teammoeg.frostedheart.content.utility;

import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.content.tips.client.gui.DebugScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DebugItem extends FHBaseItem {
    public DebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        if (worldIn.isClientSide) {
            DebugScreen.openDebugScreen();
        }
        return super.use(worldIn, playerIn, handIn);
    }
}
