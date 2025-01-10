package com.teammoeg.frostedheart.content.steamenergy.creative;

import com.simibubi.create.foundation.block.IBE;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.steamenergy.debug.DebugHeaterTileEntity;
import com.teammoeg.frostedheart.util.FHUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class CreativeHeaterBlock extends HeatBlock implements IBE<CreativeHeaterBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public CreativeHeaterBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public Class<CreativeHeaterBlockEntity> getBlockEntityClass() {
        return CreativeHeaterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CreativeHeaterBlockEntity> getBlockEntityType() {
        return FHBlockEntityTypes.CREATIVE_HEATER.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction nearestLookingDirection = context.getNearestLookingDirection();
        return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown() ? nearestLookingDirection : nearestLookingDirection.getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
                                boolean isMoving) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        //Direction d = FHUtils.dirBetween(fromPos, pos);
        //System.out.println("changed")2
        if(!worldIn.isClientSide)
            worldIn.scheduleTick(pos, this, 10);

    }

    @Override
    public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource pRandom) {
        //System.out.println("ticked "+pos);
        super.tick(state, worldIn, pos, pRandom);
        CreativeHeaterBlockEntity te= FHUtils.getExistingTileEntity(worldIn, pos, CreativeHeaterBlockEntity.class);
        if(te!=null)
            te.getNetwork().startConnectionFromBlock(te);
    }
}
