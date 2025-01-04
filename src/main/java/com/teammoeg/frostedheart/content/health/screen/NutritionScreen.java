package com.teammoeg.frostedheart.content.health.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import com.teammoeg.frostedheart.util.lang.ComponentOptimizer;
import com.teammoeg.frostedheart.util.lang.FHTextIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class NutritionScreen extends Screen {
    public NutritionScreen() {
        super(Component.empty());
    }

    static final int FAT_COLOR=0xFFd41c53;
    static final int PROTEIN_COLOR=0xFFd4a31c;
    static final int CARBOHYDRATE_COLOR=0xFFd4781c;
    static final int VEGETABLE_COLOR=0xFF31d41c;

    static Style def_style= FHTextIcon.applyFont(Style.EMPTY);

    private static Component fat_icon;
    private static Component protein_icon;
    private static Component carbohydrate_icon;
    private static Component vegetable_icon;

    @Override
    public void init() {

        ComponentOptimizer fat=new ComponentOptimizer();
        fat.appendChar("\uF504", def_style);
        ComponentOptimizer protein=new ComponentOptimizer();
        protein.appendChar("\uF505", def_style);
        ComponentOptimizer carbohydrate=new ComponentOptimizer();
        carbohydrate.appendChar("\uF502", def_style);
        ComponentOptimizer vegetable=new ComponentOptimizer();
        vegetable.appendChar("\uF503", def_style);
        fat_icon=fat.build();
        protein_icon=protein.build();
        carbohydrate_icon=carbohydrate.build();
        vegetable_icon=vegetable.build();

    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        LocalPlayer localPlayer = null;
        if (this.minecraft != null) {
            localPlayer = this.minecraft.player;
        }
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();
        pose.translate((float) this.width /2, (float) this.height /2, 0);
        FHGuiHelper.fillRoundRect(pGuiGraphics,-100, -80, 200, 160, 0.05f,0x40FFFFFF);

        NutritionCapability.getCapability(localPlayer).ifPresent(nutrition -> renderNutritionBar(pGuiGraphics, 0, 0, nutrition.get()));
        pose.popPose();
    }

    private static void renderNutritionBar(GuiGraphics guiGraphics, int x, int y, NutritionCapability.Nutrition n) {

        Font font1 = Minecraft.getInstance().font;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(2.0f,2.0f,1.0f);
        guiGraphics.drawCenteredString(font1, fat_icon, x-10, y-10-font1.lineHeight/2, 0xFFFFFF);
        guiGraphics.drawCenteredString(font1, protein_icon, x+10, y-10-font1.lineHeight/2, 0xFFFFFF);
        guiGraphics.drawCenteredString(font1, carbohydrate_icon, x-10, y+10-font1.lineHeight/2, 0xFFFFFF);
        guiGraphics.drawCenteredString(font1, vegetable_icon, x+10, y+10-font1.lineHeight/2, 0xFFFFFF);
        pose.popPose();

        FHGuiHelper.drawRing(guiGraphics, x-20,  y-20, 9, 16, 0,360, 0x80FFFFFF);
        FHGuiHelper.drawRing(guiGraphics, x+20,  y-20, 9, 16, 0,360, 0x80FFFFFF);
        FHGuiHelper.drawRing(guiGraphics, x-20,  y+20, 9, 16, 0,360, 0x80FFFFFF);
        FHGuiHelper.drawRing(guiGraphics, x+20,  y+20, 9, 16, 0,360, 0x80FFFFFF);

        FHGuiHelper.drawRing(guiGraphics, x-20,  y-20, 10, 15, 0,n.fat()/10000 *360, FAT_COLOR);
        FHGuiHelper.drawRing(guiGraphics, x+20,  y-20, 10, 15, 0,n.protein()/10000 *360, PROTEIN_COLOR);
        FHGuiHelper.drawRing(guiGraphics, x-20,  y+20, 10, 15, 0,n.carbohydrate()/10000 *360, CARBOHYDRATE_COLOR);
        FHGuiHelper.drawRing(guiGraphics, x+20,  y+20, 10, 15, 0,n.vegetable()/10000 *360, VEGETABLE_COLOR);

    }
}
