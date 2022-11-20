/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.research.FHResearch;

import dev.ftb.mods.ftbteams.data.TeamManager;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;

public class FHResearchDataManager {
    public static MinecraftServer server;
    Path local;
    File regfile;
    static final FolderName dataFolder = new FolderName("fhresearch");
    public static FHResearchDataManager INSTANCE;
    private Map<UUID, TeamResearchData> data = new HashMap<>();

    public FHResearchDataManager(MinecraftServer s) {
        server = s;
        INSTANCE = this;
    }

    public TeamResearchData getData(UUID id) {
        TeamResearchData cn = data.computeIfAbsent(id, c -> new TeamResearchData(() -> TeamManager.INSTANCE.getTeamByID(id)));
        return cn;

    }

    public static RecipeManager getRecipeManager() {
        if (server != null)
            return server.getRecipeManager();
        return ClientUtils.mc().world.getRecipeManager();
    }

    public Collection<TeamResearchData> getAllData() {
        return data.values();
    }

    public void load() {
        local = server.func_240776_a_(dataFolder);
        regfile = new File(local.toFile().getParentFile(), "fhregistries.dat");
        FHResearch.clearAll();
        if (regfile.exists()) {
            try {
                FHResearch.load(CompressedStreamTools.readCompressed(regfile));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                FHMain.LOGGER.fatal("CANNOT READ RESEARCH REGISTRIES, MAY CAUSE UNSYNC!");

            }
        }
        FHResearch.init();
        local.toFile().mkdirs();
        for (File f : local.toFile().listFiles((f) -> f.getName().endsWith(".nbt"))) {
            try {
                UUID tud = UUID.fromString(f.getName().split("\\.")[0]);
                CompoundNBT nbt = CompressedStreamTools.readCompressed(f);
                TeamResearchData trd = new TeamResearchData(() -> TeamManager.INSTANCE.getTeamByID(tud));
                trd.deserialize(nbt, false);
                data.put(tud, trd);
            } catch (IllegalArgumentException ex) {
            	FHMain.LOGGER.error("Unexpected data file " + f.getName() + ", ignoring...");
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                FHMain.LOGGER.error("Unable to read data file " + f.getName() + ", ignoring...");
            }
        }
    }

    public void save() {
        try {
            CompressedStreamTools.writeCompressed(FHResearch.save(new CompoundNBT()), regfile);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            FHMain.LOGGER.fatal("CANNOT SAVE RESEARCH REGISTRIES, MAY CAUSE UNSYNC!");
        }
        for (Entry<UUID, TeamResearchData> entry : data.entrySet()) {
            File f = local.resolve(entry.getKey().toString() + ".nbt").toFile();
            try {
                CompressedStreamTools.writeCompressed(entry.getValue().serialize(false), f);

            } catch (IOException e) {
                e.printStackTrace();
                FHMain.LOGGER.error("Unable to save data file for team " + entry.getKey().toString() + ", ignoring...");
            }
        }
    }
}
