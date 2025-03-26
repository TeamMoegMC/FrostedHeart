/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.util;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import com.google.gson.JsonParser;
import com.teammoeg.chorda.util.struct.OptionalLazy;
import com.teammoeg.frostedheart.restarter.TssapProtocolHandler;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import static com.teammoeg.frostedheart.FHMain.*;

public class FHRemote {
    public static class FHLocal extends FHRemote {
        @Override
        public void doFetch() {
        	this.stableVersion=TssapProtocolHandler.localVersion;
            if (this.stableVersion != null) {
                LOGGER.info(VERSION_CHECK, "Fetched FH local version from .twrlastversion: " + this.stableVersion);
                return;
            }
            // fromCFM();
            // if (this.stableVersion != null) return;
            fromModVersion();
            if (this.stableVersion != null) {
                LOGGER.info(VERSION_CHECK, "Fetched FH local version from mod version: " + this.stableVersion);
            } else {
                LOGGER.info(VERSION_CHECK, "Failed to fetch FH local version, check your installation.");
                this.stableVersion = "";
            }
        }

        protected void fetch() {
            doFetch();
        }

        private void fromCFM() {//from curseforge manifest
            File vers = new File(FMLPaths.GAMEDIR.get().toFile(), "manifest.json");
            if (vers.exists()) {
                try {
                    JsonParser parser = new JsonParser();
                    try (FileReader fr = new FileReader(vers)) {
                        this.stableVersion = parser.parse(fr).getAsJsonObject().get("version").getAsString();
                    }
                } catch (Throwable e) {
                    LOGGER.error(VERSION_CHECK, "Error fetching FH local version from curseforge manifest", e);
                    throw new RuntimeException("[TWR Version Check] Error fetching FH local version from curseforge manifest", e);
                }
            }
        }

        private void fromModVersion() {
            try {
                String versionWithMC = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
                this.stableVersion = versionWithMC.substring(versionWithMC.indexOf('-') + 1);
            } catch (Throwable e) {
                LOGGER.error(VERSION_CHECK, "Error fetching FH local version from mod version", e);
                throw new RuntimeException("[TWR Version Check] Error fetching FH local version from mod version", e);
            }
        }




    }

    public String stableVersion = null;
    
    /**
     * Fetch a simple string from remote URL
     *
     * @param address URL address of the string location
     * @return the trimmed String instance on the address
     */
    public static String fetchString(String address) {
        try {
            URL url = new URL(address);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            try (Scanner tScanner = new Scanner(urlConnection.getInputStream())) {
                if (tScanner.hasNextLine()) {
                    return tScanner.nextLine().trim();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    public FHRemote() {
        fetch();
    }

    protected void doFetch() {
       /* try {
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(fetchString("https://addons-ecs.forgesvc.net/api/v2/addon/535790")).getAsJsonObject();
            String fileName = json.get("latestFiles").getAsJsonArray().get(0).getAsJsonObject().get("fileName").getAsString();
            stableVersion = fileName.substring(18, fileName.indexOf(".zip"));
        } catch (Throwable e) {
            stableVersion = "";
            e.printStackTrace();
        }*/
        if (stableVersion == null || stableVersion.isEmpty()) {
            stableVersion = fetchString("http://server.teammoeg.com:15010/data/twrver");
        }
    }

    protected void fetch() {
        new Thread(this::doFetch).start();
    }
    
    public OptionalLazy<FHVersion> versionCache;
    public OptionalLazy<FHVersion> fetchVersion() {
    	if(versionCache==null)
    		versionCache= OptionalLazy.of(() -> this.stableVersion == null ? null : FHVersion.parse(this.stableVersion));
    	return versionCache;
    }
}
