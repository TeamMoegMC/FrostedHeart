/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Bounded immutable filter copy used by menus and town snapshots. */
public record P2PFilterSnapshot(
        boolean whitelist,
        boolean fuzzy,
        List<ItemStack> entries
) {
    private static final Codec<List<ItemStack>> ENTRIES_CODEC = ItemStack.CODEC.listOf()
            .flatXmap(P2PFilterSnapshot::validateEntries, P2PFilterSnapshot::validateEntries);
    public static final Codec<P2PFilterSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("whitelist", true).forGetter(P2PFilterSnapshot::whitelist),
            Codec.BOOL.optionalFieldOf("fuzzy", false).forGetter(P2PFilterSnapshot::fuzzy),
            ENTRIES_CODEC.optionalFieldOf("entries", List.of()).forGetter(P2PFilterSnapshot::entries)
    ).apply(instance, P2PFilterSnapshot::new));

    public P2PFilterSnapshot {
        List<ItemStack> safe = new ArrayList<>(P2PItemFilter.SLOT_COUNT);
        if (entries != null) {
            entries.stream().limit(P2PItemFilter.SLOT_COUNT)
                    .map(stack -> stack == null ? ItemStack.EMPTY : stack.copyWithCount(1))
                    .forEach(safe::add);
        }
        while (safe.size() < P2PItemFilter.SLOT_COUNT) {
            safe.add(ItemStack.EMPTY);
        }
        entries = List.copyOf(safe);
    }

    public static P2PFilterSnapshot from(P2PItemFilter filter) {
        List<ItemStack> entries = new ArrayList<>(P2PItemFilter.SLOT_COUNT);
        for (int slot = 0; slot < P2PItemFilter.SLOT_COUNT; slot++) {
            entries.add(filter.getEntry(slot));
        }
        return new P2PFilterSnapshot(filter.isWhitelist(), filter.isFuzzy(), entries);
    }

    private static DataResult<List<ItemStack>> validateEntries(List<ItemStack> entries) {
        return entries != null && entries.size() <= P2PItemFilter.SLOT_COUNT
                ? DataResult.success(entries)
                : DataResult.error(() -> "P2P filter snapshot exceeds "
                + P2PItemFilter.SLOT_COUNT + " entries.");
    }
}
