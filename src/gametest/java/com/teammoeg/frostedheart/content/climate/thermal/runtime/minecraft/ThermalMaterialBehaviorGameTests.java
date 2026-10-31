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
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import java.util.Arrays;
import java.util.LinkedHashMap;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftSignatureCapture;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialStateAccess;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionLimits;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FarFieldSettings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_material_runtime")
@PrefixGameTestTemplate(false)
public final class ThermalMaterialBehaviorGameTests {
    @GameTest(template = "phase0a_empty", batch = "thermal_body_phase_energy", timeoutTicks = 160)
    public static void coldIceUsesOneEnergyThroughPlateauAckAndWaterHeating(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos origin = origin(helper), ice = origin.offset(1, 1, 1);
        fill(level, origin, 4, 4, 4, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(ice, Blocks.ICE.defaultBlockState());
        level.setBlockAndUpdate(ice.west(), Blocks.AIR.defaultBlockState());
        try (Simulation simulation = new Simulation(helper, origin, 4, 4, 4, -10)) {
            int slot = simulation.slot(ice);
            MaterialThermalLaw law = simulation.arena.materialLaw(slot);
            helper.assertTrue(law != null && law.heating() != null, "real ice must have sensible capacity and a physical melting edge");
            near(helper, -10, simulation.temperatureC(slot, 0), "cold ice is not already at its melting threshold");
            var edge = law.heating();
            double warmToThreshold = edge.sourceEnthalpyJ() - simulation.enthalpyJ(slot);
            double latent = edge.targetEnthalpyJ() - edge.sourceEnthalpyJ();
            simulation.acceptEnergyJ(slot, warmToThreshold / 2);
            near(helper, (-10 + edge.transitionTemperatureC()) / 2, simulation.temperatureC(slot, 0), "sensible heating precedes melting");
            simulation.acceptEnergyJ(slot, warmToThreshold / 2 + latent / 2);
            near(helper, edge.transitionTemperatureC(), simulation.temperatureC(slot, 0), "latent heat holds the plateau");
            near(helper, .5, edge.progress(simulation.enthalpyJ(slot)), "half of latent heat means half progress");
            simulation.acceptEnergyJ(slot, -latent / 4);
            near(helper, .25, edge.progress(simulation.enthalpyJ(slot)), "cooling returns latent energy on the same branch");
            double accepted = simulation.acceptEnergyJ(slot, latent);
            near(helper, latent * .75, accepted, "incoming excess is not hidden past the ACK boundary");
            var ready = simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            helper.assertTrue(ready.phaseRequests().length == 1, "one block owns one transition request");
            var request = ready.phaseRequests()[0];
            helper.assertTrue(request.blockX() == ice.getX() && request.blockY() == ice.getY() && request.blockZ() == ice.getZ(), "request names the heated block");
            double before = simulation.totalEnergy();
            var geometry = simulation.change(ice, Block.BLOCK_STATE_REGISTRY.byId(request.targetStateId()),
                    ResolvedGeometryBatch.MaterialChanges.THERMAL_TRANSITION);
            simulation.publish(geometry, new ThermalInputBatch.PhaseAck[]{new ThermalInputBatch.PhaseAck(request, PhaseTransitionRuntime.AckOutcome.APPLIED)});
            slot = simulation.slot(ice);
            near(helper, before, simulation.totalEnergy(), "successful ACK does not subtract latent energy again");
            near(helper, edge.transitionTemperatureC(), simulation.temperatureC(slot, 0), "next ice stage begins at the completed transition temperature");
            for (int stage = 0; stage < 6 && !level.getBlockState(ice).is(Blocks.WATER); stage++) {
                var nextEdge = simulation.arena.materialLaw(slot).heating();
                helper.assertTrue(nextEdge != null, "configured ice stages must reach water");
                simulation.acceptEnergyJ(slot, nextEdge.targetEnthalpyJ() - simulation.enthalpyJ(slot));
                var nextCut = simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
                helper.assertTrue(nextCut.phaseRequests().length == 1, "one request per completed ice stage");
                var nextRequest = nextCut.phaseRequests()[0];
                double energy = simulation.totalEnergy();
                simulation.publish(simulation.change(ice, Block.BLOCK_STATE_REGISTRY.byId(nextRequest.targetStateId()),
                                ResolvedGeometryBatch.MaterialChanges.THERMAL_TRANSITION),
                        new ThermalInputBatch.PhaseAck[]{new ThermalInputBatch.PhaseAck(nextRequest, PhaseTransitionRuntime.AckOutcome.APPLIED)});
                near(helper, energy, simulation.totalEnergy(), "intermediate ice stages preserve the same energy");
                slot = simulation.slot(ice);
            }
            helper.assertTrue(level.getBlockState(ice).is(Blocks.WATER), "configured ice chain reaches water");
            simulation.acceptEnergyJ(slot, simulation.arena.capacityJPerK(slot) * 5);
            near(helper, edge.transitionTemperatureC() + 5, simulation.temperatureC(slot, 0), "target material heats after conversion");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_body_checkpoint", timeoutTicks = 160)
    public static void materialCheckpointKeepsExactEnergyAndInactiveReplacementClearsIdentity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos origin = origin(helper), material = origin.offset(1, 1, 1);
        fill(level, origin, 4, 4, 4, Blocks.STONE.defaultBlockState());
        try (Simulation simulation = new Simulation(helper, origin, 4, 4, 4, 0)) {
            int slot = simulation.slot(material);
            simulation.acceptEnergyJ(slot, 12_345.125);
            simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            var page = simulation.page(material);
            var capture = MaterialSectionState.capture(page.currentPublication(), simulation.query,
                    simulation.profiles.signatures(), new MaterialSectionState.CaptureScratch());
            helper.assertTrue(capture.valid(), "material checkpoint is one coherent publication");
            int sectionY = material.getY() >> 4;
            var state = new DormantChunkThermalState(sectionY, 1);
            state.mergeMaterials(sectionY, capture.state(), capture.sampledBricks());
            CompoundTag encoded = new CompoundTag();
            state.encode(encoded);
            var decoded = DormantChunkThermalState.decode(encoded, sectionY, 1);
            int position = pageBlock(material);
            var records = decoded.materials(sectionY);
            near(helper, 12_345.125, records.enthalpyJ(records.find(position)), "NBT preserves body joules without air quantization");
            var chunk = level.getChunkAt(material);
            var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
            var previous = attachment.frostedheart$getDormantThermalState();
            try {
                attachment.frostedheart$setDormantThermalState(decoded);
                helper.assertTrue(Double.isFinite(MinecraftThermalInput.materialTemperature(level, material)), "stored material is queryable without creating a runtime");
                level.setBlockAndUpdate(material, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(material, Blocks.STONE.defaultBlockState());
                helper.assertTrue(decoded.materials(sectionY).find(position) < 0, "inactive A-Air-A replacement cannot inherit old heat");
            } finally { attachment.frostedheart$setDormantThermalState(previous); }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_material_identity", timeoutTicks = 160)
    public static void replacementAndBulkResyncDoNotInheritOldMaterialEnergy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos start = origin(helper), block = start.offset(1, 1, 1);
        fill(level, start, 4, 4, 4, Blocks.STONE.defaultBlockState());
        try (Simulation simulation = new Simulation(helper, start, 4, 4, 4, 0)) {
            simulation.acceptEnergyJ(simulation.slot(block), 12345);
            int stone = simulation.profiles.states().signatureId(Blocks.STONE.defaultBlockState());
            int air = simulation.profiles.states().signatureId(Blocks.AIR.defaultBlockState());
            var page = simulation.page(block);
            var changes = new ResolvedGeometryBatch.Builder();
            level.setBlockAndUpdate(block, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
            changes.addResolvedCenter(page, page.beginGeometryMutation(), pageBlock(block), stone);
            changes.addMaterialChange(page, pageBlock(block), stone, air, ResolvedGeometryBatch.MaterialChanges.REPLACE);
            changes.addMaterialChange(page, pageBlock(block), air, stone, ResolvedGeometryBatch.MaterialChanges.REPLACE);
            simulation.publish(changes.buildAndReset(), ThermalInputBatch.NO_PHASE_ACKS);
            near(helper, 0, simulation.enthalpyJ(simulation.slot(block)), "same-cut A-Air-A means a new body");
            near(helper, -12345, simulation.arena.externalMaterialEnergyJ(), "removed material energy is recorded once");
            simulation.acceptEnergyJ(simulation.slot(block), 9876);
            var reset = new ResolvedGeometryBatch.Builder();
            reset.addFullResync(page, page.beginGeometryMutation(), ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED,
                    simulation.capture.captureBricks(page.sectionKey(), null, simulation.capture.unresolvedPage(), 1L << brickIndex(start)));
            simulation.publish(reset.buildAndReset(), ThermalInputBatch.NO_PHASE_ACKS);
            near(helper, 0, simulation.enthalpyJ(simulation.slot(block)), "identical signatures cannot hide a whole-section replacement");
            near(helper, -22221, simulation.arena.externalMaterialEnergyJ(), "bulk replacement records discarded energy");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_material_state_change", timeoutTicks = 160)
    public static void shapeAndGameplayChangesUseTheExistingBodyState(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos start = origin(helper), block = start.offset(1, 1, 1);
        fill(level, start, 4, 4, 4, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(block, Blocks.OAK_TRAPDOOR.defaultBlockState());
        try (Simulation simulation = new Simulation(helper, start, 4, 4, 4, 0)) {
            int slot = simulation.slot(block);
            double capacity = simulation.arena.capacityJPerK(slot);
            simulation.acceptEnergyJ(slot, capacity * 20);
            simulation.publish(simulation.change(block, level.getBlockState(block)
                    .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true), (byte) 0), ThermalInputBatch.NO_PHASE_ACKS);
            slot = simulation.slot(block);
            near(helper, capacity * 20, simulation.enthalpyJ(slot), "opening the same trapdoor preserves H");
            near(helper, capacity, simulation.arena.capacityJPerK(slot), "opening does not alter body capacity");
            double before = simulation.totalEnergy();
            simulation.publish(simulation.change(block, Blocks.DIRT.defaultBlockState(),
                    ResolvedGeometryBatch.MaterialChanges.GAMEPLAY_TRANSITION), ThermalInputBatch.NO_PHASE_ACKS);
            near(helper, 20, simulation.temperatureC(simulation.slot(block), 0), "gameplay material conversion continues at the existing temperature");
            near(helper, simulation.totalEnergy() - before, simulation.arena.externalMaterialEnergyJ(),
                    "gameplay conversion records its actual energy change");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_residency_signatures", timeoutTicks = 160)
    public static void residencyUpdatesReplaceSignaturesAndMaterialTogether(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos start = origin(helper), block = start.offset(1, 1, 1);
        fill(level, start, 4, 4, 4, Blocks.STONE.defaultBlockState());
        try (Simulation simulation = new Simulation(helper, start, 4, 4, 4, 0)) {
            var page = simulation.page(block);
            long mask = 1L << brickIndex(start);
            for (var state : new BlockState[]{Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState()}) {
                level.setBlockAndUpdate(block, state);
                var cut = simulation.capture.captureBricks(page.sectionKey(), null, simulation.capture.unresolvedPage(), mask);
                var residency = new ThermalInputBatch.PageResidencyUpdate(page, page.beginGeometryMutation(), mask, mask, cut, null, null);
                var result = RegionalEngineTestDriver.submit(helper, simulation.engine, new ThermalInputBatch(1, ++simulation.sequence,
                        simulation.tick, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS,
                        new ThermalInputBatch.PageResidencyUpdate[]{residency}, ResolvedGeometryBatch.EMPTY,
                        ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN));
                helper.assertTrue(result.status() == ThermalCompletion.Status.COMPLETED, "residency signature update completes");
                helper.assertTrue(simulation.arena.isMaterialCell(simulation.slot(block)) == state.is(Blocks.STONE),
                        "unchanged resident mask cannot retain a body from the previous signature");
                var captured = MaterialSectionState.capture(page.currentPublication(), simulation.query,
                        simulation.profiles.signatures(), new MaterialSectionState.CaptureScratch());
                helper.assertTrue(captured.valid() && captured.state() != null, "remaining stone bodies must be capturable");
                int index = captured.state().find(pageBlock(block));
                helper.assertTrue(state.isAir() ? index < 0 : index >= 0 && captured.state().stateId(index) == Block.getId(state),
                        "checkpoint material identities follow the same residency signature cut");
                int sectionY = block.getY() >> 4;
                var stored = new DormantChunkThermalState(sectionY, 1); stored.replaceMaterials(sectionY, captured.state());
                stored.encode(new CompoundTag());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_freezing_checkpoint", timeoutTicks = 160)
    public static void freezingProgressSurvivesCheckpointAndFinishesWithoutDoubleEnergyCharge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos start = origin(helper), water = start.offset(1, 1, 1);
        fill(level, start, 4, 4, 4, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        try (Simulation simulation = new Simulation(helper, start, 4, 4, 4, 10)) {
            int slot = simulation.slot(water);
            var edge = simulation.arena.materialLaw(slot).cooling();
            helper.assertTrue(edge != null, "source water has a physical freezing edge");
            double middle = (edge.sourceEnthalpyJ() + edge.targetEnthalpyJ()) / 2;
            simulation.acceptEnergyJ(slot, middle - simulation.enthalpyJ(slot));
            near(helper, edge.transitionTemperatureC(), simulation.temperatureC(slot, 0), "freezing releases latent heat at the plateau");
            simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            var page = simulation.page(water);
            var capture = MaterialSectionState.capture(page.currentPublication(), simulation.query,
                    simulation.profiles.signatures(), new MaterialSectionState.CaptureScratch());
            var stored = new DormantChunkThermalState(water.getY() >> 4, 1);
            stored.mergeMaterials(water.getY() >> 4, capture.state(), capture.sampledBricks());
            CompoundTag nbt = new CompoundTag(); stored.encode(nbt);
            var restored = DormantChunkThermalState.decode(nbt, water.getY() >> 4, 1).materials(water.getY() >> 4);
            var sample = new MaterialSample();
            restored.read(restored.find(pageBlock(water)), simulation.arena.materialLaw(slot), simulation.tick, 0, 0, sample);
            near(helper, middle, sample.enthalpyJ(), "checkpoint retains the same joules during freezing");
            helper.assertTrue(sample.branch() == MaterialThermalLaw.COOLING, "checkpoint retains the cooling branch");
            simulation.acceptEnergyJ(slot, edge.targetEnthalpyJ() - middle);
            var ready = simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            helper.assertTrue(ready.phaseRequests().length == 1, "one completed freezing request");
            var request = ready.phaseRequests()[0];
            double before = simulation.totalEnergy();
            simulation.publish(simulation.change(water, Block.BLOCK_STATE_REGISTRY.byId(request.targetStateId()),
                            ResolvedGeometryBatch.MaterialChanges.THERMAL_TRANSITION),
                    new ThermalInputBatch.PhaseAck[]{new ThermalInputBatch.PhaseAck(request, PhaseTransitionRuntime.AckOutcome.APPLIED)});
            near(helper, before, simulation.totalEnergy(), "freezing ACK preserves total H");
            near(helper, edge.transitionTemperatureC(), simulation.temperatureC(simulation.slot(water), 0), "ice stage starts at the completed freezing temperature");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_material_palette", timeoutTicks = 160)
    public static void repeatedMaterialParameterChangesReuseStorageAndSaveOnlyReferencedParameters(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos start = origin(helper), first = start.offset(1, 1, 1), second = first.east();
        fill(level, start, 4, 4, 4, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(first, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(second, Blocks.STONE.defaultBlockState());
        MaterialSectionState state = storedBrick(helper, first, 17);
        helper.assertTrue(state.size() == 2, "fixture contains exactly two material records");
        MaterialSectionState original = state;
        var editor = new MaterialSectionState.Editor(state);
        int stateId = Block.BLOCK_STATE_REGISTRY.getId(Blocks.STONE.defaultBlockState());
        int firstPosition = pageBlock(first), secondPosition = pageBlock(second);
        for (int change = 0; change < 128; change++) {
            var law = MaterialThermalLaw.sensible(100 + change);
            double untouched = state.temperatureC(state.find(secondPosition));
            editor.update(firstPosition, stateId, law, law.enthalpyAtTemperature(31.25), (byte) 0, level.getGameTime());
            state = editor.snapshot();
            near(helper, untouched, state.temperatureC(state.find(secondPosition)), "reusing a palette entry must preserve other bodies");
            editor.update(secondPosition, stateId, law, law.enthalpyAtTemperature(44.75), (byte) 0, level.getGameTime());
            state = editor.snapshot();
        }
        int[] palette = ThermalLoadedWorldGameTests.read(state, "stateIds");
        helper.assertTrue(palette.length <= state.size(), "parameter history must not grow with repeated changes");
        near(helper, 17, original.temperatureC(original.find(firstPosition)), "older published snapshots remain immutable");
        var stored = new DormantChunkThermalState(first.getY() >> 4, 1);
        stored.replaceMaterials(first.getY() >> 4, state);
        CompoundTag encoded = new CompoundTag(); stored.encode(encoded);
        CompoundTag material = encoded.getCompound("FrostedHeartThermal")
                .getList("sections", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).getCompound("materials");
        helper.assertTrue(material.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND).size() == 1,
                "the save must contain only the one material law still referenced");
        var decoded = DormantChunkThermalState.decode(encoded, first.getY() >> 4, 1).materials(first.getY() >> 4);
        near(helper, 31.25, decoded.temperatureC(decoded.find(firstPosition)), "compacted save preserves the first temperature");
        near(helper, 44.75, decoded.temperatureC(decoded.find(secondPosition)), "compacted save preserves the second temperature");
        editor.remove(firstPosition);
        helper.assertTrue(editor.find(firstPosition)<0, "removal must be visible before publishing a snapshot");
        var removed=editor.snapshot();
        helper.assertTrue(removed.size()==1 && removed.find(firstPosition)<0, "snapshot must compact removed records");
        near(helper,31.25,state.temperatureC(state.find(firstPosition)), "a snapshot before removal must remain unchanged");
        editor.update(secondPosition,stateId,MaterialThermalLaw.sensible(300),300*9.0,(byte)0,level.getGameTime());
        near(helper,44.75,removed.temperatureC(removed.find(secondPosition)), "mutating after compaction must preserve its published snapshot");
        near(helper,9,editor.snapshot().temperatureC(0), "remaining body must update after compaction");
        helper.succeed();
    }

    public static MaterialSectionState storedBrick(GameTestHelper helper, BlockPos material, double temperatureC) {
        BlockPos start = new BlockPos(material.getX() & ~3, material.getY() & ~3, material.getZ() & ~3);
        try (Simulation simulation = new Simulation(helper, start, 4, 4, 4, 0)) {
            int slot = simulation.slot(material);
            simulation.acceptEnergyJ(slot, simulation.arena.materialLaw(slot).enthalpyAtTemperature(temperatureC)
                    - simulation.enthalpyJ(slot));
            simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            var captured = MaterialSectionState.capture(simulation.page(material).currentPublication(), simulation.query,
                    simulation.profiles.signatures(), new MaterialSectionState.CaptureScratch());
            helper.assertTrue(captured.valid(), "stored material fixture must be a coherent solver checkpoint");
            var container = new DormantChunkThermalState(material.getY() >> 4, 1);
            container.replaceMaterials(material.getY() >> 4, captured.state());
            CompoundTag tag = new CompoundTag(); container.encode(tag);
            tag.getCompound("FrostedHeartThermal").getList("sections", net.minecraft.nbt.Tag.TAG_COMPOUND)
                    .getCompound(0).getCompound("materials").putLong("tick", helper.getLevel().getGameTime());
            return DormantChunkThermalState.decode(tag, material.getY() >> 4, 1).materials(material.getY() >> 4);
        }
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_material_mutation_cost", timeoutTicks = 160)
    public static void storedMaterialMutationCostAndSnapshotIsolation(GameTestHelper helper) {
        ServerLevel level=helper.getLevel();MinecraftThermalInput.closeActiveLevel(level);
        BlockPos position=origin(helper).offset(1,1,1);
        level.setBlockAndUpdate(position,Blocks.STONE.defaultBlockState());
        var seed=storedBrick(helper,position,17);
        int section=position.getY()>>4;
        var encodedState=new DormantChunkThermalState(section,1);encodedState.replaceMaterials(section,seed);
        CompoundTag tag=new CompoundTag();encodedState.encode(tag);
        CompoundTag material=tag.getCompound("FrostedHeartThermal").getList("sections",net.minecraft.nbt.Tag.TAG_COMPOUND)
                .getCompound(0).getCompound("materials");
        int originalIndex=seed.find(pageBlock(position));
        int paletteIndex=material.getIntArray("palette_indexes")[originalIndex];
        byte[] positions=new byte[8192];long[] energies=new long[4096];int[] indexes=new int[4096];
        for(int i=0;i<4096;i++) {
            positions[i*2]=(byte)(i>>>8);positions[i*2+1]=(byte)i;
            energies[i]=Double.doubleToRawLongBits(seed.enthalpyJ(originalIndex));indexes[i]=paletteIndex;
        }
        material.putByteArray("positions",positions);material.putLongArray("enthalpy",energies);
        material.putIntArray("palette_indexes",indexes);material.putByteArray("branches",new byte[4096]);
        var stored=DormantChunkThermalState.decode(tag,section,1);
        var original=stored.materials(section);
        var chunk=level.getChunkAt(position);
        var attachment=(com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment)(Object)chunk;
        var previous=attachment.frostedheart$getDormantThermalState();
        var counter=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        counter.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().getId();
        long[] times=new long[32];
        try {
            attachment.frostedheart$setDormantThermalState(stored);
            for(int i=0;i<384;i++)chunk.setBlockState(position,(i&1)==0?Blocks.ANDESITE.defaultBlockState():Blocks.STONE.defaultBlockState(),false);
            long allocated=counter.getThreadAllocatedBytes(thread);
            for(int round=0;round<times.length;round++) {
                long start=System.nanoTime();
                for(int i=0;i<32;i++)chunk.setBlockState(position,(i&1)==0?Blocks.ANDESITE.defaultBlockState():Blocks.STONE.defaultBlockState(),false);
                times[round]=System.nanoTime()-start;
            }
            allocated=counter.getThreadAllocatedBytes(thread)-allocated;java.util.Arrays.sort(times);
            com.teammoeg.frostedheart.FHMain.LOGGER.info("Material setBlockState: records=4096, calls=1024, ns/call p50={}, p95={}, heap bytes/call={}",
                    times[16]/32.0,times[30]/32.0,allocated/1024.0);
            near(helper,17,original.temperatureC(original.find(pageBlock(position))),"earlier snapshot must remain immutable");
            near(helper,WorldTemperature.naturalAir(level,position),WorldTemperature.material(level,position),"replacement must initialize the new body");
            helper.succeed();
        } finally { attachment.frostedheart$setDormantThermalState(previous); }
    }

    static final class Simulation implements AutoCloseable {
        final GameTestHelper helper;
        final ServerLevel level;
        final MinecraftThermalProfiles.Snapshot profiles = MinecraftThermalProfiles.prepare();
        final ThermalCellArena arena = new ThermalCellArena(256);
        final ThermalMemoryBudget budget = new ThermalMemoryBudget(64L << 20);
        final QueryPublication query = QueryPublication.createRegional(budget.createCategory(), 64);
        final ThermalDimensionEngine engine;
        final MaterialStateAccess material;
        final LinkedHashMap<Long, ThermalPageHandle> pages = new LinkedHashMap<>();
        final MinecraftSignatureCapture capture;
        long sequence, tick;

        Simulation(GameTestHelper helper, BlockPos origin, int sizeX, int sizeY, int sizeZ, double natural) {
            this.helper = helper;
            level = helper.getLevel();
            capture = new MinecraftSignatureCapture(level, profiles.states(), profiles.signatures());
            var tuning = profiles.tuning();
            engine = new ThermalDimensionEngine(1, 0, arena, profiles.signatures(), profiles.materials(),
                    new ThermalTopologyParameters(tuning.airHeatCapacityJPerBlockK(), 0, tuning.airMixingWPerBlockK(),
                            new BuoyancyConductance.Parameters(.25, 4, 10), 1024, 8),
                    new FarFieldSettings(tuning.farFieldConductanceWPerK(), 32), tuning.campfire(),
                    ThermalDimensionLimits.sharedGrid(64, 128), query, budget.createCategory(), 1e-5);
            material = ThermalLoadedWorldGameTests.read(engine, "regional");
            LinkedHashMap<Long, Long> masks = new LinkedHashMap<>();
            for (int x = 0; x < sizeX; x += 4) for (int y = 0; y < sizeY; y += 4) for (int z = 0; z < sizeZ; z += 4) {
                BlockPos position = origin.offset(x, y, z);
                long section = SectionPos.asLong(position);
                masks.merge(section, 1L << brickIndex(position), (first, second) -> first | second);
            }
            byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
            ThermalInputBatch.PageAdmission[] admissions = new ThermalInputBatch.PageAdmission[masks.size()];
            int index = 0;
            for (var entry : masks.entrySet()) {
                ThermalPageHandle page = new ThermalPageHandle(entry.getKey(), 1);
                pages.put(entry.getKey(), page);
                admissions[index++] = new ThermalInputBatch.PageAdmission(page, 0, entry.getValue(), entry.getValue(),
                        capture.captureBricks(entry.getKey(), null, capture.unresolvedPage(), entry.getValue()), natural, sky, null);
            }
            process(admissions, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
        }
        ThermalCompletion publish(ResolvedGeometryBatch geometry, ThermalInputBatch.PhaseAck[] acks) { return process(ThermalInputBatch.NO_ADMISSIONS, geometry, acks); }
        ThermalCompletion process(ThermalInputBatch.PageAdmission[] admissions, ResolvedGeometryBatch geometry, ThermalInputBatch.PhaseAck[] acks) {
            var result = RegionalEngineTestDriver.submit(helper, engine, new ThermalInputBatch(1, ++sequence, tick, admissions,
                    ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES, geometry,
                    ThermalInputBatch.NO_ENVIRONMENT_UPDATES, acks, Double.NaN));
            helper.assertTrue(result.status() == ThermalCompletion.Status.COMPLETED, "material engine cut: " + result.failure());
            return result;
        }
        ThermalPageHandle page(BlockPos position) { return pages.get(SectionPos.asLong(position)); }
        int slot(BlockPos position) {
            var brick = page(position).currentPublication().brick(brickIndex(position));
            int block = (position.getX() & 3) | (position.getZ() & 3) << 2 | (position.getY() & 3) << 4;
            return brick.firstSlot() + (brick.blockLayout() == null ? 0 : brick.blockLayout().nodeAt(block));
        }
        ResolvedGeometryBatch change(BlockPos position, BlockState next, byte cause) {
            int previous = profiles.states().signatureId(level.getBlockState(position));
            level.setBlockAndUpdate(position, next);
            var page = page(position);
            var geometry = new ResolvedGeometryBatch.Builder();
            int signature = profiles.states().signatureId(next);
            geometry.addResolvedCenter(page, page.beginGeometryMutation(), pageBlock(position), signature);
            geometry.addMaterialChange(page, pageBlock(position), previous, signature, cause);
            return geometry.buildAndReset();
        }
        double totalEnergy() {
            double energy = 0;
            for (int slot = arena.nextLiveSlot(0); slot >= 0; slot = arena.nextLiveSlot(slot + 1)) energy += enthalpyJ(slot);
            return energy;
        }
        double enthalpyJ(int slot) {
            if (arena.isMaterialCell(slot)) {
                var sample = new MaterialSample(); material.readMaterial(slot, sample); return sample.enthalpyJ();
            }
            var sample = new QueryPublication.MutableSample();
            helper.assertTrue(query.tryRead(slot, arena.lifecycleGeneration(slot), 0, sample), "committed Air must be readable");
            return arena.capacityJPerK(slot) * sample.temperatureC();
        }
        double temperatureC(int slot, double referenceC) {
            var sample = new MaterialSample(); material.readMaterial(slot, sample); return sample.temperatureC();
        }
        double acceptEnergyJ(int slot, double energy) {
            if (energy == 0 || arena.materialTransitionWaiting(slot)) return 0;
            var sample = new MaterialSample(); material.readMaterial(slot, sample);
            var law = sample.law();
            double h = sample.enthalpyJ(), direction = Math.signum(energy), remaining = Math.abs(energy);
            byte branch = sample.branch();
            // Test setup crosses sensible/latent segments without exceeding the next ACK.
            for (int segment = 0; segment < 3 && remaining > 0; segment++) {
                branch = law.selectBranch(h, branch, direction);
                double accepted = Math.min(remaining, law.energyLimitJ(h, branch, direction));
                if (accepted == 0) break;
                h += direction * accepted;
                remaining -= accepted;
            }
            material.applyMaterialControl(slot, h, branch);
            PhaseTransitionRuntime phases = ThermalLoadedWorldGameTests.read(engine, "phases");
            phases.markMaterialDirty(slot);
            return direction * (Math.abs(energy) - remaining);
        }
        double airConductance(int first, int second) {
            ThermalSolver solver = ThermalLoadedWorldGameTests.read(engine, "solver");
            double total = 0;
            for (var page : pages.values()) for (int brick = 0; brick < 64; brick++) {
                var fragment = solver.fragment(page.lastPublication().workerPageSlot() * 64 + brick);
                var pairs = fragment.airPairs();
                for (int index = 0; index < pairs.size(); index++) {
                    if (pairs.first(index) == first && pairs.second(index) == second || pairs.first(index) == second && pairs.second(index) == first) total += pairs.conductance(index);
                }
                var routed = fragment.routedContacts();
                for (int index = 0; index < routed.size(); index++) {
                    if (routed.active(index) && (routed.first(index) == first && routed.second(index) == second
                            || routed.first(index) == second && routed.second(index) == first)) total += routed.conductance(index);
                }
            }
            return total;
        }
        @Override public void close() { engine.close(); }
    }

    private static BlockPos origin(GameTestHelper helper) {
        BlockPos position = helper.absolutePos(new BlockPos(4, 8, 4));
        return new BlockPos(position.getX() & ~3, position.getY() & ~3, position.getZ() & ~3);
    }
    private static BlockPos axisPosition(BlockPos origin, int axis, int coordinate, int first, int second) {
        return axis == 0 ? origin.offset(coordinate, first, second) : axis == 1 ? origin.offset(first, coordinate, second) : origin.offset(first, second, coordinate);
    }
    private static int brickIndex(BlockPos p) { return (p.getX() & 15) >>> 2 | ((p.getZ() & 15) >>> 2) << 2 | ((p.getY() & 15) >>> 2) << 4; }
    private static int pageBlock(BlockPos p) { return (p.getX() & 15) | (p.getZ() & 15) << 4 | (p.getY() & 15) << 8; }
    private static void fill(ServerLevel level, BlockPos origin, int sizeX, int sizeY, int sizeZ, BlockState state) {
        for (int x = 0; x < sizeX; x++) for (int y = 0; y < sizeY; y++) for (int z = 0; z < sizeZ; z++) level.setBlockAndUpdate(origin.offset(x, y, z), state);
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-6 * Math.max(1, Math.abs(expected)), message + ": expected=" + expected + ", actual=" + actual);
    }
}
