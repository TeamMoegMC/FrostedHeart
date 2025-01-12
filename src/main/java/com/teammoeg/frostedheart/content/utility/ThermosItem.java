/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.utility;

import java.util.List;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.creativeTab.CreativeTabItemHelper;
import com.teammoeg.frostedheart.base.creativeTab.ICreativeModeTabItem;
import com.teammoeg.frostedheart.bootstrap.client.FHTabs;
import com.teammoeg.frostedheart.content.water.item.DurableDrinkContainerItem;
import com.teammoeg.frostedheart.infrastructure.data.FHDataManager;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.util.lang.Lang;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

public class ThermosItem extends DurableDrinkContainerItem implements ITempAdjustFood,ICreativeModeTabItem {
    final int unit;
    final boolean doAddItems;
    static final TagKey<Fluid> availableFluid=FluidTags.create(new ResourceLocation(FHMain.MODID, "drink"));
    static final TagKey<Fluid> hidden = FluidTags.create(new ResourceLocation(FHMain.MODID, "hidden_drink"));
    public ThermosItem(int capacity, int unit, boolean add) {
        super(new Properties().stacksTo(1).setNoRepair().durability(capacity).food(new FoodProperties.Builder().nutrition(1).saturationMod(1).build()), capacity);
        this.unit = unit;
        doAddItems = add;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(Lang.translateTooltip("meme.thermos").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void fillItemCategory(CreativeTabItemHelper helper) {
        if (helper.isType(FHTabs.itemGroup)) {
       
            ITag<Fluid> tag = ForgeRegistries.FLUIDS.tags().getTag(availableFluid);
            
            helper.accept(new ItemStack(this));
            if (tag == null) return;
            if(doAddItems)
            for (Fluid fluid : tag) {
                if (fluid.is(hidden)) continue;
                ItemStack itemStack = new ItemStack(this);
                itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(data -> data.fill(new FluidStack(fluid, data.getTankCapacity(0)), FluidAction.EXECUTE));
                helper.accept(itemStack);
            }
        }
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        ItemStack itemStack1 = itemStack.copy();
        itemStack1.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(data -> {
            FluidStack fs = data.drain(unit, IFluidHandler.FluidAction.EXECUTE);
        });


        return itemStack1;
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        return this.getDefaultInstance();
    }

    @Override
    public ItemStack getDrinkItem() {
        return this.getDefaultInstance();
    }

    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public float getHeat(ItemStack is, float env) {
        LazyOptional<IFluidHandlerItem> ih = FluidUtil.getFluidHandler(is);
        if (ih.isPresent()) {
            IFluidHandlerItem f = ih.resolve().get();
            FluidStack fs = f.getFluidInTank(0);
            if (!fs.isEmpty()) {
                return FHDataManager.getDrinkHeat(fs);
            }
        }
        return 0;
    }

    @Override
    public float getMaxTemp(ItemStack is) {
        return 1;
    }


    @Override
    public float getMinTemp(ItemStack is) {
        return -1;
    }

    public int getUnit() {
        return unit;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

}
