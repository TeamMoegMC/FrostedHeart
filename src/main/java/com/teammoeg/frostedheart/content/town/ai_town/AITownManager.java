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

package com.teammoeg.frostedheart.content.town.ai_town;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.content.town.citizen.sim.TownSimData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Current-session AI profiles and player simulations. Persistence belongs to the
 * record adapter; this registry never registers a competing SavedData writer.
 */
public final class AITownManager {
    private static final Map<UUID, AITownData> byId = new LinkedHashMap<>();
    private static final Map<UUID, TownSimData> playerSims = new LinkedHashMap<>();
    private static UUID lastAIId;
    private static Consumer<AITownData> aiAdded = town -> {};
    private static BiConsumer<UUID, TownSimData> playerSimAdded = (id, sim) -> {};
    private static Runnable metadataChanged = () -> {};

    private AITownManager() {}

    public static void bindPersistence(Consumer<AITownData> aiListener,
                                       BiConsumer<UUID, TownSimData> simListener,
                                       Runnable metadataListener) {
        aiAdded = aiListener;
        playerSimAdded = simListener;
        metadataChanged = metadataListener;
    }

    public static AITownData getOrCreate(String name, ResourceKey<Level> dimension) {
        for (AITownData town : byId.values()) if (town.getName().equals(name)) return town;
        AITownData town = new AITownData(UUID.randomUUID(), name, dimension);
        byId.put(town.getId(), town);
        aiAdded.accept(town);
        com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimScheduler.markRegistriesDirty();
        lastAIId = town.getId();
        metadataChanged.run();
        return town;
    }

    public static AITownData lastAI() { return lastAIId == null ? null : byId.get(lastAIId); }
    public static UUID lastAIId() { return lastAIId; }
    public static void restoreLastAIId(UUID id) { lastAIId = id; }
    public static Collection<AITownData> all() { return Collections.unmodifiableCollection(byId.values()); }

    public static void restoreAI(AITownData town) {
        byId.put(town.getId(), town);
        aiAdded.accept(town);
        com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimScheduler.markRegistriesDirty();
    }

    public static TownSimData getPlayerSim(UUID holderId) {
        TownSimData existing = playerSims.get(holderId);
        if (existing != null) return existing;
        TownSimData created = new TownSimData();
        playerSims.put(holderId, created);
        playerSimAdded.accept(holderId, created);
        return created;
    }

    public static TownSimData peekPlayerSim(UUID holderId) { return playerSims.get(holderId); }

    public static void restorePlayerSim(UUID holderId, TownSimData sim) {
        playerSims.put(holderId, sim);
        playerSimAdded.accept(holderId, sim);
    }

    public static TownSimData removePlayerSim(UUID holderId) {
        TownSimData previous = playerSims.remove(holderId);
        if (previous != null) previous.setMarkDirty(null);
        return previous;
    }

    public static void reset() {
        for (AITownData town : byId.values()) town.setPersistenceListener(null);
        for (TownSimData sim : playerSims.values()) sim.setMarkDirty(null);
        byId.clear();
        playerSims.clear();
        lastAIId = null;
        aiAdded = town -> {};
        playerSimAdded = (id, sim) -> {};
        metadataChanged = () -> {};
    }
}
