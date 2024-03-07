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

package com.teammoeg.frostedheart.content.temperature.heatervest;

import java.util.List;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.content.climate.player.IHeatingEquipment;
import com.teammoeg.frostedheart.content.steamenergy.IChargable;
import com.teammoeg.frostedheart.util.EquipmentCuriosSlotType;
import com.teammoeg.frostedheart.util.client.GuiUtils;

import blusunrize.immersiveengineering.common.util.EnergyHelper;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Heater Vest: wear it to warm yourself from the coldness.
 * 加温背心：穿戴抵御寒冷
 */
public class HeaterVestItem extends FHBaseItem implements EnergyHelper.IIEEnergyItem, IHeatingEquipment, IChargable {

    public HeaterVestItem(Properties properties) {
        super(properties);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        String stored = this.getEnergyStored(stack) + "/" + this.getMaxEnergyStored(stack);
        list.add(GuiUtils.translateTooltip("charger.heat_vest").mergeStyle(TextFormatting.GRAY));
        list.add(GuiUtils.translateTooltip("steam_stored", stored).mergeStyle(TextFormatting.GOLD));
    }

    @Override
    public float charge(ItemStack stack, float value) {
        return this.receiveEnergy(stack, (int) value, false);
    }


    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) {
            items.add(new ItemStack(this));
            ItemStack is = new ItemStack(this);
            this.receiveEnergy(is, this.getMaxEnergyStored(is), false);
            items.add(is);
        }

    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BipedModel getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlotType armorSlot,
                                    BipedModel _default) {
        return HeaterVestModel.getModel();
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
        return FHMain.rl("textures/models/heater_vest.png").toString();
    }

    @Nullable
    @Override
    public EquipmentSlotType getEquipmentSlot(ItemStack stack) {
        return EquipmentSlotType.CHEST;
    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {
        return 30000;
    }

	@Override
	public float getEffectiveTempAdded(EquipmentCuriosSlotType slot, ItemStack stack, float effectiveTemp, float bodyTemp) {
		if(slot==null) {
			return 50;
		}
		int energycost = 1;
		if (effectiveTemp < 30.05f) {
		    float delta = 30.05f - effectiveTemp;
		    if (delta > 50)
		        delta = 50F;
		    float rex = Math.max(this.extractEnergy(stack, energycost + (int) (delta * 0.24f), false) - energycost, 0F);
		    return rex / 0.24f;
		} else this.extractEnergy(stack, energycost, false);
		return 0;
	}

}
