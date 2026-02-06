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

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CuriosityEntityRenderer extends MobRenderer<CuriosityEntity, CuriosityEntityModel<CuriosityEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FHMain.MODID, "textures/entity/curiosity_entity.png");

    public CuriosityEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CuriosityEntityModel<>(ctx.bakeLayer(CuriosityEntityModel.LAYER_LOCATION)), 1.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(CuriosityEntity entity) {
        return TEXTURE;
    }
}
