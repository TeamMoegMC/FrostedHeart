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

package com.teammoeg.frostedheart.content.generator;

import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.data.ResearchVariant;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatProviderEndPoint;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

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

public class GeneratorData implements NBTSerializable{
	public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public int process = 0, processMax = 0;
    public int overdriveLevel = 0;
    public float steamLevel;
    public int steamProcess;
    public int heated,ranged;
    public float power;
    public Fluid fluid;
    public boolean isWorking, isOverdrive, isActive;
    public float TLevel,RLevel;
    protected NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);

    public ItemStack currentItem;
    private TeamDataHolder teamData;
    public BlockPos actualPos = BlockPos.ZERO;
    public HeatProviderEndPoint ep=new HeatProviderEndPoint(200);
    public LazyOptional<HeatProviderEndPoint> epcap=LazyOptional.of(()->ep);
    public RegistryKey<World> dimension;

    final float heatChance = .05f;
    
    
    
    public GeneratorData(TeamDataHolder teamResearchData) {
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

    protected double getEfficiency() {
        return teamData.getData(SpecialDataTypes.RESEARCH_DATA).getVariantDouble(ResearchVariant.GENERATOR_EFFICIENCY) + 0.7;
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
    	int rangedMax=getMaxRanged();
        if (isActive) {
            if(heated != heatedMax) {
	            if (world.rand.nextFloat() < heatChance*(isOverdrive?2:1)) {
	            	if (heated < heatedMax) {
		                heated++;
		            } else {
		                heated--;
		            }
	            }
            } 
            if(ranged != rangedMax) {
	            if (world.rand.nextFloat() < heatChance*(isOverdrive?2:1)) {
	            	if (ranged < rangedMax) {
	 	            	ranged++;
	 	            } else {
	 	            	ranged--;
	 	            }
	            }
            }
        } else {
            if (heated > 0){
                if (world.rand.nextFloat() < heatChance * 2) {
                    heated--;
                }
            }
        }
        TLevel=heated / 100F;
        RLevel=ranged / 100F;

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
    	ranged=0;
    	TLevel=0;
    	RLevel=0;
    	process=0;
    	processMax=0;
    	steamProcess=0;
    }
    protected double getHeatEfficiency() {

        return 1+teamData.getData(SpecialDataTypes.RESEARCH_DATA).getVariantDouble(ResearchVariant.GENERATOR_HEAT);
    }
	public float getMaxTemperatureLevel() {
		return 1+(isOverdrive?1:0)+steamLevel;
	}
	public float getMaxRangeLevel() {
		return 1+steamLevel;
	}
    public int getMaxHeated() {
        return (int) (100*this.getMaxTemperatureLevel());
    }
    public int getMaxRanged() {
        return (int) (100*this.getMaxRangeLevel());
    }
    
	@Override
	public void save(CompoundNBT result, boolean isPacket) {
		// TODO Auto-generated method stub
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
        result.putInt("ranged", ranged);
        result.putFloat("tempLevel", TLevel);
        result.putFloat("rangeLevel",RLevel);
        if (fluid != null)
            result.putString("steamFluid", RegistryUtils.getRegistryName(fluid).toString());
        CompoundNBT inv = new CompoundNBT();
        ItemStackHelper.saveAllItems(inv, inventory);
        result.put("inv", inv);
        if (currentItem != null)
            result.put("res", currentItem.serializeNBT());
        result.putLong("actualPos", actualPos.toLong());
        if (dimension != null)
            result.putString("dim", dimension.getLocation().toString());
	}

	@Override
	public void load(CompoundNBT data, boolean isPacket) {
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
        ranged=data.getInt("ranged");
        TLevel=data.getFloat("tempLevel");
        RLevel=data.getFloat("rangeLevel");
        if (data.contains("steamFluid"))
            fluid = RegistryUtils.getFluid(new ResourceLocation(data.getString("steamFluid")));
        else
            fluid = null;
        
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
