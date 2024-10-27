package com.teammoeg.frostedheart.client.renderer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;

public class HalfShaftRenderer extends KineticBlockEntityRenderer<KineticBlockEntity> {


	public HalfShaftRenderer(Context context) {
		super(context);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(KineticBlockEntity te,BlockState bs) {
		return CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, bs);
	}

}