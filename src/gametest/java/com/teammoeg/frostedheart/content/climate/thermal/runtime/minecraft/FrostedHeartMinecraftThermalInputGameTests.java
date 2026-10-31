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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WearableThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalBrickCellLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import com.teammoeg.frostedheart.content.climate.thermal.source.CutLoadBuffer;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalAirRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_thermal_input")
@PrefixGameTestTemplate(false)
public final class FrostedHeartMinecraftThermalInputGameTests {
    private static final String TEMPLATE = "phase0a_empty";
    private static final String BATCH = "frostedheart_thermal_async";
    // These fixture ports already carry their concrete thermal binding.
    private static final ThermalSourceLedger.BindingUpdates FIXED_BINDINGS = new ThermalSourceLedger.BindingUpdates() {
        @Override public void registered(SpatialSourceTimeline.Descriptor descriptor, long tick, ThermalSourceLedger ledger) { }
        @Override public void emissionChanged(SpatialSourceTimeline.Descriptor descriptor, ThermalSourceLedger ledger) { }
        @Override public void unloaded(SpatialSourceTimeline.Descriptor descriptor) { }
    };

    private FrostedHeartMinecraftThermalInputGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_source", timeoutTicks = 120)
    public static void spatialSourceDeliversExactPowerAndSignedImpulse(GameTestHelper helper) {
        BlockPos target = sourceTarget(helper);
        try (var simulation = sourceSimulation(helper, Blocks.STONE.defaultBlockState(), 0);
                var timeline = new SpatialSourceTimeline(simulation.budget, 8)) {
            int slot = simulation.slot(target);
            var ports = new EmissionPort[]{EmissionPort.of(0, 1, SourceBinding.thermalNode(slot, simulation.arena.lifecycleGeneration(slot)))};
            double before = simulation.totalEnergy();
            helper.assertTrue(timeline.register(new SpatialSourceTimeline.Descriptor(0, 1, ThermalSourceMode.POWER_SOURCE,
                    target.getX(), target.getY(), target.getZ(), 1, ports), 20, true, 0), "source producer accepts ownership");
            submitSpatialHeat(helper, simulation, timeline, slot, 20);
            ThermalSourceLedger ledger = ThermalLoadedWorldGameTests.read(simulation.engine, "sources");
            helper.assertTrue(Math.abs(ledger.energyBalance().outputJ() - 20) < 1e-9
                            && Math.abs(ledger.energyBalance().deliveredJ() - 20) < 1e-9
                            && Math.abs(simulation.totalEnergy() - before - 20) < 1e-9,
                    "one second at 20 W reaches the committed regional field exactly once");
            helper.assertTrue(timeline.register(new SpatialSourceTimeline.Descriptor(1, 1, ThermalSourceMode.IMPULSE,
                    target.getX(), target.getY(), target.getZ(), 1, ports), 0, true, 20), "impulse producer accepts ownership");
            timeline.impulse(1, 0, -5, 20);
            submitSpatialHeat(helper, simulation, timeline, slot, 20);
            helper.assertTrue(Math.abs(simulation.totalEnergy() - before - 15) < 1e-9
                            && Math.abs(ledger.energyBalance().outputJ() - 15) < 1e-9
                            && Math.abs(ledger.energyBalance().deliveredJ() - 15) < 1e-9,
                    "a same-tick signed impulse changes the accepted field without replaying the power interval");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_source_balance", timeoutTicks = 120)
    public static void spatialSourceAccountsForSaturatedRegionalMaterial(GameTestHelper helper) {
        BlockPos target = sourceTarget(helper);
        var edge = com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles
                .materialLaw(Blocks.ICE.defaultBlockState()).heating();
        helper.assertTrue(edge != null, "real ice has a melting endpoint");
        try (var simulation = sourceSimulation(helper, Blocks.ICE.defaultBlockState(), edge.transitionTemperatureC());
                var timeline = new SpatialSourceTimeline(simulation.budget, 8)) {
            int slot = simulation.slot(target);
            simulation.material.applyMaterialControl(slot, edge.targetEnthalpyJ() - 10, MaterialThermalLaw.HEATING);
            double before = simulation.totalEnergy();
            var ports = new EmissionPort[]{EmissionPort.of(0, .5, SourceBinding.thermalNode(slot, simulation.arena.lifecycleGeneration(slot))),
                    EmissionPort.of(1, .3, SourceBinding.declaredLoss(1)), EmissionPort.of(2, .2, SourceBinding.degradedLoss(2))};
            helper.assertTrue(timeline.register(new SpatialSourceTimeline.Descriptor(0, 1, ThermalSourceMode.POWER_SOURCE,
                    target.getX(), target.getY(), target.getZ(), 1, ports), 100, true, 0), "source producer accepts ownership");
            submitSpatialHeat(helper, simulation, timeline, slot, 20);
            ThermalSourceLedger ledger = ThermalLoadedWorldGameTests.read(simulation.engine, "sources");
            var balance = ledger.energyBalance();
            helper.assertTrue(Math.abs(balance.outputJ() - 100) < 1e-8 && Math.abs(balance.deliveredJ() - 10) < 1e-8
                            && Math.abs(balance.unacceptedJ() - 40) < 1e-8 && Math.abs(balance.declaredLossJ() - 30) < 1e-8
                            && Math.abs(balance.degradedLossJ() - 20) < 1e-8
                            && Math.abs(simulation.totalEnergy() - before - 10) < 1e-8,
                    "regional saturation gives every source joule one destination: " + balance);
            double completed = simulation.enthalpyJ(slot);
            submitSpatialHeat(helper, simulation, timeline, slot, 40);
            helper.assertTrue(simulation.enthalpyJ(slot) == completed && Math.abs(ledger.energyBalance().unacceptedJ() - 90) < 1e-8,
                    "waiting for the phase ACK cannot absorb or replay rejected source energy");
            balance = ledger.energyBalance();
            helper.assertTrue(Math.abs(balance.outputJ() - balance.deliveredJ() - balance.unacceptedJ()
                            - balance.declaredLossJ() - balance.degradedLossJ()) < 1e-8,
                    "the next committed prefix preserves total source accounting");
            FHMain.LOGGER.info("THERMAL_SPATIAL_SOURCE_BALANCE,plateauC={},outputJ={},deliveredJ={},unacceptedJ={},declaredLossJ={},degradedLossJ={}",
                    edge.transitionTemperatureC(), balance.outputJ(), balance.deliveredJ(), balance.unacceptedJ(), balance.declaredLossJ(), balance.degradedLossJ());
        }
        helper.succeed();
    }

    private static BlockPos sourceTarget(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 6, 2));
        return new BlockPos(anchor.getX() & ~3, anchor.getY() & ~3, anchor.getZ() & ~3).offset(1, 1, 1);
    }

