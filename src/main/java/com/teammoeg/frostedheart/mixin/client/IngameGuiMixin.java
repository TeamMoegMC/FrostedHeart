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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

@Mixin(IngameGui.class)
public class IngameGuiMixin extends AbstractGui {

    /**
     * @author yuesha-yc
     * @reason change text position
     */
    @Overwrite
    public void renderItemName(MatrixStack matrixStack) {
        Minecraft mc = Minecraft.getInstance();
        int scaledWidth = mc.getMainWindow().getScaledWidth();
        int scaledHeight = mc.getMainWindow().getScaledHeight();
        IngameGuiAccess access = (IngameGuiAccess) this;

        mc.getProfiler().startSection("selectedItemName");
        if (access.getRemainingHighlightTicks() > 0 && !access.getHighlightingItemStack().isEmpty()) {
            IFormattableTextComponent iformattabletextcomponent = (TranslateUtils.str("")).appendSibling(access.getHighlightingItemStack().getDisplayName()).mergeStyle(access.getHighlightingItemStack().getRarity().color);
            if (access.getHighlightingItemStack().hasDisplayName()) {
                iformattabletextcomponent.mergeStyle(TextFormatting.ITALIC);
            }

            ITextComponent highlightTip = access.getHighlightingItemStack().getHighlightTip(iformattabletextcomponent);
            int i = mc.fontRenderer.getStringPropertyWidth(highlightTip);
            int j = (scaledWidth - i) / 2;
            int k = scaledHeight - 59;
            if (!mc.playerController.shouldDrawHUD()) {
                k += 14;
            }
            // FH STARTS
            k -= 24;
            j -= 1;
            // FH ENDS

            int l = (int) ((float) access.getRemainingHighlightTicks() * 256.0F / 10.0F);
            if (l > 255) {
                l = 255;
            }

            if (l > 0) {
                RenderSystem.pushMatrix();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                fill(matrixStack, j - 2, k - 2, j + i + 2, k + 9 + 2, mc.gameSettings.getChatBackgroundColor(0));
                FontRenderer font = access.getHighlightingItemStack().getItem().getFontRenderer(access.getHighlightingItemStack());
                if (font == null) {
                    mc.fontRenderer.drawTextWithShadow(matrixStack, highlightTip, (float) j, (float) k, 16777215 + (l << 24));
                } else {
                    j = (scaledWidth - font.getStringPropertyWidth(highlightTip)) / 2;
                    font.drawTextWithShadow(matrixStack, highlightTip, (float) j, (float) k, 16777215 + (l << 24));
                }
                RenderSystem.disableBlend();
                RenderSystem.popMatrix();
            }
        }

        mc.getProfiler().endSection();
    }
}
