package com.teammoeg.frostedheart.content.incubator;

import com.teammoeg.frostedheart.base.block.FHGuiBlock;
import com.teammoeg.frostedheart.base.item.FHBlockItem;
import com.teammoeg.frostedheart.content.steamenergy.ISteamEnergyBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.RegistryObject;
import javax.annotation.Nullable;

public class IncubatorBlock extends FHGuiBlock implements ILiquidContainer {
    static DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    static BooleanProperty LIT = BlockStateProperties.LIT;
    private RegistryObject<TileEntityType<IncubatorTileEntity>> type;

    public IncubatorBlock(String name, Properties p, RegistryObject<TileEntityType<IncubatorTileEntity>> type) {
        super(name, p, FHBlockItem::new);
        this.type = type;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING,LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite()).with(LIT,false);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return type.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }
    @Override
    public boolean canContainFluid(IBlockReader w, BlockPos p, BlockState s, Fluid f) {
        TileEntity te = w.getTileEntity(p);
        if (te instanceof IncubatorTileEntity) {
        	IncubatorTileEntity ele = (IncubatorTileEntity) te;
            if (ele.fluid[0].fill(new FluidStack(f, 1000), IFluidHandler.FluidAction.SIMULATE) == 1000)
                return true;
        }
        return false;
    }

    @Override
    public boolean receiveFluid(IWorld w, BlockPos p, BlockState s,
                                FluidState f) {
        TileEntity te = w.getTileEntity(p);
        if (te instanceof IncubatorTileEntity) {
        	IncubatorTileEntity ele = (IncubatorTileEntity) te;
            if (ele.fluid[0].fill(new FluidStack(f.getFluid(), 1000), IFluidHandler.FluidAction.SIMULATE) == 1000) {
                ele.fluid[0].fill(new FluidStack(f.getFluid(), 1000), IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }
        return false;
    }


}
