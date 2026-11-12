package com.teammoeg.frostedresearch.knowledge.furnace;
import com.teammoeg.chorda.block.*;
import com.teammoeg.frostedresearch.FRContents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import java.util.function.Supplier;
public final class ControlledFurnaceBlock extends CBlock implements CEntityBlock<ControlledFurnaceBlockEntity>{
    public ControlledFurnaceBlock(Properties p){super(p);}
    @Override public Supplier<BlockEntityType<ControlledFurnaceBlockEntity>> getBlock(){return FRContents.BlockEntityTypes.CONTROLLED_FURNACE;}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(!l.isClientSide && hand==InteractionHand.MAIN_HAND && l.getBlockEntity(pos) instanceof ControlledFurnaceBlockEntity furnace){
            if(furnace.claim((ServerPlayer)p)){furnace.syncData();NetworkHooks.openScreen((ServerPlayer)p,furnace,pos);}
            else p.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.frostedresearch.furnace.not_owned"),true);
        }return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos pos,BlockState next,boolean moving){
        if(s.getBlock()!=next.getBlock() && !l.isClientSide && l.getBlockEntity(pos) instanceof ControlledFurnaceBlockEntity f)
            f.settle().forEach(stack -> popResource(l,pos,stack));
        super.onRemove(s,l,pos,next,moving);
    }
}
