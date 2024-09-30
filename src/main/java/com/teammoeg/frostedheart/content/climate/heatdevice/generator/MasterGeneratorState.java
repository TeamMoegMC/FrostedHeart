package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.Optional;

import com.teammoeg.frostedheart.base.multiblock.components.OwnerState;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class MasterGeneratorState extends BaseHeatingState {
	boolean isBroken;
    @Override
	public void writeSaveNBT(CompoundTag nbt) {
		super.writeSaveNBT(nbt);
		nbt.putBoolean("isBroken", isBroken);
	}
	@Override
	public void readSaveNBT(CompoundTag nbt) {
		super.readSaveNBT(nbt);
		isBroken=nbt.getBoolean("isBroken");
        Optional<GeneratorData> data = this.getData();
        data.ifPresent(t -> {
            this.isOverdrive=t.isOverdrive;
            this.isWorking=t.isWorking;
        });
	}
	public MasterGeneratorState(BlockPos origin) {
		super(origin);
	}
	public final Optional<GeneratorData> getDataNoCheck() {
        return getTeamData().map(t -> t.getData(SpecialDataTypes.GENERATOR_DATA));
    }
    public final Optional<GeneratorData> getData() {
        return getTeamData().map(t -> t.getData(SpecialDataTypes.GENERATOR_DATA)).filter(t -> getOrigin().equals(t.actualPos));
    }
    public boolean isDataPresent() {
        return getData().isPresent();
    }
    public void regist(Level level) {
    	getDataNoCheck().ifPresent(t -> {
        	if(!getOrigin().equals(t.actualPos))
        		t.onPosChange();
        	this.setWorking(t.isWorking);
        	this.setOverdrive(t.isOverdrive);
            t.actualPos = getOrigin();
            t.dimension = level.dimension();
        });
    }
    
    public void tryRegist(Level level) {
    	getDataNoCheck().ifPresent(t -> {
    		if(BlockPos.ZERO.equals(t.actualPos)) {
	        	if(!getOrigin().equals(t.actualPos))
	        		t.onPosChange();
	        	this.setWorking(t.isWorking);
	        	this.setOverdrive(t.isOverdrive);
	            t.actualPos = getOrigin();
	            t.dimension = level.dimension();
    		}
        });
    }

    @Override
    public int getLowerBound() {
        return Mth.ceil(getRangeLevel()*2+1);
    }

    @Override
    public int getUpperBound() {
        return Mth.ceil(getRangeLevel() * 4+1);
    }
	@Override
	public void onOwnerChange(IMultiblockContext<? extends OwnerState> ctx) {
		regist(ctx.getLevel().getRawLevel());
	}
}
