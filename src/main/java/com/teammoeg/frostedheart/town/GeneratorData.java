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

package com.teammoeg.frostedheart.town;

import java.util.Random;

import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatProviderEndPoint;
import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.RegistryUtils;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;

public class GeneratorData {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public int process = 0;
    public int processMax = 0;
    public int overdriveLevel = 0;
    public float steamLevel;
    public int steamProcess;
    public int heated;
    public float power;
    public Fluid fluid;
    public boolean isWorking;
    public boolean isOverdrive;
    public boolean isActive;
    public float TLevel,RLevel;
    protected NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);

    public ItemStack currentItem;
    private TeamResearchData teamData;
    public BlockPos actualPos = BlockPos.ZERO;
    public HeatProviderEndPoint ep=new HeatProviderEndPoint(200);
    public LazyOptional<HeatProviderEndPoint> epcap=LazyOptional.of(()->ep);
    public RegistryKey<World> dimension;

    final float heatAddInterval = 20;
    public GeneratorData(TeamResearchData teamResearchData) {
        teamData = teamResearchData;
    }

    public boolean consumesFuel(World w) {
        if (currentItem != null) {
            if (!inventory.get(OUTPUT_SLOT).isEmpty() && ItemHandlerHelper.canItemStacksStack(inventory.get(OUTPUT_SLOT), currentItem))
                inventory.get(OUTPUT_SLOT).grow(currentItem.getCount());
            else if (inventory.get(OUTPUT_SLOT).isEmpty())
                inventory.set(OUTPUT_SLOT, currentItem);
            currentItem = null;
        }
        GeneratorRecipe recipe = getRecipe(w);
        if (recipe != null) {
            int count = recipe.input.getCount();
            Utils.modifyInvStackSize(inventory, INPUT_SLOT, -count);
            currentItem = recipe.output.copy();
            currentItem.setCount(currentItem.getCount());
            double effi = getEfficiency();
            this.process = (int) (recipe.time * effi);
            this.processMax = process;
            return true;
        }
        if (this.processMax != 0) {
            this.process = 0;
            processMax = 0;
        }
        return false;
    }
    public CompoundNBT serialize(boolean update) {
        CompoundNBT result = new CompoundNBT();
        result.putInt("process", process);
        result.putInt("processMax", processMax);
        result.putInt("steamProcess", steamProcess);
        result.putInt("overdriveLevel", overdriveLevel);
        result.putBoolean("isWorking", isWorking);
        result.putBoolean("isOverdrive", isOverdrive);
        result.putBoolean("isActive", isActive);
        result.putFloat("steamLevel",steamLevel);
        result.putFloat("powerLevel", power);
        result.putInt("heated", heated);
        result.putFloat("tempLevel", TLevel);
        result.putFloat("rangeLevel",RLevel);
        if (fluid != null)
            result.putString("steamFluid", RegistryUtils.getRegistryName(fluid).toString());
        if (!update) {
            CompoundNBT inv = new CompoundNBT();
            ItemStackHelper.saveAllItems(inv, inventory);
            result.put("inv", inv);
            if (currentItem != null)
                result.put("res", currentItem.serializeNBT());
            result.putLong("actualPos", actualPos.toLong());
            if (dimension != null)
                result.putString("dim", dimension.getLocation().toString());
        }
        return result;
    }
    public void deserialize(CompoundNBT data, boolean update) {
        process = data.getInt("process");
        processMax = data.getInt("processMax");
        steamProcess=data.getInt("steamProcess");
        overdriveLevel = data.getInt("overdriveLevel");
        isWorking = data.getBoolean("isWorking");
        isOverdrive = data.getBoolean("isOverdrive");
        isActive = data.getBoolean("isActive");
        steamLevel = data.getFloat("steamLevel");
        power = data.getFloat("powerLevel");
        heated=data.getInt("heated");
        TLevel=data.getFloat("tempLevel");
        RLevel=data.getFloat("rangeLevel");
        if (data.contains("steamFluid"))
            fluid = RegistryUtils.getFluid(new ResourceLocation(data.getString("steamFluid")));
        else
            fluid = null;
        if (!update) {
            ItemStackHelper.loadAllItems(data.getCompound("inv"), inventory);
            if (data.contains("res"))
                currentItem = ItemStack.read(data.getCompound("res"));
            else
                currentItem = null;
            actualPos = BlockPos.fromLong(data.getLong("actualPos"));
            if (data.contains("dim")) {
                dimension = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(data.getString("dim")));
            }
        }
    }

    protected double getEfficiency() {
        return teamData.getVariantDouble(ResearchVariant.GENERATOR_EFFICIENCY) + 0.7;
    }

    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    public GeneratorRecipe getRecipe(World w) {
        if (inventory.get(INPUT_SLOT).isEmpty())
            return null;
        GeneratorRecipe recipe=null;
        for(GeneratorRecipe recipet:FHUtils.filterRecipes(w.getRecipeManager(),GeneratorRecipe.TYPE))
        	if(recipet.input.test(inventory.get(INPUT_SLOT))) {
        		recipe=recipet;
        		break;
        	}
        if (recipe == null)
            return null;
        if (inventory.get(OUTPUT_SLOT).isEmpty() || (ItemStack.areItemsEqual(inventory.get(OUTPUT_SLOT), recipe.output)
                && inventory.get(OUTPUT_SLOT).getCount() + recipe.output.getCount() <= getSlotLimit(OUTPUT_SLOT))) {
            return recipe;
        }
        return null;
    }

    public int getSlotLimit(int slot) {
        return 64;
    }



    public void tick(World w) {
        isActive = tickFuelProcess(w);
        tickHeatedProcess(w);
        if(isActive&&power>0)
        	ep.setPower((float) (power*getHeatEfficiency()));
    }
    public void tickHeatedProcess(World world) {
    	int heatedMax=getMaxHeated();
        if (isActive&&heated != heatedMax) {
            Random random = world.rand;
            boolean needAdd = false;
            float heatAddProbability = 1F / heatAddInterval;
            if (isOverdrive) {
                heatAddProbability = 2F / heatAddInterval;
            }
            if (random.nextFloat() < heatAddProbability) {
                needAdd = true;
            }
            if (heated < heatedMax && needAdd) {
                heated++;
            } else if (heated > heatedMax && needAdd) {
                heated--;
            }
        } else if (!isActive) {
            if (heated > 0){
                Random random = world.rand;
                float heatAddProbability = 2F / heatAddInterval;
                if (random.nextFloat() < heatAddProbability) {
                    heated--;
                }
            }
        }
        TLevel=(Math.min(heated / 100F,this.getMaxTemperatureLevel()));
        RLevel=(Math.min(heated / 100F,this.getMaxRangeLevel()));

    }
    public boolean tickFuelProcess(World w) {
        if (!isWorking)
            return false;
        boolean hasFuel = true;
        if (isOverdrive) {
            while (process <= 3 && hasFuel) {
                hasFuel = consumesFuel(w);
            }
            if (process > 3) {
                process -= 4;
                return true;
            }
        } else {
            while (process <= 0 && hasFuel) {
                hasFuel = consumesFuel(w);
            }
            if (process > 0) {
                process--;
                return true;
            }
        }
        return false;
    }
    public void onPosChange() {
    	heated=0;
    	TLevel=0;
    	RLevel=0;
    	process=0;
    	processMax=0;
    	steamProcess=0;
    }
    protected double getHeatEfficiency() {

        return 1+teamData.getVariantDouble(ResearchVariant.GENERATOR_HEAT);
    }
	public float getMaxTemperatureLevel() {
		return 1+(isOverdrive?1:0)+steamLevel;
	}
	public float getMaxRangeLevel() {
		return 1+steamLevel;
	}
    public int getMaxHeated() {
        return (int) (100*Math.max(this.getMaxTemperatureLevel(), this.getMaxRangeLevel()));
    }
}
