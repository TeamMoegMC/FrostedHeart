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

package com.teammoeg.frostedheart.content.climate.block.generator.t1;

import com.teammoeg.chorda.multiblock.CMultiblock;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.block.generator.HeatingMultiblock;

import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class T1GeneratorMultiblock extends HeatingMultiblock {
    @OnlyIn(Dist.CLIENT)
    private static ItemStack renderStack;

    public T1GeneratorMultiblock() {
        super(new ResourceLocation(FHMain.MODID, "multiblocks/generator"),
                new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(3, 4, 3), FHMultiblocks.Registration.GENERATOR_T1
        );
    }

    @Override
    public void disassemble(Level world, BlockPos origin, boolean mirrored, Direction clickDirectionAtCreation) {
        CMultiblockHelper.getBEHelperOptional(world, origin).ifPresent(te -> {
            T1GeneratorState state = (T1GeneratorState) te.getState();
            if (state != null) {
                state.getDataNoCheck().ifPresent(data -> {
                    data.actualPos = null;    // 取消绑定，城镇 tick 将跳过
                });
            }
        });
        super.disassemble(world, origin, mirrored, clickDirectionAtCreation);
    }

    @Override
    public float getManualScale() {
        return 16;
    }



}
