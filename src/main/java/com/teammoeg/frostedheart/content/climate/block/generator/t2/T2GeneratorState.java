/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.block.generator.t2;

import static net.minecraft.ChatFormatting.*;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorState;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import com.teammoeg.frostedheart.content.steamenergy.ClientHeatNetworkData;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;
import com.teammoeg.frostedheart.util.Lang;

import blusunrize.immersiveengineering.api.multiblocks.blocks.util.StoredCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class T2GeneratorState extends GeneratorState implements HeatNetworkProvider, IHaveGoggleInformation {
    public static final int TANK_CAPACITY = 5 * 1000;
    public FluidTank tank = new FluidTank(TANK_CAPACITY,
            f -> FHTags.Fluids.STEAM.matches(f.getFluid()));
    public StoredCapability<IFluidHandler> tankCap = new StoredCapability<>(tank);
    
    public StoredCapability<HeatEndpoint> heatCap=new StoredCapability<>(endpoint);
    HeatNetwork manager=new HeatNetwork();
    int liquidtick = 0;
    int noliquidtick = 0;
    int steamLevel=0;
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
        nbt.putInt("steamLevel", steamLevel);
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
        steamLevel=nbt.getInt("steamLevel");
        tank.readFromNBT(nbt.getCompound("tank"));
        if(manager!=null)
            manager.deserializeNBT(nbt.getCompound("manager"));
    }


    @Override
    public @Nullable HeatNetwork getNetwork() {
        return endpoint.getNetwork();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Lang.tooltip("heat_stats").forGoggles(tooltip);

        if (TemperatureGoogleRenderer.hasHeatNetworkData()) {
            ClientHeatNetworkData data = TemperatureGoogleRenderer.getHeatNetworkData();

            Lang.translate("tooltip", "pressure.network")
                    .style(GRAY)
                    .forGoggles(tooltip);

            Lang.number(data.totalEndpointIntake)
                    .translate("generic", "unit.pressure")
                    .style(ChatFormatting.AQUA)
                    .space()
                    .add(Lang.translate("tooltip", "pressure.intake")
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            Lang.number(data.totalEndpointOutput)
                    .translate("generic", "unit.pressure")
                    .style(ChatFormatting.AQUA)
                    .space()
                    .add(Lang.translate("tooltip", "pressure.output")
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            // show number of endpoints
            Lang.number(data.endpoints.size())
                    .style(ChatFormatting.AQUA)
                    .space()
                    .add(Lang.translate("tooltip", "pressure.endpoints")
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            Lang.translate("tooltip", "pressure.endpoint")
                    .style(GRAY)
                    .forGoggles(tooltip);

        }

        return true;
    }
}
