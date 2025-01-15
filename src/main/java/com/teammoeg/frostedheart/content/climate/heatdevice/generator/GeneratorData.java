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

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.base.team.SpecialData;
import com.teammoeg.frostedheart.base.team.SpecialDataHolder;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.content.research.data.ResearchVariant;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Generator data and utility functions.
 * Automatically maintained by the special data system,
 * which is shared by the team.
 * See {@link com.teammoeg.frostedheart.base.team.SpecialDataTypes}
 */
public class GeneratorData implements SpecialData {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final Codec<GeneratorData> CODEC = RecordCodecBuilder.create(t -> t.group(
            Codec.INT.fieldOf("process").forGetter(o -> o.process),
            Codec.INT.fieldOf("processMax").forGetter(o -> o.processMax),
            Codec.INT.fieldOf("steamProcess").forGetter(o -> o.steamProcess),
            Codec.INT.fieldOf("overdriveLevel").forGetter(o -> o.overdriveLevel),
            CodecUtil.<GeneratorData>booleans("flags")
                    .flag("isWorking", o -> o.isWorking)
                    .flag("isOverdrive", o -> o.isOverdrive)
                    .flag("isActive", o -> o.isActive)
                    .flag("isBroken", o -> o.isBroken).build(),
            Codec.FLOAT.fieldOf("steamLevel").forGetter(o -> o.steamLevel),
            Codec.FLOAT.fieldOf("powerLevel").forGetter(o -> o.power),
            CodecUtil.defaultValue(Codec.INT, 0).fieldOf("heated").forGetter(o -> o.heated),
            CodecUtil.defaultValue(Codec.INT, 0).fieldOf("ranged").forGetter(o -> o.ranged),
            CodecUtil.registryCodec(() -> BuiltInRegistries.FLUID).optionalFieldOf("steamFluid").forGetter(o -> Optional.ofNullable(o.fluid)),
            Codec.FLOAT.fieldOf("tempLevel").forGetter(o -> o.TLevel),
            Codec.FLOAT.fieldOf("rangeLevel").forGetter(o -> o.RLevel),
            CompoundTag.CODEC.fieldOf("items").forGetter(o -> o.inventory.serializeNBT()),
            CodecUtil.defaultValue(CodecUtil.ITEMSTACK_CODEC, ItemStack.EMPTY).fieldOf("res").forGetter(o -> o.currentItem),
            CodecUtil.BLOCKPOS.optionalFieldOf("actualPos",BlockPos.ZERO).forGetter(o -> o.actualPos),
            ResourceLocation.CODEC.optionalFieldOf("dim").forGetter(o -> o.dimension == null ? Optional.empty() : Optional.of(o.dimension.location()))
    ).apply(t, GeneratorData::new));
    public final ItemStackHandler inventory = new ItemStackHandler(2);
    public final LazyOptional<ItemStackHandler> invCap = LazyOptional.of(() -> inventory);
    final float heatChance = .05f;
    public int process = 0, processMax = 0;
    public int overdriveLevel = 0;
    public float steamLevel;
    public int steamProcess;
    public int heated, ranged;
    public float lastPower;
    public float power;
    public Fluid fluid;
    public boolean isWorking, isOverdrive, isActive, isBroken;
    public float TLevel, RLevel;
    public ItemStack currentItem = ItemStack.EMPTY;
    public BlockPos actualPos = null;
    
    public ResourceKey<Level> dimension;

    public GeneratorData(SpecialDataHolder teamData) {
    }


    public GeneratorData(int process, int processMax, int steamProcess, int overdriveLevel, boolean[] flags, float steamLevel, float power, int heated, int ranged, Optional<Fluid> fluid, float tLevel, float rLevel, CompoundTag inventory, ItemStack currentItem, BlockPos actualPos, Optional<ResourceLocation> dimension) {
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
        this.inventory.deserializeNBT(inventory);
        this.currentItem = currentItem;
        this.actualPos = actualPos;
        this.dimension = dimension.map(t -> ResourceKey.create(Registries.DIMENSION, t)).orElse(null);
    }

    public static boolean isStackValid(Level w, int slot, ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (slot == INPUT_SLOT) {
            for (GeneratorRecipe recipet : FHUtils.filterRecipes(w.getRecipeManager(), GeneratorRecipe.TYPE))
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
        return teamData.getData(SpecialDataTypes.RESEARCH_DATA).getVariantDouble(ResearchVariant.GENERATOR_EFFICIENCY) + 0.7;
    }

    public GeneratorRecipe getRecipe(Level w) {
        if (inventory.getStackInSlot(INPUT_SLOT).isEmpty())
            return null;
        GeneratorRecipe recipe = null;
        for (GeneratorRecipe recipet : FHUtils.filterRecipes(w.getRecipeManager(), GeneratorRecipe.TYPE))
            if (recipet.input.test(inventory.getStackInSlot(INPUT_SLOT))) {
                recipe = recipet;
                break;
            }
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
        lastPower=0;
        if (isActive && power > 0)
            lastPower=((float) (power * getHeatEfficiency(teamData)));
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
        }
        TLevel = heated / 100F;
        RLevel = ranged / 100F;

    }

    public boolean tickFuelProcess(Level w,SpecialDataHolder<?> teamData) {
        if (!isWorking || isBroken)
            return false;
        boolean hasFuel = true;
        overdriveLevel -= 5 * (teamData.getData(SpecialDataTypes.RESEARCH_DATA).getVariantDouble(ResearchVariant.OVERDRIVE_RECOVER) + 1);

        	
        if (isOverdrive) {
            while (process <= 1 && hasFuel) {
                hasFuel = consumesFuel(w,teamData);
            }
            overdriveLevel += 20;
            if(overdriveLevel<0)
            	overdriveLevel=0;
            if (overdriveLevel >= this.getMaxOverdrive()) {
                isBroken = true;
            }
            if (process > 1) {
                process -= 2;
                return true;
            }


        } else {
            if(overdriveLevel<0)
            	overdriveLevel=0;
            while (process <= 0 && hasFuel) {
                hasFuel = consumesFuel(w,teamData);
            }
            if (process > 0) {
                process--;
                return true;
            }
        }

        return false;
    }

    public void onPosChange() {
        heated = 0;
        ranged = 0;
        TLevel = 0;
        RLevel = 0;
        process = 0;
        processMax = 0;
        steamProcess = 0;
    }

    protected double getHeatEfficiency(SpecialDataHolder<?> teamData) {

        return 1 + teamData.getData(SpecialDataTypes.RESEARCH_DATA).getVariantDouble(ResearchVariant.GENERATOR_HEAT);
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
            return (int) (12 * rlevel);
        return (int) (12 + (rlevel - 1) * 4);
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
