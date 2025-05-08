package com.teammoeg.frostedheart.content.utility.gunpowder_barrel;

import com.simibubi.create.AllEnchantments;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GunpowderBarrelBlockItem extends BlockItem {
    public GunpowderBarrelBlockItem(Properties pProperties) {
        super(FHBlocks.GUNPOWDER_BARREL.get(), pProperties);
    }
    public static int getFortuneFromNBT(ItemStack stack){
        int i = 0;
        CompoundTag nbt = BlockItem.getBlockEntityData(stack);
        if (nbt != null && nbt.contains("Fortune")) {
            i = nbt.getInt("Fortune");
        }
        return i;
    }
    public static int getPowerFromNBT(ItemStack stack){
        int power = 1;
        CompoundTag nbt = BlockItem.getBlockEntityData(stack);
        if (nbt != null && nbt.contains("Power")) {
            power = nbt.getInt("Power");
        }
        return power;
    }
    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
        int power = getPowerFromNBT(pStack) * 2 + 1;
        int fortune = getFortuneFromNBT(pStack);
        pTooltip.add(Component.literal(power+"x"+power).withStyle(ChatFormatting.GRAY));
        MutableComponent texts = Component.translatable(Enchantments.BLOCK_FORTUNE.getDescriptionId());
        texts.append(Component.literal(" "+fortune));
        pTooltip.add(texts.withStyle(ChatFormatting.GRAY));
    }
}
