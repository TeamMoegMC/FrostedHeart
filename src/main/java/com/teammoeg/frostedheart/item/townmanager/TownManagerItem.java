package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.item.snowsack.SnowSackMenuProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
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
            System.out.println("duck_egg debug: Opening GUI");
            Minecraft.getInstance().setScreen(new TownManagerScreen(Component.translatable("gui.frostedheart.town_manager")));
        }

        return InteractionResultHolder.success(stack);
    }
}
