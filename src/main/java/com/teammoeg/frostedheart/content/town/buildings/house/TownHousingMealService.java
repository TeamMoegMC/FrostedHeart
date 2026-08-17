/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.house;

import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownCareLaw;
import com.teammoeg.frostedheart.content.town.TownHousingPlan;
import com.teammoeg.frostedheart.content.town.model.TownFoodAllocationModel;
import com.teammoeg.frostedheart.content.town.model.TownResidentCareModel;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceAttribute;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executes one centralized, resident-specific town housing meal. */
public final class TownHousingMealService {
    private static final int MEAL_CHUNKS = 8;

    private TownHousingMealService() {
    }

    /** Nutrition decays before housing triage so current deficiencies affect assignment. */
    public static void decayNutrition(Iterable<Resident> residents) {
        for (Resident resident : residents) {
            resident.setNutrition(resident.getNutrition().decay(
                    FHConfig.SERVER.TOWN.HOUSING.residentNutritionReserveLossPerDay.get()));
        }
    }

    public static Settlement settle(
            TeamTown town,
            TownHousingPlan housingPlan,
            TownCareLaw law
    ) {
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        double ration = Math.max(0.0, config.foodConsumptionPerResidentDay.get());
        TeamTownResourceHolder holder = town.getResourceHolder();

        Map<Resident, TownResidentCareModel.Need> needs = assessResidents(town);
        Comparator<Resident> careOrder = Comparator.comparing(
                needs::get, TownResidentCareModel.comparator(law));
        List<TownFoodAllocationModel.Household<Resident, HouseBuilding>> households =
                households(town, housingPlan, careOrder);
        TownFoodAllocationModel.Plan<Resident> rationPlan = TownFoodAllocationModel.plan(
                households,
                holder.get(ItemResourceType.RESIDENT_FOOD_LEVEL),
                ration,
                residents -> residents.stream().sorted(careOrder).toList());

        List<NutritionRecipe> recipes = CUtils.filterRecipes(
                CDistHelper.getRecipeManager(), NutritionRecipe.TYPE);
        List<FoodCandidate> candidates = foodCandidates(holder, recipes);
        Map<Resident, Meal> meals = new LinkedHashMap<>();
        for (Resident resident : recipientOrder(households, rationPlan, careOrder)) {
            double allowance = rationPlan.allocations().getOrDefault(resident, 0.0);
            meals.put(resident, consumeMeal(
                    town, resident, allowance, ration, candidates));
        }

        Map<BlockPos, HouseTotals> totals = new HashMap<>();
        int fullyFed = 0;
        int fulfilledGuarantees = 0;
        for (TownFoodAllocationModel.Household<Resident, HouseBuilding> household : households) {
            HouseBuilding house = household.house();
            for (Resident resident : household.residents()) {
                Meal meal = meals.getOrDefault(resident, Meal.EMPTY);
                ResidentNutrition updated = resident.getNutrition().withMeal(
                        meal.nutrition(),
                        ration * config.nutritionReferencePerFoodUnit.get(),
                        config.residentNutritionGainAtReference.get(),
                        config.residentNutritionMaximumCoverage.get());
                resident.setNutrition(updated);
                double satisfaction = ration <= 0.0 ? 1.0
                        : Math.max(0.0, Math.min(1.0, meal.foodUnits() / ration));
                house.settleResident(resident, satisfaction, updated);
                if (satisfaction >= 1.0 - TeamTownResourceHolder.DELTA) fullyFed++;
                if (rationPlan.guaranteedResidents().contains(resident)
                        && satisfaction >= 1.0 - TeamTownResourceHolder.DELTA) {
                    fulfilledGuarantees++;
                }
                totals.computeIfAbsent(house.getPos(), ignored -> new HouseTotals())
                        .add(meal);
            }
        }
        for (TownFoodAllocationModel.Household<Resident, HouseBuilding> household : households) {
            HouseTotals total = totals.getOrDefault(household.house().getPos(), new HouseTotals());
            household.house().updateCentralDailyReport(
                    household.residents().size(), total.food, total.scalarNutrition);
        }
        town.getTownBuildings().values().stream()
                .filter(value -> value instanceof HouseBuilding)
                .map(value -> (HouseBuilding) value)
                .filter(house -> totals.get(house.getPos()) == null)
                .forEach(house -> house.updateCentralDailyReport(0, 0.0, 0.0));

        return new Settlement(
                rationPlan.guaranteedResidents().size(), fulfilledGuarantees,
                town.getAllResidents().size(), fullyFed, rationPlan.allocatedFood());
    }

