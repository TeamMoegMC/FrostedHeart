package com.teammoeg.frostedheart.scenario;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.teammoeg.frostedheart.FHPacketHandler;

import com.teammoeg.frostedheart.scenario.ScenarioExecutor.ScenarioMethod;
import com.teammoeg.frostedheart.scenario.network.ClientScenarioCommentPacket;

import net.minecraftforge.fml.network.PacketDistributor;


public class FHScenario {
	public static ScenarioExecutor server=new ScenarioExecutor();

	public static void registerCommand(String cmdName, ScenarioMethod method) {
		server.registerCommand(cmdName, method);
	}

	public static void regiser(Class<?> clazz) {
		server.regiser(clazz);
	}

	public static void callCommand(String name, ScenarioRunner runner, Map<String, String> params) {
		server.callCommand(name, runner, params);
	}
	public static void registerClientDelegate(Class<?> cls) {
		for(Method met:cls.getMethods()) {
			if(Modifier.isPublic(met.getModifiers())) {
				final String name=met.getName();
				registerCommand(name,(r,p)->{FHPacketHandler.send(PacketDistributor.PLAYER.with(r::getPlayer),new ClientScenarioCommentPacket(name, p,r.getExecutionData()));});
			}
		}
		
	}
}
