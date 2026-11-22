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

import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.items.modular.IModularItem;

import java.util.UUID;

/** Exercises real Forge/Tetra harvest decisions without mining a fixture or registering a listener. */
@GameTestHolder("frostedheart_stratadrill_harvest")
@PrefixGameTestTemplate(false)
public final class StrataDrillHarvestGameTests {
    private static final String STEAM = "frostedheart:stratadrill_steam";
    private static final String DIESEL = "frostedheart:stratadrill_diesel";

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
            h.assertTrue(drill.getDestroySpeed(deepslate) == 1F, "dry drill has bare-hand speed");
            h.assertFalse(drill.isCorrectToolForDrops(dirt), "dry drill itself is not a powered tool");
            h.assertTrue(ForgeHooks.isCorrectToolForDrops(dirt, player), "the native bare-hand drop rule still applies");

            // The inactive reservoir must not power a previously fueled engine.
            drill.getOrCreateTag().putFloat(STEAM, diesel ? StrataDrillPower.STEAM_PER_BLOCK : 0);
            drill.getOrCreateTag().putInt(DIESEL, diesel ? 0 : StrataDrillPower.DIESEL_PER_BLOCK);
            h.assertFalse(ForgeHooks.isCorrectToolForDrops(deepslate, player), "inactive reserve must not grant a tier");

            drill.getOrCreateTag().putFloat(STEAM, diesel ? 0 : StrataDrillPower.STEAM_PER_BLOCK);
            drill.getOrCreateTag().putInt(DIESEL, diesel ? StrataDrillPower.DIESEL_PER_BLOCK : 0);
            var before = drill.getOrCreateTag().copy();
            h.assertTrue(ForgeHooks.isCorrectToolForDrops(Blocks.STONE.defaultBlockState(), player), "powered stone tier");
            h.assertTrue(ForgeHooks.isCorrectToolForDrops(deepslate, player), "powered deepslate tier");
            h.assertFalse(ForgeHooks.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState(), player),
                    "iron drill cannot harvest diamond-tier obsidian");
            h.assertTrue(drill.getDestroySpeed(deepslate) > 1F, "powered drill keeps its Tetra speed");
            h.assertTrue(before.equals(drill.getTag()), "read-side harvest and speed checks do not change stack data");
        }
        h.succeed();
    }

    @GameTest(template = "phase0a_empty")
    public static void onlyScopedWheelCanHarvestAtTheNeighborPrice(GameTestHelper h) {
        ItemStack steam = drill(h, false, true);
        ItemStack diesel = drill(h, true, true);
        steam.getOrCreateTag().putFloat(STEAM, StrataDrillPower.STEAM_PER_WHEEL_NEIGHBOR);
        diesel.getOrCreateTag().putInt(DIESEL, StrataDrillPower.DIESEL_PER_WHEEL_NEIGHBOR);
        var deepslate = Blocks.DEEPSLATE.defaultBlockState();
        h.assertFalse(steam.isCorrectToolForDrops(deepslate) || diesel.isCorrectToolForDrops(deepslate),
                "the neighbor price does not power a center harvest");
        StrataDrillWorkAccess scope = new StrataDrillWorkAccess() {
            @Override
            public void fh$sendStrataDrillSnapshot(ServerPlayer observer) {}

            @Override
            public boolean fh$isDiscountedWheelStack(ItemStack stack) {
                return stack == steam || stack == diesel;
            }
        };
        StrataDrillWorkAccess previous = StrataDrillPower.enterWheelNeighborScope(scope);
        try {
            h.assertTrue(steam.isCorrectToolForDrops(deepslate), "one neighbor's worth of steam preserves the real tier");
            h.assertTrue(diesel.isCorrectToolForDrops(deepslate), "one neighbor's worth of diesel preserves the real tier");
            h.assertFalse(steam.copy().isCorrectToolForDrops(deepslate), "an unrelated identical stack gets no discount");
            h.assertFalse(diesel.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
                    "the discount does not upgrade the mining tier");
            steam.getOrCreateTag().putFloat(STEAM, StrataDrillPower.STEAM_PER_WHEEL_NEIGHBOR - 1);
            diesel.getOrCreateTag().putInt(DIESEL, StrataDrillPower.DIESEL_PER_WHEEL_NEIGHBOR - 1);
            h.assertFalse(steam.isCorrectToolForDrops(deepslate) || diesel.isCorrectToolForDrops(deepslate),
                    "a reserve below the neighbor price never powers a harvest");
        } finally {
            StrataDrillPower.restoreWheelNeighborScope(previous);
        }
        h.succeed();
    }

    private static ItemStack drill(GameTestHelper h, boolean diesel, boolean wheel) {
        var item = ForgeRegistries.ITEMS.getValue(FHMain.rl(TetraStrataDrillItem.ID));
        h.assertTrue(item instanceof TetraStrataDrillItem, "load Tetra and the actual S04 item for this test");
        ItemStack stack = new ItemStack(item);
        IModularItem.putModuleInSlot(stack, "stratadrill/head", "stratadrill/head",
                wheel ? "stratadrill/wheel" : "stratadrill/spiral");
        IModularItem.putModuleInSlot(stack, "stratadrill/drive", "stratadrill/drive",
                diesel ? "stratadrill/diesel" : "stratadrill/steam");
        IModularItem.updateIdentifier(stack);
        return stack;
    }
}
