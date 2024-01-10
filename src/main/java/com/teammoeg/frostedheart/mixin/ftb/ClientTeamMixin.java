package com.teammoeg.frostedheart.mixin.ftb;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.TeamBase;
import dev.ftb.mods.ftbteams.data.TeamType;
import net.minecraft.network.PacketBuffer;

@Mixin(ClientTeam.class)
public abstract class ClientTeamMixin extends TeamBase{
	@Shadow(remap=false)
	TeamType type;

	public void write2(PacketBuffer buffer, long now) {
		buffer.writeUniqueId(getId());
		buffer.writeByte(type.ordinal());
		properties.write(buffer);
		buffer.writeVarInt(0);
		buffer.writeCompoundTag(((TeamBaseAccess)(Object)(this)).extraData);
	}
}
