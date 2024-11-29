package com.teammoeg.frostedheart.base.item;


import blusunrize.immersiveengineering.common.register.IEBlocks;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// to debug: /data get entity Dev ForgeCaps.frostedheart:temperature

public class FHBaseClothesItem extends Item {

    private final float windResistance; // How well the cloth blocks wind
    private final float warmthLevel;    // How well the cloth keeps someone warm



    private final PlayerTemperatureData.BodyPart bodyPart;
    public FHBaseClothesItem(Properties properties, float windResistance, float warmthLevel, PlayerTemperatureData.BodyPart bodyPart) {
        super(properties);
        this.windResistance = windResistance;
        this.warmthLevel = warmthLevel;
        this.bodyPart = bodyPart;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Check if the player is using the debugCloth
        PlayerTemperatureData data = PlayerTemperatureData.getCapability(player).orElse(null);
        if (data != null) {
            boolean success = false;
            if(this.bodyPart == PlayerTemperatureData.BodyPart.REMOVEALL) {
                data.clearAllClothes();
                success = true;
            }
            else {
                ItemStack[] clothes = data.getClothesByPart(this.bodyPart);
                for(int i=0;i<clothes.length;++i) {
                    if(clothes[i].isEmpty()){
                        player.sendSystemMessage(MutableComponent.create(new LiteralContents(
                                String.format("Put on slot %d", i)
                        )));
                        clothes[i]=itemStack;
                        itemStack.shrink(1);
                        success=true;
                        break;
                    }
                }
            }
            if(success) {
                return InteractionResultHolder.success(itemStack);  // Item was successfully used
            }
            else {
                return InteractionResultHolder.pass(itemStack);
            }
        } else {
            player.sendSystemMessage(MutableComponent.create(new LiteralContents("Player data not found!")));
            return InteractionResultHolder.pass(itemStack);
        }
    }
    public float getWindResistance() {
        return windResistance;
    }

    public float getWarmthLevel() {
        return warmthLevel;
    }
}