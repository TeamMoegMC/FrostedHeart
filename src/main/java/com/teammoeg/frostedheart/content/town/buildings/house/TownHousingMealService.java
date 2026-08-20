/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.house;

import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionProfile;
import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionResolver;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownCareLaw;
import com.teammoeg.frostedheart.content.town.TownHousingPlan;
import com.teammoeg.frostedheart.content.town.model.TownFoodAllocationModel;
import com.teammoeg.frostedheart.content.town.model.TownResidentCareModel;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;
import com.teammoeg.frostedheart.content.town.resident.ResidentPublicMenuModel;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceAttribute;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executes centralized ration allocation followed by priority-ordered house menus. */
public final class TownHousingMealService {
    private TownHousingMealService() {
    }

    /** Nutrition decays before housing triage so current deficiencies affect assignment. */
    public static void decayNutrition(Iterable<Resident> residents) {
        for (Resident resident : residents) {
            resident.setNutrition(resident.getNutrition().decay(
                    FHConfig.SERVER.TOWN.HOUSING.residentNutritionReserveLossPerDay.get(),
                    FHConfig.SERVER.TOWN.HOUSING.residentNutritionMaximumReserve.get()));
        }
    }

    public static Settlement settle(
            ServerLevel world,
            TeamTown town,
            TownHousingPlan housingPlan,
            TownCareLaw law,
            long settlementDay
    ) {
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        double ration = Math.max(0.0, config.foodConsumptionPerResidentDay.get());
        TeamTownResourceHolder holder = town.getResourceHolder();

        Map<Resident, TownResidentCareModel.Need> needs = assessResidents(town);
        Comparator<Resident> careOrder = Comparator.comparing(
                needs::get, TownResidentCareModel.comparator(
                        law, FHConfig.SERVER.TOWN.RESIDENT_RULES.residentialCareScoreBand.get()));
        List<TownFoodAllocationModel.Household<Resident, HouseBuilding>> households =
                households(town, housingPlan, careOrder);
        TownFoodAllocationModel.Plan<Resident> rationPlan = TownFoodAllocationModel.plan(
                households,
                holder.get(ItemResourceType.RESIDENT_FOOD_LEVEL),
                ration,
                residents -> residents.stream().sorted(careOrder).toList());

        List<FoodCandidate> candidates = foodCandidates(holder, world);
        List<ResidentPublicMenuModel.Group<HouseBuilding>> menuGroups = households.stream()
                .map(household -> menuGroup(household, rationPlan.allocations()))
                .toList();
        List<ResidentPublicMenuModel.Candidate<ItemStackResourceKey>> modelCandidates =
                candidates.stream().map(FoodCandidate::asModelCandidate).toList();
        List<ResidentPublicMenuModel.GroupPlan<HouseBuilding, ItemStackResourceKey>> plannedMenus =
                ResidentPublicMenuModel.planInPriorityOrder(
                        menuGroups, config.residentNutritionMealSelectionChunks.get(),
                        modelCandidates, menuParameters(config));

        Map<HouseBuilding, Menu> houseMenus = new LinkedHashMap<>();
        Map<Resident, Meal> meals = new LinkedHashMap<>();
        Map<HouseBuilding, TownFoodAllocationModel.Household<Resident, HouseBuilding>> byHouse =
                new HashMap<>();
        households.forEach(household -> byHouse.put(household.house(), household));
        for (ResidentPublicMenuModel.GroupPlan<HouseBuilding, ItemStackResourceKey> planned
                : plannedMenus) {
            HouseBuilding house = planned.key();
            TownFoodAllocationModel.Household<Resident, HouseBuilding> household = byHouse.get(house);
            if (household == null) continue;
            double allocatedFood = allocatedFood(household, rationPlan.allocations());
            Menu menu = consumeHouseMenu(
                    town, planned.plan(), candidates, settlementDay);
            houseMenus.put(house, menu);
            for (Resident resident : household.residents()) {
                double allowance = rationPlan.allocations().getOrDefault(resident, 0.0);
                double share = allocatedFood <= TeamTownResourceHolder.DELTA
                        ? 0.0 : allowance / allocatedFood;
                meals.put(resident, new Meal(
                        menu.foodUnits() * share, menu.nutrition().scale(share)));
            }
        }

        Set<Resident> settledResidents = new LinkedHashSet<>();
        int fullyFed = 0;
        int fulfilledGuarantees = 0;
        for (TownFoodAllocationModel.Household<Resident, HouseBuilding> household : households) {
            HouseBuilding house = household.house();
            for (Resident resident : household.residents()) {
                Meal meal = meals.getOrDefault(resident, Meal.EMPTY);
                ResidentNutrition updated = resident.getNutrition().withMeal(
                        meal.nutrition(),
					config.residentNutritionReferencePoints.get(),
                        config.residentNutritionGainAtReference.get(),
                        config.residentNutritionMaximumCoverage.get(),
                        config.residentNutritionMaximumReserve.get());
                resident.completeNutritionMeal(updated);
                settledResidents.add(resident);
                double satisfaction = ration <= 0.0 ? 1.0
                        : Math.max(0.0, Math.min(1.0, meal.foodUnits() / ration));
                house.settleResident(resident, satisfaction, updated);
                if (satisfaction >= 1.0 - TeamTownResourceHolder.DELTA) fullyFed++;
                if (rationPlan.guaranteedResidents().contains(resident)
                        && satisfaction >= 1.0 - TeamTownResourceHolder.DELTA) {
                    fulfilledGuarantees++;
                }
            }
            Menu menu = houseMenus.getOrDefault(house, Menu.empty(settlementDay));
            house.updateCentralDailyReport(
                    household.residents().size(), menu.foodUnits(), menu.dailyMeal());
        }
        for (Resident resident : town.getAllResidents()) {
            if (!settledResidents.contains(resident)) {
                resident.completeNutritionMeal(resident.getNutrition());
            }
        }
        town.getTownBuildings().values().stream()
                .filter(value -> value instanceof HouseBuilding)
                .map(value -> (HouseBuilding) value)
                .filter(house -> !houseMenus.containsKey(house))
                .forEach(house -> house.updateCentralDailyReport(
                        0, 0.0, HouseBuilding.DailyMeal.settled(settlementDay, Map.of())));

        double consumedFood = houseMenus.values().stream()
                .mapToDouble(Menu::foodUnits).sum();

        return new Settlement(
                rationPlan.guaranteedResidents().size(), fulfilledGuarantees,
                town.getAllResidents().size(), fullyFed, consumedFood);
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
                    rules.removalMentalThreshold.get(),
                    FHConfig.SERVER.TOWN.HOUSING.residentNutritionHealthyReserve.get(),
                    FHConfig.SERVER.TOWN.HOUSING.residentNutritionSevereReserve.get()));
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

    private static ResidentPublicMenuModel.Group<HouseBuilding> menuGroup(
            TownFoodAllocationModel.Household<Resident, HouseBuilding> household,
            Map<Resident, Double> allocations
    ) {
        double allocatedFood = allocatedFood(household, allocations);
        List<ResidentPublicMenuModel.Recipient> recipients = household.residents().stream()
                .map(resident -> new ResidentPublicMenuModel.Recipient(
                        resident.getNutrition(), allocatedFood <= TeamTownResourceHolder.DELTA
                        ? 0.0 : allocations.getOrDefault(resident, 0.0) / allocatedFood))
                .toList();
        return new ResidentPublicMenuModel.Group<>(
                household.house(), allocatedFood, recipients);
    }

    private static double allocatedFood(
            TownFoodAllocationModel.Household<Resident, HouseBuilding> household,
            Map<Resident, Double> allocations
    ) {
        return household.residents().stream()
                .mapToDouble(resident -> Math.max(
                        0.0, allocations.getOrDefault(resident, 0.0)))
                .sum();
    }

	private static List<FoodCandidate> foodCandidates(
			TeamTownResourceHolder holder,
			ServerLevel world
	) {
        Map<ItemStackResourceKey, FoodCandidate> byItem = new LinkedHashMap<>();
        for (int tier = ItemResourceType.RESIDENT_FOOD_LEVEL.getMaxLevel(); tier >= 0; tier--) {
            int foodLevel = tier;
            ItemResourceAttribute attribute = ItemResourceType.RESIDENT_FOOD_LEVEL
                    .generateAttribute(tier);
            for (Map.Entry<ItemStackResourceKey, Double> entry
                    : holder.getAllItemsByResourceAttribute(attribute).entrySet()) {
				byItem.computeIfAbsent(entry.getKey(), item -> createCandidate(
						item, foodLevel, entry.getValue(),
						TeamTownResourceHolder.getResourceAmount(item, attribute), world));
            }
        }
        return new ArrayList<>(byItem.values());
    }

    private static Menu consumeHouseMenu(
            TeamTown town,
            ResidentPublicMenuModel.Plan<ItemStackResourceKey> plan,
            List<FoodCandidate> candidates,
            long settlementDay
    ) {
        Map<ItemStackResourceKey, FoodCandidate> byItem = new HashMap<>();
        candidates.forEach(candidate -> byItem.put(candidate.item, candidate));
        double consumed = 0.0;
        ResidentNutrition.NutritionIntake intake = ResidentNutrition.NutritionIntake.ZERO;
        Map<ItemStackResourceKey, Double> actualItems = new LinkedHashMap<>();
        for (Map.Entry<ItemStackResourceKey, Double> entry : plan.itemAmounts().entrySet()) {
            FoodCandidate candidate = byItem.get(entry.getKey());
            if (candidate == null) continue;
            TownResourceActionResults.ItemResourceActionResult result =
                    town.getActionExecutorHandler().execute(
                            new TownResourceActions.ItemResourceAction(
                                    candidate.item.toItemStack(), ResourceActionType.COST,
                                    entry.getValue(), ResourceActionMode.MAXIMIZE));
            double usedItems = Math.max(0.0, result.modifiedAmount());
            if (usedItems <= TeamTownResourceHolder.DELTA) continue;
            actualItems.merge(candidate.item, usedItems, Double::sum);
            consumed += usedItems * candidate.foodUnitsPerItem;
            intake = intake.plus(candidate.nutritionPoints.scale(usedItems));
        }
        return new Menu(
                consumed, intake,
                HouseBuilding.DailyMeal.settled(settlementDay, actualItems));
    }

    private static ResidentPublicMenuModel.Parameters menuParameters(
            FHConfig.Server.Town.Housing config
    ) {
        return new ResidentPublicMenuModel.Parameters(
                config.residentNutritionReferencePoints.get(),
                config.residentNutritionGainAtReference.get(),
                config.residentNutritionMaximumCoverage.get(),
                config.residentNutritionMaximumReserve.get(),
                config.residentNutritionHealthyReserve.get());
    }

	private static FoodCandidate createCandidate(
			ItemStackResourceKey item,
			int level,
			double itemAmount,
			double foodUnitsPerItem,
			ServerLevel world
	) {
		ItemStack stack = item.toItemStack();
		FoodNutritionProfile profile = FoodNutritionResolver.resolve(world, stack);
		FoodProperties food = stack.getFoodProperties(null);
		double hunger = food == null ? 0.0 : Math.max(0, food.getNutrition());
		ResidentNutrition.NutritionIntake points =
				ResidentPublicMenuModel.nutritionPoints(profile, (int) hunger);
		return new FoodCandidate(
				item, level, itemAmount, foodUnitsPerItem, points, stableKey(item));
    }

    private static String stableKey(ItemStackResourceKey item) {
        String tag = item.getCompoundTag() == null ? "" : item.getCompoundTag().toString();
        return BuiltInRegistries.ITEM.getKey(item.getItem()) + "|" + tag;
    }

    private static final class FoodCandidate {
        private final ItemStackResourceKey item;
        private final int level;
        private final double itemAmount;
        private final double foodUnitsPerItem;
		private final ResidentNutrition.NutritionIntake nutritionPoints;
        private final String stableKey;

        private FoodCandidate(
                ItemStackResourceKey item,
                int level,
                double itemAmount,
                double foodUnitsPerItem,
				ResidentNutrition.NutritionIntake nutritionPoints,
                String stableKey
        ) {
            this.item = item;
            this.level = level;
            this.itemAmount = itemAmount;
            this.foodUnitsPerItem = foodUnitsPerItem;
			this.nutritionPoints = nutritionPoints;
			this.stableKey = stableKey;
		}

        private ResidentPublicMenuModel.Candidate<ItemStackResourceKey> asModelCandidate() {
            return new ResidentPublicMenuModel.Candidate<>(
                    item, stableKey, level, itemAmount, foodUnitsPerItem, nutritionPoints);
        }

    }

	private record Meal(
            double foodUnits,
            ResidentNutrition.NutritionIntake nutrition
    ) {
        private static final Meal EMPTY = new Meal(
                0.0, ResidentNutrition.NutritionIntake.ZERO);
	}

	private record Menu(
			double foodUnits,
			ResidentNutrition.NutritionIntake nutrition,
            HouseBuilding.DailyMeal dailyMeal
	) {
        private static Menu empty(long settlementDay) {
            return new Menu(
                    0.0, ResidentNutrition.NutritionIntake.ZERO,
                    HouseBuilding.DailyMeal.settled(settlementDay, Map.of()));
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
