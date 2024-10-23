/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.base.multiblock.FHBaseMultiblock;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class T2GeneratorMultiblock extends FHBaseMultiblock {
    @OnlyIn(Dist.CLIENT)
    private static ItemStack renderStack;

    public T2GeneratorMultiblock() {
        super(new ResourceLocation(FHMain.MODID, "multiblocks/generator_t2"),
                new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(3, 7, 3),
                () -> FHMultiblocks);
    }


    @Override
    public float getManualScale() {
        return 14;
    }

}
