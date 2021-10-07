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
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static com.teammoeg.frostedheart.client.hud.FrostedHud.OXYGEN;

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
            float hue = (float) overlayMessageTime - partialTicks;
            int opacity = (int) (hue * 255.0F / 20.0F);
            if (opacity > 255) opacity = 255;

            if (opacity > 8) {
                RenderSystem.pushMatrix();
                RenderSystem.translatef((float) (width / 2), (float) (height - 68), 0.0F);
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
        mc.getProfiler().startSection("air");
        int x = width / 2;
        int y = height;

        RenderSystem.enableBlend();
        int air = player.getAir();
        int maxAir = 300;
        if (player.areEyesInFluid(FluidTags.WATER) || air < maxAir) {
            mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);

            mc.ingameGUI.blit(stack, x + FrostedHud.BasePos.right_half_3.getB().getA(), y + FrostedHud.BasePos.right_half_3.getB().getB(), FrostedHud.UV.right_half_frame.x, FrostedHud.UV.right_half_frame.y, FrostedHud.UV.right_half_frame.w, FrostedHud.UV.right_half_frame.h);
            if (air <= 30) {
                mc.ingameGUI.blit(stack, x + FrostedHud.IconPos.right_half_3.getB().getA(), y + FrostedHud.IconPos.right_half_3.getB().getB(), FrostedHud.UV.icon_oxygen_abnormal_white.x, FrostedHud.UV.icon_oxygen_abnormal_white.y, FrostedHud.UV.icon_oxygen_abnormal_white.w, FrostedHud.UV.icon_oxygen_abnormal_white.h);
            } else {
                mc.ingameGUI.blit(stack, x + FrostedHud.IconPos.right_half_3.getB().getA(), y + FrostedHud.IconPos.right_half_3.getB().getB(), FrostedHud.UV.icon_oxygen_normal.x, FrostedHud.UV.icon_oxygen_normal.y, FrostedHud.UV.icon_oxygen_normal.w, FrostedHud.UV.icon_oxygen_normal.h);
            }
            int airState = MathHelper.ceil(air / (float) maxAir * 100) - 1;
            int airCol = airState / 10;
            int airRow = airState % 10;
            mc.getTextureManager().bindTexture(OXYGEN);
            mc.ingameGUI.blit(stack, x + FrostedHud.BarPos.right_half_3.getB().getA(), y + FrostedHud.BarPos.right_half_3.getB().getB(), airCol * FrostedHud.UV.oxygen_bar.w, airRow * FrostedHud.UV.oxygen_bar.h, FrostedHud.UV.oxygen_bar.w, FrostedHud.UV.oxygen_bar.h, 160, 320);
        }
        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

}
