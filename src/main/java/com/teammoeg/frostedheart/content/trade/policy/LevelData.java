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

package com.teammoeg.frostedheart.content.trade.policy;

import com.google.gson.JsonElement;
import com.teammoeg.chorda.io.Writeable;

import net.minecraft.network.FriendlyByteBuf;

public class LevelData implements Writeable {
    int min;
    int max;
    int level;

    public LevelData(int min, int max, int level) {
        super();
        this.min = min;
        this.max = max;
        this.level = level;
    }

    @Override
    public JsonElement serialize() {
        return null;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
    }

}
