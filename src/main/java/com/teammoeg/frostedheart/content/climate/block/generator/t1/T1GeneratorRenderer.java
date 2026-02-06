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

package com.teammoeg.frostedheart.content.climate.block.generator.t1;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.model.DynamicBlockModelReference;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelperMaster;
import blusunrize.immersiveengineering.api.multiblocks.blocks.registry.MultiblockBlockEntityMaster;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockOrientation;
import blusunrize.immersiveengineering.client.render.tile.BERenderUtils;
import blusunrize.immersiveengineering.client.utils.RenderUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public class T1GeneratorRenderer implements BlockEntityRenderer<MultiblockBlockEntityMaster<T1GeneratorState>> {
    public static DynamicBlockModelReference FUEL;

    public T1GeneratorRenderer(BlockEntityRendererProvider.Context pContext) {

    }

    @Override
    public void render(MultiblockBlockEntityMaster<T1GeneratorState> blockEntity, float partialTicks, PoseStack matrixStack,
                       MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        final IMultiblockBEHelperMaster<T1GeneratorState> helper = blockEntity.getHelper();
        final T1GeneratorState state = helper.getState();
        final MultiblockOrientation orientation = helper.getContext().getLevel().getOrientation();

        if (state.hasFuel()) {
            matrixStack.pushPose();
            bufferIn = BERenderUtils.mirror(orientation, matrixStack, bufferIn);
            Direction facing = orientation.front();
            matrixStack.rotateAround(ClientUtils.DIR_TO_FACING.apply(facing), 0.5f, 0.5f, 0.5f);
            List<BakedQuad> quads = FUEL.getAllQuads();
            RenderUtils.renderModelTESRFast(quads, bufferIn.getBuffer(RenderType.solid()), matrixStack, combinedLightIn, combinedOverlayIn);
            matrixStack.popPose();
        }
    }

}
