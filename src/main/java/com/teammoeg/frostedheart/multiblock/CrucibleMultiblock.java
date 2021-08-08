package com.teammoeg.frostedheart.multiblock;

import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class CrucibleMultiblock extends IETemplateMultiblock {
    public CrucibleMultiblock() {
        super(new ResourceLocation(FHMain.MODID, "multiblocks/crucible"),
                new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(3, 4, 3),
                () -> FHContent.Multiblocks.crucible.getDefaultState());
    }

    @Override
    public boolean canBeMirrored() {
        return false;
    }

    @Override
    public Direction transformDirection(Direction original) {
        return original.getOpposite();
    }

    @Override
    public Direction untransformDirection(Direction transformed) {
        return transformed.getOpposite();
    }

    @Override
    public BlockPos multiblockToModelPos(BlockPos posInMultiblock) {
        return super.multiblockToModelPos(new BlockPos(
                getSize(null).getX() - posInMultiblock.getX() - 1,
                posInMultiblock.getY(),
                getSize(null).getZ() - posInMultiblock.getZ() - 1
        ));
    }

    @Override
    public float getManualScale() {
        return 0;
    }

    @Override
    public boolean canRenderFormedStructure() {
        return false;
    }

    @Override
    public void renderFormedStructure(MatrixStack transform, IRenderTypeBuffer buffer) {

    }
}
