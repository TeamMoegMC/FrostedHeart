package com.teammoeg.frostedheart.content.robotics.logistics.core;

import java.util.function.Function;

import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.block.generator.OwnedLogic;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;

import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.StoredCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class LogisticCoreLogic implements IServerTickableComponent<LogisticState>, IClientTickableComponent<LogisticState>, IMultiblockLogic<LogisticState>, OwnedLogic<LogisticState> {

	public LogisticCoreLogic() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onOwnerChange(IMultiblockContext<LogisticState> ctx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public LogisticState createInitialState(IInitialMultiblockContext<LogisticState> capabilitySource) {
		return new LogisticState();
	}

	@Override
	public Function<BlockPos, VoxelShape> shapeGetter(ShapeType forType) {
		return a->Shapes.block();
	}

	@Override
	public void tickClient(IMultiblockContext<LogisticState> context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> LazyOptional<T> getCapability(IMultiblockContext<LogisticState> ctx, CapabilityPosition position, Capability<T> cap) {
		LogisticState state=ctx.getState();
		if(state.cap!=null&&cap==FHCapabilities.LOGISTIC.capability()) {
			return ctx.getState().cap.cast(ctx);
		}
		return LazyOptional.empty();
	
	}

	@Override
	public InteractionResult click(IMultiblockContext<LogisticState> ctx, BlockPos posInMultiblock, Player player, InteractionHand hand, BlockHitResult absoluteHit, boolean isClient) {
		// TODO Auto-generated method stub
		return IServerTickableComponent.super.click(ctx, posInMultiblock, player, hand, absoluteHit, isClient);
	}

	@Override
	public void tickServer(IMultiblockContext<LogisticState> context) {
		LogisticState state=context.getState();
		state.level=context.getLevel().getRawLevel();
		state.worldPosition=context.getLevel().getAbsoluteOrigin();
		if(state.ln==null) {
			state.ln=new LogisticNetwork(state.level,state.worldPosition);
			state.cap=new StoredCapability<>(state.ln);
			state.ticker.enqueue();
		}

		state.ticker.tick();
		state.ln.tick();
		
	}


}
