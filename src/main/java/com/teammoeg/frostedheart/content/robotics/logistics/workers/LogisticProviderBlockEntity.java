package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import java.util.Collection;
import java.util.Collections;

import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.LogisticChest;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;

public abstract class LogisticProviderBlockEntity extends LogisticBlockEntity implements ILogisticProvider {
	@Getter
	private LogisticChest container;
	public LazyOptional<LogisticChest> grid;
	public LogisticProviderBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState state, int slots, LogisticChest container) {
		super(type, pos, state, slots);
		this.container=container;
		grid=LazyOptional.of(()->container);
	}
	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		container.deserialize(nbt.getCompound("chest"));
	}
	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.put("chest",container.serialize());
	}
	@Override
	public void tick() {
		super.tick();
		if(!this.level.isClientSide) {
			container.setLevel(level);
			container.tick();
			if(!networks.isEmpty()) {
				networkStatus=2;
				uplinkStatus=1;
			}else
				uplinkStatus=networkStatus=0;
			if(container.consumeChange())
				this.setChanged();
		}
	}
	@Override
	protected void refreshNetwork() {
		super.refreshNetwork();
		Collection<LazyOptional<LogisticNetwork>> candidate=FHCapabilities.ROBOTIC_LOGISTIC_CHUNK
			.getCapability(level.getChunk(worldPosition))
			.map(chunk->chunk.getNetworkFor(level,worldPosition))
			.orElse(Collections.emptySet());
		for(LazyOptional<LogisticNetwork> lln:candidate) {
			LogisticNetwork ln = lln.orElse(null);
			ln.getHub().addElement(grid.cast(), false);
		}
	}
	protected void disconnectNetwork() {
		container.removeSlots();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		container.setLevel(level);
		if(!grid.isPresent())
			grid=LazyOptional.of(()->container);
	}
	@Override
	public void onRemoved() {
		super.onRemoved();
		disconnectNetwork();
		grid.invalidate();
	}

	@Override
	public void onUnloaded() {
		disconnectNetwork();
		grid.invalidate();
	}

}
