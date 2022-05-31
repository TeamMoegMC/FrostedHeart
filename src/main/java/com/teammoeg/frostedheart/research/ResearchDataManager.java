package com.teammoeg.frostedheart.research;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import dev.ftb.mods.ftbteams.data.TeamManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;

public class ResearchDataManager {
	public static MinecraftServer server;
	Path local;
	File regfile;
	static final FolderName dataFolder=new FolderName("fhresearch");
	public static ResearchDataManager INSTANCE;
	private Map<UUID,TeamResearchData> data=new HashMap<>();
	public ResearchDataManager(MinecraftServer s) {
		server=s;
		INSTANCE=this;
	}
	public TeamResearchData getData(UUID id) {
		TeamResearchData cn=data.computeIfAbsent(id,c->new TeamResearchData(()->TeamManager.INSTANCE.getTeamByID(id)));
		return cn;

	}
	public void load() {
		local=server.func_240776_a_(dataFolder);
		regfile=new File(local.toFile().getParentFile(),"fhregistries.dat");
		if(regfile.exists()) {
			try {
				FHResearch.load(CompressedStreamTools.readCompressed(regfile));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("CANNOT READ RESEARCH REGISTRIES, MAY CAUSE UNSYNC!");
				
			}
		}
		local.toFile().mkdirs();
		for(File f:local.toFile().listFiles((f)->f.getName().endsWith(".nbt"))) {
			try {
				UUID tud=UUID.fromString(f.getName().split("\\.")[0]);
				CompoundNBT nbt=CompressedStreamTools.readCompressed(f);
				TeamResearchData trd=new TeamResearchData(()->TeamManager.INSTANCE.getTeamByID(tud));
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
		try {
			CompressedStreamTools.writeCompressed(FHResearch.save(new CompoundNBT()),regfile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.out.println("CANNOT SAVE RESEARCH REGISTRIES, MAY CAUSE UNSYNC!");
		}
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
