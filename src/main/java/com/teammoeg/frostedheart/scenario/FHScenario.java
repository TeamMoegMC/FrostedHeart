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

package com.teammoeg.frostedheart.scenario;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.scenario.ScenarioExecutor.ScenarioMethod;
import com.teammoeg.frostedheart.scenario.commands.ActCommand;
import com.teammoeg.frostedheart.scenario.commands.ControlCommands;
import com.teammoeg.frostedheart.scenario.commands.FTBQCommands;
import com.teammoeg.frostedheart.scenario.commands.TextualCommands;
import com.teammoeg.frostedheart.scenario.commands.VariableCommand;
import com.teammoeg.frostedheart.scenario.commands.client.IClientControlCommand;
import com.teammoeg.frostedheart.scenario.network.ServerScenarioCommandPacket;
import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.parser.ScenarioParser;
import com.teammoeg.frostedheart.scenario.parser.providers.FTBQProvider;
import com.teammoeg.frostedheart.scenario.parser.providers.ScenarioProvider;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.PacketDistributor;

public class FHScenario {
	public static ScenarioExecutor<ScenarioConductor> server = new ScenarioExecutor<>(ScenarioConductor.class);
	public static Map<ServerPlayerEntity, ScenarioConductor> runners = new HashMap<>();
	private static final List<ScenarioProvider> scenarioProviders = new ArrayList<>();

	public static void startFor(ServerPlayerEntity pe) {
		ScenarioConductor sr = runners.computeIfAbsent(pe, FHScenario::load);

		sr.run(loadScenario("init"));
	}

	public static final ScenarioParser parser = new ScenarioParser();
	static File scenarioPath = new File(FMLPaths.CONFIGDIR.get().toFile(), "fhscenario");

	public static void registerScenarioProvider(ScenarioProvider p) {
		scenarioProviders.add(p);
	}

	public static Scenario loadScenario(String name) {
		System.out.println("trying to load scenario "+name);
		String[] paths=name.split("\\?");
		String[] args=new String[0];
		if(paths.length>1&&!paths[1].isEmpty())
			args=paths[1].split("&");
		Map<String,String> params=new HashMap<>();
		for(String arg:args) {
			if(arg.isEmpty())continue;
			String[] kv=arg.split("=");
			if(kv.length>1) {
				params.put(kv[0], kv[1]);
			}else if(kv.length>0) {
				params.put(kv[0], "");
			}
		}
		try {
			for (ScenarioProvider i : scenarioProviders) {
				try {
					Scenario s = i.apply(name,params);
					if (s != null)
						return s;
				}catch(Exception e) {
					new ScenarioExecutionException("Unexpected exception getting from provider '"+i.getName()+"' ,Exceptions should be caught within provider and return null. ",e).printStackTrace();
				}catch(Throwable t) {
					new ScenarioExecutionException();
				}
				
			}
			return parser.parseFile(name, new File(scenarioPath, paths[0] + ".ks"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Scenario(name);
	}

	public static void callCommand(String name, ScenarioConductor runner, Map<String, String> params) {
		server.callCommand(name, runner, params);
	}

	public static void register(Class<?> clazz) {
		server.register(clazz);
	}

	public static void callClientCommand(String name, ScenarioConductor runner, Map<String, String> params) {
		FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) runner.getPlayer()),
				new ServerScenarioCommandPacket(name.toLowerCase(), params));
	}

	public static void callClientCommand(String name, ScenarioConductor runner, String... params) {
		Map<String, String> data = new HashMap<>();
		for (int i = 0; i < params.length / 2; i++) {
			data.put(params[i * 2], params[i * 2 + 1]);
		}

		FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) runner.getPlayer()),
				new ServerScenarioCommandPacket(name.toLowerCase(), data));
	}

	public static void registerClientDelegate(Class<?> cls) {
		for (Method met : cls.getMethods()) {
			if (Modifier.isPublic(met.getModifiers())) {
				final String name = met.getName();
				registerCommand(name, (r, p) -> {
					callClientCommand(name, r, p);
				});
			}
		}
	}

	public static void registerCommand(String cmdName, ScenarioMethod<ScenarioConductor> method) {
		server.registerCommand(cmdName, method);
	}

	static {
		register(TextualCommands.class);
		register(ControlCommands.class);
		register(FTBQCommands.class);
		register(ActCommand.class);
		register(VariableCommand.class);
		registerClientDelegate(IClientControlCommand.class);
		registerScenarioProvider(new FTBQProvider());
	}
	static Path local;
	static final FolderName dataFolder = new FolderName("fhscenario");

	public static ScenarioConductor load(ServerPlayerEntity player) {
		local = FHResearchDataManager.server.func_240776_a_(dataFolder);
		local.toFile().mkdirs();
		File pfile = new File(local.toFile(), player.getUniqueID() + ".nbt");
		if (pfile.exists())
			try {
				CompoundNBT nbt = CompressedStreamTools.readCompressed(pfile);
				ScenarioConductor trd = new ScenarioConductor(player, nbt);
				return trd;
			} catch (IllegalArgumentException ex) {
				ex.printStackTrace();
				FHMain.LOGGER.error("Unexpected data file " + pfile.getName() + ", ignoring...");
			} catch (IOException e) {
				e.printStackTrace();
				FHMain.LOGGER.error("Unable to read data file " + pfile.getName() + ", ignoring...");
			}
		return new ScenarioConductor(player);

	}

	public static void save() {
		for (Entry<ServerPlayerEntity, ScenarioConductor> entry : runners.entrySet()) {
			String fn = entry.getKey().getUniqueID() + ".nbt";
			File f = local.resolve(fn).toFile();

			try {
				CompressedStreamTools.writeCompressed(entry.getValue().save(), f);
			} catch (IOException e) {

				e.printStackTrace();
				FHMain.LOGGER
						.error("Unable to save data file for player " + entry.getKey().toString() + ", ignoring...");
			}
		}

		runners.values().removeIf(t -> t.isOfflined());

	}
}
