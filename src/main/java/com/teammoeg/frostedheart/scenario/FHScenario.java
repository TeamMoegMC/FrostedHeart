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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.scenario.ScenarioExecutor.ScenarioMethod;
import com.teammoeg.frostedheart.scenario.network.ClientScenarioCommentPacket;
import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.PacketDistributor;

public class FHScenario {
    public static ScenarioExecutor server = new ScenarioExecutor();

    public static void callCommand(String name, ScenarioRunner runner, Map<String, String> params) {
        server.callCommand(name, runner, params);
    }

    public static void regiser(Class<?> clazz) {
        server.regiser(clazz);
    }

    public static void registerClientDelegate(Class<?> cls) {
        for (Method met : cls.getMethods()) {
            if (Modifier.isPublic(met.getModifiers())) {
                final String name = met.getName();
                registerCommand(name, (r, p) -> {
                    FHPacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) r.getPlayer()), new ClientScenarioCommentPacket(name, p, r.getExecutionData()));
                });
            }
        }

    }

    public static void registerCommand(String cmdName, ScenarioMethod method) {
        server.registerCommand(cmdName, method);
    }
}
