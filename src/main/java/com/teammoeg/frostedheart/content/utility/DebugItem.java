package com.teammoeg.frostedheart.content.utility;

import com.teammoeg.frostedheart.content.tips.ServerTipSender;
import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.content.tips.client.gui.DebugScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class DebugItem extends FHBaseItem {
    public DebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level worldIn, @NotNull Player playerIn, @NotNull InteractionHand handIn) {
        if (worldIn.isClientSide) {
            DebugScreen.openDebugScreen();
        } else {
            ServerTipSender.sendPopup(Component.literal("Debug Screen"), (ServerPlayer) playerIn);
        }
        return super.use(worldIn, playerIn, handIn);
    }
}
