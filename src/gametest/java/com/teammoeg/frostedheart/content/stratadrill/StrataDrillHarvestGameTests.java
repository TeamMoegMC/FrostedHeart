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

package com.teammoeg.frostedheart.content.stratadrill;

import blusunrize.immersiveengineering.common.register.IEFluids;
import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerTileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Exercises real Forge harvest decisions and native survival mining on a controlled face. */
@GameTestHolder("frostedheart_stratadrill_harvest")
@PrefixGameTestTemplate(false)
public final class StrataDrillHarvestGameTests {
    private static final String STEAM = "steam";
    private static final String DIESEL = "diesel_mb";

    private StrataDrillHarvestGameTests() {}

    @GameTest(template = "phase0a_empty")
    public static void nativeHarvestTierAndBareHandFallback(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "S04Harvest"));
        var deepslate = Blocks.DEEPSLATE.defaultBlockState();
        var dirt = Blocks.DIRT.defaultBlockState();
        for (boolean diesel : new boolean[]{false, true}) {
            ItemStack drill = drill(h, diesel, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, drill);
            h.assertFalse(ForgeHooks.isCorrectToolForDrops(deepslate, player), "dry drill has no mining tier");
            h.assertFalse(drill.isCorrectToolForDrops(dirt), "dry drill itself is not a powered tool");
            h.assertTrue(ForgeHooks.isCorrectToolForDrops(dirt, player), "the native bare-hand drop rule still applies");

            // The inactive reservoir must not power a previously fueled engine.
            StrataDrillData.edit(drill).putFloat(STEAM, diesel ? StrataDrillPower.STEAM_PER_BLOCK : 0);
            StrataDrillData.edit(drill).putInt(DIESEL, diesel ? 0 : StrataDrillPower.DIESEL_PER_BLOCK);
            h.assertFalse(ForgeHooks.isCorrectToolForDrops(deepslate, player), "inactive reserve must not grant a tier");

            StrataDrillData.edit(drill).putFloat(STEAM, diesel ? 0 : StrataDrillPower.STEAM_PER_BLOCK);
            StrataDrillData.edit(drill).putInt(DIESEL, diesel ? StrataDrillPower.DIESEL_PER_BLOCK : 0);
            var before = drill.getOrCreateTag().copy();
            h.assertTrue(ForgeHooks.isCorrectToolForDrops(Blocks.STONE.defaultBlockState(), player), "powered stone tier");
            h.assertTrue(ForgeHooks.isCorrectToolForDrops(deepslate, player), "powered deepslate tier");
            h.assertFalse(ForgeHooks.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState(), player),
                    "iron drill cannot harvest diamond-tier obsidian");
            h.assertTrue(before.equals(drill.getTag()), "harvest permission checks do not change stack data");
        }
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void spiralBoostUsesActualForgeDigSpeed(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(7, 3, 6));
        var level = h.getLevel();
        var player = miner(h, pos);
        var spiral = holdDrill(h, player, false, false, 500);
        var before = spiral.getTag().copy();
        for (var block : List.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.DEEPSLATE, Blocks.FURNACE,
                Blocks.OBSIDIAN, Blocks.TORCH, Blocks.BEDROCK)) {
            if (block == Blocks.TORCH) level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            var state = level.getBlockState(pos);
            h.assertTrue(state.is(block), "speed fixture placed " + block);
            float hardness = state.getDestroySpeed(level, pos);
            float base = state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? StrataDrillItem.nominalHeadSpeed(spiral) : 1F;
            float expected = block == Blocks.COBBLESTONE ? base * 1.1F
                    : block == Blocks.DEEPSLATE || block == Blocks.FURNACE ? base * 1.3F : base;
            float speed = player.getDigSpeed(state, pos);
            assertSpeed(h, speed, expected, block + " hardness " + hardness);
            if (block == Blocks.DEEPSLATE) {
                assertSpeed(h, spiral.getDestroySpeed(state), expected, "deepslate raw item speed");
                assertSpeed(h, player.getDigSpeed(state, null), expected, "deepslate positionless player speed");
            }
            if (block == Blocks.STONE || block == Blocks.DEEPSLATE) {
                float progress = state.getDestroyProgress(player, level, pos);
                h.assertTrue(Math.ceil(1F / progress) == (block == Blocks.STONE ? 5 : 8)
                                && Math.ceil(30F * hardness / base) == (block == Blocks.STONE ? 5 : 10),
                        block + ": actual destroy progress versus original tick budget");
            }
        }

        h.assertTrue(before.equals(spiral.getTag()), "speed queries do not rewrite native drill data");
        var cutFace = face(pos);
        fillFace(h, cutFace, 9);
        level.setBlockAndUpdate(pos, Blocks.DEEPSLATE.defaultBlockState());
        var slate = level.getBlockState(pos);
        float base = StrataDrillItem.nominalHeadSpeed(spiral);
        spiral.enchant(Enchantments.BLOCK_EFFICIENCY, 2);
        assertSpeed(h, player.getDigSpeed(slate, pos), base * 1.3F + 5F,
                "Efficiency II stays additive, not multiplied by the hardness bonus");
        // Vanilla's START/STOP and Forge's break/drop callbacks still see one paid center.
        breakCenter(h, player, pos);
        assertCut(h, cutFace, 9, 1, "spiral cuts only the center of a full face");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                        .map(ItemEntity::getItem).filter(stack -> stack.is(Items.COBBLED_DEEPSLATE))
                        .mapToInt(ItemStack::getCount).sum() == 1,
                "the paid center retains its real deepslate drop");
        assertAccounting(h, spiral, false, 500 - price(false), 1, "boosted spiral");

        level.setBlockAndUpdate(pos, slate);
        var wheel = holdDrill(h, player, false, true, 500);
        float wide = player.getDigSpeed(slate, pos);
        assertSpeed(h, wide, StrataDrillItem.nominalHeadSpeed(wheel), "wheel keeps its fixed speed");
        float focused = base * 1.3F;
        h.assertTrue(focused > wide && focused < 3 * wide,
                "one hard cell favors the spiral; three equal cells can favor the wheel");
        var diesel = holdDrill(h, player, true, false, 500);
        assertSpeed(h, player.getDigSpeed(slate, pos), StrataDrillItem.nominalHeadSpeed(diesel)
                        * StrataDrillPower.DIESEL_SPEED_MULTIPLIER * 1.3F,
                "diesel keeps its original drive multiplier alongside the spiral bonus");
        var pickaxe = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        assertSpeed(h, player.getDigSpeed(slate, pos), pickaxe.getDestroySpeed(slate),
                "a normal pickaxe in the same Forge event is untouched");
        StrataDrillData.edit(spiral).putFloat(STEAM, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, spiral);
        assertSpeed(h, player.getDigSpeed(slate, pos), 1F, "dry drill remains at bare-hand speed");
        StrataDrillData.edit(spiral).putFloat(STEAM, 500);
        spiral.setDamageValue(spiral.getMaxDamage() - 1);
        assertSpeed(h, player.getDigSpeed(slate, pos), 1F, "exhausted drill remains at bare-hand speed");
        h.succeed();
    }

    private static void assertSpeed(GameTestHelper h, float actual, float expected, String scenario) {
        h.assertTrue(Math.abs(actual - expected) < 0.0001F,
                scenario + ": expected dig speed " + expected + ", got " + actual);
    }

    @GameTest(template = "phase0a_empty")
    public static void eachRemovedCellCostsOneUnit(GameTestHelper h) {
        var center = h.absolutePos(new BlockPos(7, 3, 6));
        var face = face(center);
        var player = miner(h, center);
        for (boolean diesel : new boolean[]{false, true}) {
            for (int blocks : new int[]{1, 3, 9}) {
                fillFace(h, face, blocks);
                var tool = holdDrill(h, player, diesel, true, 500);
                breakCenter(h, player, center);
                String scenario = (diesel ? "diesel" : "steam") + " / " + blocks + " cells";
                assertCut(h, face, blocks, blocks, scenario);
                assertAccounting(h, tool, diesel, 500 - blocks * price(diesel), blocks, scenario);
            }
            fillFace(h, face, 9);
            var tool = holdDrill(h, player, diesel, true, 500);
            player.setShiftKeyDown(true);
            breakCenter(h, player, center);
            player.setShiftKeyDown(false);
            assertCut(h, face, 9, 1, "sneaking wheel");
            assertAccounting(h, tool, diesel, 500 - price(diesel), 1, "sneaking wheel");
        }
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void wheelStopsAtExactFuelBoundary(GameTestHelper h) {
        var center = h.absolutePos(new BlockPos(7, 3, 6));
        var face = face(center);
        var player = miner(h, center);
        var level = h.getLevel();
        var drops = new AABB(center).inflate(2);
        for (boolean diesel : new boolean[]{false, true}) {
            int price = price(diesel);
            for (int shortfall : new int[]{0, 1}) {
                fillFace(h, face, 9);
                var tool = holdDrill(h, player, diesel, true, 2 * price - shortfall);
                tool.enchant(Enchantments.SILK_TOUCH, 1);
                level.getEntitiesOfClass(ItemEntity.class, drops).forEach(ItemEntity::discard);
                breakCenter(h, player, center);
                int removed = shortfall == 0 ? 2 : 1;
                String scenario = (diesel ? "diesel" : "steam") + " / two cells minus " + shortfall;
                assertCut(h, face, 9, removed, scenario);
                assertAccounting(h, tool, diesel, (2 - removed) * price - shortfall, removed, scenario);
                var loot = level.getEntitiesOfClass(ItemEntity.class, drops);
                h.assertTrue(loot.stream().allMatch(drop -> drop.getItem().is(Items.STONE))
                                && loot.stream().mapToInt(drop -> drop.getItem().getCount()).sum() == removed,
                        scenario + ": even the final paid block drops itself with Silk Touch");
                h.assertFalse(StrataDrillPower.canRun(tool, false), scenario + ": cannot start another powered cut");
            }
        }
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void wheelDoesNotPayForRejectedCells(GameTestHelper h) {
        var center = h.absolutePos(new BlockPos(7, 3, 6));
        var face = face(center);
        var player = miner(h, center);
        var level = h.getLevel();
        for (boolean diesel : new boolean[]{false, true}) {
            fillFace(h, face, 0);
            h.getLevel().setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
            var protectedCell = center.above();
            var fallback = center.below().east();
            level.setBlockAndUpdate(protectedCell, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(center.below(), Blocks.BEDROCK.defaultBlockState());
            level.setBlockAndUpdate(center.east(), Blocks.OBSIDIAN.defaultBlockState());
            level.setBlockAndUpdate(fallback, Blocks.STONE.defaultBlockState());
            var tool = holdDrill(h, player, diesel, true, 2 * price(diesel));
            var listener = new NeighborBreak(player, protectedCell, event -> event.setCanceled(true));
            MinecraftForge.EVENT_BUS.register(listener);
            try {
                breakCenter(h, player, center);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
            }
            h.assertTrue(listener.calls == 1 && level.getBlockState(protectedCell).is(Blocks.STONE),
                    "Forge denies the first extra exactly once");
            h.assertTrue(level.getBlockState(center.below()).is(Blocks.BEDROCK)
                            && level.getBlockState(center.east()).is(Blocks.OBSIDIAN),
                    "unbreakable and higher-tier extras are not cut");
            h.assertTrue(level.isEmptyBlock(fallback), "the last fuel pays the next eligible extra instead");
            assertAccounting(h, tool, diesel, 0, 2, "only successful removals are billed");
        }
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void wheelRechecksFuelAfterBreakEvent(GameTestHelper h) {
        var center = h.absolutePos(new BlockPos(7, 3, 6));
        var face = face(center);
        var player = miner(h, center);
        for (boolean diesel : new boolean[]{false, true}) {
            fillFace(h, face, 9);
            int price = price(diesel);
            var tool = holdDrill(h, player, diesel, true, 2 * price);
            var listener = new NeighborBreak(player, center.above(), event -> {
                h.assertTrue((diesel ? StrataDrillPower.dieselStored(tool) : StrataDrillPower.steamStored(tool)) == price,
                        "the center is already paid and the next cell was affordable before the callback");
                if (diesel) StrataDrillData.edit(tool).putInt(DIESEL, price - 1);
                else StrataDrillData.edit(tool).putFloat(STEAM, price - 1);
            });
            MinecraftForge.EVENT_BUS.register(listener);
            try {
                breakCenter(h, player, center);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
            }
            h.assertTrue(listener.calls == 1, "the first affordable extra reached Forge's event");
            assertCut(h, face, 9, 1, "fuel changed in a neighbor BreakEvent");
            assertAccounting(h, tool, diesel, price - 1, 1, "don't wear the tool or spend fuel on the rejected extra");
        }
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void wheelLastWearPaysOneCellAndKeepsExhaustedTool(GameTestHelper h) {
        var center = h.absolutePos(new BlockPos(7, 3, 6));
        var face = face(center);
        var player = miner(h, center);
        for (boolean diesel : new boolean[]{false, true}) {
            fillFace(h, face, 9);
            var tool = holdDrill(h, player, diesel, true, 500);
            tool.setDamageValue(tool.getMaxDamage() - 2);
            var id = StrataDrillData.identity(tool);
            breakCenter(h, player, center);
            assertCut(h, face, 9, 1, "last usable hit must not spill into extras");
            h.assertTrue(player.getMainHandItem() == tool && !tool.isEmpty()
                            && id.equals(StrataDrillData.identity(tool)),
                    "the exhausted native tool remains in the same hand, with its own identity");
            assertAccounting(h, tool, diesel, 500 - price(diesel), tool.getMaxDamage() - 1, "last wear debit");
            h.assertFalse(StrataDrillPower.canRun(tool, false), "exhausted drill rejects another powered cut");
        }
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void steamChargerOwnsAndReturnsCanonicalStack(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, FHBlocks.CHARGER.get());
        var charger = (ChargerTileEntity) h.getBlockEntity(pos);
        var a = miner(h, h.absolutePos(pos));
        var b = miner(h, h.absolutePos(pos));
        var steam = holdDrill(h, a, false, false, 100);
        var diesel = holdDrill(h, b, true, true, 120);
        String idSteam = StrataDrillData.identity(steam);
        String idDiesel = StrataDrillData.identity(diesel);
        h.assertTrue(charger.onClick(b, InteractionHand.MAIN_HAND) == InteractionResult.PASS
                        && b.getMainHandItem() == diesel, "diesel must stay in the hand, not occupy the steam dock");
        var saved = new CompoundTag();
        saved.putFloat("power", 69F);
        charger.readCustomNBT(saved, false);
        h.assertTrue(charger.onClick(a, InteractionHand.MAIN_HAND).consumesAction()
                        && a.getMainHandItem().isEmpty(), "server dock, not player, now owns the inserted steam drill");
        h.assertTrue(charger.onClick(a, InteractionHand.OFF_HAND) == InteractionResult.PASS,
                "an empty offhand must not take the server-owned drill");
        charger.tick();
        var mid = new CompoundTag();
        charger.writeCustomNBT(mid, false);
        h.assertTrue(mid.getFloat("power") == 1F && mid.contains("dockedDrill"),
                "69 points buy exactly two 34-point increments and retain the one-point residue");
        charger.readCustomNBT(mid, false);
        h.assertTrue(charger.onClick(a, InteractionHand.MAIN_HAND).consumesAction(),
                "the player retrieves the deserialized drill through the normal main-hand action");
        var returned = a.getMainHandItem();
        h.assertTrue(idSteam.equals(StrataDrillData.identity(returned)) && !idSteam.equals(idDiesel),
                "dock save/load and return keep one unique native tool identity");
        h.assertTrue(StrataDrillPower.steamStored(returned) == 102F
                        && StrataDrillPower.dieselStored(returned) == 1000,
                "only the active steam reserve is filled");
        var empty = new CompoundTag();
        charger.writeCustomNBT(empty, false);
        h.assertFalse(empty.contains("dockedDrill"), "retrieval removes the dock-owned item");
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void plainNativeReadsDoNotCreateState(GameTestHelper h) {
        var item = ForgeRegistries.ITEMS.getValue(FHMain.rl(StrataDrillItem.ID));
        h.assertTrue(item instanceof StrataDrillItem, "native item registry");
        String expectedTetra = System.getProperty("s04.expectedTetra");
        if (expectedTetra != null) {
            boolean tetra = Boolean.parseBoolean(expectedTetra);
            h.assertTrue(ModList.get().isLoaded("tetra") == tetra && ModList.get().isLoaded("mutil") == tetra,
                    "Forge really loaded the intended optional-mod profile");
            h.assertTrue((item.getClass() == StrataDrillItem.class) != tetra,
                    "only the optional profile should substitute a workbench bridge for the native class");
        }
        var tool = new ItemStack(item);
        h.assertTrue(tool.getTag() == null, "native raw stack starts genuinely untagged");
        var assembly = StrataDrillAssembly.read(tool);
        h.assertTrue(assembly.steam() && !assembly.wheel() && "iron".equals(StrataDrillData.cutterId(tool)),
                "untagged drill means default steam, spiral and iron");
        h.assertTrue(tool.getDamageValue() == 0 && tool.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 1F,
                "zero-wear and dry native fallback are defined without a tag");
        h.assertTrue(tool.getTag() == null, "inspection and dry reads cannot mint persistent data");
        var legacy = tool.getOrCreateTag();
        legacy.putString("stratadrill/head_material", "stratadrill/wheel");
        legacy.putString("stratadrill/drive_material", "stratadrill/diesel");
        legacy.putFloat("frostedheart:stratadrill_steam", 200);
        legacy.putInt("frostedheart:stratadrill_diesel", 200);
        legacy.putInt("Damage", 30);
        var legacyBefore = legacy.copy();
        h.assertTrue(!StrataDrillAssembly.read(tool).wheel() && StrataDrillAssembly.read(tool).steam()
                        && StrataDrillPower.dieselStored(tool) == 0 && StrataDrillPower.steamStored(tool) == 0
                        && tool.getDamageValue() == 0 && legacyBefore.equals(tool.getTag()),
                "preproduction Tetra development tags are ignored, rather than migrated during reads");
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void replacingTheOwnedStackCancelsWheelNeighborWork(GameTestHelper h) {
        var center = h.absolutePos(new BlockPos(7, 3, 6));
        var face = face(center);
        var player = miner(h, center);
        fillFace(h, face, 9);
        var original = holdDrill(h, player, true, true, 500);
        var replacement = original.copy();
        var listener = new NeighborBreak(player, center.above(),
                event -> player.setItemInHand(InteractionHand.MAIN_HAND, replacement));
        MinecraftForge.EVENT_BUS.register(listener);
        try {
            breakCenter(h, player, center);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        h.assertTrue(listener.calls == 1 && player.getMainHandItem() == replacement,
                "the native event replaced the exact working stack during a candidate removal");
        assertCut(h, face, 9, 1, "a lookalike copy does not inherit work ownership");
        assertAccounting(h, original, true, 500 - price(true), 1, "original already-paid center");
        assertAccounting(h, replacement, true, 500, 0, "replacement has no debit or wear");
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void dieselUsesActualForgeFluidHandlerWithoutSpendingSteam(GameTestHelper h) {
        var tool = drill(h, true, false);
        StrataDrillData.edit(tool).putFloat(STEAM, 125);
        var diesel = new FluidStack(IEFluids.BIODIESEL.getStill(), StrataDrillPower.DIESEL_CAPACITY + 1);
        var fluid = tool.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElseThrow();
        var before = tool.getTag().copy();
        h.assertTrue(fluid.fill(diesel, FluidAction.SIMULATE) == StrataDrillPower.DIESEL_CAPACITY
                        && before.equals(tool.getTag()), "Forge simulate observes capacity without writing state");
        h.assertTrue(fluid.fill(diesel, FluidAction.EXECUTE) == StrataDrillPower.DIESEL_CAPACITY
                        && fluid.getFluidInTank(0).getAmount() == StrataDrillPower.DIESEL_CAPACITY,
                "a real Forge fill caps the native biodiesel tank");
        h.assertTrue(fluid.drain(1, FluidAction.EXECUTE).isEmpty() && StrataDrillPower.steamStored(tool) == 125F,
                "the non-drainable tank is independent of steam storage");
        StrataDrillData.setAssembly(tool, StrataDrillAssembly.of(true, false));
        h.assertFalse(tool.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent(),
                "a steam drive must not expose the inactive diesel tank");
        h.assertTrue(StrataDrillPower.dieselStored(tool) == StrataDrillPower.DIESEL_CAPACITY,
                "drive change preserves the now inactive reserve");
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void cutterFallbackAndStaleWearPreserveNativeData(GameTestHelper h) {
        var tool = drill(h, false, false);
        StrataDrillData.edit(tool).putFloat(STEAM, 100);
        int ironDurability = tool.getMaxDamage();
        float ironSpeed = tool.getDestroySpeed(Blocks.DEEPSLATE.defaultBlockState());
        StrataDrillData.setCutter(tool, "removed_datapack_material");
        var before = tool.getTag().copy();
        h.assertTrue("removed_datapack_material".equals(StrataDrillData.cutterId(tool))
                        && tool.getMaxDamage() == ironDurability
                        && tool.getDestroySpeed(Blocks.DEEPSLATE.defaultBlockState()) == ironSpeed,
                "a temporary missing datapack definition falls back to iron behavior, not a state rewrite");
        h.assertTrue(before.equals(tool.getTag()), "a stats read leaves the stored unknown material name in place");

        // A known cutter may arrive before its newer capacity table, not just disappear outright.
        StrataDrillData.setCutter(tool, "iron");
        var id = StrataDrillData.identity(tool);
        StrataDrillData.edit(tool).putInt("wear", 15000);
        var loaded = ItemStack.of(tool.save(new CompoundTag()));
        var receiver = new ItemStack(tool.getItem());
        receiver.readShareTag(tool.getShareTag());
        for (var item : List.of(loaded, receiver)) {
            h.assertTrue(item.getDamageValue() == item.getMaxDamage() - 1 && !StrataDrillPower.canRun(item, true),
                    "the effective wear is bounded and rejects work even when Creative does not need fuel");
            h.assertTrue(StrataDrillData.read(item).getInt("wear") == 15000 && id.equals(StrataDrillData.identity(item)),
                    "loading or synchronization must not discard recorded wear while a new datapack cap is pending");
        }
        h.succeed();
    }

    public static final class NeighborBreak {
        private final FakePlayer player;
        private final BlockPos pos;
        private final Consumer<BlockEvent.BreakEvent> callback;
        private int calls;

        private NeighborBreak(FakePlayer player, BlockPos pos, Consumer<BlockEvent.BreakEvent> callback) {
            this.player = player;
            this.pos = pos;
            this.callback = callback;
        }

        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent event) {
            if (event.getPlayer() == player && event.getPos().equals(pos)) {
                ++calls;
                callback.accept(event);
            }
        }
    }

    private static List<BlockPos> face(BlockPos c) {
        return List.of(c, c.above(), c.below(), c.east(), c.west(),
                c.above().east(), c.above().west(), c.below().east(), c.below().west());
    }

    private static void fillFace(GameTestHelper h, List<BlockPos> face, int blocks) {
        var level = h.getLevel();
        for (int i = 0; i < face.size(); ++i) {
            level.setBlockAndUpdate(face.get(i), (i < blocks ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        level.setBlockAndUpdate(face.get(0).south(), Blocks.STONE.defaultBlockState());
    }

    private static void assertCut(GameTestHelper h, List<BlockPos> face, int placed, int removed, String scenario) {
        var level = h.getLevel();
        for (int i = 0; i < placed; ++i) {
            boolean correct = i < removed ? level.isEmptyBlock(face.get(i))
                    : level.getBlockState(face.get(i)).is(Blocks.STONE);
            h.assertTrue(correct, scenario + ": expected removal state at cell " + i);
        }
        h.assertTrue(level.getBlockState(face.get(0).south()).is(Blocks.STONE), scenario + ": back layer untouched");
    }

    private static FakePlayer miner(GameTestHelper h, BlockPos center) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "S04Fuel"));
        player.setPos(center.getX() + 0.5, center.getY() - 1, center.getZ() - 1.2);
        player.setOnGround(true);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return player;
    }

    private static ItemStack holdDrill(GameTestHelper h, FakePlayer player, boolean diesel, boolean wheel, int reserve) {
        var stack = drill(h, diesel, wheel);
        StrataDrillData.edit(stack).putFloat(STEAM, diesel ? 1000 : reserve);
        StrataDrillData.edit(stack).putInt(DIESEL, diesel ? reserve : 1000);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }

    private static int price(boolean diesel) {
        return diesel ? 7 : 25;
    }

    private static void assertAccounting(GameTestHelper h, ItemStack tool, boolean diesel, int left, int wear,
                                         String scenario) {
        h.assertTrue(StrataDrillPower.steamStored(tool) == (diesel ? 1000 : left)
                        && StrataDrillPower.dieselStored(tool) == (diesel ? left : 1000),
                scenario + ": only the installed drive should be debited");
        h.assertTrue(tool.getDamageValue() == wear, scenario + ": one wear per broken block");
    }

    /** Use the native START / progress / STOP path so the real mixin latches the cut plane. */
    private static void breakCenter(GameTestHelper h, FakePlayer player, BlockPos center) {
        ServerLevel level = h.getLevel();
        var mode = player.gameMode;
        var state = level.getBlockState(center);
        mode.handleBlockBreakAction(center, Action.START_DESTROY_BLOCK, Direction.NORTH, level.getMaxBuildHeight(), 1);
        if (level.isEmptyBlock(center)) return;
        float progress = state.getDestroyProgress(player, level, center);
        h.assertTrue(progress > 0, "fixture center admits mining progress");
        for (float total = 0; total < 1; total += progress) mode.tick();
        mode.handleBlockBreakAction(center, Action.STOP_DESTROY_BLOCK, Direction.NORTH, level.getMaxBuildHeight(), 2);
        h.assertTrue(level.isEmptyBlock(center), "native STOP removed the center");
    }

    private static ItemStack drill(GameTestHelper h, boolean diesel, boolean wheel) {
        var item = ForgeRegistries.ITEMS.getValue(FHMain.rl(StrataDrillItem.ID));
        h.assertTrue(item instanceof StrataDrillItem, "the native S04 item exists with or without Tetra");
        ItemStack stack = new ItemStack(item);
        StrataDrillData.setAssembly(stack, StrataDrillAssembly.of(!diesel, wheel));
        StrataDrillData.ensureIdentity(stack);
        return stack;
    }
}
