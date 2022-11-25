package com.teammoeg.frostedheart.mixin.ftb;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.ftb.mods.ftbchunks.data.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamType;
import net.minecraft.nbt.CompoundNBT;

@Mixin(FTBChunksTeamData.class)
public class ChunkDataMixin {
	@Shadow(remap=false)
	public ClaimedChunkManager manager;
	@Shadow(remap=false)
	private Team team;
	public ChunkDataMixin() {
	}
	@Inject(at=@At("HEAD"),method="deserializeNBT",remap=false,cancellable=false)
	public void fh$deserializeNBT(CompoundNBT tag,CallbackInfo cbi) {
		if(team instanceof PlayerTeam) {
			PlayerTeam pt=(PlayerTeam) team;
			if(pt.actualTeam!=pt)
				cbi.cancel();
		}
	}

}
