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
