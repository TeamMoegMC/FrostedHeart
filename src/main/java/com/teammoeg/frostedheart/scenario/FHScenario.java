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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.scenario.ScenarioExecutor.ScenarioMethod;
import com.teammoeg.frostedheart.scenario.commands.ControlCommands;
import com.teammoeg.frostedheart.scenario.commands.FTBQCommands;
import com.teammoeg.frostedheart.scenario.commands.SceneCommand;
import com.teammoeg.frostedheart.scenario.commands.TextualCommands;
import com.teammoeg.frostedheart.scenario.network.ServerScenarioCommandPacket;
import com.teammoeg.frostedheart.scenario.parser.ScenarioParser;
import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.PacketDistributor;

public class FHScenario {
    public static ScenarioExecutor server = new ScenarioExecutor();
    public static Map<PlayerEntity,ScenarioConductor> runners=new HashMap<>();
    private static final List<Function<String,Scenario>> scenarioProviders=new ArrayList<>();
    public static void startFor(PlayerEntity pe) {
    	ScenarioConductor sr=runners.computeIfAbsent(pe, ScenarioConductor::new);
    	
    	sr.run(loadScenario("init"));
    }

    public static final ScenarioParser parser = new ScenarioParser();
    static File local = new File(FMLPaths.CONFIGDIR.get().toFile(), "fhscenario");
    public static void registerScenarioProvider(Function<String,Scenario> p) {
    	scenarioProviders.add(p);
    }
    public static Scenario loadScenario(String name) {
    	try {
    		for(Function<String, Scenario> i:scenarioProviders) {
    			Scenario s=i.apply(name);
    			if(s!=null)
    				return s;
    		}
			return parser.parseFile(name,new File(local,name+".ks"));
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
    			  new ServerScenarioCommandPacket(name, params,runner.getExecutionData()));
    }
    public static void callClientCommand(String name, ScenarioConductor runner,String... params) {
    	Map<String, String> data=new HashMap<>();
    	for(int i=0;i<params.length/2;i++) {
    		data.put(params[i*2], params[i*2+1]);
    	}
    	
  	  FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) runner.getPlayer()),
  			  new ServerScenarioCommandPacket(name, data,runner.getExecutionData()));
  }
    public static void registerClientDelegate(Class<?> cls) {
        for (Method met : cls.getMethods()) {
            if (Modifier.isPublic(met.getModifiers())) {
                final String name = met.getName();
                registerCommand(name, (r, p) -> {
                	callClientCommand(name,r,p);
                });
            }
        }

    }

    public static void registerCommand(String cmdName, ScenarioMethod method) {
        server.registerCommand(cmdName, method);
    }

    static {
        register(TextualCommands.class);
        register(ControlCommands.class);
        register(FTBQCommands.class);
        register(SceneCommand.class);
    }
}
