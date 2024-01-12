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

package com.teammoeg.frostedheart.content.foods;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;

public class FHSoupItem extends FHBaseItem {

    private final boolean isPoor;

    public FHSoupItem(String name, Properties properties, boolean isPoorlyMade) {
        super(name, properties);
        isPoor = isPoorlyMade;
    }

    public boolean isPoor() {
        return isPoor;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        ItemStack itemstack = super.onItemUseFinish(stack, worldIn, entityLiving);

        // Punish the player since soup item is not properly made
        /*if (this.isPoor && entityLiving instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entityLiving;
            if (worldIn.getRandom().nextInt(3) == 0) {
                player.addPotionEffect(new EffectInstance(Effects.HUNGER, 100, 1));
                player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 40, 1));
                IDietTracker idt = DietCapability.get(player).orElse(null);
                float nv = idt.getValue("protein") - 0.01f;
                idt.setValue("protein", nv > 0 ? nv : 0);
            }
        }*/

        return entityLiving instanceof PlayerEntity && ((PlayerEntity) entityLiving).abilities.isCreativeMode ? itemstack : new ItemStack(Items.BOWL);
    }
}
