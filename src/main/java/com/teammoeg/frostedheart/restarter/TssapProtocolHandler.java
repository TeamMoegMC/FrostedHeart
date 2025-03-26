package com.teammoeg.frostedheart.restarter;

import static com.teammoeg.frostedheart.FHMain.LOGGER;
import static com.teammoeg.frostedheart.FHMain.VERSION_CHECK;

import java.io.File;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.foundation.utility.Components;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.io.FileUtil;
import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

public class TssapProtocolHandler {
	static String selectedChannelAddr;
	public static String channel=null;
	public static String localVersion=null;
	public static String latestRemote;
	private static File tssapConfig = new File(FMLPaths.GAMEDIR.get().toFile(), "tssap-configs");//from tssap data config
	private static File data = new File(tssapConfig, "data.json");
	private static File config = new File(tssapConfig, "config.json");
	public static void init() {
		fromTLV();
		loadConfig();
		
	}
	static Thread clientThread;
	public static void clientPrepareUpdateReminder() {
		if(localVersion!=null&&selectedChannelAddr!=null&&FHConfig.COMMON.enableUpdateReminder.get()&&clientThread==null)
			clientThread=new Thread() {
				@Override
				public void run() {
					while(true) {
						String newver=fetchLatestRemoteVersion();
						
						if(newver!=null&&!newver.equals(latestRemote)) {
							latestRemote=newver;
							FHMain.remote.stableVersion=newver;
							FHMain.remote.versionCache=null;
							if(ClientUtils.getPlayer()!=null) {
								if(latestRemote!=null&&!latestRemote.equals(localVersion)) {
									ClientUtils.getPlayer().displayClientMessage(Lang.translateGui("update_recommended").append(newver), false);
								}
							}
						}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
							return;
						}
					}
	
					
				}
			};
			clientThread.start();
	}
	static Thread serverThread;
	public static void serverPrepareUpdateReminder() {
		if(localVersion!=null&&selectedChannelAddr!=null&&FHConfig.COMMON.enableAutoRestart.get()&&serverThread==null)
			serverThread=new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}
					String newver=fetchLatestRemoteVersion();
					if(newver!=null&&!newver.equals(latestRemote)) {
						latestRemote=newver;
						if(latestRemote!=null&&!latestRemote.equals(localVersion)) {
							CDistHelper.getServer().getCommands().performPrefixedCommand(CDistHelper.getServer().createCommandSourceStack(), "/tellraw @a "+Component.Serializer.toJson(Components.translatable("message.frostedheart.restarting")));
							try {
								Thread.sleep(60000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							CDistHelper.getServer().getCommands().performPrefixedCommand(CDistHelper.getServer().createCommandSourceStack(),"/stop");
						}
					}
					
					
	
					
				}
			};
			serverThread.start();
	}
	private static String fetchLatestRemoteVersion() {
		
		try(InputStream is=FileUtil.fetch(selectedChannelAddr);InflaterInputStream dis=new InflaterInputStream(is)) {
			return JsonParser.parseString(FileUtil.readString(dis)).getAsJsonObject().get("latestVersion").getAsJsonObject().get("versionName").getAsString();
		} catch (Throwable e) {
			//e.printStackTrace();
			FHMain.LOGGER.debug(VERSION_CHECK, "Error fetching FH local version from remote");
		}
		return null;
	}
    private static void fromTLV() {
        
        if (tssapConfig.exists()) {
        	
        	if(data.exists())
	            try {
	            	JsonElement je=JsonParser.parseString(FileUtil.readString(data));
	                localVersion = je.getAsJsonObject().get("cachedModpack").getAsJsonObject().get("version").getAsString();
	                FHMain.local.stableVersion=localVersion;
	                FHMain.local.versionCache=null;
	                channel=je.getAsJsonObject().get("cachedChannel").getAsString();
	            } catch (Throwable e) {
	                LOGGER.error(VERSION_CHECK, "Error fetching FH local version from tssap data", e);
	                throw new RuntimeException("[TWR Version Check] Error fetching FH local version from .twrlastversion", e);
	            }
        }
    }
    private static void loadConfig() {
    	if(config.exists()) {
    		 try {
    			 JsonObject je=JsonParser.parseString(FileUtil.readString(config)).getAsJsonObject();
    			 if(channel==null&&je.has("selectedChannel"))
    				 channel=je.get("selectedChannel").getAsString();
    			 JsonArray channels=je.get("channels").getAsJsonArray();
    			 if(channel!=null&&!channel.isEmpty()) {
    				 for(JsonElement jelm:channels) {
    					 if(channel.equals(jelm.getAsJsonObject().get("id").getAsString())) {
    						 selectedChannelAddr=jelm.getAsJsonObject().get("url").getAsString();
    						 break;
    					 }
    				 }
    			 }
    			 if(selectedChannelAddr==null) {
    				 selectedChannelAddr=channels.get(0).getAsJsonObject().get("url").getAsString();
    			 }
    		 } catch (Throwable e) {
                 LOGGER.error(VERSION_CHECK, "Error fetching tssap config", e);
                 throw new RuntimeException("[TWR Version Check] Error fetching FH local version from .twrlastversion", e);
             }
    	}
    }
}
