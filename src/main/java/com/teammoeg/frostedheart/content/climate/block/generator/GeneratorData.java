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

package com.teammoeg.frostedheart.content.climate.block.generator;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.capability.capabilities.ItemHandlerWrapper;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.data.ResearchVariant;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Generator data and utility functions.
 * Automatically maintained by the special data system,
 * which is shared by the team.
 * See {@link FHSpecialDataTypes}
 */
public class GeneratorData implements SpecialData {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final Codec<GeneratorData> CODEC = RecordCodecBuilder.create(t -> t.group(
            Codec.INT.fieldOf("process").forGetter(o -> o.process),
            Codec.INT.fieldOf("processMax").forGetter(o -> o.processMax),
           // Codec.INT.fieldOf("steamProcess").forGetter(o -> o.steamProcess),
            Codec.INT.fieldOf("overdriveLevel").forGetter(o -> o.overdriveLevel),
            CodecUtil.<GeneratorData>booleans("flags")
                    .flag("isWorking", o -> o.isWorking)
                    .flag("isOverdrive", o -> o.isOverdrive)
                    .flag("isActive", o -> o.isActive)
                    .flag("isBroken", o -> o.isBroken).build(),
            Codec.FLOAT.fieldOf("steamLevel").forGetter(o -> o.steamLevel),
            Codec.FLOAT.fieldOf("powerLevel").forGetter(o -> o.power),
            Codec.INT.optionalFieldOf("heated",0).forGetter(o -> o.heated),
            Codec.INT.optionalFieldOf("ranged",0).forGetter(o -> o.ranged),
            Codec.FLOAT.fieldOf("tempLevel").forGetter(o -> o.TLevel),
            Codec.FLOAT.fieldOf("rangeLevel").forGetter(o -> o.RLevel),
            CompoundTag.CODEC.fieldOf("items").forGetter(o -> o.inventory.serializeNBT()),
            ItemStack.CODEC.optionalFieldOf("res",ItemStack.EMPTY).forGetter(o -> o.currentItem),
            BlockPos.CODEC.optionalFieldOf("actualPos").forGetter(o -> Optional.ofNullable(o.actualPos)),
            ResourceLocation.CODEC.optionalFieldOf("dim").forGetter(o -> o.dimension == null ? Optional.empty() : Optional.of(o.dimension.location()))
    ).apply(t, GeneratorData::new));
    public final ItemStackHandler inventory = new ItemStackHandler(2) {
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			if(slot==1)return false;
			return isStackValid(slot,stack);
		}
    	
    };
    public final ItemHandlerWrapper invWrapper=new ItemHandlerWrapper(()->inventory) {

		@Override
		public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
			if(slot==OUTPUT_SLOT)
				return stack;
			return super.insertItem(slot, stack, simulate);
		}

		@Override
		public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
			if(slot==INPUT_SLOT)
				return ItemStack.EMPTY;
			return super.extractItem(slot, amount, simulate);
		}
    	
    };
    public final LazyOptional<IItemHandler> invCap = LazyOptional.of(() -> invWrapper);
    final float heatChance = .05f;
    public int process = 0, processMax = 0;
    public int overdriveLevel = 0;
    public int heated, ranged;
    public float steamLevel;
    public float lastPower;
    public float power;
    public boolean isWorking, isOverdrive, isActive, isBroken;
    public float TLevel, RLevel;
    public ItemStack currentItem = ItemStack.EMPTY;
    public BlockPos actualPos = null;
    
    public ResourceKey<Level> dimension;

    public GeneratorData(SpecialDataHolder teamData) {
    }


    public GeneratorData(int process, int processMax, int overdriveLevel, boolean[] flags, float steamLevel, float power, int heated, int ranged, float tLevel, float rLevel, CompoundTag inventory, ItemStack currentItem, Optional<BlockPos> actualPos, Optional<ResourceLocation> dimension) {
        super();
        this.process = process;
        this.processMax = processMax;
        this.overdriveLevel = overdriveLevel;
        this.steamLevel = steamLevel;
        this.heated = heated;
        this.ranged = ranged;
        this.power = power;
        this.isWorking = flags[0];
        this.isOverdrive = flags[1];
        this.isActive = flags[2];
        this.isBroken = flags[3];
        this.TLevel = tLevel;
        this.RLevel = rLevel;
        this.inventory.deserializeNBT(inventory);
        this.currentItem = currentItem;
        this.actualPos = actualPos.orElse(null);
        this.dimension = dimension.map(t -> ResourceKey.create(Registries.DIMENSION, t)).orElse(null);
    }

    public static boolean isStackValid(int slot, ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (slot == INPUT_SLOT) {
            for (GeneratorRecipe recipet : CUtils.filterRecipes(null, GeneratorRecipe.TYPE))
                if (recipet.input.test(stack)) {
                    return true;
                }
        }
        return false;
    }

    public boolean consumesFuel(Level w,SpecialDataHolder<?> teamData) {
        if (!currentItem.isEmpty()) {
            if (!inventory.getStackInSlot(OUTPUT_SLOT).isEmpty() && ItemHandlerHelper.canItemStacksStack(inventory.getStackInSlot(OUTPUT_SLOT), currentItem))
                inventory.getStackInSlot(OUTPUT_SLOT).grow(currentItem.getCount());
            else if (inventory.getStackInSlot(OUTPUT_SLOT).isEmpty())
                inventory.setStackInSlot(OUTPUT_SLOT, currentItem);
            currentItem = ItemStack.EMPTY;
        }
        GeneratorRecipe recipe = getRecipe(w);
        if (recipe != null) {
            int count = recipe.input.getCount();
            inventory.extractItem(INPUT_SLOT, count, false);
            currentItem = recipe.output.copy();
            currentItem.setCount(currentItem.getCount());
            double effi = getEfficiency(teamData);
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

    protected double getEfficiency(SpecialDataHolder<?> teamData) {
        return teamData.getData(FRSpecialDataTypes.RESEARCH_DATA).getVariantDouble(ResearchVariant.GENERATOR_EFFICIENCY) + 0.7;
    }

	public GeneratorRecipe getRecipe(Level w,ItemStack stack) {
    	for (GeneratorRecipe recipet : CUtils.filterRecipes(w.getRecipeManager(), GeneratorRecipe.TYPE))
            if (recipet.input.test(inventory.getStackInSlot(INPUT_SLOT))) {
                return recipet;
            }
    	return null;
    }
    public GeneratorRecipe getRecipe(Level w) {
        if (inventory.getStackInSlot(INPUT_SLOT).isEmpty())
            return null;
        GeneratorRecipe recipe=getRecipe(w,inventory.getStackInSlot(INPUT_SLOT));
        
        if (recipe == null)
            return null;
        if (inventory.getStackInSlot(OUTPUT_SLOT).isEmpty() || (ItemStack.isSameItemSameTags(inventory.getStackInSlot(OUTPUT_SLOT), recipe.output)
                && inventory.getStackInSlot(OUTPUT_SLOT).getCount() + recipe.output.getCount() <= getSlotLimit(OUTPUT_SLOT))) {
            return recipe;
        }
        return null;
    }

    public int getSlotLimit(int slot) {
        return 64;
    }
    
    /**
     * Core tick function for generator called in HeatingLogic#tickFuel
     * It does the internal logic for fuel and heating.
     * But does not update the heat adjust. That is done in HeatingLogic#serverTick
     * @param w
     */
    public void tick(Level w,SpecialDataHolder<?> teamData) {
        isActive = tickFuelProcess(w,teamData);
        tickHeatedProcess(w);

    }

    public void tickHeatedProcess(Level world) {
        int heatedMax = getMaxHeated();
        int rangedMax = getMaxRanged();
        
        if (isActive) {
            if (heated != heatedMax) {
                if (world.random.nextFloat() < heatChance * (isOverdrive ? 2 : 1)) {
                    if (heated < heatedMax) {
                        heated++;
                    } else {
                        heated--;
                    }
                }
            }
            if (ranged != rangedMax) {
                if (world.random.nextFloat() < heatChance * (isOverdrive ? 2 : 1)) {
                    if (ranged < rangedMax) {
                        ranged++;
                    } else {
                        ranged--;
                    }
                }
            }
        } else {
            if (heated > 0) {
                if (world.random.nextFloat() < heatChance * 2) {
                    heated--;
                }
            }
            if (ranged >0) {
            	if (world.random.nextFloat() < heatChance * 2) {
            		ranged --;
                }
            }
        }
        TLevel = heated / 100F;
        RLevel = ranged / 100F;

    }
    public boolean hasFuel() {
    	return process>0||!inventory.getStackInSlot(INPUT_SLOT).isEmpty();
    }
    private static final Style WARN_STYLE=Style.EMPTY.withColor(ChatFormatting.RED).withBold(true);
    public boolean tickFuelProcess(Level w,SpecialDataHolder<?> teamData) {

        boolean hasFuel = true;
        int lastOverdrive=overdriveLevel;
        if (isBroken)
            return false;
        overdriveLevel -= 5 * (teamData.getData(FRSpecialDataTypes.RESEARCH_DATA).getVariantDouble(ResearchVariant.OVERDRIVE_RECOVER) + 1);
        boolean isWorking=false;
        
        if(this.isWorking) {
        	int baseFuelCost=1;
        	float maxPower=steamLevel*25;//calculate max steam heat
        	float powerRemain=Math.max(0, maxPower-lastPower);//then calculate heat required to generate 
        	double efficiency=getHeatEfficiency(teamData);//get team heat efficiency
        	
            float actualPowerCost=(float) (powerRemain/efficiency/25f*8f);//25 heat per 8 fuel tick
            int extraCost=Mth.floor(actualPowerCost);
	        if (isOverdrive) {
	        	baseFuelCost+=1;
	        }
            while (process <= baseFuelCost+extraCost && hasFuel) {
                hasFuel = consumesFuel(w,teamData);
            }
           
            if (process >= baseFuelCost) {
                process -= baseFuelCost;
                isWorking=true;
                if(isOverdrive) {
	                overdriveLevel += 20;
	                if (overdriveLevel >= this.getMaxOverdrive()) {
	                    isBroken = true;
	                }
                }
            }
            if(isWorking) {
            	int actualCost=Math.min(extraCost, process);
            	process-=actualCost;
            	power=lastPower+(float)(efficiency*actualCost/8f*25);//refills power
            	
            }
	        if(teamData instanceof TeamDataHolder team) {
	        	if(lastOverdrive<=getCriticalOverdriveValue()&&overdriveLevel>getCriticalOverdriveValue()) {
		        	team.forEachOnline(t->t.sendSystemMessage(Lang.translateTooltip("generator.critical").withStyle(WARN_STYLE)));
		        }else
		        if(lastOverdrive<=getWarningOverdriveValue()&&overdriveLevel>getWarningOverdriveValue()) {
		        	team.forEachOnline(t->t.sendSystemMessage(Lang.translateTooltip("generator.warning").withStyle(WARN_STYLE)));
		        }
		        
	        }
        }
        lastPower=0;
        overdriveLevel=Math.max(0, overdriveLevel);
        return isWorking;
    }

    public void onPosChange() {
        heated = 0;
        ranged = 0;
        TLevel = 0;
        RLevel = 0;
        process = 0;
        processMax = 0;
        power = 0;
        lastPower=0;
        
    }

    protected double getHeatEfficiency(SpecialDataHolder<?> teamData) {

        return 1 + teamData.getData(FRSpecialDataTypes.RESEARCH_DATA).getVariantDouble(ResearchVariant.GENERATOR_HEAT);
    }

    public float getMaxTemperatureLevel() {
        return 1 + (isOverdrive ? 1 : 0) + steamLevel;
    }

    public float getMaxRangeLevel() {
        return 1 + steamLevel;
    }

    public int getMaxHeated() {
        return (int) (100 * this.getMaxTemperatureLevel());
    }

    public int getMaxRanged() {
        return (int) (100 * this.getMaxRangeLevel());
    }

    public int getMaxOverdrive() {
        return 240400;
    }
    //Warning at 80% overdrive
    public int getWarningOverdriveValue() {
        return 192320;
    }
    //Critical at 90% overdrive
    public int getCriticalOverdriveValue() {
        return 216360;
    }
    /**
     * Get the actual range of the heating device.
     * The range is calculated by the formula:
     * 12 + 4 * (rangeLevel - 1) if rangeLevel>1
     * <p>
     * The Base range at level 1 is 12 blocks.
     * For each additional level, the range increases by 4 blocks.
     *
     * @return in blocks
     */
    public int getRadius() {
        float rlevel = RLevel;
        if (rlevel <= 1)
            return (int) (16 * rlevel);
        return (int) (16 + (rlevel - 1) * 8);
    }

    /**
     * Get the actual temperature modification of the heating device.
     * The temperature modification is calculated by the formula:
     * 10 * temperatureLevel
     * <p>
     * The Base temperature modification at level 1 is 10.
     * For each additional level, the temperature modification increases by 10.
     *
     * @return in degrees
     */
    public int getTempMod() {
        return (int) (TLevel * 10);
    }
}
