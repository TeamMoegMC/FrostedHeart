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

package com.teammoeg.frostedheart.content.health.dailykitchen;

import java.util.Random;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.Lang;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.capability.ItemFluidContainer;

class Benefits {
    private final MobEffect[] basicEffects = new MobEffect[]{MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED};
    private final ServerPlayer player;
    private final WantedFoodCapability capability;
    private final int eatenFoodsAmount;
    private final int benefitLevel;
    private final Random random = new Random();
    private final int basicEffectDuration;

    public Benefits(ServerPlayer player) {
        this.player = player;
        this.capability = FHCapabilities.WANTED_FOOD.getCapability(player).orElse(null);
        this.eatenFoodsAmount = capability.getEatenFoodsAmount();
        this.benefitLevel = Math.min((eatenFoodsAmount / 10), 7);
        this.basicEffectDuration = Math.min(3600 + 150 * eatenFoodsAmount, 20000);//can't be more than one day
    }


    public void giveEnergy() {
        //EnergyCore.addEnergy(player, (int) ((5000 + EnergyCore.getEnergy(player)) * Math.min((float) eatenFoodsAmount / 200, 0.5)));
    }

    /**
     * only used when amount<3
     * if amount = 3, use giveBasicEffects(int amount, int[] potionLevel)
     */
    private void giveBasicEffects(int amount) {
        if (amount == 1) {
            player.addEffect(new MobEffectInstance(basicEffects[random.nextInt(2)], basicEffectDuration, 0));

        } else if (amount == 2) {
            int notGiveEffect = random.nextInt(2);
            for (int i = 0; i < 2; i++) {
                if (i != notGiveEffect) {
                    player.addEffect(new MobEffectInstance(basicEffects[i], basicEffectDuration, 0));
                }
            }
        }
    }

    private void giveBasicEffects(int amount, int[] potionLevel) {
        if (amount < 3) {
            this.giveBasicEffects(amount);
        } else if (amount == 3)
            for (int i = 0; i < 3; i++) {
                player.addEffect(new MobEffectInstance(basicEffects[i], basicEffectDuration, potionLevel[i]));
            }
        else FHMain.LOGGER.error("Invalid effect amount input!");
    }

    private void giveHealthRegen(int duration) {
        this.player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration));
    }


    private void giveEffects() {
        switch (benefitLevel) {
            case 0: {
                this.giveBasicEffects(1);
                break;
            }
            case 1: {
                this.giveBasicEffects(1);
                this.giveHealthRegen(60);
                break;
            }
            case 2: {
                this.giveBasicEffects(2);
                this.giveHealthRegen(60);
                break;
            }
            case 3: {
                this.giveBasicEffects(2);
                this.giveHealthRegen(100);
                break;
            }
            case 4: {
                this.giveBasicEffects(3, new int[]{0, 0, 0});
                this.giveHealthRegen(100);
                break;
            }
            case 5: {
                this.giveBasicEffects(3, new int[]{0, 1, 0});
                this.giveHealthRegen(120);
                break;
            }
            case 6: {
                this.giveBasicEffects(3, new int[]{0, 1, 1});
                this.giveHealthRegen(160);
                break;
            }
            case 7: {
                this.giveBasicEffects(3, new int[]{0, 1, 1});
                this.giveHealthRegen(200);
                break;
            }
        }
    }

    public void give() {
        this.giveEffects();
        this.giveEnergy();
        capability.countEatenTimes();

        player.displayClientMessage(Lang.translateMessage("eat_wanted_food"), false);
    }

    public void tryGive(Item food) {
        if (capability.getWantedFoods().contains(food)) {
            this.give();
        }
    }

    public void tryGive(ItemStack foodItemStack) {
        if (capability.getEatenTimes() > 3) return;
        Item foodOrSoupContainer = foodItemStack.getItem();
        if (foodOrSoupContainer instanceof ItemFluidContainer) {
            assert foodItemStack.getTag() != null;
            Fluid fluid = CRegistryHelper.getFluid(new ResourceLocation(foodItemStack.getTag().getCompound("Fluid").getString("FluidName")));
            //TODO add caupona dependency
            //BowlContainingRecipe recipe = BowlContainingRecipe.recipes.get(fluid);
            //if (recipe != null) {
            //    tryGive(recipe.handle(fluid).getItem());
            //}
        } else tryGive(foodOrSoupContainer);
    }
}
