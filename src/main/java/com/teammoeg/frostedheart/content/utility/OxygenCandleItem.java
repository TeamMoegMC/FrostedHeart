package com.teammoeg.frostedheart.content.utility;

import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.player.BodyHeatingCapability;
import com.teammoeg.frostedheart.content.climate.player.HeatingDeviceContext;
import com.teammoeg.frostedheart.content.climate.player.HeatingDeviceSlot;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.item.FHBaseItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import java.util.List;

public class OxygenCandleItem extends FHBaseItem {
    public OxygenCandleItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack pStack, Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (pStack.getDamageValue() == pStack.getMaxDamage()) {
            pTooltipComponents.add(Component.translatable("tooltip.frostedheart.oxygencandle.flamedout").withStyle(ChatFormatting.GOLD));
            return;
        }
        pTooltipComponents.add(Component.translatable("tooltip.frostedheart.oxygencandle").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);
        if (itemstack.getDamageValue() == 0) {
            itemstack.setDamageValue(1);
            pPlayer.setItemInHand(pUsedHand, itemstack);
            return InteractionResultHolder.success(itemstack);
        }
        return InteractionResultHolder.pass(itemstack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return FHCapabilities.EQUIPMENT_HEATING.provider(() -> new BodyHeatingCapability() {
            @Override
            public void tickHeating(HeatingDeviceSlot slot, ItemStack stack, HeatingDeviceContext data) {
                int damage = stack.getDamageValue();
                if (damage > 0 && damage < stack.getMaxDamage()) {
                    stack.setDamageValue(damage + 1);
                    data.addEffectiveTemperature(PlayerTemperatureData.BodyPart.TORSO, 5);
                }
            }

            @Override
            public float getMaxTempAddValue(ItemStack stack) {
                int damage = stack.getDamageValue();
                return damage > 0 && damage < stack.getMaxDamage() ? 5 : 0;
            }

            @Override
            public float getMinTempAddValue(ItemStack stack) {
                int damage = stack.getDamageValue();
                return damage > 0 && damage < stack.getMaxDamage() ? 5 : 0;
            }
        });
    }
}
