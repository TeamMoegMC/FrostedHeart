package com.teammoeg.frostedheart.client.renderer;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHContent.FHMultiblocks;
import com.teammoeg.frostedheart.content.generator.t1.T1GeneratorTileEntity;

import blusunrize.immersiveengineering.api.IEProperties.IEObjState;
import blusunrize.immersiveengineering.api.IEProperties.Model;
import blusunrize.immersiveengineering.api.IEProperties.VisibilityList;
import blusunrize.immersiveengineering.api.utils.client.SinglePropertyModelData;
import blusunrize.immersiveengineering.client.render.tile.DynamicModel;
import blusunrize.immersiveengineering.client.utils.RenderUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class T1GeneratorRenderer extends TileEntityRenderer<T1GeneratorTileEntity> {
	public static DynamicModel<Direction> FUEL;
	public T1GeneratorRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
		super(rendererDispatcherIn);
	}

	@Override
	public void render(T1GeneratorTileEntity te, float partialTicks, MatrixStack matrixStack,
			IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
		if(!te.formed||te.isDummy()||!te.getWorldNonnull().isBlockLoaded(te.getPos()))
			return;
		List<String> renderedParts = new ArrayList<>();
		if(te.process>0||!te.getInventory().get(0).isEmpty()) {
			renderedParts.add("FCS");
			renderedParts.add("FCN");
			renderedParts.add("FCE");
			renderedParts.add("FCW");
			renderedParts.add("Mid");
		}
		if(renderedParts.isEmpty())
			return;
		BlockPos blockPos = te.getPos();
		BlockState state = te.getWorld().getBlockState(blockPos);
		if(state.getBlock()!=FHMultiblocks.generator)
			return;
		IEObjState objState = new IEObjState(VisibilityList.show(renderedParts));
		
		matrixStack.push();
		List<BakedQuad> quads = FUEL.getNullQuads(te.getFacing(), state, new SinglePropertyModelData<>(objState, Model.IE_OBJ_STATE));
		RenderUtils.renderModelTESRFast(quads, bufferIn.getBuffer(RenderType.getSolid()), matrixStack, combinedLightIn, combinedOverlayIn);
		matrixStack.pop();
	}

}
