/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.heating;

import blusunrize.immersiveengineering.common.util.EnergyHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.content.heatervest.HeaterVestModel;
import com.teammoeg.frostedheart.content.steamenergy.IChargable;
import com.teammoeg.frostedheart.climate.IHeatingEquipment;
import com.teammoeg.frostedheart.base.item.FHBaseItem;

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
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Heater Vest: wear it to warm yourself from the coldness.
 * 加温背心：穿戴抵御寒冷
 */
public class HeaterVestItem extends FHBaseItem implements EnergyHelper.IIEEnergyItem, IHeatingEquipment, IChargable {

    public HeaterVestItem(String name, Properties properties) {
        super(name, properties);
    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {
        return 9600;
    }

    @Nullable
    @Override
    public EquipmentSlotType getEquipmentSlot(ItemStack stack) {
        return EquipmentSlotType.CHEST;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
        return FHMain.rl("textures/models/heater_vest.png").toString();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BipedModel getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlotType armorSlot,
                                    BipedModel _default) {
        return HeaterVestModel.getModel();
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        String stored = this.getEnergyStored(stack) + "/" + this.getMaxEnergyStored(stack);
        list.add(GuiUtils.translateTooltip("steam_stored", stored).mergeStyle(TextFormatting.GOLD));
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
    public float compute(ItemStack stack, float bodyTemp, float environmentTemp) {
        int energycost = 2;
        if (bodyTemp < 0.2) {
            float delta = 0.2F - bodyTemp;
            if (delta > 0.25)
                delta = 0.25F;
            float rex = Math.max(this.extractEnergy(stack, energycost + (int) (delta * 120F), false) - 2F, 0F);
            bodyTemp += rex / 120F;
        }
        return bodyTemp;
    }

    @Override
    public float charge(ItemStack stack, float value) {
        return this.receiveEnergy(stack, (int) value, false);
    }

	@Override
	public float getMax(ItemStack stack) {
		return 0.25F;
	}

}
