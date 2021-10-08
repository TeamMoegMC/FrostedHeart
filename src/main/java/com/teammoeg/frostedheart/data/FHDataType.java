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

import java.lang.reflect.InvocationTargetException;

public enum FHDataType {
    Armor(new DataType<>(ArmorTempData.class, "armor")),
    Biome(new DataType<>(BiomeTempData.class, "biome")),
    Food(new DataType<>(FoodTempData.class, "food")),
    Block(new DataType<>(BlockTempData.class, "block")),
    Drink(new DataType<>(DrinkTempData.class, "drink")),
    Cup(new DataType<>(CupData.class,"cup"));

    static class DataType<T extends JsonDataHolder> {
        final Class<T> dataCls;
        final String location;

        public DataType(Class<T> dataCls, String location) {
            this.location = location;
            this.dataCls = dataCls;
        }

        public T create(JsonObject jo) {
            try {
                return dataCls.getConstructor(JsonObject.class).newInstance(jo);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e);
            }
        }

        public String getLocation() {
            return "temperature/" + location;
        }
    }

    public final DataType type;

    private FHDataType(DataType type) {
        this.type = type;
    }

}
