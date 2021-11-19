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

package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.climate.ITempAdjustFood;

import net.minecraft.item.ItemStack;

public class FoodTempData extends JsonDataHolder implements ITempAdjustFood {

    public FoodTempData(JsonObject data) {
        super(data);
    }

    @Override
    public float getMaxTemp(ItemStack is) {
        return this.getFloatOrDefault("max", 15F);
    }

    @Override
    public float getMinTemp(ItemStack is) {
        return this.getFloatOrDefault("min", -15F);
    }

    @Override
    public float getHeat(ItemStack is) {
        return this.getFloatOrDefault("heat", 0F);
    }
}
