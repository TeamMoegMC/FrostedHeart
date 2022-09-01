package com.teammoeg.frostedheart.research.api;

import com.teammoeg.frostedheart.research.data.TeamResearchData;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientResearchDataAPI {

    private ClientResearchDataAPI() {
    }

    @OnlyIn(Dist.CLIENT)
    public static TeamResearchData getData() {
        return TeamResearchData.getClientInstance();

    }

    @OnlyIn(Dist.CLIENT)
    public static CompoundNBT getVariants() {
        return TeamResearchData.getClientInstance().getVariants();

    }
}
