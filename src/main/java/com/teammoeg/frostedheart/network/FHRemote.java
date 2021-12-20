package com.teammoeg.frostedheart.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.FHVersion;
import com.teammoeg.frostedheart.util.LazyOptional;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class FHRemote {
	public static class FHLocal extends FHRemote{

		@Override
		public void fetch() {
			File vers=new File(FMLPaths.CONFIGDIR.get().toFile(),".twrlastversion");
			try {
				this.stableVersion=readString(vers);
			} catch (Throwable e) {
				try {
					this.stableVersion=ModList.get().getModContainerById(FHMain.MODID).get().getModInfo().getVersion().toString();
				} catch (Throwable e2) {
					e2.printStackTrace();
					this.stableVersion="";
				}
			}
		}
		public static byte[] readAll(InputStream i) throws IOException {
			ByteArrayOutputStream ba = new ByteArrayOutputStream(16384);
			int nRead;
			byte[] data = new byte[4096];
			try {
				while ((nRead = i.read(data, 0, data.length)) != -1) { ba.write(data, 0, nRead); }
			} catch (IOException e) {
				throw e;
			}
			return ba.toByteArray();
		}
		public static byte[] readAll(File f) throws IOException {
			try(FileInputStream fis=new FileInputStream(f)){
				return readAll(fis);
			}
		}
		public static String readString(File f) throws IOException {
			return new String(readAll(f));
		}
		
	}
	public static class FHPreRemote extends FHRemote{

		@Override
		public void fetch() {
			try {
				this.stableVersion=fetchString("https://khjxiaogu.com/datalink?name=twrprever");
			} catch (Throwable e) {
				this.stableVersion="";
			}
		}
	}
    public String stableVersion = null;

    public FHRemote() {
    	fetch();
    }
    public void fetch() {
    	new Thread(()->{
    		try {
                JsonParser parser = new JsonParser();
                JsonObject json = parser.parse(fetchString("https://addons-ecs.forgesvc.net/api/v2/addon/535790")).getAsJsonObject();
                String fileName = json.get("latestFiles").getAsJsonArray().get(0).getAsJsonObject().get("fileName").getAsString();
                stableVersion = fileName.substring(18, fileName.indexOf(".zip"));
            } catch (Throwable e) {
            	stableVersion = "";
                e.printStackTrace();
            }
    	}).start();
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
