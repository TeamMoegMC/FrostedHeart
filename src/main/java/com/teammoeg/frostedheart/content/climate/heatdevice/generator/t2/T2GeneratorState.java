package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2;

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorState;
import com.teammoeg.frostedheart.content.steamenergy.HeatEnergyNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class T2GeneratorState extends GeneratorState {
    public static final int TANK_CAPACITY = 200 * 1000;
    public FluidTank tank = new FluidTank(TANK_CAPACITY,
            f -> GeneratorSteamRecipe.findRecipe(f) != null);
    public LazyOptional<IFluidHandler> tankCap = LazyOptional.of(() -> tank);
    HeatEnergyNetwork manager=new HeatEnergyNetwork();

    int liquidtick = 0;
    int noliquidtick = 0;
    int tickUntilStopBoom = 20;
    int notFullPowerTick = 0;
    final int nextBoom = 200; //10s

    public T2GeneratorState() {
        super();
    }

    @Override
    public void writeSaveNBT(CompoundTag nbt) {
        super.writeSaveNBT(nbt);
        nbt.putInt("liquidtick", liquidtick);
        nbt.putInt("noliquidtick", noliquidtick);
        nbt.putInt("tickUntilStopBoom", tickUntilStopBoom);
        nbt.putInt("notFullPowerTick", notFullPowerTick);
        nbt.put("tank", tank.writeToNBT(new CompoundTag()));
        if(manager!=null)
            nbt.put("manager", manager.serializeNBT());
    }

    @Override
    public void readSaveNBT(CompoundTag nbt) {
        super.readSaveNBT(nbt);
        liquidtick = nbt.getInt("liquidtick");
        noliquidtick = nbt.getInt("noliquidtick");
        tickUntilStopBoom = nbt.getInt("tickUntilStopBoom");
        notFullPowerTick = nbt.getInt("notFullPowerTick");
        tank.readFromNBT(nbt.getCompound("tank"));
        if(manager!=null)
            manager.deserializeNBT(nbt.getCompound("manager"));
    }

}