    private static ThermalMaterialBehaviorGameTests.Simulation sourceSimulation(GameTestHelper helper,
            net.minecraft.world.level.block.state.BlockState material, double naturalC) {
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        BlockPos target = sourceTarget(helper), origin = target.offset(-1, -1, -1);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(target, material);
        helper.getLevel().setBlockAndUpdate(target.east(), Blocks.AIR.defaultBlockState());
        return new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, naturalC);
    }

    /** Use production producer ownership, Store binding and regional commits with explicit bound test ports. */
    private static void submitSpatialHeat(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation,
            SpatialSourceTimeline timeline, int slot, long tick) {
        var runtime = (RegionalAirRuntime) simulation.material;
        ThermalSourceLedger ledger = ThermalLoadedWorldGameTests.read(simulation.engine, "sources");
        var windows = timeline.seal(tick);
        helper.assertTrue(windows.length != 0, "the producer must seal a real input window");
        for (int ordinal = 0; ordinal < windows.length; ordinal++) {
            var producer = windows[ordinal];
            var descriptor = producer.descriptor;
            var target = new SpatialHeatInput.Target(BlockPos.asLong(simulation.arena.minimum(slot, 0),
                    simulation.arena.minimum(slot, 1), simulation.arena.minimum(slot, 2)), SpatialHeatInput.MATERIAL,
                    slot, simulation.arena.lifecycleGeneration(slot), simulation.arena.materialProfileId(slot),
                    simulation.arena.materialIncarnation(slot), descriptor.sourceId(), descriptor.generation());
            var origins = new SpatialHeatInput.Target[descriptor.ports().length];
            for (int port = 0; port < origins.length; port++)
                if (descriptor.ports()[port].binding().kind() != SourceBinding.Kind.DECLARED_LOSS) origins[port] = target;
            runtime.prepareSourceWindow(producer, origins);
            var store = runtime.inputStore();
            var owned = store.acceptSourceWindow(producer, simulation.sequence + 1, ordinal, origins, ledger);
            var loads = new CutLoadBuffer();
            store.bindSourcePrefix(owned, tick, ledger, FIXED_BINDINGS, loads);
            runtime.prepareSpatialInput(loads.size());
            runtime.receiveSpatialHeat(SpatialHeatInput.takeWorldIdentity(loads.freeze(null, simulation.arena)));
        }
        simulation.tick = tick;
        simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_phase", timeoutTicks = 40)
    public static void phaseRequestAcknowledgementIsExactlyOnce(GameTestHelper helper) {
        ThermalCellArena arena = new ThermalCellArena(2);
        ArenaSpan allocation = phase(arena);
        PhaseTransitionRuntime phases = new PhaseTransitionRuntime(arena, 2);
        int phaseSlot = allocation.firstSlot() + 1;
        phases.registerMaterial(phaseSlot);
        arena.applyMaterialControl(phaseSlot, 10.0D, MaterialThermalLaw.HEATING);
        phases.collectMaterialRequests();
        double completedEnergy = arena.enthalpyJ(phaseSlot);
        PhaseTransitionRuntime.Request[] requests = phases.drainRequests(2);
        helper.assertTrue(requests.length == 1, "one completed material yields one request");
        PhaseTransitionRuntime.Request firstRequest = requests[0];
        helper.assertTrue(phases.applyAck(firstRequest, PhaseTransitionRuntime.AckOutcome.RETRY),
                "a deferred mutation must remain retryable");
        phases.collectMaterialRequests();
        requests = phases.drainRequests(2);
        helper.assertTrue(requests.length == 1
                        && requests[0].requestSequence() == firstRequest.requestSequence(),
                "retry retains the original request identity");
        helper.assertTrue(
                requests.length == 1
                        && phases.applyAck(
                                requests[0], PhaseTransitionRuntime.AckOutcome.APPLIED)
                        && !phases.applyAck(
                                requests[0], PhaseTransitionRuntime.AckOutcome.APPLIED),
                "phase ACK must mutate one request once");
        helper.assertTrue(arena.enthalpyJ(phaseSlot) == completedEnergy,
                "ACK must not subtract latent energy a second time");
        phases.unregisterMaterial(phaseSlot);
        arena.releasePageCells(0, 1, allocation);
        ArenaSpan replacement = phase(arena);
        int replacementSlot = replacement.firstSlot() + 1;
        phases.registerMaterial(replacementSlot);
        arena.applyMaterialControl(replacementSlot, 10.0D, MaterialThermalLaw.HEATING);
        phases.collectMaterialRequests();
        helper.assertTrue(!phases.applyAck(firstRequest, PhaseTransitionRuntime.AckOutcome.APPLIED),
                "a reused slot in the same Page cannot inherit a previous body's ACK");
        var replacementRequest = phases.drainRequests(2)[0];
        helper.assertTrue(phases.applyAck(replacementRequest, PhaseTransitionRuntime.AckOutcome.REJECTED)
                        && arena.enthalpyJ(replacementSlot) == 10,
                "rejection preserves the material's stored energy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_resync", timeoutTicks = 40)
    public static void resyncTokenRejectsAChangedLiveRevision(GameTestHelper helper) {
        ThermalPageHandle page = new ThermalPageHandle(0L, 1L);
        page.requireFullGeometryResync(
                ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED);
        ThermalPageHandle.GeometryResyncToken token =
                page.pendingFullGeometryResync();
        page.beginGeometryMutation();
        helper.assertTrue(
                !page.acknowledgeFullGeometryResync(token),
                "a changed live revision must reject the stale token");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH + "_signature", timeoutTicks = 40)
    public static void signatureDirectoryKeepsUnchangedBricksShared(GameTestHelper helper) {
        ThermalSignatureTable.Builder registry = ThermalSignatureTable.builder();
        int air = registry.intern(fullAir());
        ThermalSignatureTable signatures = registry.build();
        PageSignatures original = page(signatures, air);
        int[] changed = new int[64];
        Arrays.fill(changed, air);
        PageSignatures next = original.withBricks(
                signatures, new int[]{0}, new int[][]{changed});
        helper.assertTrue(
                original.brickPayload(1) == next.brickPayload(1),
                "unchanged Brick payload must be shared");
        helper.succeed();
    }

    @GameTest(
            template = TEMPLATE,
            batch = BATCH + "_minecraft_residency_handoff",
            timeoutTicks = 1_300)
    public static void minecraftResidencyHandoffHeatsHalfAirNeighbor(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int minX = alignedTwoBrickBase(origin.getX(), origin.getX() + 20);
        int minY = alignedBrickBase(origin.getY(), origin.getY() + 20);
        int minZ = alignedBrickBase(origin.getZ(), origin.getZ() + 20);
        BlockPos sourceAir = new BlockPos(minX, minY + 3, minZ);
        BlockPos neighborAir = new BlockPos(minX + 4, minY + 3, minZ);
        BlockPos pageCenter = new BlockPos(
                SectionPos.sectionToBlockCoord(
                        SectionPos.blockToSectionCoord(sourceAir.getX())) + 8,
                SectionPos.sectionToBlockCoord(
                        SectionPos.blockToSectionCoord(sourceAir.getY())) + 8,
                SectionPos.sectionToBlockCoord(
                        SectionPos.blockToSectionCoord(sourceAir.getZ())) + 8);
        double pageNatural = WorldTemperature.naturalAir(level, pageCenter);

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    level.setBlockAndUpdate(
                            new BlockPos(minX + x, minY + y, minZ + z),
                            (y < 2 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
                }
            }
        }
        helper.assertTrue(level.getBlockState(sourceAir).isAir() && level.getBlockState(neighborAir).isAir(),
                "both temperature probes must be in the fixture's upper Air half");
        for (int x = 1; x <= 2; x++) {
            for (int z = 1; z <= 2; z++) {
                BlockPos campfire = new BlockPos(
                        minX + x, minY + 2, minZ + z);
                level.setBlockAndUpdate(
                        campfire,
                        Blocks.CAMPFIRE.defaultBlockState()
                                .setValue(CampfireBlock.LIT, true));
                BlockEntity entity = level.getBlockEntity(campfire);
                ((ICampfireExtra) entity).setLifeTime(20_000);
            }
        }

        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(
                sourceAir.getX() + 0.5D,
                sourceAir.getY() + 0.5D - player.getEyeHeight(),
                sourceAir.getZ() + 0.5D);
        MinecraftThermalInput.gameplayPlayerEnvironment(
                player, pageNatural, new ThermalEnvironmentSample());
        helper.runAfterDelay(1_200, () -> helper.succeedWhen(() -> {
            double source = MinecraftThermalInput.gameplayPassiveEnvironment(
                    level, sourceAir, pageNatural);
            double neighbor = MinecraftThermalInput.gameplayPassiveEnvironment(
                    level, neighborAir, pageNatural);
            helper.assertTrue(
                    source > pageNatural + 1.0D,
                    "pre-existing campfires did not heat source Air: source="
                            + source + ", natural=" + pageNatural
                            + ", campfireLit=" + level.getBlockState(sourceAir.offset(1, -1, 1))
                                    .getValue(CampfireBlock.LIT));
            helper.assertTrue(
                    neighbor > pageNatural + 0.25D,
                    "half-Air neighbor did not warm: neighbor="
                            + neighbor + ", natural=" + pageNatural
                            + ", source=" + source);
            MinecraftThermalInput.closeActiveLevel(level);
        }));
    }

    @GameTest(
            template = TEMPLATE,
            batch = BATCH + "_phase_topology_churn",
            timeoutTicks = 600)
    public static void phaseRequestSurvivesSameBrickTopologyChurn(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int minX = alignedBrickBase(origin.getX(), origin.getX() + 20);
        int minY = alignedBrickBase(origin.getY(), origin.getY() + 20);
        int minZ = alignedBrickBase(origin.getZ(), origin.getZ() + 20);
        BlockPos phaseBlock = new BlockPos(minX + 1, minY + 1, minZ + 1);
        BlockPos targetAir = phaseBlock.above();
        BlockPos changingTopology = targetAir.east();
        BlockPos source = new BlockPos(minX, minY + 2, minZ);

        // The tiny template does not clear the whole Brick used by this fixture.
        // In particular, the source outlet must not inherit a previous test's block.
        for (BlockPos position : BlockPos.betweenClosed(minX, minY, minZ, minX + 3, minY + 3, minZ + 3)) {
            level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(
                phaseBlock, Blocks.PACKED_ICE.defaultBlockState());
        level.setBlockAndUpdate(
                changingTopology,
                Blocks.OAK_TRAPDOOR.defaultBlockState()
                        .setValue(BlockStateProperties.OPEN, false));
        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(
                targetAir.getX() + 0.5D,
                targetAir.getY() + 0.5D - player.getEyeHeight(),
                targetAir.getZ() + 0.5D);
        MinecraftThermalInput.gameplayPlayerEnvironment(
                player,
                WorldTemperature.naturalAir(level, targetAir),
                new ThermalEnvironmentSample());

        int randomTickSpeed = level.getGameRules().getInt(
                GameRules.RULE_RANDOMTICKING);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING)
                .set(0, level.getServer());
        int[] elapsed = {0};
        boolean[] phaseRequested = {false};
        helper.onEachTick(() -> {
            elapsed[0]++;
            MinecraftThermalInput.onGeneratorTick(
                    level, source, targetAir, 1_000.0D, true);
            if (elapsed[0] % 21 == 1) {
                level.setBlockAndUpdate(
                        changingTopology,
                        level.getBlockState(changingTopology).cycle(
                                BlockStateProperties.OPEN));
            }
            // Establish an actual pending request. Reaching 9 C alone only proves
            // onset of the latent plateau, not that this ACK/churn fixture is ready.
            if (elapsed[0] >= 100 && !phaseRequested[0]) {
                phaseRequested[0] = MinecraftThermalInput.requestGameplayPhase(level, phaseBlock,
                        level.getBlockState(phaseBlock),
                        com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw.HEATING);
            }
            if (elapsed[0] == 360) {
                var pending = new MaterialSample();
                helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, phaseBlock, pending)
                                && pending.requestSequence() > 0 && pending.law().heating() != null
                                && pending.law().heating().complete(pending.enthalpyJ()),
                        "a completed energy transition must be waiting for ACK before enabling world mutation");
                level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING)
                        .set(randomTickSpeed, level.getServer());
            }
            if (elapsed[0] > 360 && elapsed[0] % 40 == 0
                    && level.getBlockState(phaseBlock).is(Blocks.PACKED_ICE)) {
                helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, phaseBlock, new MaterialSample()),
                        "unchanged phase material became unreadable during topology churn");
            }
        });
        helper.succeedWhen(() -> {
            var phaseSample = new MaterialSample();
            boolean phaseReadable = MinecraftThermalInput.sampleMaterial(level, phaseBlock, phaseSample);
            helper.assertTrue(
                    !level.getBlockState(phaseBlock).is(Blocks.PACKED_ICE),
                    "phase Block never completed during topology churn: temperature=" + WorldTemperature.material(level, phaseBlock)
                            + ", randomTickSpeed=" + level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING)
                            + ", sample=" + phaseSample.source() + ", H=" + phaseSample.enthalpyJ() + ", request=" + phaseSample.requestSequence()
                            + ", targetH=" + (phaseReadable && phaseSample.law().heating() != null ? phaseSample.law().heating().targetEnthalpyJ() : Double.NaN));
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING)
                    .set(randomTickSpeed, level.getServer());
            MinecraftThermalInput.onPhysicalSourceRemoved(level, source);
            MinecraftThermalInput.closeActiveLevel(level);
        });
    }

    @GameTest(
            template = TEMPLATE,
            batch = BATCH + "_dropped_reservoir",
            timeoutTicks = 100)
    public static void droppedWarmStoneReceivesCampfireRadiation(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos source = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos itemPosition = source.east(2);
        level.setBlockAndUpdate(
                source,
                Blocks.CAMPFIRE.defaultBlockState()
                        .setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(source)).setLifeTime(20_000);

        double initial = WorldTemperature.naturalAir(level, itemPosition);
        ItemStack stack = new ItemStack(FHBlocks.WARM_STONE.asItem());
        new WearableThermalState(initial, initial).writeTo(stack);
        ItemEntity item = new ItemEntity(
                level,
                itemPosition.getX() + 0.5D,
                source.getY() + 0.5D,
                itemPosition.getZ() + 0.5D,
                stack);
        item.setNoGravity(true);
        item.setDeltaMovement(0.0D, 0.0D, 0.0D);
        item.setDefaultPickUpDelay();
        helper.assertTrue(level.addFreshEntity(item),
                "dropped warm stone must enter the test level");

        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(
                source.getX() + 8.5D,
                source.getY(),
                source.getZ() + 0.5D);
        MinecraftThermalInput.gameplayPlayerEnvironment(
                player,
                WorldTemperature.naturalAir(level, player.blockPosition()),
                new ThermalEnvironmentSample());

        helper.runAfterDelay(70, () -> {
            try {
                WearableThermalState warmed = WearableThermalState.read(
                        item.getItem()).orElseThrow();
                ThermalEnvironmentSample sample =
                        new ThermalEnvironmentSample();
                MinecraftThermalInput.gameplayItemEnvironment(
                        item,
                        WorldTemperature.naturalAir(
                                level, item.blockPosition()),
                        sample);
                helper.assertTrue(sample.radiantFluxWPerM2() > 0.0D,
                        "Campfire must provide positive dropped-item radiation");
                helper.assertTrue(
                        Double.compare(
                                warmed.surfaceTemperatureC(), initial) != 0,
                        "the exact ItemEntity stack must advance its state");
            } finally {
                item.discard();
                MinecraftThermalInput.closeActiveLevel(level);
            }
            helper.succeed();
        });
    }

    private static int alignedTwoBrickBase(int minimum, int maximum) {
        for (int value = minimum; value + 7 < maximum; value++) {
            if (Math.floorMod(value, 4) == 0
                    && (value >> 4) == (value + 7 >> 4)) {
                return value;
            }
        }
        throw new IllegalStateException("GameTest has no aligned two-Brick span");
    }

    private static int alignedBrickBase(int minimum, int maximum) {
        for (int value = minimum; value + 3 < maximum; value++) {
            if (Math.floorMod(value, 4) == 0) {
                return value;
            }
        }
        throw new IllegalStateException("GameTest has no aligned Brick span");
    }

    private static PageSignatures page(
            ThermalSignatureTable signatures,
            int id
    ) {
        PageSignatures.Builder builder = new PageSignatures.Builder(signatures);
        for (int brick = 0; brick < ThermalPageHandle.BASE_BRICK_COUNT; brick++) {
            builder.setUniformBrick(brick, id);
        }
        return builder.buildBricks();
    }

    private static byte[] sky() {
        byte[] result = new byte[256];
        Arrays.fill(result, (byte) 16);
        return result;
    }

    private static ResolvedThermalSignature fullAir() {
        return new ResolvedThermalSignature(100, 0);
    }

    private static ArenaSpan regular(
            ThermalCellArena arena,
            int pageSlot,
            int generation,
            int minX
    ) {
        ThermalBrickCellLayout layout = new ThermalBrickCellLayout();
        layout.reset(minX, 0, 0);
        layout.setRegularAir(100.0D / 64.0D);
        ArenaSpan allocation = arena.stageBrickCells(
                pageSlot, generation, layout, 0.0D, 0.0D, 4_096);
        arena.commitStagedCells(allocation);
        return allocation;
    }

    private static ArenaSpan phase(
            ThermalCellArena arena
    ) {
        ThermalBrickCellLayout layout = new ThermalBrickCellLayout();
        layout.reset(0, 0, 0);
        layout.setRegularAir(100.0D / 64.0D);
        layout.addMaterialCell(0, 0, 0, 100.0D, 0.0D);
        ArenaSpan allocation = arena.stageBrickCells(
                0, 1, layout, 0.0D, 0.0D, 4_096);
        var law = new com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw(
                100, 0, new com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw.Transition(
                        net.minecraft.world.level.block.Block.getId(net.minecraft.world.level.block.Blocks.WATER.defaultBlockState()),
                        0, 0, 10), null);
        arena.stageMaterialLaw(allocation.firstSlot() + 1, 1, law, 0);
        arena.commitStagedCells(allocation);
        return allocation;
    }

}
