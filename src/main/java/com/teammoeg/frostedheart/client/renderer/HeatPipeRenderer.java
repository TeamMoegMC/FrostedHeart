package com.teammoeg.frostedheart.client.renderer;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHContent.FHBlocks;
import com.teammoeg.frostedheart.content.steamenergy.HeatPipeBlock;
import com.teammoeg.frostedheart.content.steamenergy.HeatPipeTileEntity;

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
import net.minecraft.world.World;

public class HeatPipeRenderer extends TileEntityRenderer<HeatPipeTileEntity> {
	public static DynamicModel<Void> RIM;
	public HeatPipeRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
		super(rendererDispatcherIn);
	}

	@Override
	public void render(HeatPipeTileEntity te, float partialTicks, MatrixStack matrixStack,
			IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
		BlockState state=te.getBlockState();
		BlockPos pos = te.getPos();
		World w=te.getWorld();
		if(state.getBlock()!=FHBlocks.heat_pipe)
			return;
		List<String> renderedParts = new ArrayList<>();
		HeatPipeBlock pipe=(HeatPipeBlock) FHBlocks.heat_pipe;
		for (Direction d : Direction.values())
			if(pipe.shouldDrawRim(w,pos, state, d))
				renderedParts.add(d.getName2());
		if(pipe.shouldDrawCasing(w,pos, state))
			renderedParts.add("casing");
		if(renderedParts.isEmpty())
			return;
		IEObjState objState = new IEObjState(VisibilityList.show(renderedParts));
		
		matrixStack.push();
		List<BakedQuad> quads = RIM.getNullQuads(null,state, new SinglePropertyModelData<>(objState, Model.IE_OBJ_STATE));
		RenderUtils.renderModelTESRFast(quads, bufferIn.getBuffer(RenderType.getSolid()), matrixStack, combinedLightIn, combinedOverlayIn);
		matrixStack.pop();
	}

}
