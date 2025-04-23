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

package com.teammoeg.frostedheart.bootstrap.reference;

import net.minecraft.world.food.FoodProperties;

public class FHFoodProperties {
    public static final FoodProperties VEGETABLE_SAWDUST_SOUP = buildStew(6, 0.3F);
    public static final FoodProperties RYE_SAWDUST_PORRIDGE = buildStew(6, 0.4F);
    public static final FoodProperties RYE_BREAD = buildStew(6);
    public static final FoodProperties BLACK_BREAD = buildStew(5, 0.0F);
    public static final FoodProperties WHITE_TURNIP = buildStew(3, 0.5F);
    public static final FoodProperties DRIED_WOLFBERRIES = buildStew(1);
    public static final FoodProperties COOKED_SQUID_TENTACLES = (new FoodProperties.Builder()).nutrition(5).saturationMod(0.6F).build();
    public static final FoodProperties COOKED_FOX_MEAT = (new FoodProperties.Builder()).nutrition(6).saturationMod(0.8F).meat().build();
    public static final FoodProperties COOKED_WOLF_MEAT = (new FoodProperties.Builder()).nutrition(6).saturationMod(0.8F).meat().build();
    public static final FoodProperties COOKED_POLAR_BEAR_MEAT = (new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).meat().build();
    public static final FoodProperties SQUID_TENTACLES = (new FoodProperties.Builder()).nutrition(2).saturationMod(0.1F).build();
    public static final FoodProperties FOX_MEAT = (new FoodProperties.Builder()).nutrition(2).saturationMod(0.3F).meat().build();
    public static final FoodProperties WOLF_MEAT = (new FoodProperties.Builder()).nutrition(2).saturationMod(0.3F).meat().build();
    public static final FoodProperties POLAR_BEAR_MEAT = (new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).meat().build();

    private static FoodProperties buildStew(int hunger) {
        return (new FoodProperties.Builder()).nutrition(hunger).saturationMod(0.6F).build();
    }

    private static FoodProperties buildStew(int hunger, float saturation) {
        return (new FoodProperties.Builder()).nutrition(hunger).saturationMod(saturation).build();
    }
}
