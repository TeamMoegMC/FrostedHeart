package com.teammoeg.frostedheart.common.block;

import blusunrize.immersiveengineering.common.blocks.IEMultiblockBlock;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StoneMultiBlock<T extends MultiblockPartTileEntity<? super T>> extends IEMultiblockBlock {
    private RegistryObject<TileEntityType<T>> type;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public StoneMultiBlock(String name, RegistryObject<TileEntityType<T>> type) {
        super(name, Properties.create(Material.ROCK).hardnessAndResistance(2.0F, 20.0F).notSolid());
        this.type = type;
        this.lightOpacity = 0;
    }

    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(LIT);
    }

    @Nullable
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return ((TileEntityType) this.type.get()).create();
    }
}
