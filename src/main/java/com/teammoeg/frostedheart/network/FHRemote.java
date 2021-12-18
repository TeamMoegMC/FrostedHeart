package com.teammoeg.frostedheart.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class FHRemote {

    public String stableVersion = "";

    public FHRemote() {
        try {
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(fetchString("https://addons-ecs.forgesvc.net/api/v2/addon/535790")).getAsJsonObject();
            String fileName = json.get("latestFiles").getAsJsonArray().get(0).getAsJsonObject().get("fileName").getAsString();
            stableVersion = fileName.substring(18, fileName.indexOf(".zip"));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetch a simple string from remote URL
     * @param address URL address of the string location
     * @return the trimmed String instance on the address
     */
    public static String fetchString(String address) {
        try {
            URL url = new URL(address);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            Scanner tScanner = new Scanner(urlConnection.getInputStream());
            if (tScanner.hasNextLine()) {
                return tScanner.nextLine().trim();
            }
            tScanner.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

}
