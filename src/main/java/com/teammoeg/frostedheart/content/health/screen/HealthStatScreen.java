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
import com.teammoeg.chorda.client.AnimationUtil;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.content.health.screen.widget.ColoredCubeWidget;
import com.teammoeg.frostedheart.content.health.screen.widget.NutritionBarWidget;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.client.FGuis;
import com.teammoeg.frostedheart.util.client.FHTextIcon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Inventory;

public class HealthStatScreen extends Screen implements MenuAccess<HealthStatMenu> {
	HealthStatMenu menu;
    public HealthStatScreen(HealthStatMenu menu, Inventory inv, Component title) {
        super(title);
        this.menu=menu;
    }

    static final int FAT_COLOR=0xFFd41c53;
    static final int PROTEIN_COLOR=0xFFd4a31c;
    static final int CARBOHYDRATE_COLOR=0xFFd4781c;
    static final int VEGETABLE_COLOR=0xFF31d41c;

    static Style def_style= FHTextIcon.applyFont(Style.EMPTY);

    public static Component fat_icon = Components.str("\uF504").withStyle(def_style);
    public static Component protein_icon = Components.str("\uF505").withStyle(def_style);
    public static Component carbohydrate_icon = Components.str("\uF502").withStyle(def_style);
    public static Component vegetable_icon = Components.str("\uF503").withStyle(def_style);

    private static float progress;

    private NutritionBarWidget fatBar;
    private NutritionBarWidget proteinBar;
    private NutritionBarWidget carbohydrateBar;
    private NutritionBarWidget vegetableBar;

    private ColoredCubeWidget head;
    private ColoredCubeWidget body;
    private ColoredCubeWidget hand_left;
    private ColoredCubeWidget hand_right;
    private ColoredCubeWidget leg_left;
    private ColoredCubeWidget leg_right;
    private ColoredCubeWidget foot_left;
    private ColoredCubeWidget foot_right;


    private boolean shouldClose =false;

    public void onClose() {
    	if(!shouldClose) {
	        shouldClose = true;
	        AnimationUtil.remove("NutritionScreen");
    	}
    }

    @Override
    public void init() {
        AnimationUtil.remove("NutritionScreen");
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        fatBar = new NutritionBarWidget(centerX+40,centerY-45,0,1f,fat_icon, Lang.gui("nutrition.fat").component(),FAT_COLOR);
        proteinBar = new NutritionBarWidget(centerX+40,centerY-15,0.2f,1f,protein_icon,Lang.gui("nutrition.protein").component(),PROTEIN_COLOR);
        carbohydrateBar = new NutritionBarWidget(centerX+40,centerY+15,0,1.2f,carbohydrate_icon,Lang.gui("nutrition.carbohydrate").component(),CARBOHYDRATE_COLOR);
        vegetableBar = new NutritionBarWidget(centerX+40,centerY+45,0.2f,1.2f,vegetable_icon,Lang.gui("nutrition.vegetable").component(),VEGETABLE_COLOR);

        head = new ColoredCubeWidget(centerX-50,centerY-60,24,24,Lang.gui("temperature.head").component());
        body = new ColoredCubeWidget(centerX-50,centerY-32,24,30,Lang.gui("temperature.body").component());
        hand_left = new ColoredCubeWidget(centerX-66,centerY-32,12,36,Lang.gui("temperature.hand").component());
        hand_right = new ColoredCubeWidget(centerX-22,centerY-32,12,36,Lang.gui("temperature.hand").component());
        leg_left = new ColoredCubeWidget(centerX-51,centerY+2,12,32,Lang.gui("temperature.leg").component());
        leg_right = new ColoredCubeWidget(centerX-37,centerY+2,12,32,Lang.gui("temperature.leg").component());
        foot_left = new ColoredCubeWidget(centerX-51,centerY+36,12,9,Lang.gui("temperature.foot").component());
        foot_right = new ColoredCubeWidget(centerX-37,centerY+36,12,9,Lang.gui("temperature.foot").component());

        this.addRenderableWidget(fatBar);
        this.addRenderableWidget(proteinBar);
        this.addRenderableWidget(carbohydrateBar);
        this.addRenderableWidget(vegetableBar);

        this.addRenderableWidget(head);
        this.addRenderableWidget(body);
        this.addRenderableWidget(hand_left);
        this.addRenderableWidget(hand_right);
        this.addRenderableWidget(leg_left);
        this.addRenderableWidget(leg_right);
        this.addRenderableWidget(foot_left);
        this.addRenderableWidget(foot_right);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {

        fatBar.setValue(menu.fat.getValue());
        proteinBar.setValue(menu.protein.getValue());
        carbohydrateBar.setValue(menu.carbohydrate.getValue());
        vegetableBar.setValue(menu.vegetable.getValue());

        head.setTemp(menu.headTemperature.getValue());
        body.setTemp(menu.bodyTemperature.getValue());
        hand_left.setTemp(menu.handsTemperature.getValue());
        hand_right.setTemp(menu.handsTemperature.getValue());
        leg_left.setTemp(menu.legsTemperature.getValue());
        leg_right.setTemp(menu.legsTemperature.getValue());
        foot_left.setTemp(menu.feetTemperature.getValue());
        foot_right.setTemp(menu.feetTemperature.getValue());

        if(shouldClose) {
            progress = 1-AnimationUtil.fadeOut(500, "NutritionScreen", false);
            if(progress == 0) {
                super.onClose();
            }
        }else {
            progress = AnimationUtil.fadeIn(500, "NutritionScreen", false);
        }
        NutritionBarWidget.progress = progress;
        ColoredCubeWidget.progress = progress;

        // 渲染背景
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();
        pose.translate((float) this.width /2, (float) this.height /2, 0);
        pose.scale(3-progress*2,3-progress*2,1.0f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, progress*progress);
        FGuis.fillRoundRect(pGuiGraphics,-100, -80, 200, 160, 0.05f,0x4091b6f7);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        pose.popPose();


        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
	public HealthStatMenu getMenu() {
		return menu;
	}
}
