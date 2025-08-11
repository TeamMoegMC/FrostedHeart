package com.teammoeg.frostedheart.content.town.house;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.TownWorker;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.TownResourceManager;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HouseWorker implements TownWorker {
    private HouseWorker() {}
    public static final HouseWorker INSTANCE = new HouseWorker();
    @Override
    public boolean work(Town town, CompoundTag workData) {
        List<UUID> residentsUUID = workData.getCompound("tileEntity").getList("residents", 10)
                .stream()
                .map(nbt -> UUID.fromString(nbt.getAsString()))
                .toList();
        double residentNum = residentsUUID.size();
        ItemResourceType[] foodTypes = new ItemResourceType[]{
                ItemResourceType.FOOD_GRAINS,
                ItemResourceType.FOOD_FRUIT_AND_VEGETABLES,
                ItemResourceType.FOOD_PROTEIN,
                ItemResourceType.FOOD_EDIBLE_OIL
        };
        Map<ItemResourceType, Double> foodAmounts = new HashMap<>();
        double totalFoods = 0;
        IActionExecutorHandler executorHandler = town.getActionExecutorHandler();
        for (ItemResourceType foodType : foodTypes) {
            foodAmounts.put(foodType, TownResourceActions.get(executorHandler, foodType));
            totalFoods += foodAmounts.get(foodType);
        }

        if (residentNum > totalFoods) return false;

        double toCost = residentNum * 5;
        foodAmounts.clear(); // 清空这个Map的内容，之后当做costedFoods来使用

        // duck_egg: 未来或许会按照食物的质量(result.averageLevel)和均衡程度，影响房屋内居民的健康。
        // 目前仅做一个基础的cost内容，以消除编译错误。
        for (ItemResourceType foodType : foodTypes) {
            TownResourceActions.TownResourceTypeCostAction costTypeAction = new TownResourceActions.TownResourceTypeCostAction
                    (foodType, residentNum, 0, 100, ResourceActionMode.MAXIMIZE, ResourceActionOrder.ASCENDING);
            TownResourceActions.TownResourceTypeCostActionResult result = (TownResourceActions.TownResourceTypeCostActionResult) executorHandler.execute(costTypeAction);
            foodAmounts.put(foodType, result.totalModifiedAmount());
            toCost -= result.totalModifiedAmount();
        }

        if (toCost <= 0) {
            return true;
        } else if(town instanceof TeamTown teamTown){
            Map<UUID, Resident> townResidents = teamTown.getResidents();
            for(UUID uuid : residentsUUID){
                Resident resident = townResidents.get(uuid);
                if(resident != null){
                    resident.costHealth((int)Math.round(toCost / residentNum));
                }
            }
        }

        return true;
    }
}
