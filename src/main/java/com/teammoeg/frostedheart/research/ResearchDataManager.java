package com.teammoeg.frostedheart.research;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;

public class ResearchDataManager {
	MinecraftServer server;
	Path local;
	static final FolderName fn=new FolderName("tmresearch");
	public static ResearchDataManager INSTANCE;
	private Map<UUID,TeamResearchData> data=new HashMap<>();
	public ResearchDataManager(MinecraftServer s) {
		server=s;
		INSTANCE=this;
	}
	public TeamResearchData getData(UUID id) {
		TeamResearchData cn=data.get(id);
		if(cn==null) {
			cn=new TeamResearchData();
			data.put(id,cn);
		}
		return cn;

	}
	public void load() {
		local=server.func_240776_a_(fn);
		for(File f:local.toFile().listFiles((f)->f.getName().endsWith(".nbt"))) {
			try {
				UUID tud=UUID.fromString(f.getName().split("\\.")[0]);
				CompoundNBT nbt=CompressedStreamTools.readCompressed(f);
				TeamResearchData trd=new TeamResearchData();
				trd.deserialize(nbt);
				data.put(tud,trd);
			}catch(IllegalArgumentException ex) {
				System.out.println("Unexpected data file "+f.getName()+", ignoring...");
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Unable to read data file "+f.getName()+", ignoring...");
			}
		}
	}
	public void save() {
		for(Entry<UUID, TeamResearchData> entry:data.entrySet()) {
			File f=local.resolve(entry.getKey().toString()+".nbt").toFile();
			try {
				CompressedStreamTools.writeCompressed(entry.getValue().serialize(),f);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Unable to save data file for team "+entry.getKey().toString()+", ignoring...");
			}
		}
	}
}
