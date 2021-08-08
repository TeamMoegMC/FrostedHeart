package com.teammoeg.frostedheart.block;

import blusunrize.immersiveengineering.common.blocks.IEMultiblockBlock;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CrucibleBlock<T extends MultiblockPartTileEntity<? super T>> extends IEMultiblockBlock {
    private RegistryObject<TileEntityType<T>> type;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public CrucibleBlock(String name, RegistryObject type) {
        super(name, Properties.create(Material.IRON).hardnessAndResistance(4.0F, 40.0F).notSolid());
        this.type = type;
        this.setDefaultState(this.stateContainer.getBaseState().with(LIT, false));
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return type.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public ResourceLocation createRegistryName() {
        return new ResourceLocation(FHMain.MODID, name);
    }
    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(LIT);
    }
}

