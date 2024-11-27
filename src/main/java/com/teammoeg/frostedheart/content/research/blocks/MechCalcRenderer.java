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

package com.teammoeg.frostedheart.content.research.blocks;

import java.util.List;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.foundation.model.DynamicBlockModelReference;

import blusunrize.immersiveengineering.api.IEProperties.VisibilityList;
import blusunrize.immersiveengineering.client.models.obj.callback.DynamicSubmodelCallbacks;
import blusunrize.immersiveengineering.client.utils.RenderUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;

public class MechCalcRenderer implements BlockEntityRenderer<MechCalcTileEntity> {
    public static DynamicBlockModelReference MODEL;
    public static VisibilityList drum = VisibilityList.show("c7");
    public static VisibilityList register = VisibilityList.show("c6");

    public MechCalcRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn) {
    }

    @Override
    public void render(MechCalcTileEntity te, float partialTicks, PoseStack matrixStack,
                       MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        BlockPos blockPos = te.getBlockPos();
        BlockState state = te.getLevel().getBlockState(blockPos);
        if (state.getBlock() != FHBlocks.MECHANICAL_CALCULATOR.get())
            return;
        Direction rd = te.getDirection().getClockWise();
       
        matrixStack.pushPose();
        matrixStack.rotateAround(new Quaternionf().rotateY(te.getDirection().toYRot()/180F*Mth.PI), 0.5f, 0.5f, 0.5f);
        double forward = ((double) te.process / 1067) / 16d;
        matrixStack.translate(rd.getStepX() * forward, 0, rd.getStepZ() * forward);
        
        List<BakedQuad> quads = MODEL.apply(ModelData.builder().with(DynamicSubmodelCallbacks.getProperty(), register).build());
        RenderUtils.renderModelTESRFast(quads, bufferIn.getBuffer(RenderType.solid()), matrixStack, combinedLightIn, combinedOverlayIn);
        matrixStack.popPose();
        Direction fw = te.getDirection();
        matrixStack.pushPose();
        int deg = 0, dx = 0, dz = 0;
        switch (fw) {
            case SOUTH:
                deg = 180;
                dx = -1;
                dz = -1;
                break;
            case EAST:
                deg = -90;
                dz = -1;
                break;
            case WEST:
                deg = 90;
                dx = -1;
                break;
        }
        matrixStack.mulPose(new Quaternionf().rotateY(deg*Mth.PI/180f));
        matrixStack.translate(dx, 0, dz);

        matrixStack.translate(0, 13.75 / 16, 7d / 16);
        float rotn = ((te.process) % 160) * 2.25f;
        Quaternionf rot = new Quaternionf().rotateX((rotn*Mth.PI/180f));
        matrixStack.mulPose(rot);

        matrixStack.translate(0, -1.5 / 16, -1.5 / 16);
        quads = MODEL.apply(ModelData.builder().with(DynamicSubmodelCallbacks.getProperty(), drum).build());
        RenderUtils.renderModelTESRFast(quads, bufferIn.getBuffer(RenderType.solid()), matrixStack, combinedLightIn, combinedOverlayIn);
        matrixStack.popPose();
    }

}
