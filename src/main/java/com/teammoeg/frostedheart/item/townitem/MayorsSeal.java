package com.teammoeg.frostedheart.item.townitem;

import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.item.snowsack.SnowSackMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class MayorsSeal extends FHBaseItem {
    public MayorsSeal(Properties properties) {
        super(properties);
    }
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            //todo: 打开GUI

        }

        return InteractionResultHolder.success(stack);
    }
}
