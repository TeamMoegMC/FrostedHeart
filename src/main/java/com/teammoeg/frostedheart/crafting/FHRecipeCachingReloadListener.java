package com.teammoeg.frostedheart.crafting;

import blusunrize.immersiveengineering.common.crafting.RecipeReloadListener;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;

import javax.annotation.Nonnull;

public class FHRecipeCachingReloadListener implements IResourceManagerReloadListener
{
    private final DataPackRegistries dataPackRegistries;

    public FHRecipeCachingReloadListener(DataPackRegistries dataPackRegistries)
    {
        this.dataPackRegistries = dataPackRegistries;
    }

    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager)
    {
        RecipeReloadListener.buildRecipeLists(dataPackRegistries.getRecipeManager());
    }
}