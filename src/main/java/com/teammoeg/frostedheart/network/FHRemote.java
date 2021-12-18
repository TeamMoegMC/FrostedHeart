package com.teammoeg.frostedheart.network;

import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class FHRemote {

    public String stableVersion;

    public FHRemote() {
        stableVersion = fetchString("https://info.teammoeg.com/twr/stable_version.txt");
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
