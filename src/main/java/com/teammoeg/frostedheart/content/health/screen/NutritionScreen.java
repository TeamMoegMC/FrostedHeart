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

package com.teammoeg.frostedheart.content.health.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.lang.ComponentOptimizer;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.util.client.FGuis;
import com.teammoeg.frostedheart.util.client.FHTextIcon;
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

    private static Font font = Minecraft.getInstance().font;

    private static float progress;

    @Override
    public void init() {
        progress=0.0f;
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
        progress = Math.min(progress + pPartialTick/10, 1.0f);
        LocalPlayer localPlayer = null;
        if (this.minecraft != null) {
            localPlayer = this.minecraft.player;
        }
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();
        pose.translate((float) this.width /2, (float) this.height /2, 0);
        FGuis.fillRoundRect(pGuiGraphics,-100, -80, 200, 160, 0.05f,0x40FFFFFF);

        NutritionCapability.getCapability(localPlayer).ifPresent(nutrition -> renderNutritionBar(pGuiGraphics, 0, 0, nutrition.get()));
        pose.popPose();
    }

    private static void renderNutritionBar(GuiGraphics guiGraphics, int x, int y, NutritionCapability.Nutrition n) {

        renderBar(guiGraphics,x-40,y-30,fat_icon,Component.literal("fat"),n.fat()/10000,FAT_COLOR);
        renderBar(guiGraphics,x+40,y-30,protein_icon,Component.literal("protein"),n.protein()/10000,PROTEIN_COLOR);
        renderBar(guiGraphics,x-40,y+30,carbohydrate_icon,Component.literal("carbohydrate"),n.carbohydrate()/10000,CARBOHYDRATE_COLOR);
        renderBar(guiGraphics,x+40,y+30,vegetable_icon,Component.literal("vegetable"),n.vegetable()/10000,VEGETABLE_COLOR);

    }

    private static void renderBar(GuiGraphics guiGraphics,int x,int y,Component icon,Component desc ,float value,int color){
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(2.0f,2.0f,1.0f);
        guiGraphics.drawCenteredString(font, icon, x/2, y/2-font.lineHeight/2, 0xFFFFFF);
        pose.popPose();
        FGuis.drawRing(guiGraphics, x,  y, 9, 16, 0,360 * progress, 0x80FFFFFF);
        FGuis.drawRing(guiGraphics, x,  y, 10, 15, 0,value *360 * progress, color);
        guiGraphics.drawCenteredString(font, desc, x, y+16+font.lineHeight/2, 0xFFFFFF);
    }
}
