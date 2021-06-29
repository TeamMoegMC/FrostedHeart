package com.teammoeg.frostedheart.common;

import blusunrize.immersiveengineering.common.blocks.multiblocks.StoneMultiblock;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class GeneratorMultiblock extends StoneMultiblock {
    public GeneratorMultiblock() {
        super(new ResourceLocation(FHMain.MODID, "multiblocks/generator"),
                new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(3, 4, 3),
                () -> FHBlocks.generator.getDefaultState());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean canRenderFormedStructure() {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderFormedStructure(MatrixStack transform, IRenderTypeBuffer buffer) {
    }

    @Override
    public float getManualScale() {
        return 16;
    }
}
