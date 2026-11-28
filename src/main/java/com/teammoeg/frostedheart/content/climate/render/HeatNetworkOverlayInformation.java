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

package com.teammoeg.frostedheart.content.climate.render;

import java.util.List;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.steamenergy.ClientHeatNetworkData;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkRequestC2SPacket;
import com.teammoeg.frostedheart.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Owns live heat-network data and its goggles text adapter. */
final class HeatNetworkOverlayInformation {
    private ClientHeatNetworkData data;

    boolean hasData() { return data != null && !data.invalid(); }
    ClientHeatNetworkData getData() { return data; }
    void setData(ClientHeatNetworkData data) { this.data = data; }

    /** Requests live data before block tooltip callbacks, preserving the original per-frame cadence. */
    void request(GoggleOverlayContext context) {
        boolean blockProvider = context.blockEntity() instanceof IHaveGoggleInformation
                && context.blockEntity() instanceof HeatNetworkProvider;
        boolean multiblockProvider = context.multiblock() instanceof IHaveGoggleInformation
                && context.multiblock() instanceof HeatNetworkProvider;
        if (!context.wearingGoggles() || !(blockProvider || multiblockProvider)) return;
        if (data == null || !data.pos.equals(context.position())) data = new ClientHeatNetworkData(context.position());
        FHNetwork.INSTANCE.sendToServer(new HeatNetworkRequestC2SPacket(context.position()));
    }

    /** Appends network summary and matching endpoint rates in the original order. */
    boolean appendTooltip(List<Component> tooltip, BlockPos position) {
        Lang.tooltip("heat_stats").forGoggles(tooltip);
        if (!hasData()) {
            Lang.translate("tooltip", "pressure.no_network").style(ChatFormatting.RED).forGoggles(tooltip);
            return true;
        }
        Lang.translate("tooltip", "pressure.network").style(ChatFormatting.GRAY).forGoggles(tooltip);
        appendRate(tooltip, data.totalEndpointIntake, "pressure.intake");
        appendRate(tooltip, data.totalEndpointOutput, "pressure.output");
        Lang.number(data.endpoints.size()).style(ChatFormatting.AQUA).space()
                .add(Lang.translate("tooltip", "pressure.endpoints").style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);
        Lang.translate("tooltip", "pressure.endpoint").style(ChatFormatting.GRAY).forGoggles(tooltip);
        for (var endpoint : data.endpoints) {
            if (endpoint.getPos().equals(position)) appendEndpoint(tooltip, endpoint);
        }
        return true;
    }

    private static void appendEndpoint(List<Component> tooltip, HeatEndpoint endpoint) {
        if (endpoint.getMaxIntake() > 0) appendRate(tooltip, endpoint.getMaxIntake(), "pressure.max_intake");
        if (endpoint.getMaxOutput() > 0) appendRate(tooltip, endpoint.getMaxOutput(), "pressure.max_output");
        if (endpoint.getAvgIntake() > 0) appendRate(tooltip, endpoint.getAvgIntake(), "pressure.average_intake");
        if (endpoint.getAvgOutput() > 0) appendRate(tooltip, endpoint.getAvgOutput(), "pressure.average_output");
    }

    private static void appendRate(List<Component> tooltip, float value, String label) {
        Lang.number(value).translate("generic", "unit.pressure").style(ChatFormatting.AQUA).space()
                .add(Lang.translate("tooltip", label).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }
}
