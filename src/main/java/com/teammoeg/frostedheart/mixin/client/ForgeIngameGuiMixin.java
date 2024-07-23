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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.client.hud.FrostedHud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.ForgeIngameGui;

@Mixin(ForgeIngameGui.class)
public class ForgeIngameGuiMixin extends Gui {

    public ForgeIngameGuiMixin(Minecraft mcIn) {
        super(mcIn);
    }

    /**
     * @author yuesha-yc
     * @reason change style
     */
    @Overwrite(remap = false)
    public void renderAir(int width, int height, PoseStack stack) {
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
    public void renderRecordOverlay(int width, int height, float partialTicks, PoseStack mStack) {
        if (overlayMessageTime > 0) {
            minecraft.getProfiler().push("overlayMessage");
            float hue = overlayMessageTime - partialTicks;
            int opacity = (int) (hue * 255.0F / 20.0F);
            if (opacity > 255) opacity = 255;

            if (opacity > 8) {
                RenderSystem.pushMatrix();
                RenderSystem.translatef((float) width / 2, height - 68, 0.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                int color = (animateOverlayMessageColor ? Mth.hsvToRgb(hue / 50.0F, 0.7F, 0.6F) & 0xFFFFFF : 0xFFFFFF);
                // original: Minecraft.getInstance().fontRenderer.draw(mStack, overlayMessage.getVisualOrderText(), -Minecraft.getInstance().fontRenderer.getStringPropertyWidth(overlayMessage) / 2, -4, color | (opacity << 24));
                // new:
                Minecraft.getInstance().font.draw(mStack, overlayMessageString.getVisualOrderText(), (float) -Minecraft.getInstance().font.width(overlayMessageString) / 2, -15, color | (opacity << 24));
                drawBackdrop(mStack, Minecraft.getInstance().font, -4, Minecraft.getInstance().font.width(overlayMessageString), 16777215 | (opacity << 24));
                RenderSystem.disableBlend();
                RenderSystem.popMatrix();
            }

            minecraft.getProfiler().pop();
        }
    }

}
