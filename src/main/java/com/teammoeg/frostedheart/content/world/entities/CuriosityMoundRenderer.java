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

package com.teammoeg.frostedheart.content.world.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.model.data.ModelData;

/**
 * 隆起 mound 渲染器：快速升起又消散的半透明雪丘（复用原版雪块贴图）。
 * <p>
 * Renders the tracker's surface mound as a translucent snow dome that rises
 * and fades quickly, reusing the vanilla snow block texture.
 */
public class CuriosityMoundRenderer extends EntityRenderer<CuriosityMoundEntity> {
    private static final ResourceLocation SNOW_TEXTURE = new ResourceLocation("textures/block/snow.png");

    public CuriosityMoundRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(CuriosityMoundEntity entity) {
        return SNOW_TEXTURE;
    }

    @Override
    public void render(CuriosityMoundEntity entity, float yaw, float partialTick, PoseStack stack,
                       MultiBufferSource buffer, int packedLight) {
        float life = Mth.clamp((entity.tickCount + partialTick) / 10.0F, 0.0F, 1.0F);
        float alpha = life < 0.5F ? life * 2.0F : 2.0F - life * 2.0F;
        float scale = 0.4F + life * 0.6F;
        stack.pushPose();
        stack.translate(-0.5F, 0.0F, -0.5F);
        stack.scale(scale, scale * 0.55F, scale);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.SNOW_BLOCK.defaultBlockState(), stack, buffer, packedLight, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, RenderType.entityTranslucent(SNOW_TEXTURE));
        stack.popPose();
        super.render(entity, yaw, partialTick, stack, buffer, packedLight);
    }
}
