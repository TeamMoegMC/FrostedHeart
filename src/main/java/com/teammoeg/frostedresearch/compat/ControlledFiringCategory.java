package com.teammoeg.frostedresearch.compat;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.furnace.ControlledFiringRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
public final class ControlledFiringCategory implements IRecipeCategory<ControlledFiringRecipe>{
    public static final RecipeType<ControlledFiringRecipe> TYPE=RecipeType.create("frostedresearch","controlled_firing",ControlledFiringRecipe.class);
    private final IDrawable background,icon;
    public ControlledFiringCategory(IGuiHelper h){background=h.createBlankDrawable(164,84);icon=h.createDrawableItemStack(new ItemStack(FRContents.Blocks.CONTROLLED_FURNACE.get()));}
    @Override public RecipeType<ControlledFiringRecipe> getRecipeType(){return TYPE;}
    @Override public Component getTitle(){return Component.translatable("block.frostedresearch.controlled_furnace");}
    @Override public IDrawable getBackground(){return background;}
    @Override public IDrawable getIcon(){return icon;}
    @Override public void setRecipe(IRecipeLayoutBuilder b,ControlledFiringRecipe r,IFocusGroup focus){
        b.addInputSlot(2,2).setStandardSlotBackground().addItemStack(r.input());
        b.addInputSlot(26,2).setStandardSlotBackground().addItemStack(r.fuel());
        b.addOutputSlot(105,2).setOutputSlotBackground().addItemStack(r.result());
        b.addOutputSlot(140,2).setStandardSlotBackground().addItemStack(r.failure());
    }
    @Override public void draw(ControlledFiringRecipe r,mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,GuiGraphics g,double mx,double my){
        var font=Minecraft.getInstance().font;
        g.drawString(font,Component.translatable("gui.frostedresearch.furnace."+(r.experiment().isPresent()?"trial":"production")),2,29,0x555555,false);
        g.drawString(font,r.temperatureMin()+" - "+r.temperatureMax()+" C",2,43,0x555555,false);
        g.drawString(font,Component.translatable("gui.frostedresearch.furnace.jei_duration",r.duration()/20),2,57,0x555555,false);
        g.drawString(font,Component.translatable("gui.frostedresearch.furnace.jei_failure"),2,71,0x555555,false);
    }
}
