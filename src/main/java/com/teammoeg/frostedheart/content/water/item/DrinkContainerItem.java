/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.water.item;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.water.util.FluidHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import static net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack.FLUID_NBT_KEY;

public class DrinkContainerItem extends ItemFluidContainer {

    public DrinkContainerItem(Properties properties, int capacity) {
        super(properties, capacity);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    	BlockHitResult ray = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
		ItemStack cur=player.getItemInHand(hand);
		if (ray.getType() == Type.BLOCK) {
			BlockPos blockpos = ray.getBlockPos();
			FluidActionResult res=CUtils.pickupFluidFromWorld(cur, player, level, blockpos,ray.getDirection(),true);
			if(res.isSuccess()) {
				ItemStack result=res.getResult();
				//player.setItemInHand(hand, result);
				return InteractionResultHolder.sidedSuccess(result,level.isClientSide);
			}

            // if failed at picking up, empty the fluid inside if shift
            if (player.isShiftKeyDown()) {
            	IFluidHandlerItem handler=cur.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            	if(handler!=null ) {
                    handler.drain(capacity, FluidAction.EXECUTE);
                    return InteractionResultHolder.sidedSuccess(handler.getContainer(), level.isClientSide);
                }
            }
        }
		
        if (canDrink(player, cur)) return ItemUtils.startUsingInstantly(level, player, hand);
        return InteractionResultHolder.pass(cur);

    }

    public ItemStack getFilledItem(ItemStack stack, Player player) {

        player.playSound(SoundEvents.BUCKET_FILL, 1.0f, 1.0f);
        player.awardStat(Stats.ITEM_USED.get(this));
        return FluidHelper.fillContainer(stack, Fluids.WATER);
        
    }



	@Override
    public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity livingEntity) {
        
        if (livingEntity instanceof Player player) {
            if (itemStack.getCount() == 1){
            	IFluidHandlerItem handler = itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
                handler.drain(250, IFluidHandler.FluidAction.EXECUTE);
                return handler.getContainer();
            }else {
            	ItemStack using=itemStack.split(1);
            	IFluidHandlerItem handler = using.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
                handler.drain(250, IFluidHandler.FluidAction.EXECUTE);
                CUtils.giveItem(player, using);
                
                return itemStack;
            }
        }
        return itemStack;
    }

    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    public UseAnim getUseAnimation(ItemStack p_42997_) {
        return UseAnim.DRINK;
    }


    /**
     * Check if the item has fluid and fluid amount is greater than 250mB
     * @param stack
     * @return
     */
    public boolean isDrinkable(ItemStack stack) {
        IFluidHandlerItem fluidHandlerItem = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        return !fluidHandlerItem.getFluidInTank(0).isEmpty() && fluidHandlerItem.getFluidInTank(0).getAmount() >= 250;
    }

    /**
     * Check if player can drink
     * @param playerIn
     * @param stack
     * @return
     */
    public boolean canDrink(Player playerIn, ItemStack stack) {
        
        IFluidHandlerItem fluidHandlerItem = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        return !fluidHandlerItem.getFluidInTank(0).isEmpty() && fluidHandlerItem.getFluidInTank(0).getAmount() >= 250;
    }

    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, this.capacity) {
			@Override
            public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
                return stack.getFluid().is(FHTags.Fluids.DRINK.tag);
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> components, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, level, components, tooltipFlag);
        MutableComponent textComponent = Component.literal(Component.translatable("tooltip.frostedheart.empty").getString());
        int amount = 0;
        if (itemStack.getTagElement(FLUID_NBT_KEY) != null) {
            IFluidHandlerItem fluidHandlerItem = FluidUtil.getFluidHandler(itemStack).orElse(null);
            textComponent = Component.literal(fluidHandlerItem.getFluidInTank(0).getDisplayName().getString());
            amount = fluidHandlerItem.getFluidInTank(0).getAmount();
        }
        components.add(textComponent.copy().append(String.format(": %d / %dmB", amount, capacity)).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        components.add(Component.translatable("tooltip.frostedheart.drink_unit").append(" : 250mB").setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
    }

}
