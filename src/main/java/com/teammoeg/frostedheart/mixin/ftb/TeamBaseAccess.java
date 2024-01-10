package com.teammoeg.frostedheart.mixin.ftb;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import dev.ftb.mods.ftbteams.data.TeamBase;
import net.minecraft.nbt.CompoundNBT;
@Mixin(TeamBase.class)
public class TeamBaseAccess {
	@Shadow(remap=false)
	CompoundNBT extraData;
}