    private static Map<Resident, TownResidentCareModel.Need> assessResidents(TeamTown town) {
        FHConfig.Server.Town.ResidentRules rules = FHConfig.SERVER.TOWN.RESIDENT_RULES;
        Map<Resident, TownResidentCareModel.Need> result = new HashMap<>();
        for (Resident resident : town.getAllResidents()) {
            result.put(resident, TownResidentCareModel.assess(
                    resident.getUUID(), resident.getAge(), resident.getHealth(),
                    resident.getMental(), resident.getNutrition(),
                    rules.minimumWorkingAge.get(),
                    rules.minimumWorkingHealthExclusive.get(),
                    rules.minimumWorkingMentalExclusive.get(),
                    rules.removalHealthThreshold.get(),
                    rules.removalMentalThreshold.get()));
        }
        return result;
    }

    private static List<TownFoodAllocationModel.Household<Resident, HouseBuilding>> households(
            TeamTown town,
            TownHousingPlan plan,
            Comparator<Resident> careOrder
    ) {
        List<TownFoodAllocationModel.Household<Resident, HouseBuilding>> result =
                new ArrayList<>();
        for (TownHousingPlan.Entry entry : plan.entries()) {
            if (!(town.getTownBuildings().get(entry.building()) instanceof HouseBuilding house)) {
                continue;
            }
            List<Resident> residents = house.getResidents(town).stream()
                    .sorted(careOrder).toList();
            result.add(new TownFoodAllocationModel.Household<>(
                    house, residents, entry.guaranteedResidents()));
        }
        return result;
    }

    private static List<Resident> recipientOrder(
            List<TownFoodAllocationModel.Household<Resident, HouseBuilding>> households,
            TownFoodAllocationModel.Plan<Resident> plan,
            Comparator<Resident> careOrder
    ) {
        Set<Resident> result = new LinkedHashSet<>();
        for (TownFoodAllocationModel.Household<Resident, HouseBuilding> household : households) {
            household.residents().stream().filter(plan.guaranteedResidents()::contains)
                    .sorted(careOrder).forEach(result::add);
        }
        for (TownFoodAllocationModel.Household<Resident, HouseBuilding> household : households) {
            household.residents().stream().filter(r -> !plan.guaranteedResidents().contains(r))
                    .sorted(careOrder).forEach(result::add);
        }
        return List.copyOf(result);
    }

    private static List<FoodCandidate> foodCandidates(
            TeamTownResourceHolder holder,
            List<NutritionRecipe> recipes
    ) {
        Map<ItemStackResourceKey, FoodCandidate> byItem = new LinkedHashMap<>();
        for (int level = ItemResourceType.RESIDENT_FOOD_LEVEL.getMaxLevel(); level >= 0; level--) {
            int foodLevel = level;
            ItemResourceAttribute attribute = ItemResourceType.RESIDENT_FOOD_LEVEL
                    .generateAttribute(level);
            for (Map.Entry<ItemStackResourceKey, Double> entry
                    : holder.getAllItemsByResourceAttribute(attribute).entrySet()) {
                byItem.computeIfAbsent(entry.getKey(), item -> new FoodCandidate(
                        item, foodLevel, entry.getValue(),
                        TeamTownResourceHolder.getResourceAmount(item, attribute),
                        nutrition(item, recipes), stableKey(item)));
            }
        }
        return new ArrayList<>(byItem.values());
    }

    private static Meal consumeMeal(
            TeamTown town,
            Resident resident,
            double allowance,
            double fullRation,
            List<FoodCandidate> candidates
    ) {
        double remainingFood = Math.max(0.0, allowance);
        double chunkSize = fullRation > 0.0 ? fullRation / MEAL_CHUNKS : remainingFood;
        ResidentNutrition.NutritionIntake intake = ResidentNutrition.NutritionIntake.ZERO;
        double consumed = 0.0;
        while (remainingFood > TeamTownResourceHolder.DELTA) {
            ResidentNutrition.NutritionIntake projectedIntake = intake;
            FoodCandidate selected = candidates.stream()
                    .filter(candidate -> candidate.itemAmount > TeamTownResourceHolder.DELTA)
                    .filter(candidate -> candidate.foodUnitsPerItem > 0.0)
                    .max(Comparator.comparingInt((FoodCandidate candidate) -> candidate.level)
                            .thenComparingDouble(candidate -> foodUtility(
                                    resident, candidate, projectedIntake, fullRation))
                            .thenComparing(candidate -> candidate.stableKey,
                                    Comparator.reverseOrder()))
                    .orElse(null);
            if (selected == null) break;
            double requestedFood = Math.min(remainingFood, Math.max(chunkSize,
                    TeamTownResourceHolder.DELTA));
            double requestedItems = Math.min(selected.itemAmount,
                    requestedFood / selected.foodUnitsPerItem);
            TownResourceActionResults.ItemResourceActionResult result =
                    town.getActionExecutorHandler().execute(
                            new TownResourceActions.ItemResourceAction(
                                    selected.item.toItemStack(), ResourceActionType.COST,
                                    requestedItems, ResourceActionMode.MAXIMIZE));
            double usedItems = Math.max(0.0, result.modifiedAmount());
            if (usedItems <= TeamTownResourceHolder.DELTA) {
                selected.itemAmount = 0.0;
                continue;
            }
            selected.itemAmount -= usedItems;
            double usedFood = usedItems * selected.foodUnitsPerItem;
            consumed += usedFood;
            remainingFood -= usedFood;
            intake = intake.plus(selected.nutrition.scale(usedItems));
        }
        return new Meal(consumed, intake);
    }

