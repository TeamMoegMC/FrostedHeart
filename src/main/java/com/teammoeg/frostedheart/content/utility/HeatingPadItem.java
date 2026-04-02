package com.teammoeg.frostedheart.content.utility;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.climate.player.BodyHeatingCapability;
import com.teammoeg.frostedheart.content.climate.player.HeatingDeviceContext;
import com.teammoeg.frostedheart.content.climate.player.HeatingDeviceSlot;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.item.FHBaseItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

public class HeatingPadItem extends FHBaseItem {
	public static final int BURN_TIME = 200;
	public static final int HEAT_ADJUST = 20;
    public HeatingPadItem(Properties properties) {
        super(properties);
    }
    
    public static int getState(ItemStack stack) {
    	if (stack.getTag() == null || !stack.getTag().contains("fuel")) return 0;
    	if (stack.getTag().getInt("fuel") >= BURN_TIME) return 2;
    	return 1;
    }
    
    public static int getBurnTime(ItemStack stack) {
    	if (stack.getTag() == null || !stack.getTag().contains("fuel")) return BURN_TIME;
    	return BURN_TIME - stack.getTag().getInt("fuel");
    }
    
    @Override
    public void appendHoverText(ItemStack pStack, Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        int state = getState(pStack);
    	if (state == 2) {
            pTooltipComponents.add(Component.translatable("tooltip.frostedheart.heatingpad.flamedout").withStyle(ChatFormatting.GOLD));
            return;
        }
        pTooltipComponents.add(Component.translatable("tooltip.frostedheart.heatingpad").withStyle(ChatFormatting.GOLD));
        pTooltipComponents.add(Component.translatable("tooltip.frostedheart.heatingpad.time", getBurnTime(pStack)));
        }
    
    @Override
    public boolean isBarVisible(ItemStack pStack) {
        return getBurnTime(pStack) < BURN_TIME ? true : false;
    }
    
    @Override
    public int getBarWidth(ItemStack pStack) {
        return Math.round((float)getBurnTime(pStack) * 13.0F / (float)BURN_TIME);
    }
    
    @Override
    public int getBarColor(ItemStack pStack) {
        float f = Math.max(0.0F, ((float)getBurnTime(pStack) / (float)BURN_TIME));
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }
    
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);
        if (getState(itemstack) == 0 && !pLevel.isClientSide()) {
            itemstack.getOrCreateTag().putInt("fuel", 0);
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
                if (getState(stack) == 1) {
                    data.addEffectiveTemperature(PlayerTemperatureData.BodyPart.TORSO, HEAT_ADJUST);
                    stack.getTag().putInt("fuel", stack.getTag().getInt("fuel") + 1);
                }
            }

            @Override
            public float getMaxTempAddValue(ItemStack stack) {
                return getBurnTime(stack) > 0 ? HEAT_ADJUST : 0;
            }

            @Override
            public float getMinTempAddValue(ItemStack stack) {
            	return getBurnTime(stack) > 0 ? HEAT_ADJUST : 0;
            }
        });
    }
    

    @EventBusSubscriber(modid = FHMain.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public class ClientProperty{
        @SubscribeEvent
        public static void propertyOverrideRegistry(FMLClientSetupEvent event){
            event.enqueueWork(()->{
                ItemProperties.register(FHItems.heating_pad.get(), new ResourceLocation(FHMain.MODID,"state"),(itemStack,level,livingEntity,num)->{
                    return getState(itemStack);
                });
            });
        }
    }
}
