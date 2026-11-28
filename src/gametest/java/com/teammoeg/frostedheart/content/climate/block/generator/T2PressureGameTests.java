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

package com.teammoeg.frostedheart.content.climate.block.generator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorState;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real town/base settlement, T2 state, recipe, and native network. The station sink continuously drains 5 heat/tick. */
@GameTestHolder("frostedheart_t2_pressure")
@PrefixGameTestTemplate(false)
public final class T2PressureGameTests {
    private static final String EMPTY = "phase0a_empty";
    private static final float HEAT_PER_PROCESS = 25F / 8F;

    private T2PressureGameTests() {}

    private static final class Rig {
        final ServerLevel level;
        final BlockPos master;
        final TeamDataHolder team = new TeamDataHolder(UUID.randomUUID(), null);
        final HeatEndpoint station = HeatEndpoint.consumer(-10, 5);
        final HeatNetwork network = new HeatNetwork();
        T2GeneratorState tower;
        GeneratorData data;
        float delivered;
        int baseSpent;

        Rig(GameTestHelper h, int process) {
            level = h.getLevel();
            master = h.absolutePos(BlockPos.ZERO);
            data = team.getData(FHSpecialDataTypes.GENERATOR_DATA);
            data.actualPos = master;
            data.dimension = level.dimension();
            data.isWorking = true;
            data.steamLevel = 1;
            data.process = process;
            network.tick(level); // First native topology reset; wire this isolated fixture afterwards.
            wire();
        }

        void wire() {
            tower = new T2GeneratorState() {
                @Override public Optional<TeamDataHolder> getTeamData() { return Optional.of(team); }
            };
            network.setEndpoints(List.of(tower.endpoint, station));
        }

        void step() {
            // Production T2GeneratorLogic: network extracts/returns actual unused pressure BEFORE GeneratorState.tickData.
            network.tick(level);
            tower.tickData(level, master);
            delivered += station.drainHeat(5);
        }

        void town() {
            data.townTick(level, team);
        }

        float allHeat() {
            return delivered + station.getHeat() + tower.endpoint.getHeat();
        }

        CompoundTag teamSave() {
            return (CompoundTag) GeneratorData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow(false, s -> {});
        }

        void restore(CompoundTag teamNbt, CompoundTag blockNbt) {
            data = GeneratorData.CODEC.parse(NbtOps.INSTANCE, teamNbt).getOrThrow(false, s -> {});
            team.setData(FHSpecialDataTypes.GENERATOR_DATA, data);
            wire();
            tower.readSaveNBT(blockNbt);
        }
    }

    private static void near(GameTestHelper h, float expected, float actual, String label) {
        h.assertTrue(Math.abs(actual - expected) < .001f, label + ": expected=" + expected + ", actual=" + actual);
    }

