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

package com.teammoeg.frostedheart.base.block;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.FHUtils;

import blusunrize.immersiveengineering.common.blocks.IEMultiblockBlock;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

import net.minecraft.block.AbstractBlock.Properties;

public class FHStoneMultiblockBlock<T extends MultiblockPartTileEntity<? super T>> extends IEMultiblockBlock<T> {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public FHStoneMultiblockBlock(String name, Properties props, RegistryObject<TileEntityType<T>> type) {
        super(name, props, type);
        this.lightOpacity = 0;
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    public FHStoneMultiblockBlock(String name, RegistryObject<TileEntityType<T>> type) {
        super(name, Properties.of(Material.STONE).strength(2.0F, 20.0F).noOcclusion().lightLevel(FHUtils.getLightValueLit(15)), type);
        this.lightOpacity = 0;
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    @Override
    public ResourceLocation createRegistryName() {
        return new ResourceLocation(FHMain.MODID, name);
    }

   /* @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return type.get().create();
    }*/

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }
}
