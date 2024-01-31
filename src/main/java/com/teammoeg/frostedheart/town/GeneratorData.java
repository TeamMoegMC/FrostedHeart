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

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.RegistryUtils;

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
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

public class GeneratorData {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public int process = 0;
    public int processMax = 0;
    public int overdriveLevel = 0;
    public int steamLevel;
    public float power;
    public Fluid fluid;
    public boolean isWorking;
    public boolean isOverdrive;
    public boolean isActive;
    protected NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
    public ItemStack currentItem;
    private TeamResearchData teamData;
    public BlockPos actualPos = BlockPos.ZERO;
    public RegistryKey<World> dimension;

    public GeneratorData(TeamResearchData teamResearchData) {
        teamData = teamResearchData;
    }

    public boolean consumesFuel() {
        if (currentItem != null) {
            if (!inventory.get(OUTPUT_SLOT).isEmpty() && ItemHandlerHelper.canItemStacksStack(inventory.get(OUTPUT_SLOT), currentItem))
                inventory.get(OUTPUT_SLOT).grow(currentItem.getCount());
            else if (inventory.get(OUTPUT_SLOT).isEmpty())
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
        }
        if (this.processMax != 0) {
            this.process = 0;
            processMax = 0;
        }
        return false;
    }

    public void deserialize(CompoundNBT data, boolean update) {
        process = data.getInt("process");
        processMax = data.getInt("processMax");
        overdriveLevel = data.getInt("overdriveLevel");
        isWorking = data.getBoolean("isWorking");
        isOverdrive = data.getBoolean("isOverdrive");
        isActive = data.getBoolean("isActive");
        steamLevel = data.getInt("steamLevel");
        power = data.getFloat("power");
        if (data.contains("steamFluid"))
            fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(data.getString("steamFluid")));
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

    public int getSlotLimit(int slot) {
        return 64;
    }

    public CompoundNBT serialize(boolean update) {
        CompoundNBT result = new CompoundNBT();
        result.putInt("process", process);
        result.putInt("processMax", processMax);
        result.putInt("overdriveLevel", overdriveLevel);
        result.putBoolean("isWorking", isWorking);
        result.putBoolean("isOverdrive", isOverdrive);
        result.putBoolean("isActive", isActive);
        result.putFloat("power", power);
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

    public void tick() {
        isActive = tickFuelProcess();
    }

    public boolean tickFuelProcess() {
        if (!isWorking)
            return false;
        boolean hasFuel = true;
        if (isOverdrive) {
            while (process <= 3 && hasFuel) {
                hasFuel = consumesFuel();
            }
            if (process > 3) {
                process -= 4;
                return true;
            }
        } else {
            while (process <= 0 && hasFuel) {
                hasFuel = consumesFuel();
            }
            if (process > 0) {
                process--;
                return true;
            }
        }
        return false;
    }
}
