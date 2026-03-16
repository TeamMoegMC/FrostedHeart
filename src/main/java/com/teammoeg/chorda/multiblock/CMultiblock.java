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

/**
 * Chorda 多方块结构的抽象基类，扩展了沉浸工程的 {@link IETemplateMultiblock}。
 * 提供了以下功能：
 * <ul>
 *   <li>自动加载并注册动态方块模型，用于手册中的结构预览渲染</li>
 *   <li>禁用镜像功能</li>
 *   <li>在多方块拆解时通知实现了 {@link DisassembleListener} 的逻辑</li>
 *   <li>计算主方块在世界坐标中的绝对位置</li>
 * </ul>
 * <p>
 * Abstract base class for Chorda multiblock structures, extending Immersive Engineering's
 * {@link IETemplateMultiblock}. Provides the following features:
 * <ul>
 *   <li>Automatic loading and registration of dynamic block models for structure preview rendering in the manual</li>
 *   <li>Disabling mirror functionality</li>
 *   <li>Notifying {@link DisassembleListener} implementations when the multiblock is disassembled</li>
 *   <li>Computing the master block's absolute position in world coordinates</li>
 * </ul>
 *
 * @see CMultiblockHelper
 * @see DisassembleListener
 */
public abstract class CMultiblock extends IETemplateMultiblock {

	/** 用于手册渲染的动态方块模型引用 / Dynamic block model reference for manual rendering */
	DynamicBlockModelReference dm;

	/**
	 * 使用结构定义参数构造多方块，并自动加载对应的动态方块模型。
	 * <p>
	 * Constructs a multiblock with structure definition parameters and automatically loads
	 * the corresponding dynamic block model.
	 *
	 * @param loc 多方块结构的资源位置（同时用于加载模型）/ The resource location of the multiblock structure (also used for model loading)
	 * @param masterFromOrigin 主方块相对于原点的偏移 / The master block offset from the origin
	 * @param triggerFromOrigin 触发方块相对于原点的偏移 / The trigger block offset from the origin
	 * @param size 多方块结构的尺寸 / The size of the multiblock structure
	 * @param baseState 多方块的注册信息 / The multiblock registration
	 */
	public CMultiblock(ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size, MultiblockRegistration<?> baseState) {
        super(loc, masterFromOrigin, triggerFromOrigin, size, baseState);
        dm=DynamicBlockModelReference.getModelCached(loc.getNamespace(), "block/"+loc.getPath()).register();
    }

	/**
	 * 始终返回 false，禁止多方块结构的镜像放置。
	 * <p>
	 * Always returns false, disabling mirrored placement of the multiblock structure.
	 *
	 * @return 始终返回 false / Always returns false
	 */
	@Override
    public boolean canBeMirrored() {
        return false;
    }

	/**
	 * 初始化客户端渲染数据，提供用于沉浸工程手册中多方块结构预览的渲染逻辑。
	 * 使用动态模型的所有面片以全亮度半透明方式渲染成型后的结构。
	 * <p>
	 * Initializes client-side rendering data, providing rendering logic for the multiblock
	 * structure preview in the Immersive Engineering manual. Renders the formed structure
	 * using all quads from the dynamic model with full brightness and translucency.
	 *
	 * @param consumer 接受多方块手册数据的消费者 / The consumer that accepts multiblock manual data
	 */
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

	/**
	 * 拆解多方块结构。在调用父类拆解逻辑之前，先检查多方块逻辑是否实现了
	 * {@link DisassembleListener}，如果是则通知其进行拆解前的处理。
	 * <p>
	 * Disassembles the multiblock structure. Before calling the parent disassembly logic,
	 * checks if the multiblock logic implements {@link DisassembleListener} and notifies it
	 * for pre-disassembly handling if so.
	 *
	 * @param world 当前世界 / The current level
	 * @param origin 多方块原点位置 / The multiblock origin position
	 * @param mirrored 是否为镜像放置 / Whether the placement is mirrored
	 * @param clickDirectionAtCreation 创建时的点击方向 / The click direction at creation
	 */
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

	/**
	 * 根据原点位置、镜像状态和创建方向计算主方块在世界坐标中的绝对位置。
	 * <p>
	 * Computes the master block's absolute position in world coordinates based on the origin
	 * position, mirror state, and creation direction.
	 *
	 * @param origin 多方块原点位置 / The multiblock origin position
	 * @param mirrored 是否为镜像放置 / Whether the placement is mirrored
	 * @param clickDirectionAtCreation 创建时的点击方向 / The click direction at creation
	 * @return 主方块的世界坐标绝对位置 / The master block's absolute position in world coordinates
	 */
    public BlockPos getMasterPos(BlockPos origin, boolean mirrored, Direction clickDirectionAtCreation) {
        BlockPos master = this.getMasterFromOriginOffset();
        BlockPos offset = getAbsoluteOffset(master, mirrored, clickDirectionAtCreation);
        return origin.offset(offset);
    }
}
