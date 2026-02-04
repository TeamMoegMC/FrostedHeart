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

package com.teammoeg.frostedheart.content.town.buildings.house;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import com.teammoeg.frostedheart.content.town.ITownWithResidents;
import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.TownWorker;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import com.teammoeg.frostedheart.content.town.worker.WorkOrder;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import static com.teammoeg.frostedheart.content.town.resource.ItemResourceType.RESIDENT_FOOD_LEVEL;

public class HouseWorker implements TownWorker<HouseState> {
    private HouseWorker() {}
    public static final HouseWorker INSTANCE = new HouseWorker();
    @Override
    public boolean work(Town town, HouseState workData, WorkOrder workOrder) {
    	if(workOrder!=WorkOrder.NORMAL)return false;
        //获取所有居民
        List<UUID> residentsUUID = workData.getResidents();
        double residentNum = residentsUUID.size();
/*        ItemResourceType[] foodTypes = new ItemResourceType[]{
                ItemResourceType.FOOD_GRAINS,
                ItemResourceType.FOOD_FRUIT_AND_VEGETABLES,
                ItemResourceType.FOOD_PROTEIN,
                ItemResourceType.FOOD_EDIBLE_OIL
        };*/

        Map<ItemResourceType, Double> foodAmounts = new HashMap<>();
        double totalFoods = 0;
        IActionExecutorHandler executorHandler = town.getActionExecutorHandler();
        //获取所有食物总量，食物总量不足的话不供应食物
//        for (ItemResourceType foodType : foodTypes) {
            foodAmounts.put(RESIDENT_FOOD_LEVEL, TownResourceActions.get(executorHandler, RESIDENT_FOOD_LEVEL));
            totalFoods += foodAmounts.get(RESIDENT_FOOD_LEVEL);
//        }
        //不供应食物会导致居民属性降低
        if (residentNum * 20 > totalFoods) {
            if(town instanceof ITownWithResidents residentTown){
                for(UUID uuid : residentsUUID){
                    Optional<Resident> resident = residentTown.getResident(uuid);
                    if(resident.isPresent()){
                        Resident r = resident.get();
                        r.costHealth(10);
                        r.costMental(10);
                        r.costStrength(5);
                    } else {
                        throw new IllegalArgumentException("HouseWorker ERROR: Can't find resident in town :" + town + " \nResident uuid:" + uuid);
                    }
                }
            }
            return false;
        }

        double toCost = residentNum * 20;//1单位食物=半格鸡腿，这里假设一天吃一整条饥饿条，20单位食物
        /*foodAmounts.clear(); // 清空这个Map的内容，之后当做costedFoods来使用*/
        List<ItemResourceType> availableFoodTypes = new ArrayList<>(List.of(RESIDENT_FOOD_LEVEL));
//        double avgLevel = 0;
        double nutrition_Average = 0;
        while(toCost > 0.001){
            ItemResourceType toRemove = null;
            for (ItemResourceType foodType : availableFoodTypes) {
                TownResourceActions.TownResourceTypeCostAction costTypeAction = new TownResourceActions.TownResourceTypeCostAction
                        (foodType, toCost / residentNum, 0, 4, ResourceActionMode.MAXIMIZE, ResourceActionOrder.DESCENDING);//优先消耗高质量食物
                TownResourceActionResults.TownResourceTypeCostActionResult result = (TownResourceActionResults.TownResourceTypeCostActionResult) executorHandler.execute(costTypeAction);
                toCost -= result.totalModifiedAmount();

                for (ITownResourceAttributeActionResult detail : result.details()) {

                    if (detail instanceof TownResourceActionResults.ItemResourceAttributeCostActionResult itemResult) {
                        Map<ItemStackResourceKey, Double> itemDetails = itemResult.details();
                        //遍历具体的物品营养
                        for (Map.Entry<ItemStackResourceKey, Double> entry : itemDetails.entrySet()) {
                            ItemStackResourceKey key = entry.getKey();
                            Double amount = entry.getValue(); // 获取消耗数量

                            for(NutritionRecipe recipe : CUtils.filterRecipes(CDistHelper.getRecipeManager(), NutritionRecipe.TYPE)){
                                if (recipe.conform(key.itemStack)) {
                                    nutrition_Average += (recipe.getNutrition().getNutritionValue() / 4.0) * amount;
                                }
                            }
                        }
                    } else {
                        // 如果不是，可能是其他的资源类型结果
                    }
                }


                /*foodAmounts.merge(foodType, result.totalModifiedAmount(), Double::sum);*/
//                avgLevel += result.getAverageLevel() * result.totalModifiedAmount();//先加等级乘数量，后面再除以数量
                if(!result.allCosted()){
                    toRemove = foodType;
                }
            }
            availableFoodTypes.remove(toRemove);
        }

        if(town instanceof ITownWithResidents residentTown){
            double houseComprehensiveRating = workData.getRating();
            double temperatureRating = workData.temperatureRating;
            double decorationRating = workData.decorationRating;
            double spaceRating = workData.spaceRating;
//            avgLevel /= residentNum * 20;//计算平均食物等级
            nutrition_Average /= residentNum * 20; //计算平均营养价值

            double deviation = (nutrition_Average - 10000) / 10000; // 相对偏差（-1到+1）
            double balanceScore = Math.exp(-3.0 * deviation * deviation); // 10用来控制曲线宽度
            double levelScore = 1; //目前不采用食物等级，只是用来控制消耗食物优先级


            for(UUID uuid : residentsUUID){
                if(residentTown.getResident(uuid).isPresent()){
                    Resident r = residentTown.getResident(uuid).get();

//                    foodAmounts.replaceAll((type, amount) -> amount / residentNum);//避免居民数量影响方差计算结果
//                    double levelScore = 1 + Math.log(1 + avgLevel);/*即使食物等级为0，也加属性*/
/*                    double balanceScore = foodAmounts.values().stream()
                            .mapToDouble(amount ->
                                    Math.pow(amount - residentNum * foodTypes.length,显然residentNum * foodTypes.length等于平均值 2))
                            .average()
                            .orElse(0);
                    balanceScore = Math.exp( - 0.2 * balanceScore);*/    //已改为营养值控制

                    r.addHealth( 2.5 * temperatureRating * levelScore * balanceScore * (100 - r.getHealth())/ 100);
                    r.addMental( houseComprehensiveRating * Math.pow(levelScore, 2.0) * balanceScore * (100 - r.getMental())/ 100);
                    r.addStrength( 0.2 * balanceScore * Math.exp( - 0.1 * r.getStrength()));
                }else {
                    throw new IllegalArgumentException("HouseWorker ERROR: Can't find resident in town :" + town + " \nResident uuid:" + uuid);
                }
            }
        } else {
            FHMain.LOGGER.error("HouseWorker ERROR: Town is not a ITownWithResidents :{}", town);
        }

        return true;
    }
	@Override
	public WorkerState createState() {
		return new HouseState();
	}

}
