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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.base.team.SpecialData;
import com.teammoeg.frostedheart.base.team.SpecialDataHolder;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.content.research.data.ResearchVariant;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatProviderEndPoint;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.codec.DiscreteListCodec;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;

public class GeneratorData implements SpecialData{
	public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public int process = 0, processMax = 0;
    public int overdriveLevel = 0;
    public float steamLevel;
    public int steamProcess;
    public int heated,ranged;
    public float power;
    public Fluid fluid;
    public boolean isWorking, isOverdrive, isActive, isBroken;
    public float TLevel,RLevel;
    protected NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);

    public ItemStack currentItem;
    private SpecialDataHolder<? extends SpecialDataHolder> teamData;
    public BlockPos actualPos = BlockPos.ZERO;
    public HeatProviderEndPoint ep=new HeatProviderEndPoint(200);
    public LazyOptional<HeatProviderEndPoint> epcap=LazyOptional.of(()->ep);
    public RegistryKey<World> dimension;

    final float heatChance = .05f;
    
    
    
    public GeneratorData(SpecialDataHolder teamResearchData) {
        teamData = teamResearchData;
    }

    public boolean consumesFuel(World w) {
        if (!currentItem.isEmpty()) {
            if (!inventory.get(OUTPUT_SLOT).isEmpty() && ItemHandlerHelper.canItemStacksStack(inventory.get(OUTPUT_SLOT), currentItem))
                inventory.get(OUTPUT_SLOT).grow(currentItem.getCount());
            else if (inventory.get(OUTPUT_SLOT).isEmpty())
                inventory.set(OUTPUT_SLOT, currentItem);
            currentItem = ItemStack.EMPTY;
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
        if (!isWorking||isBroken)
            return false;
        boolean hasFuel = true;
        overdriveLevel-=5*(teamData.getData(SpecialDataTypes.RESEARCH_DATA).getVariantDouble(ResearchVariant.OVERDRIVE_RECOVER)+1);
        if (isOverdrive) {
            while (process <= 1 && hasFuel) {
                hasFuel = consumesFuel(w);
            }
            overdriveLevel+=20;
            if(overdriveLevel>=this.getMaxOverdrive()) {
            	isBroken=true;
            }
            if (process > 1) {
                process -= 2;
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
    public int getMaxOverdrive() {
    	return 240400;
    }
    public static final Codec<GeneratorData> CODEC=RecordCodecBuilder.create(t->t.group(
    	Codec.INT.fieldOf("process").forGetter(o->o.process),
    	Codec.INT.fieldOf("processMax").forGetter(o->o.processMax),
    	Codec.INT.fieldOf("steamProcess").forGetter(o->o.steamProcess),
    	Codec.INT.fieldOf("overdriveLevel").forGetter(o->o.overdriveLevel),
    	CodecUtil.<GeneratorData>booleans("flags")
    	.flag("isWorking", o->o.isWorking)
    	.flag("isOverdrive", o->o.isOverdrive)
    	.flag("isActive", o->o.isActive)
    	.flag("isBroken", o->o.isBroken).build(),
    	Codec.FLOAT.fieldOf("steamLevel").forGetter(o->o.steamLevel),
    	Codec.FLOAT.fieldOf("powerLevel").forGetter(o->o.power),
    	CodecUtil.defaultValue(Codec.INT,0).fieldOf("heated").forGetter(o->o.heated),
    	CodecUtil.defaultValue(Codec.INT,0).fieldOf("ranged").forGetter(o->o.ranged),
    	CodecUtil.registryCodec(()->Registry.FLUID).optionalFieldOf("steamFluid").forGetter(o->Optional.ofNullable(o.fluid)),
    	Codec.FLOAT.fieldOf("tempLevel").forGetter(o->o.TLevel),
    	Codec.FLOAT.fieldOf("rangeLevel").forGetter(o->o.RLevel),
    	CodecUtil.path(new DiscreteListCodec<>(CodecUtil.ITEMSTACK_CODEC,ItemStack::isEmpty,()->ItemStack.EMPTY,"Slot"),"inv","Items").forGetter(o->o.inventory),
    	CodecUtil.defaultValue(CodecUtil.ITEMSTACK_CODEC, ItemStack.EMPTY).fieldOf("res").forGetter(o->o.currentItem),
    	CodecUtil.BLOCKPOS.fieldOf("actualPos").forGetter(o->o.actualPos),
    	ResourceLocation.CODEC.optionalFieldOf("dim").forGetter(o->o.dimension==null?Optional.empty():Optional.of(o.dimension.getLocation()))
    	).apply(t,GeneratorData::new));
    
	public GeneratorData(int process, int processMax, int steamProcess, int overdriveLevel, boolean[] flags, float steamLevel, float power, int heated, int ranged, Optional<Fluid> fluid, float tLevel, float rLevel, List<ItemStack> inventory, ItemStack currentItem, BlockPos actualPos, Optional<ResourceLocation> dimension) {
		super();
		this.process = process;
		this.processMax = processMax;
		this.overdriveLevel = overdriveLevel;
		this.steamLevel = steamLevel;
		this.steamProcess = steamProcess;
		this.heated = heated;
		this.ranged = ranged;
		this.power = power;
		this.fluid = fluid.orElse(null);
		this.isWorking = flags[0];
		this.isOverdrive = flags[1];
		this.isActive = flags[2];
		this.isBroken = flags[3];
		this.TLevel = tLevel;
		this.RLevel = rLevel;
		for(int i=0;i<inventory.size();i++)
			this.inventory.set(i, inventory.get(i));
		this.currentItem = currentItem;
		this.actualPos = actualPos;
		this.dimension = dimension.map(t->RegistryKey.getOrCreateKey(Registry.WORLD_KEY, t)).orElse(null);
	}
	@Override
	public void setHolder(SpecialDataHolder holder) {
		this.teamData=holder;
	}
}
