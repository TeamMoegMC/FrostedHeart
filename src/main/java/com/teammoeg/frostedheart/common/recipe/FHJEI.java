package com.teammoeg.frostedheart.common.recipe;

import com.google.common.collect.ImmutableSet;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import electrodynamics.common.recipe.categories.fluiditem2fluid.FluidItem2FluidRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@JeiPlugin
public class FHJEI implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(FHMain.MODID, "jei_plugin");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FHContent.Blocks.electrolyzer), EleRecipeCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHContent.Blocks.burning_chamber_core), CrucibleCategory.UID);

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft mc = Minecraft.getInstance();
        ClientWorld world = Objects.requireNonNull(mc.world);

        Set<FluidItem2FluidRecipe> eleRecipes = ImmutableSet
                .copyOf(world.getRecipeManager().getRecipesForType(ElectrolyzerRecipe.TYPE));

        registration.addRecipes(eleRecipes, EleRecipeCategory.UID);

//        Set<CrucibleRecipe> crucible = ImmutableSet
//                .copyOf(world.getRecipeManager().getRecipesForType(CrucibleRecipe.TYPE));
//
//        registration.addRecipes(crucible, CrucibleCategory.UID);
        registration.addRecipes(new ArrayList<>(CrucibleRecipe.recipeList.values()), CrucibleCategory.UID);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new EleRecipeCategory(guiHelper),
                new CrucibleCategory(guiHelper)
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
        //registry.addRecipeClickArea(ElectrolyzerScreen.class, 80, 31, 55, 55, EleRecipeCategory.UID);
        //registry.addRecipeClickArea(CrucibleScreen.class, 80, 31, o2oarrowLoc[2], o2oarrowLoc[3], CrucibleCategory.UID);
    }
}
