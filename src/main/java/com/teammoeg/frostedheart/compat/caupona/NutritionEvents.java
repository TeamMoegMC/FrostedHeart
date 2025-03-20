package com.teammoeg.frostedheart.compat.caupona;

import java.util.List;
import java.util.Optional;
import com.teammoeg.caupona.CPConfig;
import com.teammoeg.caupona.api.CauponaHooks;
import com.teammoeg.caupona.data.recipes.FluidFoodValueRecipe;
import com.teammoeg.caupona.data.recipes.FoodValueRecipe;
import com.teammoeg.caupona.util.FloatemStack;
import com.teammoeg.caupona.util.IFoodInfo;
import com.teammoeg.caupona.util.StewInfo;
import com.teammoeg.frostedheart.content.health.capability.MutableNutrition;
import com.teammoeg.frostedheart.content.health.event.GatherFoodNutritionEvent;

import net.minecraft.world.item.ItemStack;

public class NutritionEvents {
	public static void gatherNutritionFromSoup(GatherFoodNutritionEvent event) {
		Optional<IFoodInfo> opt = CauponaHooks.getInfo(event.getStack());
		if (opt.isPresent()) {
			IFoodInfo ois = opt.get();
			List<FloatemStack> is = ois.getStacks();
			MutableNutrition groups = event.getForModify();
			float b = (float) (double) CPConfig.SERVER.benefitialMod.get();
			for (FloatemStack sx : is) {
				FoodValueRecipe fvr = null;
				if (FoodValueRecipe.recipes != null)
					fvr = FoodValueRecipe.recipes.get(sx.getItem());
				ItemStack stack;
				int heal;
				if (fvr == null || fvr.getRepersent() == null) {
					stack = sx.getStack();
					heal = stack.getFoodProperties(event.getConsumer()).getNutrition();
				} else {
					stack = fvr.getRepersent();
					heal = fvr.heal;
				}
				groups.addScaled(event.queryNutrition(stack), sx.getCount() * heal);
			}
			if (ois instanceof StewInfo si) {
				FluidFoodValueRecipe ffvr = null;
				if (FluidFoodValueRecipe.recipes != null)
					ffvr = FluidFoodValueRecipe.recipes.get(si.base);
				if (ffvr != null && ffvr.getRepersent() != null) {
					groups.addScaled(event.queryNutrition(ffvr.getRepersent()), ffvr.heal);
				}
			}
			if (ois.getHealing()!=0)
				groups.scale(1 / ois.getHealing()*b);
		}
	}
}
