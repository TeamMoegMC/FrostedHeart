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
import com.simibubi.create.content.curiosities.armor.CopperBacktankArmorLayer;
import com.simibubi.create.events.ClientEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mixin(ClientEvents.class)
public class MixinClientEvents {
    /**
     * @author yuesha-yc
     * @reason fix overlay when the hotbar event is canceled
     */
    @SubscribeEvent
    @Overwrite(remap = false)
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        MatrixStack ms = event.getMatrixStack();
        IRenderTypeBuffer.Impl buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        int light = 15728880;
        int overlay = OverlayTexture.NO_OVERLAY;
        float pt = event.getPartialTicks();
        if (event.getType() == RenderGameOverlayEvent.ElementType.AIR) {
            CopperBacktankArmorLayer.renderRemainingAirOverlay(ms, buffers, light, overlay, pt);
        }

        if (event.getType() == RenderGameOverlayEvent.ElementType.SUBTITLES) {
            ClientEvents.onRenderHotbar(ms, buffers, light, overlay, pt);
        }
    }
}
