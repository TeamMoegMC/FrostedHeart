/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.SimpleItemKey;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.VirtualItemStack;
import com.teammoeg.frostedheart.content.town.network.WarehouseUpdatePacket;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Uses the loaded Forge registries and the actual warehouse packet constructors. */
@GameTestHolder("frostedheart_warehouse_network")
@PrefixGameTestTemplate(false)
public final class WarehouseNetworkGameTests {
    private WarehouseNetworkGameTests() {}

    @GameTest(template = "phase0a_empty", timeoutTicks = 40)
    public static void plainKeyConsumesTheRegistryPrefixBeforeNbt(GameTestHelper helper) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SimpleItemKey expected = new SimpleItemKey(Items.COAL, null);
            expected.writeTo(buffer);
            helper.assertTrue(expected.equals(SimpleItemKey.fromBuffer(buffer)),
                    "a plain warehouse item must decode without treating minecraft:item as NBT");
            helper.assertTrue(!buffer.isReadable(), "the key consumes its complete wire representation");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 40)
    public static void moddedKeyKeepsNestedNbtAndTheFollowingAmount(GameTestHelper helper) {
        SimpleItemKey expected = new SimpleItemKey(FHBlocks.WAREHOUSE.asItem(), nestedTag());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        long amount = 1L << 40;
        try {
            expected.writeTo(buffer);
            buffer.writeVarLong(amount);
            helper.assertTrue(expected.equals(SimpleItemKey.fromBuffer(buffer)),
                    "the mod item identity and nested Unicode NBT must survive decoding");
            helper.assertTrue(buffer.readVarLong() == amount,
                    "reading the key must leave the following long quantity aligned");
            helper.assertTrue(!buffer.isReadable(), "there is no unread tail after the quantity");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 40)
    public static void fullAndIncrementalPacketsKeepAllEntriesAligned(GameTestHelper helper) {
        SimpleItemKey plain = new SimpleItemKey(Items.COAL, null);
        SimpleItemKey tagged = new SimpleItemKey(FHBlocks.WAREHOUSE.asItem(), nestedTag());
        checkPacket(helper, false, List.of(new VirtualItemStack(plain, 64), new VirtualItemStack(tagged, 1L << 40)));
        checkPacket(helper, true, List.of(new VirtualItemStack(tagged, 7), new VirtualItemStack(plain, 0)));
        checkPacket(helper, false, List.of());
        helper.succeed();
    }

    private static void checkPacket(GameTestHelper helper, boolean incremental, List<VirtualItemStack> expected) {
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf decoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new WarehouseUpdatePacket(expected, incremental).encode(encoded);
            new WarehouseUpdatePacket(encoded).encode(decoded);
            helper.assertTrue(!encoded.isReadable(), "the warehouse packet consumes every encoded entry");
            helper.assertTrue(decoded.readBoolean() == incremental, "the update mode survives decoding");
            helper.assertTrue(decoded.readVarInt() == expected.size(), "the entry count survives decoding");
            for (VirtualItemStack entry : expected) {
                helper.assertTrue(decoded.readRegistryIdSafe(Item.class) == entry.getKey().item(),
                        "each entry retains its registry and item identity");
                helper.assertTrue(java.util.Objects.equals(decoded.readNbt(), entry.getKey().tag()),
                        "each entry retains its own NBT");
                helper.assertTrue(decoded.readVarLong() == entry.getAmount(),
                        "long quantities and zero-quantity removals stay aligned");
            }
            helper.assertTrue(!decoded.isReadable(), "the last entry leaves no unread packet bytes");
        } finally {
            encoded.release();
            decoded.release();
        }
    }

    private static CompoundTag nestedTag() {
        CompoundTag display = new CompoundTag();
        display.putString("Name", "{\"text\":\"仓库物品\"}");
        ListTag contents = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putString("id", "minecraft:coal");
        entry.putInt("Count", 12);
        contents.add(entry);
        CompoundTag tag = new CompoundTag();
        tag.put("display", display);
        tag.put("Items", contents);
        tag.putLong("stock", 1L << 40);
        return tag;
    }
}
