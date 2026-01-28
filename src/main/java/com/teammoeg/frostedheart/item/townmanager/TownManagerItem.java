package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.frostedheart.item.FHBaseItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TownManagerItem extends FHBaseItem {
    public TownManagerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // 打开GUI
            Minecraft.getInstance().setScreen(new TownManagerScreen(Component.translatable("gui.frostedheart.town_manager")));
        }

        return InteractionResultHolder.success(stack);
    }
}
