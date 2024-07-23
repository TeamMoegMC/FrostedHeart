/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util.utility;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.item.ItemStack;

public class BlackListPredicate extends ItemPredicate {
    ItemPredicate white;

    public BlackListPredicate(JsonObject jo) {
        JsonElement intern = new JsonParser().parse(jo.toString());
        intern.getAsJsonObject().remove("type");
        white = ItemPredicate.fromJson(intern);
    }

    public JsonElement serializeToJson() {
        new Exception().printStackTrace();

        JsonElement je = white.serializeToJson();
        je.getAsJsonObject().addProperty("type", FHMain.MODID + ":blacklist");
        return je;
    }

    @Override
    public boolean matches(ItemStack item) {
        return !white.matches(item);
    }
}
