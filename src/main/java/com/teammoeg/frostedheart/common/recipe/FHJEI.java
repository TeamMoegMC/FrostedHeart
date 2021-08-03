package com.teammoeg.frostedheart.common.recipe;

import com.google.common.collect.ImmutableSet;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.screen.ElectrolyzerScreen;
import electrodynamics.common.recipe.categories.fluiditem2fluid.FluidItem2FluidRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;
import java.util.Set;

@JeiPlugin
public class FHJEI implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(FHMain.MODID, "fh_jei_plugin");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FHContent.Blocks.electrolyzer), EleRecipeCategory.UID);
       // registration.addRecipeCatalyst(new ItemStack(FHContent.Blocks.burning_chamber_core), CrucibleCategory.UID);
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
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new EleRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        //registration.addRecipeCategories(new CrucibleCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
        int[] o2oarrowLoc = {80 + 5, 35, 22, 15};
        registry.addRecipeClickArea(ElectrolyzerScreen.class, 80, 31, o2oarrowLoc[2], o2oarrowLoc[3], EleRecipeCategory.UID);
        //registry.addRecipeClickArea(CrucibleScreen.class, 80, 31, o2oarrowLoc[2], o2oarrowLoc[3], CrucibleCategory.UID);
    }
}
