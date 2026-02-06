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

import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
@Mixin(PlayerRenderer.class)
public abstract class PlayerRenderer_isShaking extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {


	public PlayerRenderer_isShaking(Context pContext, PlayerModel<AbstractClientPlayer> pModel, float pShadowRadius) {
		super(pContext, pModel, pShadowRadius);
	}

	@Override
	protected boolean isShaking(AbstractClientPlayer pEntity) {
		return pEntity.hasEffect(FHMobEffects.HYPOTHERMIA.get());
	}
}
