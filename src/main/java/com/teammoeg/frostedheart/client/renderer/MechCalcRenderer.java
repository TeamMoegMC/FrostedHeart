package com.teammoeg.frostedheart.client.renderer;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.research.machines.MechCalcTileEntity;

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
import net.minecraft.util.math.vector.Quaternion;

public class MechCalcRenderer extends TileEntityRenderer<MechCalcTileEntity> {
	public static DynamicModel<Direction> MODEL;
	public static IEObjState drum=new IEObjState(VisibilityList.show("c7"));
	public static IEObjState register=new IEObjState(VisibilityList.show("c6"));
	public MechCalcRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
		super(rendererDispatcherIn);
	}

	@Override
	public void render(MechCalcTileEntity te, float partialTicks, MatrixStack matrixStack,
			IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
		BlockPos blockPos = te.getPos();
		BlockState state = te.getWorld().getBlockState(blockPos);
		if(state.getBlock()!=FHBlocks.mech_calc)
			return;
		matrixStack.push();
		Direction rd=te.getDirection().rotateY();
		double forward=(te.process/1067)/16d;
		matrixStack.translate(rd.getXOffset()*forward, 0,rd.getZOffset()*forward);
		List<BakedQuad> quads = MODEL.getNullQuads(te.getDirection(), state, new SinglePropertyModelData<>(register, Model.IE_OBJ_STATE));
		RenderUtils.renderModelTESRFast(quads, bufferIn.getBuffer(RenderType.getSolid()), matrixStack, combinedLightIn, combinedOverlayIn);
		matrixStack.pop();
		Direction fw=te.getDirection();
		matrixStack.push();
		int deg=0,dx=0,dz=0;
		switch(fw) {
		case SOUTH:deg=180;dx=-1;dz=-1;break;
		case EAST:deg=-90;dz=-1;break;
		case WEST:deg=90;dx=-1;break;
		}
		matrixStack.rotate(new Quaternion(0,deg,0,true));
		matrixStack.translate(dx,0, dz);
		
		matrixStack.translate(0,13.75/16,7d/16);
		float rotn=((te.process)%160)*2.25f;
		Quaternion rot=new Quaternion(rotn,0f,0f,true);
		matrixStack.rotate(rot);
	
		matrixStack.translate(0,-1.5/16,-1.5/16);
		quads = MODEL.getNullQuads(Direction.NORTH, state, new SinglePropertyModelData<>(drum, Model.IE_OBJ_STATE));
		RenderUtils.renderModelTESRFast(quads, bufferIn.getBuffer(RenderType.getSolid()), matrixStack, combinedLightIn, combinedOverlayIn);
		matrixStack.pop();
	}

}
