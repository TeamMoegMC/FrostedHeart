/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.crucible;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.multiblock.FHBaseMultiblock;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class CrucibleMultiblock extends FHBaseMultiblock {
    public CrucibleMultiblock() {
        super(new ResourceLocation(FHMain.MODID, "multiblocks/crucible"),
                new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(3, 4, 3),
                () -> FHContent.FHMultiblocks.crucible.getDefaultState());
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
