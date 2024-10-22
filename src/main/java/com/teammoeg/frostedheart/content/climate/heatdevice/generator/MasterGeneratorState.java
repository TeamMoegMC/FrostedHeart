package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.Optional;

import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;

public class MasterGeneratorState extends BaseHeatingState {
	int remTicks;
	public ContainerData guiData;

    @Override
	public void writeSaveNBT(CompoundTag nbt) {
		super.writeSaveNBT(nbt);
	}
	@Override
	public void readSaveNBT(CompoundTag nbt) {
		super.readSaveNBT(nbt);
        Optional<GeneratorData> data = this.getDataNoCheck();
        data.ifPresent(t -> {
            this.isOverdrive=t.isOverdrive;
            this.isWorking=t.isWorking;
        });
	}
	public MasterGeneratorState(IETemplateMultiblock multiblock,IETemplateMultiblock nextLevelMultiblock) {
		super();
		guiData = new SimpleContainerData(6);
	}
	public final Optional<GeneratorData> getDataNoCheck() {
        return getTeamData().map(t -> t.getData(SpecialDataTypes.GENERATOR_DATA));
    }
    public final Optional<GeneratorData> getData(BlockPos origin) {
        return getTeamData().map(t -> t.getData(SpecialDataTypes.GENERATOR_DATA)).filter(t -> origin.equals(t.actualPos));
    }
    public boolean isDataPresent(BlockPos origin) {
        return getData(origin).isPresent();
    }
    public void regist(Level level,BlockPos origin) {
    	getDataNoCheck().ifPresent(t -> {
        	if(!origin.equals(t.actualPos))
        		t.onPosChange();
        	this.setWorking(t.isWorking);
        	this.setOverdrive(t.isOverdrive);
            t.actualPos = origin;
            t.dimension = level.dimension();
        });
    }


    public void tryRegist(Level level,BlockPos origin) {
    	getDataNoCheck().ifPresent(t -> {
    		if(BlockPos.ZERO.equals(t.actualPos)) {
	        	if(!origin.equals(t.actualPos))
	        		t.onPosChange();
	        	this.setWorking(t.isWorking);
	        	this.setOverdrive(t.isOverdrive);
	            t.actualPos = origin;
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

}
