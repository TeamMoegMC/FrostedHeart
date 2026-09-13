/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.content.climate.PhysicalState;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.world.level.block.Blocks;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Compiles only unambiguous, reciprocal adjacent phases; other recipes stay gameplay actions. */
final class MinecraftMaterialLawCompiler {
    private final Map<BlockState, MaterialThermalLaw> laws = new IdentityHashMap<>();

    MinecraftMaterialLawCompiler(List<Block> blocks, ToDoubleFunction<BlockState> capacity, double latentBaseJ) {
        Map<BlockState, Double> offsets = new IdentityHashMap<>();
        Map<BlockState, BlockState> hotter = new IdentityHashMap<>();
        Map<BlockState, BlockState> colder = new IdentityHashMap<>();
        for (Block block : blocks) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BlockState canonical = canonical(state);
                offsets.putIfAbsent(canonical, 0.0);
                StateTransitionData data = StateTransitionData.getData(canonical);
                if (data == null || !data.willTransit() || data.heatCapacity() <= 0) continue;
                StateTransitionData.HeatingTransition edge = data.heatingTransition(canonical);
                if (edge == null || edge.targetBlock().isAir()) continue;
                BlockState target = canonical(edge.targetBlock());
                StateTransitionData targetData = StateTransitionData.getData(target);
                if (targetData == null || !targetData.willTransit()
                        || targetData.state().ordinal() != data.state().ordinal() - 1
                        || canonical(coolingTarget(targetData)) != canonical) continue;
                hotter.put(canonical, target);
                colder.put(target, canonical);
            }
        }
        // At most solid -> liquid -> gas. This does not solve a general recipe graph.
        for (PhysicalState phase : List.of(PhysicalState.SOLID, PhysicalState.LIQUID)) {
            for (var entry : hotter.entrySet()) {
                BlockState source = entry.getKey(), target = entry.getValue();
                StateTransitionData data = StateTransitionData.getData(source);
                if (data.state() != phase) continue;
                double transitionC = data.heatingTransition(source).temperatureC();
                double sourceCapacity = capacity.applyAsDouble(source);
                double targetCapacity = capacity.applyAsDouble(target);
                if (sourceCapacity <= 0 || targetCapacity <= 0) continue;
                offsets.put(target, offsets.get(source)
                        + (sourceCapacity - targetCapacity) * transitionC
                        + latentBaseJ * data.heatCapacity());
            }
        }
        for (Block block : blocks) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                double bodyCapacity = capacity.applyAsDouble(state);
                if (bodyCapacity <= 0) continue;
                BlockState canonical = canonical(state);
                double canonicalCapacity = capacity.applyAsDouble(canonical);
                // Fluid amount variants retain the same specific energy reference.
                double offset = offsets.getOrDefault(canonical, 0.0)
                        * bodyCapacity / canonicalCapacity;
                MaterialThermalLaw.Transition heating = edge(state, canonical, hotter.get(canonical),
                        true, bodyCapacity, offset, capacity, offsets);
                MaterialThermalLaw.Transition cooling = edge(state, canonical, colder.get(canonical),
                        false, bodyCapacity, offset, capacity, offsets);
                laws.put(state, new MaterialThermalLaw(bodyCapacity, offset, heating, cooling));
            }
        }
        compileIceStages(capacity, latentBaseJ);
    }

    /**
     * The pack uses solid/liquid/gas as stage slots even for ice -> thinner ice.
     * Preserve its actual target chain and thresholds; a skipped stage consumes
     * the difference of the endpoint laws, not one recipe-sized latent unit.
     */
    private void compileIceStages(ToDoubleFunction<BlockState> capacities, double latentBaseJ) {
        BlockState[] stages = {Blocks.BLUE_ICE.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState(),
                FHBlocks.FIRM_ICE_BLOCK.get().defaultBlockState(), Blocks.ICE.defaultBlockState(),
                FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState(), Blocks.WATER.defaultBlockState()};
        double[] offsets = new double[stages.length];
        for (int index = 0; index + 1 < stages.length; index++) {
            StateTransitionData data = StateTransitionData.getData(stages[index]);
            if (data == null || !data.willTransit() || data.heatCapacity() <= 0) return;
            BlockState target = stages[index + 1];
            double threshold;
            if (data.liquid() == target) threshold = data.meltTemp();
            else if (data.gas() == target) threshold = data.evaporateTemp();
            else return;
            offsets[index + 1] = offsets[index]
                    + (capacities.applyAsDouble(stages[index]) - capacities.applyAsDouble(target)) * threshold
                    + latentBaseJ * data.heatCapacity();
        }
        for (int index = 0; index < stages.length; index++) {
            BlockState state = stages[index];
            StateTransitionData data = StateTransitionData.getData(state);
            MaterialThermalLaw.Transition heating = null, cooling = null;
            if (data != null) {
                var hot = data.heatingTransition(state);
                if (hot != null) heating = iceEdge(stages, offsets, index, hot.targetBlock(), hot.temperatureC(), capacities, true);
                BlockState cold = data.state() == PhysicalState.GAS ? data.liquid() : data.solid();
                double threshold = data.state() == PhysicalState.GAS ? data.condenseTemp() : data.freezeTemp();
                if (cold != null && cold != state) cooling = iceEdge(stages, offsets, index, cold, threshold, capacities, false);
            } else if (state.is(Blocks.WATER)) {
                cooling = iceEdge(stages, offsets, index, stages[index - 1], WorldTemperature.WATER_FREEZES, capacities, false);
            }
            laws.put(state, new MaterialThermalLaw(capacities.applyAsDouble(state), offsets[index], heating, cooling));
        }
        double sourceWaterCapacity = capacities.applyAsDouble(Blocks.WATER.defaultBlockState());
        for (BlockState water : Blocks.WATER.getStateDefinition().getPossibleStates()) {
            if (water == Blocks.WATER.defaultBlockState()) continue;
            double capacity = capacities.applyAsDouble(water);
            laws.put(water, new MaterialThermalLaw(capacity,
                    offsets[offsets.length - 1] * capacity / sourceWaterCapacity, null, null));
        }
    }

    private static MaterialThermalLaw.Transition iceEdge(BlockState[] states, double[] offsets, int source,
            BlockState target, double temperature, ToDoubleFunction<BlockState> capacities, boolean heating) {
        int destination = 0;
        while (destination < states.length && states[destination] != target) destination++;
        if (destination == states.length || (heating ? destination <= source : destination >= source)) return null;
        double sourceEnergy = offsets[source] + capacities.applyAsDouble(states[source]) * temperature;
        double targetEnergy = offsets[destination] + capacities.applyAsDouble(target) * temperature;
        if (heating ? targetEnergy <= sourceEnergy : targetEnergy >= sourceEnergy) return null;
        return new MaterialThermalLaw.Transition(Block.BLOCK_STATE_REGISTRY.getId(target), temperature, sourceEnergy, targetEnergy);
    }

    MaterialThermalLaw law(BlockState state) {
        return laws.get(state);
    }

    private static MaterialThermalLaw.Transition edge(BlockState state, BlockState canonical,
            BlockState target, boolean heating, double capacity, double offset,
            ToDoubleFunction<BlockState> capacities, Map<BlockState, Double> offsets) {
        if (target == null || state != canonical) return null;
        StateTransitionData data = StateTransitionData.getData(canonical);
        double temperature = heating ? data.heatingTransition(canonical).temperatureC()
                : data.state() == PhysicalState.LIQUID ? data.freezeTemp() : data.condenseTemp();
        double sourceEnergy = offset + capacity * temperature;
        double targetEnergy = offsets.getOrDefault(target, 0.0) + capacities.applyAsDouble(target) * temperature;
        if (!Double.isFinite(sourceEnergy) || !Double.isFinite(targetEnergy)
                || (heating ? targetEnergy <= sourceEnergy : targetEnergy >= sourceEnergy)) return null;
        return new MaterialThermalLaw.Transition(Block.BLOCK_STATE_REGISTRY.getId(target),
                temperature, sourceEnergy, targetEnergy);
    }

    private static BlockState coolingTarget(StateTransitionData data) {
        return data.state() == PhysicalState.LIQUID ? data.solid()
                : data.state() == PhysicalState.GAS ? data.liquid() : null;
    }

    private static BlockState canonical(BlockState state) {
        if (state == null) return null;
        StateTransitionData data = StateTransitionData.getData(state);
        return data != null && data.block() != null && data.ignoreState() ? data.block() : state;
    }
}
