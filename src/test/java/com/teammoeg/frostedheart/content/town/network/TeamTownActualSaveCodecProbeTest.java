package com.teammoeg.frostedheart.content.town.network;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamTownActualSaveCodecProbeTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommentedConfig serverConfig = CommentedConfig.inMemory();
        FHConfig.SERVER_CONFIG.correct(serverConfig);
        FHConfig.SERVER_CONFIG.setConfig(serverConfig);
    }

    @Test
    void persistedTownSurvivesTheFullSyncCodec() throws Exception {
        TeamTownData fixture = new TeamTownData(
                "Portable Save Fixture",
                new TeamTownResourceHolder(Map.of()),
                Map.of(), Map.of(), Map.of(),
                0, 0, List.of(), TownStaffingPlan.EMPTY, -1L);
        CompoundTag data = new CompoundTag();
        data.put("town", FHSpecialDataTypes.TOWN_DATA.saveData(NbtOps.INSTANCE, fixture));
        CompoundTag root = new CompoundTag();
        root.put("data", data);

        TeamTownData serverData = FHSpecialDataTypes.TOWN_DATA.loadData(
                NbtOps.INSTANCE, root.getCompound("data").get("town"));
        TeamTown serverTown = serverData.createTeamTown();

        FriendlyByteBuf bytes = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new TeamTownDataS2CPacket(serverData).encode(bytes);
            TeamTownData clientData = new TeamTownDataS2CPacket(bytes).decodeTownData();
            TeamTown clientTown = clientData.createTeamTown();

            System.out.printf("server name=%s buildings=%d residents=%d%n",
                    serverTown.getName(), serverTown.getTownBuildings().size(),
                    serverTown.getAllResidents().size());
            System.out.printf("client name=%s buildings=%d residents=%d%n",
                    clientTown.getName(), clientTown.getTownBuildings().size(),
                    clientTown.getAllResidents().size());

            assertEquals(serverTown.getName(), clientTown.getName());
            assertEquals(serverTown.getTownBuildings().size(), clientTown.getTownBuildings().size());
            assertEquals(serverTown.getAllResidents().size(), clientTown.getAllResidents().size());
        } finally {
            bytes.release();
        }
    }
}
