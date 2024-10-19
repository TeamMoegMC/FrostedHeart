/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.client.model.DynamicBlockModelReference;

import blusunrize.immersiveengineering.api.IEProperties.IEObjState;
import blusunrize.immersiveengineering.api.IEProperties.Model;
import blusunrize.immersiveengineering.api.IEProperties.VisibilityList;
import blusunrize.immersiveengineering.api.utils.client.SinglePropertyModelData;
import blusunrize.immersiveengineering.client.render.tile.DynamicModel;
import blusunrize.immersiveengineering.client.utils.RenderUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class T2GeneratorRenderer extends BlockEntityRenderer<T2GeneratorTileEntity> {
    public static DynamicBlockModelReference FUEL;

    public T2GeneratorRenderer(BlockEntityRenderDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);
    }

    @Override
    public void render(T2GeneratorTileEntity te, float partialTicks, PoseStack matrixStack,
                       MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        if (!te.formed || te.isDummy() || !te.getWorldNonnull().hasChunkAt(te.getBlockPos()))
            return;
        if (!te.hasFuel())
        	return;
        BlockPos blockPos = te.getBlockPos();
        BlockState state = te.getLevel().getBlockState(blockPos);
        if (state.getBlock() != FHMultiblocks.generator_t2)
            return;
        IEObjState objState = new IEObjState(VisibilityList.showAll());

        matrixStack.pushPose();
        List<BakedQuad> quads = FUEL.getNullQuads(te.getFacing(), state, new SinglePropertyModelData<>(objState, Model.IE_OBJ_STATE));
        RenderUtils.renderModelTESRFast(quads, bufferIn.getBuffer(RenderType.solid()), matrixStack, combinedLightIn, combinedOverlayIn);
        matrixStack.popPose();
    }

}
