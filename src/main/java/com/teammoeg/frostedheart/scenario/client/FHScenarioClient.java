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

package com.teammoeg.frostedheart.scenario.client;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.ScenarioExecutor;
import com.teammoeg.frostedheart.scenario.ScenarioExecutor.ScenarioMethod;
import com.teammoeg.frostedheart.scenario.commands.ClientControl;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class FHScenarioClient {
    static ScenarioExecutor client = new ScenarioExecutor();
    public static boolean sendInitializePacket=false;
    public static void callCommand(String name, ScenarioConductor runner, Map<String, String> params) {
        client.callCommand(name, runner, params);
    }

    public static void register(Class<?> clazz) {
        client.register(clazz);
    }

    public static void registerCommand(String cmdName, ScenarioMethod method) {
        client.registerCommand(cmdName, method);
    }
    static {
    	register(ClientControl.class);
    }
}
