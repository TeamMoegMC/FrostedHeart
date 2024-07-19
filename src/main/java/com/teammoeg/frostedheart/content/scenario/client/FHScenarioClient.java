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

package com.teammoeg.frostedheart.content.scenario.client;

import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.ScenarioExecutor;
import com.teammoeg.frostedheart.content.scenario.ScenarioExecutor.ScenarioMethod;
import com.teammoeg.frostedheart.content.scenario.commands.client.ClientControl;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.util.ResourceLocation;

public class FHScenarioClient {
    static ScenarioExecutor<IClientScene> client = new ScenarioExecutor<>(IClientScene.class);
    public static void callCommand(String name, IClientScene runner, Map<String, String> params) {
        client.callCommand(name, runner, params);
    }
    public static ResourceLocation getPathOf(ResourceLocation orig,String path) {
    	ResourceLocation rl= new ResourceLocation(orig.getNamespace(),path+ClientUtils.mc().getLanguageManager().getCurrentLanguage().getCode()+"/"+orig.getPath());
    	if(ClientUtils.mc().getResourceManager().hasResource(rl)) {
    		return rl;
    	}
    	return new ResourceLocation(orig.getNamespace(),path+orig.getPath());
    }
    public static void register(Class<?> clazz) {
        client.register(clazz);
    }

    public static void registerCommand(String cmdName, ScenarioMethod<IClientScene> method) {
        client.registerCommand(cmdName, method);
    }
    static {
    	register(ClientControl.class);
    }
}
