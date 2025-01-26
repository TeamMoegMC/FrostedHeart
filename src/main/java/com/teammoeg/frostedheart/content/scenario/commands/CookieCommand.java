package com.teammoeg.frostedheart.content.scenario.commands;

import java.util.ArrayList;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.network.S2CRequestCookieMessage;
import com.teammoeg.frostedheart.content.scenario.network.S2CSetCookiesMessage;
import com.teammoeg.frostedheart.content.scenario.runner.RunStatus;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class CookieCommand {
	
	public void setCookie(ScenarioCommandContext runner,@Param("k")String key,@Param("exp")String exp,@Param("var")String var,@Param("str")String str) {
		CompoundTag tag=(CompoundTag) runner.context().getCommandData().computeIfAbsent("set_cookie", t->new CompoundTag());
		if (var != null) {
			tag.put(key, runner.context().getVarData().evalPath(var));
        } else if (str != null) {
        	tag.putString(key, str);
        } else {
            tag.putDouble(key, runner.eval(exp));
        }
			
	}
	public void sendCookie(ScenarioCommandContext runner) {
		Object obj= runner.context().getCommandData().remove("set_cookie");
		if(obj instanceof CompoundTag tag) {
			FHNetwork.sendPlayer((ServerPlayer) runner.context().player(), new S2CSetCookiesMessage(tag));
			
		}
	}
	
	public void getCookie(ScenarioCommandContext runner,@Param("k")String key) {
		ArrayList<String> tag=(ArrayList<String>) runner.context().getCommandData().computeIfAbsent("get_cookie", t->new ArrayList<String>());
		tag.add(key);
			
	}
	public void requestCookie(ScenarioCommandContext runner) {
		Object obj= runner.context().getCommandData().remove("get_cookie");
		if(obj instanceof ArrayList tag) {
			FHNetwork.sendPlayer((ServerPlayer) runner.context().player(), new S2CRequestCookieMessage(runner.thread().getRunId(),tag));
			runner.thread().setStatus(RunStatus.WAITNETWORK);
		}
	}
	
}
