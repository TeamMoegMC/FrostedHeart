package com.teammoeg.frostedheart.town;

import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

public class GeneratorData {
	public int process = 0;
	public int processMax = 0;
	public int temperatureLevel;
	public int rangeLevel;
	public int overdriveLevel;
	public boolean isUserOperated;
	public boolean isWorking;
	public boolean isOverdrive;
	public boolean isActive;
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	protected NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
	public ItemStack currentItem;
	private TeamResearchData teamData;
	public BlockPos actualPos=BlockPos.ZERO;
	public RegistryKey<World> dimension;

	public NonNullList<ItemStack> getInventory() {
		return inventory;
	}

	public GeneratorData(TeamResearchData teamResearchData) {
		teamData=teamResearchData;
	}

	public int getSlotLimit(int slot) {
		return 64;
	}

	public GeneratorRecipe getRecipe() {
		if (inventory.get(INPUT_SLOT).isEmpty())
			return null;
		GeneratorRecipe recipe = GeneratorRecipe.findRecipe(inventory.get(INPUT_SLOT));
		if (recipe == null)
			return null;
		if (inventory.get(OUTPUT_SLOT).isEmpty() || (ItemStack.areItemsEqual(inventory.get(OUTPUT_SLOT), recipe.output)
				&& inventory.get(OUTPUT_SLOT).getCount() + recipe.output.getCount() <= getSlotLimit(OUTPUT_SLOT))) {
			return recipe;
		}
		return null;
	}

	public boolean consumesFuel() {
		if (currentItem != null) {
			if (!inventory.get(OUTPUT_SLOT).isEmpty()&&ItemHandlerHelper.canItemStacksStack(inventory.get(OUTPUT_SLOT), currentItem))
				inventory.get(OUTPUT_SLOT).grow(currentItem.getCount());
			else if(inventory.get(OUTPUT_SLOT).isEmpty())
				inventory.set(OUTPUT_SLOT, currentItem);
			currentItem = null;
		}
		GeneratorRecipe recipe = getRecipe();
		if (recipe != null) {
			int count = recipe.input.getCount();
			Utils.modifyInvStackSize(inventory, INPUT_SLOT, -count);
			currentItem = recipe.output.copy();
			currentItem.setCount(currentItem.getCount());
			double effi = getEfficiency();
			this.process = (int) (recipe.time * effi);
			this.processMax = process;
			return true;
		} else {
			if (this.processMax != 0) {
				this.process = 0;
				processMax = 0;
			}
		}
		return false;
	}

	public boolean tickFuelProcess() {
		if(!isWorking)
			return false;
		boolean hasFuel=true;
		if (isOverdrive) {
			while(process <= 3&&hasFuel) {
				hasFuel=consumesFuel();
			} 
			if(process>3){
				process-=4;
				return true;
			}
		}else {
			while(process <= 0&&hasFuel) {
				hasFuel=consumesFuel();
			} 
			if (process > 0) {
				process--;
				return true;
			}
		}
		return false;
	}

	protected double getEfficiency() {
		return teamData.getVariantDouble(ResearchVariant.GENERATOR_EFFICIENCY)+0.7;
	}

	public void tick() {
		isActive=tickFuelProcess();
    }
}