    private static double foodUtility(
            Resident resident,
            FoodCandidate candidate,
            ResidentNutrition.NutritionIntake projected,
            double fullRation
    ) {
        ResidentNutrition current = resident.getNutrition();
        double reference = Math.max(1.0,
                FHConfig.SERVER.TOWN.HOUSING.nutritionReferencePerFoodUnit.get());
        double foodUnits = Math.max(TeamTownResourceHolder.DELTA,
                candidate.foodUnitsPerItem);
        ResidentNutrition.NutritionIntake perFood = candidate.nutrition.scale(1.0 / foodUnits);
        double healthNeed = 1.0 - resident.getHealth() / 100.0;
        double mentalNeed = 1.0 - resident.getMental() / 100.0;
        double fatNeed = channelNeed(current.fat(), projected.fat(), reference, fullRation);
        double carbohydrateNeed = channelNeed(
                current.carbohydrate(), projected.carbohydrate(), reference, fullRation);
        double proteinNeed = channelNeed(
                current.protein(), projected.protein(), reference, fullRation);
        double vegetableNeed = channelNeed(
                current.vegetable(), projected.vegetable(), reference, fullRation);
        double growthFat = resident.getAge() == Resident.AGE_ELDER ? 0.0 : fatNeed;
        double growthProtein = resident.getAge() <= Resident.AGE_CHILD ? proteinNeed : 0.0;
        return perFood.fat() * (fatNeed + 0.5 * growthFat)
                + perFood.carbohydrate() * (carbohydrateNeed + mentalNeed)
                + perFood.protein() * (proteinNeed + 0.5 * growthProtein)
                + perFood.vegetable() * (vegetableNeed + healthNeed);
    }

    private static double channelNeed(
            double reserve,
            double projectedIntake,
            double reference,
            double fullRation
    ) {
        double projectedGain = FHConfig.SERVER.TOWN.HOUSING
                .residentNutritionGainAtReference.get() * projectedIntake
                / Math.max(reference * Math.max(fullRation, TeamTownResourceHolder.DELTA), 1.0);
        return Math.max(0.0, ResidentNutrition.HEALTHY - reserve - projectedGain)
                / ResidentNutrition.HEALTHY;
    }

    private static ResidentNutrition.NutritionIntake nutrition(
            ItemStackResourceKey item,
            List<NutritionRecipe> recipes
    ) {
        double fat = 0.0;
        double carbohydrate = 0.0;
        double protein = 0.0;
        double vegetable = 0.0;
        for (NutritionRecipe recipe : recipes) {
            if (recipe.conform(item.getItem())) {
                fat += recipe.fat;
                carbohydrate += recipe.carbohydrate;
                protein += recipe.protein;
                vegetable += recipe.vegetable;
            }
        }
        return new ResidentNutrition.NutritionIntake(fat, carbohydrate, protein, vegetable);
    }

    private static String stableKey(ItemStackResourceKey item) {
        String tag = item.getCompoundTag() == null ? "" : item.getCompoundTag().toString();
        return BuiltInRegistries.ITEM.getKey(item.getItem()) + "|" + tag;
    }

    private static final class FoodCandidate {
        private final ItemStackResourceKey item;
        private final int level;
        private double itemAmount;
        private final double foodUnitsPerItem;
        private final ResidentNutrition.NutritionIntake nutrition;
        private final String stableKey;

        private FoodCandidate(
                ItemStackResourceKey item,
                int level,
                double itemAmount,
                double foodUnitsPerItem,
                ResidentNutrition.NutritionIntake nutrition,
                String stableKey
        ) {
            this.item = item;
            this.level = level;
            this.itemAmount = itemAmount;
            this.foodUnitsPerItem = foodUnitsPerItem;
            this.nutrition = nutrition;
            this.stableKey = stableKey;
        }
    }

    private record Meal(
            double foodUnits,
            ResidentNutrition.NutritionIntake nutrition
    ) {
        private static final Meal EMPTY = new Meal(
                0.0, ResidentNutrition.NutritionIntake.ZERO);
    }

    private static final class HouseTotals {
        private double food;
        private double scalarNutrition;

        private void add(Meal meal) {
            food += meal.foodUnits();
            ResidentNutrition.NutritionIntake value = meal.nutrition();
            scalarNutrition += (value.fat() + value.carbohydrate()
                    + value.protein() + value.vegetable()) / 4.0;
        }
    }

    public record Settlement(
            int guaranteedResidents,
            int fulfilledGuarantees,
            int residentCount,
            int fullyFedResidents,
            double foodConsumed
    ) {
    }
}
