package com.teammoeg.frostedheart.client.renderer;

import blusunrize.immersiveengineering.api.IEProperties.IEObjState;
import blusunrize.immersiveengineering.api.IEProperties.Model;
import blusunrize.immersiveengineering.api.IEProperties.VisibilityList;
import blusunrize.immersiveengineering.api.utils.client.SinglePropertyModelData;
import blusunrize.immersiveengineering.client.render.tile.DynamicModel;
import blusunrize.immersiveengineering.client.utils.RenderUtils;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.content.generator.t2.T2GeneratorTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class T2GeneratorRenderer extends TileEntityRenderer<T2GeneratorTileEntity> {
    public static DynamicModel<Direction> FUEL;

    public T2GeneratorRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);
    }

    @Override
    public void render(T2GeneratorTileEntity te, float partialTicks, MatrixStack matrixStack,
                       IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        if (!te.formed || te.isDummy() || !te.getWorldNonnull().isBlockLoaded(te.getPos()))
            return;
        if (te.process > 0 || !te.getInventory().get(0).isEmpty()) {

        } else
            return;
        BlockPos blockPos = te.getPos();
        BlockState state = te.getWorld().getBlockState(blockPos);
        if (state.getBlock() != FHMultiblocks.generator_t2)
            return;
        IEObjState objState = new IEObjState(VisibilityList.showAll());

        matrixStack.push();
        List<BakedQuad> quads = FUEL.getNullQuads(te.getFacing(), state, new SinglePropertyModelData<>(objState, Model.IE_OBJ_STATE));
        RenderUtils.renderModelTESRFast(quads, bufferIn.getBuffer(RenderType.getSolid()), matrixStack, combinedLightIn, combinedOverlayIn);
        matrixStack.pop();
    }

}
