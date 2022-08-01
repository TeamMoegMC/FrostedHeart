package com.teammoeg.frostedheart.compat.jei;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class CuttingRecipe implements IRecipe<IInventory> {
    public final ItemStack in;
    public final ItemStack out;

    public CuttingRecipe(ItemStack in, ItemStack out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public boolean matches(IInventory inv, World worldIn) {
        return false;
    }

    @Override
    public ItemStack getCraftingResult(IInventory inv) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return out;
    }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(FHMain.MODID, "cutting/" + out.getItem().getRegistryName().getPath());
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return null;
    }

    @Override
    public IRecipeType<?> getType() {
        return IRecipeType.CRAFTING;
    }

}
