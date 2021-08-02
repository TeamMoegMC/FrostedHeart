package com.teammoeg.frostedheart.common.recipe;

import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import electrodynamics.common.recipe.recipeutils.CountableIngredient;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;

public class CrucibleCategory implements IRecipeCategory<CrucibleRecipe> {
    public static ResourceLocation UID = new ResourceLocation(FHMain.MODID, "crucible");
    private IDrawable BACKGROUND;
    private IDrawable ICON;

    public CrucibleCategory(IGuiHelper guiHelper) {
        this.ICON = guiHelper.createDrawableIngredient(FHContent.Multiblocks.crucible);
        this.BACKGROUND = guiHelper.createDrawable(new ResourceLocation("textures/gui/sol_and_liq_to_liq_recipe_gui.png"), 10, 10, 10, 10);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends CrucibleRecipe> getRecipeClass() {
        return CrucibleRecipe.class;
    }


    public String getTitle() {
        return (new TranslationTextComponent("gui.jei.category." + "crucible")).getString();
    }

    @Override
    public IDrawable getBackground() {
        return BACKGROUND;
    }

    @Override
    public IDrawable getIcon() {
        return ICON;
    }

    @Override
    public void setIngredients(CrucibleRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, this.getIngredients(recipe));
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getRecipeOutput());
    }

    public List<List<ItemStack>> getIngredients(CrucibleRecipe recipe) {
        List<List<ItemStack>> ingredients = new ArrayList();
        ingredients.add(((CountableIngredient) recipe.getIngredients().get(0)).fetchCountedStacks());
        return ingredients;
    }


    @Override
    public void setRecipe(IRecipeLayout recipeLayout, CrucibleRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
        guiItemStacks.init(0, true, 20, 20);
        guiItemStacks.init(2, false, 300, 30);
        guiItemStacks.set(ingredients);
    }
}
