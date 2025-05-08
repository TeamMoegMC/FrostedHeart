package com.teammoeg.frostedheart.content.agriculture.biogassystem.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public class BiogasDigesterControllerBlock extends BaseEntityBlock implements IWrenchable {
    public BiogasDigesterControllerBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide){
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof BiogasDigesterControllerBlockEntity be) {
            NetworkHooks.openScreen((ServerPlayer) pPlayer,be,pPos);
        }
        return InteractionResult.CONSUME;
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide()){
            return null;
        }
        return createTickerHelper(pBlockEntityType, FHBlockEntityTypes.BIOGAS_DIGESTER_CONTROLLER.get(),
                (pLevel1,pPos,pState1,blockEntity) -> blockEntity.tick(pLevel1,pPos));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BiogasDigesterControllerBlockEntity(blockPos,blockState);
    }
}
