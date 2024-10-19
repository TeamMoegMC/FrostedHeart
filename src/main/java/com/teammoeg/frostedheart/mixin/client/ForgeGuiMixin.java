/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.client.hud.FrostedHud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ForgeGui.class)
public class ForgeGuiMixin extends Gui {

    @Shadow private Font font;

    public ForgeGuiMixin(Minecraft pMinecraft, ItemRenderer pItemRenderer) {
        super(pMinecraft, pItemRenderer);
    }

    /**
     * @author yuesha-yc
     * @reason change style
     */
    @Overwrite(remap = false)
    public void renderAir(int width, int height, GuiGraphics stack) {
        Player player = FrostedHud.getRenderViewPlayer();
        if (player == null) return;
        int x = width / 2;
        FrostedHud.renderAirBar(stack, x, height, minecraft, player);
    }

    /**
     * @author yuesha-yc
     * @reason change text position
     */
    @Overwrite(remap = false)
    public void renderRecordOverlay(int width, int height, float partialTicks, GuiGraphics mStack) {
        if (overlayMessageTime > 0) {
            minecraft.getProfiler().push("overlayMessage");
            float hue = overlayMessageTime - partialTicks;
            int opacity = (int) (hue * 255.0F / 20.0F);
            if (opacity > 255) opacity = 255;

            if (opacity > 8) {
                mStack.pose().pushPose();
                mStack.pose().translate((float) width / 2, height - 68, 0.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                int color = (animateOverlayMessageColor ? Mth.hsvToRgb(hue / 50.0F, 0.7F, 0.6F) & 0xFFFFFF : 0xFFFFFF);

                int messageWidth = font.width(overlayMessageString);
                drawBackdrop(mStack, font, -4, messageWidth, 16777215 | (opacity << 24));
                // we changed pY position to -15
                mStack.drawString(font, overlayMessageString.getVisualOrderText(), -messageWidth / 2, -15, color | (opacity << 24));

                RenderSystem.disableBlend();
                mStack.pose().popPose();
            }

            minecraft.getProfiler().pop();
        }
    }

}
