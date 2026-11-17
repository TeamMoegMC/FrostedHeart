/*
 * Copyright (c) 2026 TeamMoeg
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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRConfig;
import com.teammoeg.frostedresearch.FRMain;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 观察容量的扩展点：队伍上限 = 配置基础值 + 各扩展提供的容量。其他系统（如城镇研究所）登记 {@link Provider}。
 * <p>
 * Observation capacity extension point. A team's limit is the configured base plus what every registered
 * {@link Provider} grants it; other systems, such as the town research institute, register providers.
 */
public final class KnowledgeCapacity {
    /**
     * 一个扩展给出的额外档案、来件观察条数。 / Extra archive and inbox observation records one provider grants.
     */
    public record Extension(int archive, int inbox) {
        public static final Extension NONE = new Extension(0, 0);

        public Extension {
            archive = Math.max(0, archive);
            inbox = Math.max(0, inbox);
        }
    }

    /**
     * 按队伍提供容量；在服务器线程调用，应当只读取已保存的状态。 / Grants capacity to a team. Called on the server thread;
     * implementations should only read saved state.
     */
    @FunctionalInterface
    public interface Provider {
        Extension extension(TeamDataHolder team);
    }

    /**
     * 队伍当前的上限及其中的配置基础值。 / A team's current limits and the configured base inside them.
     */
    public record Limits(int archive, int inbox, int archiveBase, int inboxBase) {
        public int archiveExtension() {
            return archive - archiveBase;
        }

        public int inboxExtension() {
            return inbox - inboxBase;
        }
    }

    private static volatile Map<ResourceLocation, Provider> providers = Map.of();
    private static boolean failureLogged;

    private KnowledgeCapacity() {
    }

    /**
     * Registers or replaces the provider with this id.
     */
    public static synchronized void register(ResourceLocation id, Provider provider) {
        Map<ResourceLocation, Provider> next = new LinkedHashMap<>(providers);
        next.put(id, provider);
        providers = Collections.unmodifiableMap(next);
    }

    public static synchronized void unregister(ResourceLocation id) {
        if (!providers.containsKey(id)) return;
        Map<ResourceLocation, Provider> next = new LinkedHashMap<>(providers);
        next.remove(id);
        providers = Collections.unmodifiableMap(next);
    }

    public static Map<ResourceLocation, Provider> providers() {
        return providers;
    }

    /**
     * The team's limits under the current configuration and providers. A provider that fails grants nothing.
     */
    public static Limits limits(TeamDataHolder team) {
        return limits(team, FRConfig.SERVER.knowledgeArchiveObservations.get(), FRConfig.SERVER.knowledgeInboxObservations.get());
    }

    public static Limits limits(TeamDataHolder team, int archiveBase, int inboxBase) {
        long archive = Math.max(0, archiveBase), inbox = Math.max(0, inboxBase);
        if (team != null) {
            for (Provider provider : providers.values()) {
                Extension extension;
                try {
                    extension = provider.extension(team);
                } catch (RuntimeException failure) {
                    if (!failureLogged) {
                        failureLogged = true;
                        FRMain.LOGGER.error("Knowledge capacity provider failed; it grants no capacity", failure);
                    }
                    continue;
                }
                if (extension == null) continue;
                archive += extension.archive();
                inbox += extension.inbox();
            }
        }
        return new Limits(saturate(archive), saturate(inbox), Math.max(0, archiveBase), Math.max(0, inboxBase));
    }

    private static int saturate(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }
}
