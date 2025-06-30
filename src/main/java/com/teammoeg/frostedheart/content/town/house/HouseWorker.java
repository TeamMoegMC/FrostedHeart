package com.teammoeg.frostedheart.content.town.house;

import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.TownWorker;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.TownResourceManager;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class HouseWorker implements TownWorker {
    private HouseWorker() {}
    public static final HouseWorker INSTANCE = new HouseWorker();
    @Override
    public boolean work(Town town, CompoundTag workData) {
        double residentNum = workData.getCompound("tileEntity").getList("residents", 10).size();
        ItemResourceType[] foodTypes = new ItemResourceType[]{
                ItemResourceType.FOOD_GRAINS,
                ItemResourceType.FOOD_FRUIT_AND_VEGETABLES,
                ItemResourceType.FOOD_PROTEIN,
                ItemResourceType.FOOD_EDIBLE_OIL
        };
        Map<ItemResourceType, Double> foodAmounts = new HashMap<>();
        double totalFoods = 0;

        for (ItemResourceType foodType : foodTypes) {
            foodAmounts.put(foodType, town.getResourceManager().get(foodType));
            totalFoods += foodAmounts.get(foodType);
        }

        if (residentNum > totalFoods) return false;

        double toCost = residentNum * 5;
        foodAmounts.clear(); // 清空这个Map的内容，之后当做costedFoods来使用

        // duck_egg: 未来或许会按照食物的质量(result.averageLevel)和均衡程度，影响房屋内居民的健康。
        // 目前仅做一个基础的cost内容，以消除编译错误。
        for (ItemResourceType foodType : foodTypes) {
            TownResourceManager.SimpleResourceActionResult result = town.getResourceManager().costHighestLevelToEmpty(foodType, residentNum);
            foodAmounts.put(foodType, result.actualAmount());
            toCost -= result.actualAmount();
        }

        if (toCost <= 0) {
            return true;
        }

        for (ItemResourceType foodType : foodTypes) {
            TownResourceManager.SimpleResourceActionResult result = town.getResourceManager().costHighestLevelToEmpty(foodType, toCost);
            foodAmounts.merge(foodType, result.actualAmount(), Double::sum);
            toCost -= result.actualAmount();
        }

        return true;
    }
}
