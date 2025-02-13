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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.util.client.FGuis;
import com.teammoeg.frostedheart.util.client.FHTextIcon;
import com.teammoeg.frostedheart.util.client.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Inventory;

public class NutritionScreen extends Screen implements MenuAccess<NutritionMenu> {
	NutritionMenu menu;
    public NutritionScreen(NutritionMenu menu, Inventory inv, Component title) {
        super(title);
        this.menu=menu;
    }

    static final int FAT_COLOR=0xFFd41c53;
    static final int PROTEIN_COLOR=0xFFd4a31c;
    static final int CARBOHYDRATE_COLOR=0xFFd4781c;
    static final int VEGETABLE_COLOR=0xFF31d41c;

    static Style def_style= FHTextIcon.applyFont(Style.EMPTY);

    public static Component fat_icon;
    public static Component protein_icon;
    public static Component carbohydrate_icon;
    public static Component vegetable_icon;

    private static Font font = Minecraft.getInstance().font;

    private static float progress;

    @Override
    public void init() {
        progress=0.0f;
        fat_icon=Components.str("\uF504").withStyle(def_style);
        protein_icon=Components.str("\uF505").withStyle(def_style);
        carbohydrate_icon=Components.str("\uF502").withStyle(def_style);
        vegetable_icon=Components.str("\uF503").withStyle(def_style);

    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        progress = Math.min(progress + pPartialTick/10, 1.0f);
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();
        pose.translate((float) this.width /2, (float) this.height /2, 0);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, progress);
        FGuis.fillRoundRect(pGuiGraphics,-100, -80, 200, 160, 0.05f,0x40FFFFFF);
        renderNutritionBar(pGuiGraphics, 0, 0);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        pose.popPose();
    }

    private void renderNutritionBar(GuiGraphics guiGraphics, int x, int y) {

        renderBar(guiGraphics,x-40,y-30,fat_icon, Lang.gui("nutrition.fat").component(),menu.fat.getValue(),FAT_COLOR);
        renderBar(guiGraphics,x+40,y-30,protein_icon,Lang.gui("nutrition.protein").component(),menu.protein.getValue(),PROTEIN_COLOR);
        renderBar(guiGraphics,x-40,y+30,carbohydrate_icon,Lang.gui("nutrition.carbohydrate").component(),menu.carbohydrate.getValue(),CARBOHYDRATE_COLOR);
        renderBar(guiGraphics,x+40,y+30,vegetable_icon,Lang.gui("nutrition.vegetable").component(),menu.vegetable.getValue(),VEGETABLE_COLOR);

    }

    private static void renderBar(GuiGraphics guiGraphics,int x,int y,Component icon,Component desc ,float value,int color){
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(2.0f,2.0f,1.0f);
        guiGraphics.drawCenteredString(font, icon, x/2, y/2-font.lineHeight/2, 0xFFFFFF);
        pose.popPose();
        FGuis.drawRing(guiGraphics, x,  y, 9, 16, 0,360 * progress, 0x80FFFFFF,0x80DDDDDD);
        FGuis.drawRing(guiGraphics, x,  y, 10, 15, 0,value *360 * progress, color);
        guiGraphics.drawCenteredString(font, desc, x, y+16+font.lineHeight/2, 0xFFFFFF);
    }

	@Override
	public NutritionMenu getMenu() {
		return menu;
	}
}
