package com.teammoeg.frostedheart.base.item.rankine.alloys;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockRecipeHelper {


    public static ItemStack getBlockItemStack(JsonObject json)
    {
        String itemName = GsonHelper.getAsString(json, "block");

        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(itemName));

        if (block == null)
            throw new JsonSyntaxException("Unknown block '" + itemName + "'");

        return new ItemStack(block);
    }

    public static FluidStack getBlockFluidStack(JsonObject json)
    {
        String itemName = GsonHelper.getAsString(json, "block");

        Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(itemName));

        if (fluid == null)
            throw new JsonSyntaxException("Unknown fluid '" + itemName + "'");

        return new FluidStack(fluid,1);
    }
}