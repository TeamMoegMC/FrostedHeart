package com.teammoeg.frostedheart.content.town.provider;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TeamTownProvider implements ITownProviderSerializable<TeamTown>{

    public UUID ownerUUID;

    public TeamTownProvider(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    //用于ITownProviderSerializable中注册
    public TeamTownProvider() {
    }

    @Override
    public @Nullable TeamTown getTown() {
        TeamDataHolder datatype;
        if (CTeamDataManager.INSTANCE == null) {
            // 专用服务器上的客户端：CTeamDataManager 只在服务端启动时实例化，
            // 纯客户端恒为 null，此时 getDataByResearchID 只会返回一份空数据。
            // 改为读取客户端同步快照（由 TeamTownDataS2CPacket 全量包 + 增量包填充）。
            datatype = CClientTeamDataManager.INSTANCE.getInstance();
        } else {
            // 服务端（或集成服务器）：按队伍 research ID 查真实数据。
            datatype = CTeamDataManager.getDataByResearchID(ownerUUID);
        }
        if (datatype == null) {
            return null;
        }
        TeamTownData townData = datatype.getData(FHSpecialDataTypes.TOWN_DATA);
        return townData.createTeamTown();
    }

    @Override
    public StringTag serializeNBT() {
        return StringTag.valueOf(ownerUUID.toString());
    }

    @Override
    public void deserializeNBT(Tag nbt) {
        StringTag tag = (StringTag) nbt;
        ownerUUID = UUID.fromString(tag.getAsString());
    }


    @Override
    public Class<TeamTown> getTownType() {
        return TeamTown.class;
    }

    @Override
    public String toString() {
        return "{Team ITown Provider: " +
                ownerUUID +
                '}';
    }
}
