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

package com.teammoeg.frostedheart.content.scenario;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Type;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.compat.CompatModule;
import com.teammoeg.frostedheart.content.scenario.ScenarioExecutor.ScenarioMethod;
import com.teammoeg.frostedheart.content.scenario.commands.ActCommand;
import com.teammoeg.frostedheart.content.scenario.commands.ControlCommands;
import com.teammoeg.frostedheart.content.scenario.commands.CookieCommand;
import com.teammoeg.frostedheart.content.scenario.commands.FTBQCommands;
import com.teammoeg.frostedheart.content.scenario.commands.MCCommands;
import com.teammoeg.frostedheart.content.scenario.commands.TextualCommands;
import com.teammoeg.frostedheart.content.scenario.commands.VariableCommand;
import com.teammoeg.frostedheart.content.scenario.commands.client.IClientControlCommand;
import com.teammoeg.frostedheart.content.scenario.network.S2CScenarioCommandPacket;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.parser.ScenarioParser;
import com.teammoeg.frostedheart.content.scenario.parser.providers.FTBQProvider;
import com.teammoeg.frostedheart.content.scenario.parser.providers.ScenarioProvider;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioContext;
import com.teammoeg.frostedheart.content.scenario.runner.trigger.IVarTrigger;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.language.ModFileScanData.AnnotationData;
import net.minecraftforge.network.PacketDistributor;

public class FHScenario {
	static Marker MARKER = MarkerManager.getMarker("Scenario Conductor");
	public static ScenarioExecutor<ScenarioCommandContext> server = new ScenarioExecutor<>(ScenarioCommandContext.class);
	private static final List<ScenarioProvider> scenarioProviders = new ArrayList<>();
	public static Map<Player,Map<EventTriggerType,List<IVarTrigger>>> triggers=new HashMap<>();
	//private static Map<ServerPlayerEntity,ScenarioConductor> runners=new HashMap<>();
	public static void startFor(ServerPlayer pe,String lang) {
		ScenarioConductor sr = get(pe);
		FHMain.LOGGER.debug(MARKER, "From saved: "+pe.getLanguage()+" From packet: "+lang);
		sr.init(pe,lang);
		

		sr.run("init");
	}
	public static void addVarTrigger(Player pe,EventTriggerType type,IVarTrigger trig) {
		triggers.computeIfAbsent(pe,k->new HashMap<>()).computeIfAbsent(type, k->new ArrayList<>()).add(trig);
	}
	public static void trigVar(Player pe,EventTriggerType type) {
		Map<EventTriggerType,List<IVarTrigger>> me=triggers.get(pe);
		if(me!=null) {
			List<IVarTrigger> le=me.get(type);
			le.removeIf(t->{
				t.trigger();
				return !t.canStillTrig();
			});
		}
	}
	public static final ScenarioParser parser = new ScenarioParser();
	static File scenarioPath = new File(FMLPaths.CONFIGDIR.get().toFile(), "fhscenario");
	
	public static void registerScenarioProvider(ScenarioProvider p) {
		scenarioProviders.add(p);
	}

	public static Scenario loadScenario(ScenarioContext ctx,String name) {
		if("empty".equals(name))
			return Scenario.EMPTY;
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
					Scenario s = i.apply(paths[0],params,name);
					
					if (s != null) {
						FHMain.LOGGER.info("Loaded scenario from provider "+i.getName());
						return s;
					}
				}catch(Exception e) {
					new ScenarioExecutionException("Unexpected exception getting from provider '"+i.getName()+"' ,Exceptions should be caught within provider and return null. ",e).printStackTrace();
				}
				
			}
			File f=new File(scenarioPath, ctx.getLang()+"/"+paths[0] + ".ks");
			if(!f.exists()) {
				f=new File(scenarioPath, paths[0] + ".ks");
            }
			if(f.exists()) {
				FHMain.LOGGER.info("Loading scenario from "+f.getAbsolutePath());
				return parser.parseFile(name, f);
			}
		} catch (Exception e) {
			ctx.sendMessage("Exception loading scenario "+name+": "+e.getMessage()+" see log for more detail");
			e.printStackTrace();
			
		}
		FHMain.LOGGER.error("Scenario "+name+" not found, falling back to empty scenario");
		return new Scenario(name);
	}

	public static void callCommand(String name, ScenarioCommandContext scenarioVM, Map<String, String> params) {
		server.callCommand(name, scenarioVM, params);
	}

	public static void register(Class<?> clazz) {
		server.register(clazz);
	}

	public static void callClientCommand(String name, ScenarioCommandContext runner, Map<String, String> params) {
		FHNetwork.INSTANCE.sendPlayer((ServerPlayer) runner.context().player(),
				new S2CScenarioCommandPacket(runner.thread().getRunId(),name.toLowerCase(), params));
	}

	public static void callClientCommand(String name, ScenarioCommandContext runner, String... params) {
		Map<String, String> data = new HashMap<>();
		for (int i = 0; i < params.length / 2; i++) {
			data.put(params[i * 2], params[i * 2 + 1]);
		}

		FHNetwork.INSTANCE.sendPlayer((ServerPlayer) runner.context().player(),
				new S2CScenarioCommandPacket(runner.thread().getRunId(),name.toLowerCase(), data));
	}

	public static void registerClientDelegate(Class<?> cls) {
		for (Method met : cls.getMethods()) {
			if (Modifier.isPublic(met.getModifiers())) {
				final String name = met.getName();
				registerCommand(name, (r, p) -> callClientCommand(name, r, p));
			}
		}
	}

	public static void registerCommand(String cmdName, ScenarioMethod<ScenarioCommandContext> method) {
		server.registerCommand(cmdName, method);
	}

	static {
		/*Type anno=Type.getType(ScenarioCommandProvider.class);
		for(ModFileScanData data:ModList.get().getAllScanData()) {
			AnnotationData dat=null;
			for(AnnotationData an:data.getAnnotations()) {
				if(Objects.equals(an.annotationType(), anno)) {
					dat=an;
					break;
				}
			}
		}*/
		register(TextualCommands.class);
		register(ControlCommands.class);
		if(CompatModule.isFTBQLoaded())
			register(FTBQCommands.class);
		register(ActCommand.class);
		register(VariableCommand.class);
		registerClientDelegate(IClientControlCommand.class);
		registerScenarioProvider(new FTBQProvider());
		register(MCCommands.class);
		register(CookieCommand.class);
	}
	static Path local;
	static final LevelResource dataFolder = new LevelResource("fhscenario");
/*
	public static ScenarioConductor load(ServerPlayerEntity player) {
		local = FHResearchDataManager.server.getWorldPath(dataFolder);
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

	}*/
	public static ScenarioConductor getNullable(Player playerEntity) {
		//return runners.computeIfAbsent((ServerPlayerEntity) playerEntity, FHScenario::load);
		return ScenarioConductor.getCapability(playerEntity).orElse(null);
	}
	public static ScenarioConductor get(Player playerEntity) {
		//return runners.computeIfAbsent((ServerPlayerEntity) playerEntity, FHScenario::load);
		return ScenarioConductor.getCapability(playerEntity).orElseThrow(()->new NoSuchElementException("conductor not present"));
	}
}
