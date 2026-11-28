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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Authoritative S04 cutter stats; independent of the workbench adapter and item identity. */
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class StrataDrillMaterials {
    public static final String IRON = "iron";
    private static final int WHITE = 0xFFFFFF;
    private static final int MAX_CUTTERS = 256;
    private static final Catalog DEFAULTS = new Catalog(Map.of(
            IRON, new Cutter(IRON, 9_000, 2, 6, WHITE),
            "steel", new Cutter("steel", 12_000, 2, 7, 0xDAE7F5)));
    private static volatile Catalog server = DEFAULTS;
    private static volatile Catalog client = DEFAULTS;

    private StrataDrillMaterials() {}

    /** Absolute harvest tier (wood=0 … netherite=4) and speed before head/drive offsets. */
    public record Cutter(String id, int durability, int tier, float efficiency, int rgb) {}

    /** One immutable generation shared across item and model reads. */
    public static final class Catalog {
        private final Map<String, Cutter> byId;
        private final Cutter fallback;

        private Catalog(Map<String, Cutter> byId) {
            this.byId = Map.copyOf(byId);
            this.fallback = this.byId.get(IRON);
        }

        private Cutter get(String id) {
            Cutter cutter = byId.get(id);
            return cutter != null ? cutter : fallback;
        }

        /** The dormant ID of a datapack cutter does not get a different cutter's tint. */
        public int rgb(String id) {
            Cutter cutter = byId.get(id);
            return cutter != null ? cutter.rgb() : WHITE;
        }
    }

    private static Catalog current() {
        return EffectiveSide.get().isServer() ? server : client;
    }

    public static Cutter get(String id) {
        return current().get(id);
    }

    public static boolean contains(String id) {
        return current().byId.containsKey(id);
    }

    public static Catalog clientCatalog() {
        return client;
    }

    public static void resetClient() {
        client = DEFAULTS;
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent event) {
        Sync message = new Sync(List.copyOf(server.byId.values()));
        if (event.getPlayer() != null) {
            FHNetwork.INSTANCE.sendPlayer(event.getPlayer(), message);
        } else {
            for (var player : event.getPlayerList().getPlayers()) FHNetwork.INSTANCE.sendPlayer(player, message);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        server = DEFAULTS;
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private Loader() {
            super(new Gson(), "stratadrill/cutters");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
            Map<String, Cutter> next = new HashMap<>(DEFAULTS.byId);
            files.entrySet().stream().filter(e -> e.getKey().getNamespace().equals(FHMain.MODID))
                    .sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                        String id = entry.getKey().getPath();
                        try {
                            JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), "cutter");
                            int durability = GsonHelper.getAsInt(object, "durability");
                            int tier = GsonHelper.getAsInt(object, "tier");
                            float efficiency = GsonHelper.getAsFloat(object, "efficiency");
                            String rgb = GsonHelper.getAsString(object, "rgb", "FFFFFF");
                            if (!id.matches("[a-z0-9_.-]{1,64}") || durability < 2 || tier < 0 || tier > 4
                                    || !Float.isFinite(efficiency) || efficiency < 0 || !rgb.matches("[0-9a-fA-F]{6}")
                                    || (next.size() >= MAX_CUTTERS && !next.containsKey(id))) {
                                throw new IllegalArgumentException("invalid stats or too many S04 cutters");
                            }
                            next.put(id, new Cutter(id, durability, tier, efficiency, Integer.parseInt(rgb, 16)));
                        } catch (RuntimeException invalid) {
                            FHMain.LOGGER.warn("Ignoring invalid S04 cutter {}: {}", entry.getKey(), invalid.getMessage());
                        }
                    });
            server = new Catalog(next);
        }
    }

    /** Rare server-to-client catalog sync, only at login or server datapack reload. */
    public record Sync(List<Cutter> cutters) implements CMessage {
        public Sync(FriendlyByteBuf buffer) {
            this(readCutters(buffer));
        }

        private static List<Cutter> readCutters(FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_CUTTERS) throw new IllegalArgumentException("Invalid S04 cutter count");
            java.util.ArrayList<Cutter> list = new java.util.ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                list.add(new Cutter(buffer.readUtf(64), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readFloat(), buffer.readInt()));
            }
            return List.copyOf(list);
        }

        @Override
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(cutters.size());
            for (Cutter cutter : cutters) {
                buffer.writeUtf(cutter.id(), 64);
                buffer.writeVarInt(cutter.durability());
                buffer.writeVarInt(cutter.tier());
                buffer.writeFloat(cutter.efficiency());
                buffer.writeInt(cutter.rgb());
            }
        }

        @Override
        public void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                context.enqueueWork(() -> {
                    Map<String, Cutter> colors = new HashMap<>(DEFAULTS.byId);
                    for (Cutter cutter : cutters) colors.put(cutter.id(), cutter);
                    client = new Catalog(colors);
                });
            }
            context.setPacketHandled(true);
        }
    }
}
