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

package com.teammoeg.frostedheart.content.town.citizen.client;

import com.teammoeg.frostedheart.content.town.citizen.FakeCitizenEntity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 假居民渲染器：复用原版宽臂玩家模型与内置玩家皮肤。
 * 近距居民数量通常只有个位数到几十个，走标准实体渲染管线代价可忽略，
 * 换来的是完整的手臂/腿部行走摆动与原版光照。
 * <p>
 * Fake citizen renderer: reuses the vanilla wide-arm player model with a
 * deterministic built-in player skin. Near-range citizens typically number in the single
 * digits to dozens, so the standard entity render pipeline is negligible,
 * and in exchange we get full arm/leg walk animation and vanilla lighting.
 */
public class FakeCitizenRenderer extends MobRenderer<FakeCitizenEntity, PlayerModel<FakeCitizenEntity>> {

	public FakeCitizenRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(FakeCitizenEntity entity) {
		return CitizenSkins.textureFor(entity.getCitizenId());
	}
}