    @GameTest(template = EMPTY)
    public static void coldLiveGeneratorDoesNotDirtyItsSave(GameTestHelper h) {
        Rig cold = new Rig(h, 0);
        cold.data.isWorking = false;
        cold.data.steamLevel = 0F;
        cold.step();
        CompoundTag first = cold.teamSave();
        h.runAfterDelay(3, () -> {
            cold.step();
            h.assertTrue(first.equals(cold.teamSave()) && !first.contains("baseAccountedThrough"),
                    "live deduplication must not dirty the otherwise unchanged cold team save");
            h.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void liveThenTownInDerivedLevelPaysOverlapOnce(GameTestHelper h) {
        // Derived levels share the already advanced overworld clock; a late town callback can cover
        // exactly the same game tick as an earlier block callback.
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(nether != null, "derived test level must exist");
        int interval = FHConfig.SERVER.TOWN.townUpdateIntervalGameTicks.get();
        int cost = FHConfig.SERVER.TOWN.GENERATOR_T1.baseProcessTicksPerGameTick.get();
        int initial = (interval + 2) * cost;
        Rig rig = new Rig(h, initial);
        rig.data.dimension = Level.NETHER;
        rig.data.tickBlock(nether, rig.team);
        rig.data.tickBlock(nether, rig.team);
        h.assertTrue(rig.data.process == initial - cost && !rig.teamSave().contains("baseAccountedThrough"),
                "two callbacks on the same game tick use one live burn and persist no live deadline");
        rig.data.townTick(nether, rig.team);
        h.assertTrue(rig.data.process == initial - interval * cost,
                "town only pays for the still-uncovered portion of the batch");
        if (interval > 1) {
            CompoundTag save = rig.teamSave();
            h.assertTrue(save.getLong("baseAccountedThrough") == nether.getGameTime() + interval - 1L,
                    "town-only deadline retains the existing encoded key");
            rig.restore(save, new CompoundTag());
            rig.data.tickBlock(nether, rig.team);
            h.assertTrue(rig.data.process == initial - interval * cost,
                    "town handoff coverage survives when the live watermark is lost on reload");
        }
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void offBrokenAndEmptyCanDrainOnlyWhatWasPaid(GameTestHelper h) {
        Rig off = new Rig(h, 9), broken = new Rig(h, 9), spent = new Rig(h, 9);
        for (Rig r : List.of(off, broken, spent)) {
            r.step();
            near(h, 25, r.tower.endpoint.getHeat(), "initial prepaid outlet");
            h.assertTrue(r.data.process == 0, "one base tick + eight pressure process ticks spent");
        }
        off.data.isWorking = false;
        broken.data.isBroken = true;
        for (int i = 1; i <= 12; i++) {
            final boolean last = i == 12;
            h.runAfterDelay(i, () -> {
                for (Rig r : List.of(off, broken, spent)) {
                    r.step();
                    near(h, 25, r.allHeat(), "shutdown conservation, working=" + r.data.isWorking + ", broken=" + r.data.isBroken);
                    if (last) {
                        near(h, 25, r.delivered, "all paid heat left the outlet once");
                        near(h, 0, r.tower.endpoint.getHeat(), "outlet stayed empty");
                    }
                }
                if (last) h.succeed();
            });
        }
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void prepaidBaseCannotOverrideStopAndNoDemandCannotBurnExtra(GameTestHelper h) {
        Rig off = new Rig(h, 1000), broken = new Rig(h, 1000), idle = new Rig(h, 1000);
        int batch = FHConfig.SERVER.TOWN.townUpdateIntervalGameTicks.get();
        for (Rig r : List.of(off, broken, idle)) r.town();
        idle.network.setEndpoints(List.of(idle.tower.endpoint)); // A physically loaded but disconnected tower.
        for (int t = 1; t <= batch; t++) {
            final int tick = t;
            h.runAfterDelay(t, () -> {
                for (Rig r : List.of(off, broken, idle)) r.step();
                if (tick == 1) {
                    off.data.isWorking = false;
                    broken.data.isBroken = true;
                }
                for (Rig r : List.of(off, broken, idle)) {
                    h.assertTrue(r.data.process == 1000 - batch - 8,
                            "prepaid stop / disconnected outlet spent extra, tick=" + tick);
                    near(h, 25, r.allHeat(), "residual only, tick=" + tick);
                }
                if (tick == batch) {
                    near(h, 25, off.delivered, "off while prepaid");
                    near(h, 25, broken.delivered, "broken while prepaid");
                    near(h, 25, idle.tower.endpoint.getHeat(), "undemanded heat stays in the very same outlet");
                    h.succeed();
                }
            });
        }
    }

    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void savedTownWindowAndOutletNeitherReplayNorExpireLate(GameTestHelper h) {
        Rig r = new Rig(h, 300);
        int n = FHConfig.SERVER.TOWN.townUpdateIntervalGameTicks.get();
        r.town();
        int afterFirstBatch = r.data.process;
        r.town();
        h.assertTrue(afterFirstBatch == r.data.process, "overlapping town callbacks cannot charge base twice");
        h.runAfterDelay(1, () -> {
            r.step();
            r.data.isWorking = false;
            // Empty the station-side reservoir so this round trip isolates the producer's two real save owners.
            r.network.tick(r.level);
            r.delivered += r.station.drainHeat(r.station.getHeat());
            float stored = r.tower.endpoint.getHeat();
            h.assertTrue(stored > 0 && stored < 25, "non-empty partially drained outlet before save");
            int remaining = r.data.process;
            CompoundTag block = new CompoundTag();
            r.tower.writeSaveNBT(block);
            r.restore(r.teamSave(), block);
            near(h, stored, r.tower.endpoint.getHeat(), "restored actual outlet, not its earlier full offer");
            r.data.isWorking = true;
            r.data.steamLevel = 0; // No new pressure purchases during the clock handoff.
            r.step();
            h.assertTrue(r.data.process == remaining, "same paid game tick did not run base twice after codec reload");
            h.runAfterDelay(n + 4, () -> {
                // No BE and no town calls for more than one interval (unloaded/paused).
                r.step();
                h.assertTrue(r.data.process == remaining - 1, "resuming after stale town coverage pays exactly one live base tick");
                near(h, 25, r.allHeat(), "reload/gap retained only the original purchased pressure");
                h.succeed();
            });
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 50)
    public static void oldDuplicatedOffersMigrateOnlyIntoALegacyOutlet(GameTestHelper h) {
        Rig r = new Rig(h, 0);
        r.data.isWorking = false;
        CompoundTag old = r.teamSave();
        old.putFloat("powerLevel", 15);
        old.putFloat("lastPowerLevel", 15);
        r.restore(old, new CompoundTag());
        r.step();
        near(h, 15, r.tower.endpoint.getHeat(), "old save restored one balance, not two");
        CompoundTag newBlock = new CompoundTag();
        r.tower.writeSaveNBT(newBlock);
        r.restore(old, newBlock); // Independently saved old team + new chunk must not add the offer twice.
        for (int i = 1; i <= 10; i++) {
            final boolean last = i == 10;
            h.runAfterDelay(i, () -> {
                r.step();
                near(h, 15, r.allHeat(), "legacy balance not reissued");
                if (last) {
                    near(h, 15, r.delivered, "only one legacy balance was delivered");
                    h.succeed();
                }
            });
        }
    }

    @GameTest(template = EMPTY, timeoutTicks = 540)
    public static void oneCoalFeedsAHotSingleStationWithAndWithoutTown(GameTestHelper h) {
        Rig live = new Rig(h, 0), town = new Rig(h, 0);
        int batch = FHConfig.SERVER.TOWN.townUpdateIntervalGameTicks.get();
        for (Rig r : List.of(live, town)) r.data.inventory.setStackInSlot(0, new ItemStack(Items.COAL));
        for (int i = 0; i <= 500; i++) {
            final int tick = i;
            h.runAfterDelay(i + 1, () -> {
                if (tick > 0) {
                    live.step();
                    if (live.data.isActive) live.baseSpent++;
                    town.step();
                    for (Rig r : List.of(live, town)) {
                        int loaded = r.data.inventory.getStackInSlot(0).isEmpty() ? 1120 : 0;
                        near(h, (loaded - r.baseSpent - r.data.process) * HEAT_PER_PROCESS, r.allHeat(),
                                (r == live ? "live" : "town") + " coal ledger tick " + tick);
                    }
                }
                if (tick == batch) {
                    h.assertTrue(town.delivered >= 5 * (batch - 1),
                            "the hot town station is continuously supplied during the initial interval");
                }
                if (tick % batch == 0 && tick < 500) {
                    // Town START prepays the following block tick through the end of the new interval.
                    town.town();
                    if (town.data.isActive) town.baseSpent += batch;
                }
                if (tick == 500) {
                    for (Rig r : List.of(live, town)) {
                        h.assertTrue(!r.data.isActive && r.data.inventory.getStackInSlot(0).isEmpty()
                                && r.delivered > 2000 && r.delivered < 2200, "one real recipe; hot station in expected range");
                        near(h, 0, r.tower.endpoint.getHeat() + r.station.getHeat(), "all charged heat delivered");
                        FHMain.LOGGER.info("T2_ONE_COAL mode={} deliveredHeat={} baseProcess={} remainingProcess={}",
                                r == live ? "live" : "town", r.delivered, r.baseSpent, r.data.process);
                    }
                    h.succeed();
                }
            });
        }
    }
}
