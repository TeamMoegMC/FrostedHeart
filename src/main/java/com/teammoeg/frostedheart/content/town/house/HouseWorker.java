package com.teammoeg.frostedheart.content.town.house;

import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.ITownWithResidents;
import com.teammoeg.frostedheart.content.town.TownWorker;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

public class HouseWorker implements TownWorker {
    private HouseWorker() {}
    public static final HouseWorker INSTANCE = new HouseWorker();
    @Override
    public boolean work(Town town, CompoundTag workData) {
        //获取所有居民
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
        //获取所有食物总量，食物总量不足的话不供应食物
        for (ItemResourceType foodType : foodTypes) {
            foodAmounts.put(foodType, TownResourceActions.get(executorHandler, foodType));
            totalFoods += foodAmounts.get(foodType);
        }
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

        double toCost = residentNum * 20;//1单位食物=半格鸡腿，这里假设一天吃一整条饥饿条
        foodAmounts.clear(); // 清空这个Map的内容，之后当做costedFoods来使用
        List<ItemResourceType> availableFoodTypes = new ArrayList<>(List.of(foodTypes));
        double avgLevel = 0;
        while(toCost > 0.001){
            ItemResourceType toRemove = null;
            for (ItemResourceType foodType : availableFoodTypes) {
                TownResourceActions.TownResourceTypeCostAction costTypeAction = new TownResourceActions.TownResourceTypeCostAction
                        (foodType, toCost / residentNum, 0, 100, ResourceActionMode.MAXIMIZE, ResourceActionOrder.ASCENDING);
                TownResourceActionResults.TownResourceTypeCostActionResult result = (TownResourceActionResults.TownResourceTypeCostActionResult) executorHandler.execute(costTypeAction);
                toCost -= result.totalModifiedAmount();
                foodAmounts.merge(foodType, result.totalModifiedAmount(), Double::sum);
                avgLevel += result.getAverageLevel() * result.totalModifiedAmount();//先加等级乘数量，后面再除以数量
                if(!result.allCosted()){
                    toRemove = foodType;
                }
            }
            availableFoodTypes.remove(toRemove);
        }

        if(town instanceof ITownWithResidents residentTown){
            avgLevel /= residentNum * 20;//计算平均食物等级
            for(UUID uuid : residentsUUID){
                if(residentTown.getResident(uuid).isPresent()){
                    Resident r = residentTown.getResident(uuid).get();
                    foodAmounts.replaceAll((type, amount) -> amount / residentNum);//避免居民数量影响方差计算结果
                    double levelScore = 1 + Math.log(1 + avgLevel);/*即使食物等级为0，也加属性*/
                    double balanceScore = foodAmounts.values().stream()
                            .mapToDouble(amount -> Math.pow(amount - residentNum * foodTypes.length/*显然residentNum * foodTypes.length等于平均值*/, 2))
                            .average()
                            .orElse(0);
                    balanceScore = Math.exp( - 0.2 * balanceScore);
                    r.addHealth( 2.5 * levelScore * balanceScore * (100 - r.getHealth())/ 100);
                    r.addMental( Math.pow(levelScore, 2.0) * balanceScore * (100 - r.getMental())/ 100);
                    r.addStrength( 0.2 * balanceScore * Math.exp( - 0.1 * r.getStrength()));
                }else {
                    throw new IllegalArgumentException("HouseWorker ERROR: Can't find resident in town :" + town + " \nResident uuid:" + uuid);
                }
            }
        }

        return true;
    }
}
