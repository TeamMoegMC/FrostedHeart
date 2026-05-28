/*
 * Copyright (c) 2026 TeamMoeg
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

import java.util.*;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import net.minecraft.core.BlockPos;

import static com.teammoeg.frostedheart.content.town.ITown.DEBUG_MODE;
import static com.teammoeg.frostedheart.content.town.resource.ItemResourceType.RESIDENT_FOOD_LEVEL;

/**
 * 城镇住宅。
 * 它不继承AbstractTownResidentWorkBuilding，因为那个类用于需要居民参与工作的城镇建筑，但居民在房屋中并非工作。
 */
public class HouseBuilding extends AbstractTownBuilding implements ITownResidentBuilding {

    public static final Codec<HouseBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
            BlockPos.CODEC.optionalFieldOf("pos",BlockPos.ZERO).forGetter(o -> o.pos),
            Codec.BOOL.optionalFieldOf("isStructureValid",false).forGetter(o -> o.isStructureValid),
            OccupiedVolume.CODEC.optionalFieldOf("occupiedVolume",OccupiedVolume.EMPTY).forGetter(o -> o.occupiedVolume),
            Codec.INT.optionalFieldOf("area",0).forGetter(o -> o.area),
            Codec.INT.optionalFieldOf("volume",0).forGetter(o -> o.volume),
            Codec.DOUBLE.optionalFieldOf("temperature",0D).forGetter(o -> o.temperature),
            Codec.DOUBLE.optionalFieldOf("decorationRating",0D).forGetter(o -> o.decorationRating),
            Codec.INT.optionalFieldOf("maxResident",0).forGetter(o -> o.maxResidents),
            Codec.DOUBLE.optionalFieldOf("temperatureModifier",0D).forGetter(o -> o.temperatureModifier))
            .apply(t, HouseBuilding::new));

    private final Set<UUID> residentsUUID = new HashSet<>();

    /**
     * 住宅结构是否有效
     */
    boolean isStructureValid;
    /**
     * 住宅的有效面积
     */
    public int area;
    /**
     * 住宅的体积
     */
    public int volume;
    /**
     * 住宅内部平均温度
     */
    public double temperature;
    /**
     * 住宅内部装饰物的综合评分，取值范围为0-1
     */
    public double decorationRating;
    /**
     * 最大可居住居民数
     */
    public int maxResidents;
    /**
     * 温度修正系数，用于调节房屋内温度效果
     */
    public double temperatureModifier;


    public HouseBuilding(BlockPos pos) {
        super(pos);
    }

    public HouseBuilding(BlockPos pos, boolean isStructureValid, OccupiedVolume occupiedVolume, int area, int volume, double temperature, double decorationRating, int maxResidents, double temperatureModifier) {
        super(pos);
        this.isStructureValid = isStructureValid;
        this.occupiedVolume = occupiedVolume;
        this.area = area;
        this.volume = volume;
        this.temperature = temperature;
        this.decorationRating = decorationRating;
        this.maxResidents = maxResidents;
        this.temperatureModifier = temperatureModifier;
    }

    /**
     * 测试用构造方法，不包含OccupiedVolume
     */
    public HouseBuilding(BlockPos pos, boolean isStructureValid, int area, int volume, double temperature, double decorationRating, int maxResidents, double temperatureModifier) {
        super(pos);
        this.isStructureValid = isStructureValid;
        this.area = area;
        this.volume = volume;
        this.temperature = temperature;
        this.decorationRating = decorationRating;
        this.maxResidents = maxResidents;
        this.temperatureModifier = temperatureModifier;
    }


    public boolean addResident(Resident resident) {
        resident.setHousePos(this.getPos());
        return residentsUUID.add(resident.getUUID());
    }

    public boolean removeResident(Resident resident){
        return residentsUUID.remove(resident.getUUID());
    }

    @Override
    public int getMaxResidents() {
        return maxResidents;
    }

    @Override
    public Collection<UUID> getResidentsID() {
        return residentsUUID;
    }

    @Override
    public Collection<Resident> getResidents(ITownWithResidents townOfBuilding) {
        return residentsUUID.stream()
                .map(townOfBuilding::getResident)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static boolean isTemperatureValid(double effectiveTemperature){
        if (DEBUG_MODE) return true;
        return effectiveTemperature >= TownMathFunctions.MIN_TEMP_HOUSE && effectiveTemperature <= TownMathFunctions.MAX_TEMP_HOUSE;
    }

    public boolean isTemperatureValid(){
        return isTemperatureValid(getEffectiveTemperature());
    }

    public double getRating(){
        return decorationRating * 0.3 + TownMathFunctions.calculateTemperatureRating(temperature) * 0.4 + TownMathFunctions.calculateSpaceRating(volume, area) * 0.3;
    }

    @Override
    public boolean isBuildingWorkable() {
        return super.isBuildingWorkable()
                && isTemperatureValid()
                && area >= 4
                && volume >= 8;
    }


    @Override
    public boolean work(ITownWithBuildings buildingTown) {
        if (!(buildingTown instanceof ITown town)) {
            FHMain.LOGGER.error("HouseBuilding: town is not a complete town!");
            return false;
        }

        Set<UUID> residentsUUID = this.residentsUUID;
        int residentNum = residentsUUID.size();
        if (residentNum == 0) {
            return true; // 没有居民，无需执行任何逻辑
        }

        IActionExecutorHandler executorHandler = town.getActionExecutorHandler();
        double totalFoods = TownResourceActions.get(executorHandler, RESIDENT_FOOD_LEVEL);
        float residentConsumption = 6.5F; // 1单位食物 = 半格鸡腿（1饥饿值）
        double toCost = residentNum * residentConsumption;

        // 食物不足：惩罚所有居民并返回
        if (toCost > totalFoods) {
            double nutritionSum = consumeFoodAndComputeNutrition(executorHandler, totalFoods, residentNum);

            double nutritionAverage = nutritionSum / (residentNum * residentConsumption);
            // 营养折扣：营养越高惩罚越轻，最多减轻 50%
            double nutritionSatisfaction = Math.min(1.0, nutritionAverage / 10000.0); // 满足率 0~1
            double penaltyModifier = 1.0 - nutritionSatisfaction * 0.5;

            double deficitRatio = (toCost - totalFoods) / toCost;// 0~1，缺得越多系数越大
            double healthPenalty  = 10 * deficitRatio * penaltyModifier;
            double mentalPenalty  = 10 * deficitRatio * penaltyModifier;
            double strengthPenalty = 5 * deficitRatio * penaltyModifier;
            punishResidents(town, residentsUUID, healthPenalty, mentalPenalty, strengthPenalty);
        }

        // 消耗食物并累计营养值
        double nutritionSum = consumeFoodAndComputeNutrition(executorHandler, toCost, residentNum);

        // 根据营养和房屋评分提升居民属性
        applyResidentBuffs(town, residentsUUID, nutritionSum, residentNum, residentConsumption);
        return true;
    }

    /**
     * 因食物不足惩罚所有居民（降低健康/精神/力量）
     */
    private void punishResidents(ITown town, Set<UUID> residentsUUID, double healthLoss, double mentalLoss, double strengthLoss) {
        for (UUID uuid : residentsUUID) {
            Resident r = town.getResident(uuid).orElseThrow(() ->
                    new IllegalArgumentException("HouseBuilding ERROR: Can't find resident in town: " + town + ", uuid: " + uuid));
            r.costHealth(healthLoss);
            r.costMental(mentalLoss);
            r.costStrength(strengthLoss);
        }
    }

    /**
     * 执行食物消耗动作，并累计所有消耗物品的营养值。
     * @return 消耗物品的营养值总和
     */
    private double consumeFoodAndComputeNutrition(IActionExecutorHandler executorHandler, double toCost, int residentNum) {
        // 构造消耗动作：期望数量为每位居民的平均消耗量，优先消耗高质量食物
        TownResourceActions.TownResourceTypeCostAction action = new TownResourceActions.TownResourceTypeCostAction(
                RESIDENT_FOOD_LEVEL, toCost / residentNum, 0, 4,
                ResourceActionMode.MAXIMIZE, ResourceActionOrder.DESCENDING);
        TownResourceActionResults.TownResourceTypeCostActionResult result = executorHandler.execute(action);

        double nutritionSum = 0.0;
        for (ITownResourceAttributeActionResult<?> detail : result.details()) {
            if (detail instanceof TownResourceActionResults.ItemResourceAttributeCostActionResult itemResult) {
                for (Map.Entry<ItemStackResourceKey, Double> entry : itemResult.details().entrySet()) {
                    ItemStackResourceKey key = entry.getKey();
                    double amount = entry.getValue();
                    // 查找对应的营养配方并累加营养值
                    for (NutritionRecipe recipe : CUtils.filterRecipes(CDistHelper.getRecipeManager(), NutritionRecipe.TYPE)) {
                        if (recipe.conform(key.getItem())) {
                            nutritionSum += (recipe.getNutrition().getNutritionValue() / 4.0) * amount;
                        }
                    }
                }
            }
        }
        return nutritionSum;
    }

    /**
     * 根据营养均衡度与房屋评分计算居民属性增益并应用。
     */
    private void applyResidentBuffs(ITown town, Set<UUID> residentsUUID, double nutritionSum, int residentNum, float residentConsumption) {
        // 计算平均营养价值
        double nutritionAverage = nutritionSum / (residentNum * residentConsumption);
        double deviation = (nutritionAverage - 10000) / 10000;
        double balanceScore = Math.exp(-3.0 * deviation * deviation);
        double levelScore = 1.0; // 暂不使用食物等级

        double temperatureRating = TownMathFunctions.calculateTemperatureRating(this.temperature);
        double spaceRating = TownMathFunctions.calculateSpaceRating(volume, area);
        double houseComprehensiveRating = (spaceRating * (1 + decorationRating) + temperatureRating) / 3;

        for (UUID uuid : residentsUUID) {
            Resident r = town.getResident(uuid).orElseThrow(() ->
                    new IllegalArgumentException("HouseBuilding ERROR: Can't find resident in town: " + town + ", uuid: " + uuid));

            double healthGain = 5.0 * temperatureRating * levelScore * balanceScore * (100 - r.getHealth()) / 100;
            if (r.getHealth() < 30) {
                healthGain = Math.max(healthGain, 0.5);  // 保底每天至少恢复 0.5 点
            }
            r.addHealth(healthGain);

            double mentalGain = 2.0 * houseComprehensiveRating * Math.pow(levelScore, 2.0) * balanceScore * (100 - r.getMental()) / 100;
            if (r.getMental() < 30) {
                mentalGain = Math.max(mentalGain, 0.5);
            }
            r.addMental(mentalGain);

            double strengthGain = 1.0 * balanceScore * Math.exp(-0.05 * r.getStrength());
            if (r.getStrength() < 30) {
                strengthGain = Math.max(strengthGain, 0.25);
            }
            r.addStrength(strengthGain);
        }
    }

    @Override
    public void onRemoved(ITownWithBuildings buildingTown) {
        if(buildingTown instanceof ITownWithResidents residentTown){
            for(UUID uuid : residentsUUID){
                residentTown.getResident(uuid).ifPresent(resident -> resident.setHousePos(null));
            }
        }
    }

    public double getEffectiveTemperature() {
        return temperature + temperatureModifier;
    }
}
