package com.teammoeg.frostedheart.content.town.network;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

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
    void actualSaveSurvivesTheFullSyncCodec() throws Exception {
        String path = "/Users/wyc/Development/FrostedHeart/run/saves/20030716/chorda_data/"
                + "6dfdd05b-5a40-4b73-8467-cdd9b68ff7a2.nbt";
        CompoundTag root = NbtIo.readCompressed(new File(path));
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
