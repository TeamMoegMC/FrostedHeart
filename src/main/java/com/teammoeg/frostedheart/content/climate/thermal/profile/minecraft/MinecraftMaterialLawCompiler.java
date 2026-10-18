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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Two passes: establish each state's energy reference, then resolve its declared edges. */
final class MinecraftMaterialLawCompiler {
    private final Map<BlockState, MaterialThermalLaw> laws = new IdentityHashMap<>();

    MinecraftMaterialLawCompiler(List<Block> blocks, ToDoubleFunction<BlockState> defaultCapacity) {
        for (Block block : blocks)
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                double capacity = defaultCapacity.applyAsDouble(state);
                if (capacity <= 0) continue;
                var data = StateTransitionData.getData(state);
                double offset = 0;
                if (data != null) {
                    double quantity =
                            data.allStates()
                                    ? capacity / defaultCapacity.applyAsDouble(data.block())
                                    : 1;
                    if (Double.isFinite(data.capacityJPerK()))
                        capacity = data.capacityJPerK() * quantity;
                    offset = data.offsetJ() * quantity;
                }
                laws.put(state, new MaterialThermalLaw(capacity, offset, null, null));
            }
        for (var entry : laws.entrySet()) {
            var data = StateTransitionData.getData(entry.getKey());
            if (data == null || !data.hasTransitions()) continue;
            var base = entry.getValue();
            entry.setValue(
                    new MaterialThermalLaw(
                            base.capacityJPerK(),
                            base.offsetJ(),
                            compileEdge(base, data.heating(), true),
                            compileEdge(base, data.cooling(), false)));
        }
    }

    private MaterialThermalLaw.Transition compileEdge(
            MaterialThermalLaw source, StateTransitionData.Edge edge, boolean heating) {
        if (edge == null) return null;
        double sourceEnergyJ = source.enthalpyAtTemperature(edge.temperatureC());
        var target = laws.get(edge.target());
        double targetEnergyJ =
                target == null
                        ? sourceEnergyJ + (heating ? edge.latentHeatJ() : -edge.latentHeatJ())
                        : target.enthalpyAtTemperature(edge.temperatureC());
        return new MaterialThermalLaw.Transition(
                Block.BLOCK_STATE_REGISTRY.getId(edge.target()),
                edge.temperatureC(),
                sourceEnergyJ,
                targetEnergyJ);
    }

    MaterialThermalLaw law(BlockState state) {
        return laws.get(state);
    }
}
