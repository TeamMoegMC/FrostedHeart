package com.teammoeg.frostedheart.network;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.FHVersion;
import com.teammoeg.frostedheart.util.FileUtil;
import com.teammoeg.frostedheart.util.LazyOptional;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

public class FHRemote {
	public static class FHLocal extends FHRemote{

		@Override
		public void doFetch() {
			File vers=new File(FMLPaths.CONFIGDIR.get().toFile(),".twrlastversion");
			if(vers.exists()) {
				try {
					this.stableVersion=FileUtil.readString(vers);
				} catch (Throwable e) {
					try {
						this.stableVersion=ModList.get().getModContainerById(FHMain.MODID).get().getModInfo().getVersion().toString();
					} catch (Throwable e2) {
						e2.printStackTrace();
						this.stableVersion="";
					}
				}
			}else
				this.stableVersion="";
		}
			
		
	}
	public static class FHPreRemote extends FHRemote{

		@Override
		public void doFetch() {
			try {
				this.stableVersion=fetchString("http://server.teammoeg.com:15010/data/twrprever");
			} catch (Throwable e) {
				this.stableVersion="";
			}
		}
	}
	
    public String stableVersion = null;

    public FHRemote() {
    	fetch();
    }
    private final void fetch() {
    	new Thread(()->{
    		doFetch();
    	}).start();
    }
    protected void doFetch() {
		try {
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(fetchString("https://addons-ecs.forgesvc.net/api/v2/addon/535790")).getAsJsonObject();
            String fileName = json.get("latestFiles").getAsJsonArray().get(0).getAsJsonObject().get("fileName").getAsString();
            stableVersion = fileName.substring(18, fileName.indexOf(".zip"));
        } catch (Throwable e) {
        	stableVersion = "";
            e.printStackTrace();
        }
		if(stableVersion==null||stableVersion.isEmpty()) {
			stableVersion =fetchString("http://server.teammoeg.com:15010/data/twrver");
		}
    }
    /**
     * Fetch a simple string from remote URL
     * @param address URL address of the string location
     * @return the trimmed String instance on the address
     */
    public static String fetchString(String address) {
        try{
        	URL url = new URL(address);
        	URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            try(Scanner tScanner = new Scanner(urlConnection.getInputStream())){
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
    	return LazyOptional.of(()->this.stableVersion==null?null:FHVersion.parse(this.stableVersion));
    }
}
