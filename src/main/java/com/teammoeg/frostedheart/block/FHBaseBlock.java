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

package com.teammoeg.frostedheart.block;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.FHContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.item.Item;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import java.util.function.BiFunction;

public class FHBaseBlock extends Block implements IWaterLoggable {
    public final String name;
    protected int lightOpacity;

    public FHBaseBlock(String name, Properties blockProps, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(blockProps.variableOpacity());
        this.name = name;
        lightOpacity = 15;

        this.setDefaultState(getInitDefaultState());
        ResourceLocation registryName = createRegistryName();
        setRegistryName(registryName);

        FHContent.registeredFHBlocks.add(this);
        Item item = createItemBlock.apply(this, new Item.Properties().group(FHMain.itemGroup));
        if (item != null) {
            item.setRegistryName(registryName);
            FHContent.registeredFHItems.add(item);
        }
    }

    public ResourceLocation createRegistryName() {
        return new ResourceLocation(FHMain.MODID, name);
    }

    protected BlockState getInitDefaultState() {
        BlockState state = this.stateContainer.getBaseState();
        if (state.hasProperty(BlockStateProperties.WATERLOGGED))
            state = state.with(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
        return state;
    }

    public FHBaseBlock setLightOpacity(int opacity) {
        lightOpacity = opacity;
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity(BlockState state, IBlockReader worldIn, BlockPos pos) {
        if (state.isOpaqueCube(worldIn, pos))
            return lightOpacity;
        else
            return state.propagatesSkylightDown(worldIn, pos) ? 0 : 1;
    }
}
