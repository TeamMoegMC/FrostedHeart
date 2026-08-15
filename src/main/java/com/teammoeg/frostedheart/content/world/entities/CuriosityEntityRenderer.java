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
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 核心渲染器：半透明纱质胞体，仅在 EXPOSED 阶段可见（其余阶段实体不可见）。
 * <p>
 * Renderer for the exposed core: translucent gauze-like cluster. The entity is
 * invisible in every other phase, so the vanilla invisibility check hides it.
 */
public class CuriosityEntityRenderer extends MobRenderer<CuriosityEntity, CuriosityEntityModel<CuriosityEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FHMain.MODID, "textures/entity/curiosity_entity.png");

    public CuriosityEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CuriosityEntityModel<>(ctx.bakeLayer(CuriosityEntityModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CuriosityEntity entity) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(CuriosityEntity entity, boolean visible, boolean invisibleToPlayer, boolean glowing) {
        return RenderType.entityTranslucent(TEXTURE);
    }

    @Override
    protected void scale(CuriosityEntity entity, PoseStack stack, float partialTick) {
        float pulse = 1.0F + Mth.sin((entity.tickCount + partialTick) * 0.1F) * 0.03F;
        stack.scale(pulse, pulse, pulse);
    }
}
