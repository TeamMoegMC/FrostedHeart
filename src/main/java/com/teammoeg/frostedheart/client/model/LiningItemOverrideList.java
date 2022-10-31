/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.client.model;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHMain;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class LiningItemOverrideList extends ItemOverrideList {

    public LiningItemOverrideList() {
        super();
    }

    @Override
    public IBakedModel getOverrideModel(IBakedModel originalModel, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity) {
        String s = ItemNBTHelper.getString(stack, "inner_cover");
        EquipmentSlotType slotType = ((ArmorItem) stack.getItem()).getEquipmentSlot();
        if (s.length() > 0 && slotType != null) {
            String liningType = new ResourceLocation(s).getPath();
            String slotName = "feet";
            if (slotType.getName().equals("feet")) {
                slotName = "feet";
            } else if (slotType.getName().equals("legs")) {
                slotName = "legs";
            } else if (slotType.getName().equals("chest")) {
                slotName = "torso";
            } else if (slotType.getName().equals("head")) {
                slotName = "helmet";
            }
            ResourceLocation textureLocation = FHMain.rl("item/lining_overlay/" + liningType + "_" + slotName);
            return new LiningFinalizedModel(originalModel, textureLocation);
        }
        return originalModel;
    }
}
