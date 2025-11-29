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
