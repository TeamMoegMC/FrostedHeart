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

package com.teammoeg.frostedheart.mixin.ftb;


import dev.ftb.mods.ftbchunks.data.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FTBChunksTeamData.class)
public class ChunkDataMixin {
    @Shadow(remap = false)
    public ClaimedChunkManager manager;
    @Shadow(remap = false)
    private Team team;

    public ChunkDataMixin() {
    }

    @Inject(at = @At("HEAD"), method = "deserializeNBT", remap = false, cancellable = false)
    public void fh$deserializeNBT(CompoundNBT tag, CallbackInfo cbi) {
        if (team instanceof PlayerTeam) {
            PlayerTeam pt = (PlayerTeam) team;
            if (pt.actualTeam != pt)
                cbi.cancel();
        }
    }


}
