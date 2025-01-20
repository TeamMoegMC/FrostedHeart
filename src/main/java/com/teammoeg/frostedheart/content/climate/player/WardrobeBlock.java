package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.decoration.RelicChestTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class WardrobeBlock extends FHBaseBlock implements FHEntityBlock<WardrobeBlockEntity> {
    public WardrobeBlock(Properties properties) {
        super(properties);
    }

    @Nonnull
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide) {
            // Open the Wardrobe GUI
            System.out.println("Opening GUI");
            WardrobeBlockEntity tile = (WardrobeBlockEntity) world.getBlockEntity(pos);
            if (tile != null) {
                System.out.println("Tile is not null");
                NetworkHooks.openScreen((ServerPlayer) player, tile, pos);
                world.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.5F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nonnull
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public Supplier<BlockEntityType<WardrobeBlockEntity>> getBlock() {
        return FHBlockEntityTypes.WARDROBE;
    }
}