package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;

import java.lang.reflect.InvocationTargetException;

public enum FHDataTypes {
    Armor(new DataType<>(ArmorTempData.class, "armor")),
    Biome(new DataType<>(BiomeTempData.class, "biome")),
    Food(new DataType<>(FoodTempData.class, "food")),
    Block(new DataType<>(BlockTempData.class, "block"));

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

    private FHDataTypes(DataType type) {
        this.type = type;
    }

}
