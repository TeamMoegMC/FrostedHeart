package com.teammoeg.frostedheart.content.town.provider;

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
        TeamDataHolder datatype= CTeamDataManager.getDataByResearchID(ownerUUID);
        if(datatype==null){
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
}
