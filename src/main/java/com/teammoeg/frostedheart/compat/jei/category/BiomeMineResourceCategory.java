package com.teammoeg.frostedheart.compat.jei.category;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.mine.BiomeMineResourceRecipe;
import com.teammoeg.frostedheart.util.Lang;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BiomeMineResourceCategory implements IRecipeCategory<BiomeMineResourceRecipe> {
    public static final RecipeType<BiomeMineResourceRecipe> UID = RecipeType.create(FHMain.MODID, "biome_mine_resource", BiomeMineResourceRecipe.class);
    private final IDrawable background;
    private final IDrawable icon;

    public BiomeMineResourceCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(176, 100);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FHBlocks.MINE.get()));
    }

    @Override
    public RecipeType<BiomeMineResourceRecipe> getRecipeType() {
        return UID;
    }

    @Override
    public Component getTitle() {
        return Lang.translateKey("gui.jei.category." + FHMain.MODID + ".biome_mine_resource");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BiomeMineResourceRecipe recipe, IFocusGroup focuses) {
        int x = 0;
        int y = 20;
        int columns = 9;

        int totalWeight = recipe.weights.values().stream().mapToInt(Integer::intValue).sum();
        AtomicInteger count = new AtomicInteger(0);

        recipe.weights.entrySet().stream()
                .sorted(Map.Entry.<Item, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    Item item = entry.getKey();
                    int weight = entry.getValue();
                    int currentCount = count.getAndIncrement();

                    builder.addSlot(RecipeIngredientRole.OUTPUT, x + (currentCount % columns) * 18, y + (currentCount / columns) * 18)
                            .addItemStack(new ItemStack(item))
                            .addTooltipCallback((recipeSlotView, tooltip) -> {
                                tooltip.add(Component.literal("Weight: " + weight));
                                if (totalWeight > 0) {
                                    String chance = String.format("%.2f%%", (double) weight / totalWeight * 100);
                                    tooltip.add(Component.literal("Chance: " + chance));
                                }
                            });
                });
    }

    @Override
    public void draw(BiomeMineResourceRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Component biomeName = Component.translatable("biome." + recipe.biomeID.getNamespace() + "." + recipe.biomeID.getPath());
        guiGraphics.drawString(Minecraft.getInstance().font, biomeName, 0, 5, 0xFF404040, false);
    }
}
