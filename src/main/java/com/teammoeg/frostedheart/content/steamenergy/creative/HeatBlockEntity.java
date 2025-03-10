/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy.creative;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.teammoeg.chorda.util.Lang;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import com.teammoeg.frostedheart.content.steamenergy.ClientHeatNetworkData;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;

import static net.minecraft.ChatFormatting.GRAY;

/**
 * A BlockEntity that maintains a HeatEndpoint.
 * It handles default client effects and tooltips and capabilities.
 */
public class HeatBlockEntity extends SmartBlockEntity implements HeatNetworkProvider, IHaveGoggleInformation {
    HeatEndpoint endpoint = new HeatEndpoint(0, 0, 0, 0);
    LazyOptional<HeatEndpoint> heatcap = LazyOptional.of(() -> endpoint);
    public HeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

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

            // stream through endpoints, filter by pos
            data.endpoints.stream()
                    .filter(e -> e.getPos().equals(worldPosition))
                    .forEach(e -> {
                        float maxIntake = e.getMaxIntake();
                        float maxOutput = e.getMaxOutput();
                        float avgIntake = e.getAvgIntake();
                        float avgOutput = e.getAvgOutput();

                        if (maxIntake > 0)
                            Lang.number(e.getMaxIntake())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(Lang.translate("tooltip", "pressure.max_intake")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);

                        if (maxOutput > 0)
                            Lang.number(e.getMaxOutput())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(Lang.translate("tooltip", "pressure.max_output")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);

                        if (avgIntake > 0)
                            Lang.number(e.getAvgIntake())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(Lang.translate("tooltip", "pressure.average_intake")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);

                        if (avgOutput > 0)
                            Lang.number(e.getAvgOutput())
                                .translate("generic", "unit.pressure")
                                .style(ChatFormatting.AQUA)
                                .space()
                                .add(Lang.translate("tooltip", "pressure.average_output")
                                        .style(ChatFormatting.DARK_GRAY))
                                .forGoggles(tooltip, 1);
                    });

        } else {
            Lang.translate("tooltip", "pressure.no_network")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
        }

        return true;

    }

    @Override
    public HeatNetwork getNetwork() {
        return endpoint.getNetwork();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == FHCapabilities.HEAT_EP.capability())
            return heatcap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        heatcap.invalidate();
        super.invalidateCaps();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        endpoint.unload();
    }
}
