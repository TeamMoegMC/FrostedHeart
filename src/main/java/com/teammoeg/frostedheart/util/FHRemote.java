package com.teammoeg.frostedheart.util;

import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.FHMain;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class FHRemote {
    public static class FHLocal extends FHRemote {
        protected void fetch() {
             doFetch();
        }
		private void fromTLV() {
            File vers = new File(FMLPaths.CONFIGDIR.get().toFile(), ".twrlastversion");//from twrlastvers
            if (vers.exists()) {
                try {
                    this.stableVersion = FileUtil.readString(vers);
                } catch (Throwable e) {
                }
            }
        }

        private void fromModVersion() {
            try {
                this.stableVersion = ModList.get().getModContainerById(FHMain.MODID).get().getModInfo().getVersion().toString();
            } catch (Throwable e) {
            }
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
                }
            }
        }

        @Override
        public void doFetch() {
            fromTLV();
            if (this.stableVersion != null) return;
//            fromCFM();
//            if (this.stableVersion != null) return;
            fromModVersion();
            if (this.stableVersion == null) this.stableVersion = "";
        }


    }

    public static class FHPreRemote extends FHRemote {

        @Override
        public void doFetch() {
            try {
                this.stableVersion = fetchString("http://server.teammoeg.com:15010/data/twrprever");
            } catch (Throwable e) {
                this.stableVersion = "";
            }
        }
    }

    public String stableVersion = null;

    public FHRemote() {
        fetch();
    }

    protected void fetch() {
        new Thread(() -> {
            doFetch();
        }).start();
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

    public LazyOptional<FHVersion> fetchVersion() {
        return LazyOptional.of(() -> this.stableVersion == null ? null : FHVersion.parse(this.stableVersion));
    }
}
