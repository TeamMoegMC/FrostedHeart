package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.Optional;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class GeneratorState extends HeatingState {
    /**
     * Remaining ticks to explode
     */
    int explodeTicks;
    public HeatEndpoint ep = new HeatEndpoint(200, 0);
    public GeneratorState() {
        super();
    }

    @Override
    public void writeSaveNBT(CompoundTag nbt) {
        super.writeSaveNBT(nbt);
        nbt.putInt("explodeTicks", explodeTicks);
    }

    @Override
    public void readSaveNBT(CompoundTag nbt) {
        super.readSaveNBT(nbt);
        //Optional<GeneratorData> data = this.getDataNoCheck();
        explodeTicks = nbt.getInt("explodeTicks");
    }

    /**
     * @return the GeneratorData from the owned team.
     */
    public final Optional<GeneratorData> getDataNoCheck() {
        return getTeamData().map(t -> t.getData(SpecialDataTypes.GENERATOR_DATA));
    }

    /**
     * @param origin the origin to check
     * @return the GeneratorData from the owned team with the given origin.
     */
    public final Optional<GeneratorData> getData(BlockPos origin) {
        return getTeamData().map(t -> t.getData(SpecialDataTypes.GENERATOR_DATA)).filter(t -> origin.equals(t.actualPos));
    }
    public final void tickData(Level level,BlockPos origin) {
        Optional<TeamDataHolder> data= getTeamData();
        if(data.isPresent()) {
        	TeamDataHolder teamData=data.get();
        	GeneratorData dat=teamData.getData(SpecialDataTypes.GENERATOR_DATA);
        	if(origin.equals(dat.actualPos)) {
        		dat.tick(level, teamData);
        		ep.setHeat(dat.lastPower);
        		dat.lastPower=0;
        	}
        	
        }
    }
    /**
     * @param origin the origin to check
     * @return if the GeneratorData from the owned team with the given origin is present.
     */
    public boolean isDataPresent(BlockPos origin) {
        return getData(origin).isPresent();
    }

    /**
     * Register the given origin to the GeneratorData from the owned team.
     *
     * @param level  the level to check
     * @param origin the origin to check
     */
    public void regist(Level level, BlockPos origin) {
        getDataNoCheck().ifPresent(t -> {
            if (!origin.equals(t.actualPos)) {
                t.onPosChange();
                onDataChange();
            }
            t.actualPos = origin;
            t.dimension = level.dimension();
        });
    }

    /**
     * Try to register the given origin to the GeneratorData from the owned team.
     * Check if the origin is not the zero position.
     *
     * @param level  the level to check
     * @param origin the origin to check
     */
    public void tryRegist(Level level, BlockPos origin) {
        getDataNoCheck().ifPresent(t -> {
            if (t.actualPos==null) {
                if (!origin.equals(t.actualPos)) {
                    t.onPosChange();
                    onDataChange();
                }
                t.actualPos = origin;
                t.dimension = level.dimension();
            }
        });
    }

    @Override
    public int getDownwardRange() {
        return Mth.ceil(getRangeLevel() * 2 + 1);
    }

    @Override
    public int getUpwardRange() {
        return Mth.ceil(getRangeLevel() * 4 + 1);
    }
    public void onDataChange() {
    	
    }

}
