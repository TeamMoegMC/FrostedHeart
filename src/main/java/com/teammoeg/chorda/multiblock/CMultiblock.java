/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.multiblock;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teammoeg.chorda.client.model.DynamicBlockModelReference;

import blusunrize.immersiveengineering.api.multiblocks.ClientMultiblocks.MultiblockManualData;
import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.client.utils.BasicClientProperties;
import blusunrize.immersiveengineering.client.utils.IERenderTypes;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public abstract class CMultiblock extends IETemplateMultiblock {
	DynamicBlockModelReference dm;
	public CMultiblock(ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size, MultiblockRegistration<?> baseState) {
        super(loc, masterFromOrigin, triggerFromOrigin, size, baseState);
        dm=DynamicBlockModelReference.getModelCached(loc.getNamespace(), "block/"+loc.getPath()).register();
    }

	@Override
    public boolean canBeMirrored() {
        return false;
    }
	@Override
	public void initializeClient(Consumer<MultiblockManualData> consumer)
	{
		consumer.accept(new BasicClientProperties(this) {

			@Override
			public void renderFormedStructure(PoseStack transform, MultiBufferSource bufferSource) {
				transform.pushPose();
				BlockPos offset = getMasterFromOriginOffset();
				transform.translate(offset.getX(), offset.getY(), offset.getZ());
				List<BakedQuad> nullQuads = dm.getAllQuads();
				VertexConsumer buffer = bufferSource.getBuffer(IERenderTypes.TRANSLUCENT_FULLBRIGHT);
				nullQuads.forEach(quad -> buffer.putBulkData(
						transform.last(), quad, 1, 1, 1, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
				));
				transform.popPose();
			}
			
		});
	}

	@Override
	public void disassemble(Level world, BlockPos origin, boolean mirrored, Direction clickDirectionAtCreation) {
        CMultiblockHelper.getBEHelperOptional(world, origin).ifPresent(te -> {
        	IMultiblockLogic<?> logic=te.getMultiblock().logic();
            if (logic instanceof DisassembleListener lis) {
            	lis.onDisassemble(this, te);
            }
        });
        super.disassemble(world, origin, mirrored, clickDirectionAtCreation);
	}
    public BlockPos getMasterPos(BlockPos origin, boolean mirrored, Direction clickDirectionAtCreation) {
        BlockPos master = this.getMasterFromOriginOffset();
        BlockPos offset = getAbsoluteOffset(master, mirrored, clickDirectionAtCreation);
        return origin.offset(offset);
    }
}
