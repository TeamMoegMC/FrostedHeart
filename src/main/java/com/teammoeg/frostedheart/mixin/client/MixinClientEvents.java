/*
 * Copyright (c) 2026 TeamMoeg
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
import com.simibubi.create.content.equipment.armor.BacktankArmorLayer;
import com.simibubi.create.foundation.events.ClientEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mixin(ClientEvents.class)
public class MixinClientEvents {
    /**
     * @author yuesha-yc
     * @reason fix overlay when the hotbar event is canceled
     */
    // TODO: Check if we stil need this
//    @SubscribeEvent
//    @Overwrite(remap = false)
//    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
//        PoseStack ms = event.getMatrixStack();
//        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
//        int light = 15728880;
//        int overlay = OverlayTexture.NO_OVERLAY;
//        float pt = event.getPartialTicks();
//        if (event.getType() == RenderGuiOverlayEvent.ElementType.AIR) {
//            BacktankArmorLayer.renderRemainingAirOverlay(ms, buffers, light, overlay, pt);
//        }
//
//        if (event.getType() == RenderGuiOverlayEvent.ElementType.SUBTITLES) {
//            FHClientEvents.onRenderHotbar(ms, buffers, light, overlay, pt);
//        }
//    }
}
