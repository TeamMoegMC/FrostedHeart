package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTaskKey;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;

public abstract class LogisticBlockEntity  extends CBlockEntity implements CTickableBlockEntity,LogisticStatusBlockEntity {

	Set<LazyOptional<LogisticNetwork>> networks=new HashSet<>();
	@Getter
	protected int networkStatus=0;
	@Getter
	protected int uplinkStatus=0;
	private int networkCheckTicks;
	@Getter
	private final List<Supplier<LogisticTaskKey>> keys;
	public LogisticBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState state, int slots) {
		super(type, pos, state);
		if(slots>0) {
			keys=new ArrayList<>(slots);
			for(int slot=0;slot<slots;slot++) {
				final int taskSlot=slot;
				keys.add(()->new LogisticTaskKey(pos,taskSlot));
			}
		}else
			keys=null;
	}
	protected void refreshNetwork() {
		networks.removeIf(t->!t.isPresent());
	}
	@Override
	public void tick() {
		if(!this.level.isClientSide) {
			if(networkCheckTicks--<=0) {
				refreshNetwork();
				networkCheckTicks=20;
			}
		}
	}
	@Override
	public void onRemoved() {
		super.onRemoved();
		for(LazyOptional<LogisticNetwork> lln:networks) {
			LogisticNetwork ln=lln.orElse(null);
			if(ln!=null)
			ln.cancelTasksAt(worldPosition);
		}
	}
	@Override
	public void onLoad() {
		super.onLoad();
		networkCheckTicks=0;
	}
}
