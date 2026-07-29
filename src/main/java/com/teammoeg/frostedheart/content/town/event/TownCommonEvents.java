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

package com.teammoeg.frostedheart.content.town.event;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.network.TeamTownDataS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TownCommonEvents {
    @SubscribeEvent
    public static void attachToChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!event.getObject().isEmpty()) {
            Level world = event.getObject().getLevel();
            if (!world.isClientSide) {
                event.addCapability(new ResourceLocation(FHMain.MODID, "chunk_town_resource"), FHCapabilities.CHUNK_TOWN_RESOURCE.provider());
            }
        }
    }

    /**
     * 玩家登录时全量同步城镇数据。增量同步只发变化(delta)，新登录客户端的
     * TeamTownData 为空，必须发一次全量作为兜底。
     */
    @SubscribeEvent
    public static void syncTownDataOnLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FHNetwork.INSTANCE.sendPlayer(player, new TeamTownDataS2CPacket(player));
        }
    }

    /**
     * 玩家切换维度时全量同步城镇数据（维度切换后客户端需要重新获得权威快照）。
     */
    @SubscribeEvent
    public static void syncTownDataOnDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FHNetwork.INSTANCE.sendPlayer(player, new TeamTownDataS2CPacket(player));
        }
    }

    /**
     * 玩家打开城镇相关 GUI（基于方块实体的城镇建筑菜单）时全量同步城镇数据，
     * 确保界面打开瞬间即有最新权威数据。
     */
    @SubscribeEvent
    public static void syncTownDataOnGuiOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getContainer() instanceof CBlockEntityMenu<?> menu
                && menu.getBlock() instanceof AbstractTownBuildingBlockEntity) {
            FHNetwork.INSTANCE.sendPlayer(player, new TeamTownDataS2CPacket(player));
        }
    }
}
