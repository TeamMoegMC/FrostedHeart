package com.teammoeg.frostedheart.content.utility.gunpowder_barrel;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.agriculture.biogassystem.block.BiogasDigesterIOBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GunpowderBarrelBlock extends BaseEntityBlock {
    public GunpowderBarrelBlock(Properties pProperties) {
        super(pProperties);
    }
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    private static final VoxelShape SHAPED = Block.box(2,0,2,14,16,14);

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(LIT,false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPED;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new GunpowderBarrelBlockEntity(blockPos,blockState);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        if (pState.getBlock() != pNewState.getBlock()){
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof GunpowderBarrelBlockEntity entity){
                entity.drops();
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(LIT)) {
            double x = (double)pPos.getX() + .5;
            double y = pPos.getY() + 1;
            double z = (double)pPos.getZ() + .5;
            for (int i = 0; i < 3; i++){
                pLevel.addParticle(ParticleTypes.POOF, x, y, z, (pRandom.nextDouble()-0.5)/3,  pRandom.nextDouble()/5, (pRandom.nextDouble()-0.5)/3);
                pLevel.addParticle(ParticleTypes.SMOKE, x, y, z, (pRandom.nextDouble()-0.5)/3,  pRandom.nextDouble()/5, (pRandom.nextDouble()-0.5)/3);
                pLevel.addParticle(ParticleTypes.FLAME, x, y, z, (pRandom.nextDouble()-0.5)/3,  pRandom.nextDouble()/5, (pRandom.nextDouble()-0.5)/3);
            }
        }
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide()){
            return null;
        }
        return createTickerHelper(pBlockEntityType, FHBlockEntityTypes.GUNPOWDER_BARREL.get(),
                (pLevel1,pPos,pState1,blockEntity) -> blockEntity.tick(pLevel1,pPos,pState1));
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack stack = pPlayer.getItemInHand(pHand);
        if (stack.getItem() == Items.FLINT_AND_STEEL){
            pLevel.playLocalSound(pPos.getX(), pPos.getY(), pPos.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            if (!pLevel.isClientSide()){
                pLevel.setBlock(pPos,pState.setValue(LIT,true),Block.UPDATE_ALL);
                stack.hurtAndBreak(1, (LivingEntity) pPlayer, playerEntity -> playerEntity.broadcastBreakEvent(pHand));
            }
            return InteractionResult.SUCCESS;
        } else if (stack.getItem() == Items.FIRE_CHARGE) {
            pLevel.playLocalSound(pPos.getX(), pPos.getY(), pPos.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            if (!pLevel.isClientSide()){
                pLevel.setBlock(pPos,pState.setValue(LIT,true),Block.UPDATE_ALL);
                stack.setCount(stack.getCount()-1);
            }
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }
}
