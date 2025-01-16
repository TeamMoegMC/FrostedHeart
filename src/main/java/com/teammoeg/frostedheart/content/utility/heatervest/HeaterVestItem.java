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

package com.teammoeg.frostedheart.content.utility.heatervest;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Either;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.bootstrap.client.FHTabs;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.player.IHeatingEquipment;
import com.teammoeg.frostedheart.content.steamenergy.charger.IChargable;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatStorageCapability;
import com.teammoeg.frostedheart.util.client.Lang;
import com.teammoeg.frostedheart.content.climate.player.EquipmentSlotType;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import top.theillusivec4.curios.api.type.ISlotType;

/**
 * Heater Vest: wear it to warm yourself from the coldness.
 * 加温背心：穿戴抵御寒冷
 */
public class HeaterVestItem extends FHBaseItem implements IHeatingEquipment, IChargable {
    public static final String NBT_HEATER_VEST = FHMain.MODID + "heater_vest";
    private static final String ENERGY_KEY="steam";
    public HeaterVestItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
        String stored = FHCapabilities.ITEM_HEAT.getCapability(stack).map(t->{
			return t.getEnergyStored();
		}).orElse(0F) + "/" + this.getMaxEnergyStored(stack);
        list.add(Lang.translateTooltip("charger.heat_vest").withStyle(ChatFormatting.GRAY));
        list.add(Lang.translateTooltip("steam_stored", stored).withStyle(ChatFormatting.GOLD));
    }

    @Override
    public float charge(ItemStack stack, float value) {
		return FHCapabilities.ITEM_HEAT.getCapability(stack).map(t->{
			return t.receiveEnergy(value, false);
		}).orElse(value);
    }



    @Override
	public void fillItemCategory(CreativeTabItemHelper helper) {
		if(helper.isType(FHTabs.itemGroup)) {
        	helper.accept(new ItemStack(this));
            ItemStack is = new ItemStack(this);
            FHCapabilities.ITEM_HEAT.getCapability(is).ifPresent(t->t.receiveEnergy(30000, false));
            helper.accept(is);
	        
		}
	}

	@OnlyIn(Dist.CLIENT)
    @Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		super.initializeClient(consumer);
		consumer.accept(new HeaterVestExtension());
	}

	@Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return FHMain.rl("textures/models/heater_vest.png").toString();
    }

    @Nullable
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    public int getMaxEnergyStored(ItemStack container) {
        return 30000;
    }

	@Override
	public float getEffectiveTempAdded(Either<ISlotType,EquipmentSlotType> slot, ItemStack stack, float effectiveTemp, float bodyTemp) {
		if(slot==null) {
			return 50;
		}
		int energycost = 1;
		return FHCapabilities.ITEM_HEAT.getCapability(stack).map(t->{
			if (effectiveTemp < 30.05f) {
			    float delta = 30.05f - effectiveTemp;
			    if (delta > 50)
			        delta = 50F;
			    float rex = Math.max(t.extractEnergy( energycost + (int) (delta * 0.24f), false) - energycost, 0F);
			    return rex / 0.24f;
			} else t.extractEnergy(energycost, false);
			return 0f;
		}).orElse(0f);
		
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack,CompoundTag nbt) {
		return FHCapabilities.ITEM_HEAT.provider(()->new HeatStorageCapability(stack, 30000));
	}


}
