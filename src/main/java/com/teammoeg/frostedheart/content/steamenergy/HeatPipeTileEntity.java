/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.teammoeg.frostedheart.base.block.FHTickableBlockEntity;
import com.teammoeg.frostedheart.base.block.FluidPipeBlock;
import com.teammoeg.frostedheart.base.block.PipeTileEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatCapabilities;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static net.minecraft.ChatFormatting.GRAY;

public class HeatPipeTileEntity extends PipeTileEntity implements NetworkConnector, IHaveGoggleInformation,FHTickableBlockEntity {
	ConnectorNetworkRevalidator<HeatPipeTileEntity> networkHandler=new ConnectorNetworkRevalidator<>(this);
    int cnt = 1;

    public HeatPipeTileEntity(BlockPos l, BlockState state) {
        super(FHBlockEntityTypes.HEATPIPE.get(), l, state);
    }

    @Override
    public boolean canConnectTo(Direction to) {
        return this.getState().getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(to));
    }
	@Override
	public void setNetwork(HeatNetwork network) {
		this.networkHandler.setNetwork(network);
	}
    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
    }

    @Override
    public void tick() {
    	networkHandler.tick();
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
    }

    @Override
    public void onFaceChange(Direction dir, boolean isConnect) {
    	//System.out.println(dir+":"+isConnect);
    	networkHandler.onConnectionChange(dir, isConnect);
    }

    @Override
    public HeatNetwork getNetwork() {
        return networkHandler.getNetwork();
    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        float output = 0;
        float intake = 0;

        Lang.tooltip("heat_stats").forGoggles(tooltip);

        if (!TemperatureGoogleRenderer.lastHeatNetworkData.invalid()) {
            ClientHeatNetworkData data = TemperatureGoogleRenderer.lastHeatNetworkData;

            Lang.translate("tooltip", "pressure")
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

        } else {
            Lang.translate("tooltip", "pressure.no_network")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
        }

        return true;

    }


}
