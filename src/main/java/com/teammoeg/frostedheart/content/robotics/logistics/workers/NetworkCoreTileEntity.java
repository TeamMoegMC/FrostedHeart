package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkCoreTileEntity extends CBlockEntity implements CTickableBlockEntity{
	LogisticNetwork ln;
	public NetworkCoreTileEntity( BlockPos pos, BlockState state) {
		super(FHBlockEntityTypes.NETWORK_CORE.get(),pos, state);
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		// TODO Auto-generated method stub
		
	}
	LazyTickWorker ticker=new LazyTickWorker(20,()->{
		if(ln==null) {
			ln=new LogisticNetwork(level,worldPosition);
			ChunkPos cp=new ChunkPos(worldPosition);
			for(int i=cp.x-1;i<=cp.x+1;i++)
				for(int j=cp.z-1;j<=cp.z+1;j++) {
					if(level.hasChunk(i, j)) {
						FHCapabilities.ROBOTIC_LOGISTIC_CHUNK.getCapability(
						level.getChunk(i, j)
						).ifPresent(t->t.register(worldPosition));
					}
				}
		}
	});
	@Override
	public void tick() {
		ticker.tick();
		ln.tick();
		
	}

}
