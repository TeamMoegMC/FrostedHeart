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

package com.teammoeg.frostedheart.content.climate.data;

import com.google.gson.JsonObject;

public class BlockTempData extends JsonDataHolder {

    public BlockTempData(JsonObject data) {
        super(data);
    }

    public int getRange() {
        return this.getIntOrDefault("range", 5);
    }

    public float getTemp() {
        return this.getFloatOrDefault("temperature", 0F);
    }

    public boolean isLevel() {
        return this.getBooleanOrDefault("level_divide", false);
    }

    public boolean isLit() {
        return this.getBooleanOrDefault("must_lit", false);
    }
}
