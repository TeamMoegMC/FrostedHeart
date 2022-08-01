/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.client.hud.FrostedHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ForgeIngameGui.class)
public class ForgeIngameGuiMixin extends IngameGui {

    public ForgeIngameGuiMixin(Minecraft mcIn) {
        super(mcIn);
    }

    /**
     * @author yuesha-yc
     * @reason change text position
     */
    @Overwrite(remap = false)
    public void renderRecordOverlay(int width, int height, float partialTicks, MatrixStack mStack) {
        if (overlayMessageTime > 0) {
            mc.getProfiler().startSection("overlayMessage");
            float hue = overlayMessageTime - partialTicks;
            int opacity = (int) (hue * 255.0F / 20.0F);
            if (opacity > 255) opacity = 255;

            if (opacity > 8) {
                RenderSystem.pushMatrix();
                RenderSystem.translatef(width / 2, height - 68, 0.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                int color = (animateOverlayMessageColor ? MathHelper.hsvToRGB(hue / 50.0F, 0.7F, 0.6F) & 0xFFFFFF : 0xFFFFFF);
                // original: Minecraft.getInstance().fontRenderer.func_238422_b_(mStack, overlayMessage.func_241878_f(), -Minecraft.getInstance().fontRenderer.getStringPropertyWidth(overlayMessage) / 2, -4, color | (opacity << 24));
                // new:
                Minecraft.getInstance().fontRenderer.func_238422_b_(mStack, overlayMessage.func_241878_f(), -Minecraft.getInstance().fontRenderer.getStringPropertyWidth(overlayMessage) / 2, -15, color | (opacity << 24));
                renderChatBackground(mStack, Minecraft.getInstance().fontRenderer, -4, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(overlayMessage), 16777215 | (opacity << 24));
                RenderSystem.disableBlend();
                RenderSystem.popMatrix();
            }

            mc.getProfiler().endSection();
        }
    }

    /**
     * @author yuesha-yc
     * @reason change style
     */
    @Overwrite(remap = false)
    public void renderAir(int width, int height, MatrixStack stack) {
        PlayerEntity player = FrostedHud.getRenderViewPlayer();
        if (player == null) return;
        int x = width / 2;
        int y = height;
        FrostedHud.renderAirBar(stack, x, y, mc, player);
    }

}
