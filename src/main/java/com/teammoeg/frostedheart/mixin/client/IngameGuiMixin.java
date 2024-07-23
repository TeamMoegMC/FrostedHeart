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
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

@Mixin(Gui.class)
public class IngameGuiMixin extends GuiComponent {

    /**
     * @author yuesha-yc
     * @reason change text position
     */
    @Overwrite
    public void renderItemName(PoseStack matrixStack) {
        Minecraft mc = Minecraft.getInstance();
        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();
        IngameGuiAccess access = (IngameGuiAccess) this;

        mc.getProfiler().push("selectedItemName");
        if (access.getRemainingHighlightTicks() > 0 && !access.getHighlightingItemStack().isEmpty()) {
            MutableComponent iformattabletextcomponent = (TranslateUtils.str("")).append(access.getHighlightingItemStack().getHoverName()).withStyle(access.getHighlightingItemStack().getRarity().color);
            if (access.getHighlightingItemStack().hasCustomHoverName()) {
                iformattabletextcomponent.withStyle(ChatFormatting.ITALIC);
            }

            Component highlightTip = access.getHighlightingItemStack().getHighlightTip(iformattabletextcomponent);
            int i = mc.font.width(highlightTip);
            int j = (scaledWidth - i) / 2;
            int k = scaledHeight - 59;
            if (!mc.gameMode.canHurtPlayer()) {
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
                fill(matrixStack, j - 2, k - 2, j + i + 2, k + 9 + 2, mc.options.getBackgroundColor(0));
                Font font = access.getHighlightingItemStack().getItem().getFontRenderer(access.getHighlightingItemStack());
                if (font == null) {
                    mc.font.drawShadow(matrixStack, highlightTip, (float) j, (float) k, 16777215 + (l << 24));
                } else {
                    j = (scaledWidth - font.width(highlightTip)) / 2;
                    font.drawShadow(matrixStack, highlightTip, (float) j, (float) k, 16777215 + (l << 24));
                }
                RenderSystem.disableBlend();
                RenderSystem.popMatrix();
            }
        }

        mc.getProfiler().pop();
    }
}
